package handler;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import handler.music.MusicManager;
import handler.music.ServerMusicManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import util.EntrySongConfig;
import util.EntrySongRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.YtdlpResolver;

/**
 * 특정 유저가 보이스 채널에 입장했을 때
 * - EntrySongRegistry 에 등록된 입장곡이 있으면 그걸 사용
 *   (로컬 파일 or URL 모두 가능)
 * - 없으면 서버의 uploads/{username}#{discriminator}.mp3 파일이 있으면 그걸 사용
 */
public class EntrySongHandler extends ListenerAdapter {

    // 레지스트리 설정이 없거나, username#discriminator.mp3 를 쓸 때 기본 재생 길이 (초)
    private static final int DEFAULT_ENTRY_DURATION_SEC = 7;

    // 파일명/키에 쓸 수 있도록 간단히 정제
    private String sanitize(String s) {
        return s.replaceAll("[^0-9A-Za-z가-힣_.\\-]", "_");
    }

    // ─────────────────────────────
    // Slash Command
    //  - /setentrysong  url:링크   → 입장곡 소스를 URL로 설정
    //  - /setentrytime  start/duration → 현재 입장곡의 재생 구간 수정
    // ─────────────────────────────
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {

            // 🔹 유튜브 링크(또는 다른 URL)로 입장곡 설정
            case "setentrysong" -> {
                User user = event.getUser();
                String url = event.getOption("url").getAsString();

                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    event.reply("❌ 유효한 URL을 입력해주세요. (http/https)")
                            .setEphemeral(true).queue();
                    return;
                }

                String username = sanitize(user.getName());
                String discriminator = sanitize(user.getDiscriminator());

                // 기본값: 0 ~ DEFAULT_ENTRY_DURATION_SEC 초
                EntrySongRegistry.setSong(username, discriminator, url, 0, DEFAULT_ENTRY_DURATION_SEC);

                event.reply("🎵 입장곡을 해당 링크로 설정했어요!\n" +
                                "URL: `" + url + "`\n" +
                                "구간: 0초 ~ " + DEFAULT_ENTRY_DURATION_SEC + "초\n" +
                                "원하면 `/setentrytime` 명령어로 다시 구간을 설정할 수 있어요.")
                        .queue();
            }

            // 🔹 재생 구간 설정 (로컬 파일/URL 둘 다 지원)
            case "setentrytime" -> {
                User user = event.getUser();

                int start = event.getOption("start").getAsInt();
                int duration = event.getOption("duration").getAsInt();

                if (start < 0 || duration <= 0) {
                    event.reply("⚠️ start ≥ 0, duration ≥ 1 이어야 합니다.")
                            .setEphemeral(true).queue();
                    return;
                }

                String username = sanitize(user.getName());
                String discriminator = sanitize(user.getDiscriminator());

                EntrySongConfig cfg = EntrySongRegistry.getSong(username, discriminator);

                String sourceValue; // 파일명 또는 URL

                if (cfg != null) {
                    // 이미 등록된 입장곡 있을 때 → 그 소스 유지하고 시간만 수정
                    sourceValue = cfg.fileName;
                    System.out.println("[EntrySong] /setentrytime use registry source=" + sourceValue);
                } else {
                    // 레지스트리에 없으면, 업로드된 username#disc.mp3 기반으로 세팅
                    String fileName = username + "#" + discriminator + ".mp3";
                    Path candidatePath = Path.of("uploads", fileName);
                    System.out.println("[EntrySong] /setentrytime fallback candidate path=" + candidatePath.toAbsolutePath());

                    if (!Files.exists(candidatePath)) {
                        event.reply("❌ 설정된 입장곡이 없고, 서버에도 `" + fileName + "` 파일이 없습니다.\n" +
                                        "먼저 mp3 파일을 업로드하거나 `/setentrysong` 으로 URL을 설정해주세요.")
                                .setEphemeral(true).queue();
                        return;
                    }

                    sourceValue = fileName;
                    System.out.println("[EntrySong] /setentrytime use fallback fileName=" + sourceValue);
                }

                // 최종 설정 저장 (소스는 그대로, 구간만 변경)
                EntrySongRegistry.setSong(username, discriminator, sourceValue, start, duration);

                event.reply("⏱️ 입장곡 재생 구간을 " +
                                start + "초 ~ " + (start + duration) + "초로 설정했습니다.\n" +
                                "(소스: `" + sourceValue + "`)")
                        .queue();
            }

            default -> {}
        }
    }

    // ─────────────────────────────
    // Voice Update: 유저가 채널에 들어왔을 때
    // ─────────────────────────────
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {

        // 디버그 로그
        System.out.println("[EntrySong] VoiceUpdate fired: " +
                event.getMember().getUser().getAsTag() +
                " joined=" + event.getChannelJoined() +
                ", left=" + event.getChannelLeft());

        // 봇이면 무시
        if (event.getMember().getUser().isBot()) {
            System.out.println("[EntrySong] skip: bot");
            return;
        }

        // 채널에 "입장"한 이벤트만 처리
        if (event.getChannelJoined() == null) {
            System.out.println("[EntrySong] skip: no channelJoined");
            return;
        }

        User user = event.getMember().getUser();

        // username#disc 기반 키
        String username = sanitize(user.getName());
        String discriminator = sanitize(user.getDiscriminator());

        // 1) 우선 EntrySongRegistry 에 등록된 입장곡 있는지 확인
        EntrySongConfig cfg = EntrySongRegistry.getSong(username, discriminator);

        // source: 로컬 파일 경로 or URL
        String source = null;
        boolean isUrl = false;
        int startSec = 0;
        int durationSec = DEFAULT_ENTRY_DURATION_SEC;

        if (cfg != null) {
            String src = cfg.fileName;
            System.out.println("[EntrySong] registry config found: " + src +
                    ", startSec=" + cfg.startSec + ", durationSec=" + cfg.durationSec);

            if (src.startsWith("http://") || src.startsWith("https://")) {
                // 🔹 URL 모드
                isUrl = true;
                source = src;
                startSec = cfg.startSec;
                durationSec = cfg.durationSec;
                System.out.println("[EntrySong] using URL entry song: " + source);
            } else {
                // 🔹 로컬 파일 모드
                Path candidatePath = Path.of("uploads", src);
                System.out.println("[EntrySong] registry file candidate = " + candidatePath.toAbsolutePath());

                if (Files.exists(candidatePath)) {
                    source = candidatePath.toString();
                    startSec = cfg.startSec;
                    durationSec = cfg.durationSec;
                    System.out.println("[EntrySong] using registry file: " + candidatePath.toAbsolutePath());
                } else {
                    System.out.println("[EntrySong] registry file not found, fallback to username#disc.mp3");
                    source = null; // fallback 으로 넘어감
                }
            }
        } else {
            System.out.println("[EntrySong] no registry config for tag=" +
                    username + "#" + discriminator + " → fallback to username#disc.mp3");
        }

        // 2) 레지스트리 기반이 없거나, 파일이 없으면 fallback:
        //    uploads/{username}#{discriminator}.mp3 를 입장곡으로 사용
        if (source == null) {
            String fileName = username + "#" + discriminator + ".mp3";
            Path candidatePath = Path.of("uploads", fileName);
            System.out.println("[EntrySong] fallback candidate path = " + candidatePath.toAbsolutePath());

            if (!Files.exists(candidatePath)) {
                System.out.println("[EntrySong] no fallback file for tag=" + username + "#" + discriminator + ", stop.");
                return;
            }

            isUrl = false;
            source = candidatePath.toString();
            startSec = 0;
            durationSec = DEFAULT_ENTRY_DURATION_SEC;

            System.out.println("[EntrySong] using fallback entry song tag=" +
                    username + "#" + discriminator +
                    ", path=" + candidatePath.toAbsolutePath());
        }

        var guild = event.getGuild();
        ServerMusicManager music = MusicManager.get().of(guild);

        var joinedChannel = event.getChannelJoined();
        var audioManager = guild.getAudioManager();

        System.out.println("[EntrySong] audioManager connected? " + audioManager.isConnected());

        // 🔹 sendingHandler 강제 세팅
        if (audioManager.getSendingHandler() == null) {
            System.out.println("[EntrySong] no sendingHandler, setting from ServerMusicManager.");
            audioManager.setSendingHandler(music.getSendHandler());
        }

        audioManager.openAudioConnection(joinedChannel);

        // ─────────────────────────────
        // 기존 재생중인 트랙 백업
        // ─────────────────────────────
        AtomicReference<AudioTrack> originalCloneRef = new AtomicReference<>(null);

        AudioTrack original = music.player.getPlayingTrack();
        if (original != null) {
            System.out.println("[EntrySong] currently playing track exists, backing up.");
            AudioTrack clone = original.makeClone();
            clone.setPosition(original.getPosition());
            originalCloneRef.set(clone);
        } else {
            System.out.println("[EntrySong] no track currently playing.");
        }

        // ─────────────────────────────
        // 입장곡 로드 + 재생
        // ─────────────────────────────
        final String finalSource = source;
        final int finalStartSec = startSec;
        final int finalDurationSec = durationSec;
        final boolean finalIsUrl = isUrl;

        if (finalIsUrl) {
            // 🔹 URL이면 /play처럼 yt-dlp로 실제 스트림 URL을 먼저 구한다
            System.out.println("[EntrySong] resolving URL via yt-dlp: " + finalSource);

            YtdlpResolver.resolveAudioUrl(finalSource).whenComplete((streamUrl, err) -> {
                if (err != null || streamUrl == null) {
                    System.out.println("[EntrySong] yt-dlp resolve failed for " + finalSource);
                    if (err != null) err.printStackTrace();
                    return;
                }

                System.out.println("[EntrySong] yt-dlp resolved URL: " + streamUrl +
                        " (start=" + finalStartSec + ", duration=" + finalDurationSec + ")");

                MusicManager.get().playerManager().loadItemOrdered(
                        music,
                        streamUrl,
                        createEntrySongHandler(music, originalCloneRef, streamUrl, finalStartSec, finalDurationSec)
                );
            });

        } else {
            // 🔹 로컬 파일이면 그대로 lavaplayer에 넘긴다
            System.out.println("[EntrySong] loading file: " + Path.of(finalSource).toAbsolutePath() +
                    " (start=" + finalStartSec + ", duration=" + finalDurationSec + ")");

            MusicManager.get().playerManager().loadItemOrdered(
                    music,
                    finalSource,
                    createEntrySongHandler(music, originalCloneRef, finalSource, finalStartSec, finalDurationSec)
            );
        }
    }

    private AudioLoadResultHandler createEntrySongHandler(
            ServerMusicManager music,
            AtomicReference<AudioTrack> originalCloneRef,
            String playSource,   // 실제로 lavaplayer에 넘길 URL 또는 파일 경로
            int startSec,
            int durationSec
    ) {
        final String finalPlaySource = playSource;
        final int finalStartSec = startSec;
        final int finalDurationSec = durationSec;

        return new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack entryTrack) {
                System.out.println("[EntrySong] trackLoaded OK: " + finalPlaySource +
                        ", trackDuration=" + entryTrack.getDuration() + "ms");

                long startPosMs = finalStartSec * 1000L;
                if (startPosMs >= entryTrack.getDuration()) {
                    System.out.println("[EntrySong] start position is beyond track duration, skip entry song.");
                    AudioTrack restore = originalCloneRef.get();
                    if (restore != null) {
                        music.player.startTrack(restore, false);
                    }
                    return;
                }
                entryTrack.setPosition(startPosMs);

                boolean started = music.player.startTrack(entryTrack, false);
                System.out.println("[EntrySong] startTrack(entry) returned = " + started);

                if (!started) {
                    System.out.println("[EntrySong] entry track did NOT start. Keep original track.");
                    AudioTrack restore = originalCloneRef.get();
                    if (restore != null) {
                        music.player.startTrack(restore, false);
                    }
                    return;
                }

                System.out.println("[EntrySong] entry track started.");

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                scheduler.schedule(() -> {
                    try {
                        System.out.println("[EntrySong] entry duration reached, restoring original track.");
                        music.player.stopTrack();

                        AudioTrack restore = originalCloneRef.get();
                        if (restore != null) {
                            music.player.startTrack(restore, false);
                            System.out.println("[EntrySong] original track restored.");
                        } else {
                            System.out.println("[EntrySong] no original track to restore.");
                        }
                    } finally {
                        scheduler.shutdown();
                    }
                }, finalDurationSec, TimeUnit.SECONDS);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println("[EntrySong] playlistLoaded(예상치X): " + playlist.getName());
            }

            @Override
            public void noMatches() {
                System.out.println("[EntrySong] noMatches for source = " + finalPlaySource);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println("[EntrySong] loadFailed for source = " + finalPlaySource);
                e.printStackTrace();
            }
        };
    }
}