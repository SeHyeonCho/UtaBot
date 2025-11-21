package entrysong.handler;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import music.service.MusicManager;
import music.service.ServerMusicManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import entrysong.repository.EntrySongConfig;
import entrysong.repository.EntrySongRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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
            case "setentrysong" -> handleSetEntrySong(event);
            case "setentrytime" -> handleSetEntryTime(event);
            default -> {}
        }
    }

    private void handleSetEntrySong(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        String url = event.getOption("url").getAsString();

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            event.reply("❌ 유효한 URL을 입력해주세요. (http/https)")
                    .setEphemeral(true).queue();
            return;
        }

        String username = sanitize(user.getName());
        String discriminator = sanitize(user.getDiscriminator());

        EntrySongRegistry.setSong(username, discriminator, url, 0, DEFAULT_ENTRY_DURATION_SEC);

        event.reply("🎵 입장곡을 해당 링크로 설정했어요!\n" +
                        "URL: `" + url + "`\n" +
                        "구간: 0초 ~ " + DEFAULT_ENTRY_DURATION_SEC + "초\n" +
                        "원하면 `/setentrytime` 명령어로 다시 구간을 설정할 수 있어요.")
                .queue();
    }

    private void handleSetEntryTime(SlashCommandInteractionEvent event) {
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
        String sourceValue = findSourceForTimeSetting(username, discriminator, event);

        if (sourceValue == null) {
            return; // 에러 메시지는 findSourceForTimeSetting에서 이미 전송됨
        }

        EntrySongRegistry.setSong(username, discriminator, sourceValue, start, duration);

        event.reply("⏱️ 입장곡 재생 구간을 " +
                        start + "초 ~ " + (start + duration) + "초로 설정했습니다.\n" +
                        "(소스: `" + sourceValue + "`)")
                .queue();
    }

    private String findSourceForTimeSetting(String username, String discriminator, 
                                            SlashCommandInteractionEvent event) {
        EntrySongConfig cfg = EntrySongRegistry.getSong(username, discriminator);

        if (cfg != null) {
            System.out.println("[EntrySong] /setentrytime use registry source=" + cfg.fileName);
            return cfg.fileName;
        }

        String fileName = username + "#" + discriminator + ".mp3";
        Path candidatePath = Path.of("uploads", fileName);
        System.out.println("[EntrySong] /setentrytime fallback candidate path=" + candidatePath.toAbsolutePath());

        if (!Files.exists(candidatePath)) {
            event.reply("❌ 설정된 입장곡이 없고, 서버에도 `" + fileName + "` 파일이 없습니다.\n" +
                            "먼저 mp3 파일을 업로드하거나 `/setentrysong` 으로 URL을 설정해주세요.")
                    .setEphemeral(true).queue();
            return null;
        }

        System.out.println("[EntrySong] /setentrytime use fallback fileName=" + fileName);
        return fileName;
    }

    // ─────────────────────────────
    // Voice Update: 유저가 채널에 들어왔을 때
    // ─────────────────────────────
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (!shouldProcessVoiceUpdate(event)) {
            return;
        }

        User user = event.getMember().getUser();
        String username = sanitize(user.getName());
        String discriminator = sanitize(user.getDiscriminator());

        EntrySongSource sourceInfo = findEntrySongSource(username, discriminator);
        if (sourceInfo == null) {
            return;
        }

        Guild guild = event.getGuild();
        ServerMusicManager music = MusicManager.get().of(guild);
        prepareAudioConnection(guild, music, event.getChannelJoined());

        AtomicReference<AudioTrack> originalCloneRef = backupCurrentTrack(music);
        loadAndPlayEntrySong(music, originalCloneRef, sourceInfo);
    }

    private boolean shouldProcessVoiceUpdate(GuildVoiceUpdateEvent event) {
        System.out.println("[EntrySong] VoiceUpdate fired: " +
                event.getMember().getUser().getAsTag() +
                " joined=" + event.getChannelJoined() +
                ", left=" + event.getChannelLeft());

        if (event.getMember().getUser().isBot()) {
            System.out.println("[EntrySong] skip: bot");
            return false;
        }

        if (event.getChannelJoined() == null) {
            System.out.println("[EntrySong] skip: no channelJoined");
            return false;
        }

        return true;
    }

    private static class EntrySongSource {
        final String source;
        final boolean isUrl;
        final int startSec;
        final int durationSec;

        EntrySongSource(String source, boolean isUrl, int startSec, int durationSec) {
            this.source = source;
            this.isUrl = isUrl;
            this.startSec = startSec;
            this.durationSec = durationSec;
        }
    }

    private EntrySongSource findEntrySongSource(String username, String discriminator) {
        EntrySongConfig cfg = EntrySongRegistry.getSong(username, discriminator);

        if (cfg != null) {
            return findSourceFromRegistry(cfg, username, discriminator);
        }

        return findFallbackSource(username, discriminator);
    }

    private EntrySongSource findSourceFromRegistry(EntrySongConfig cfg, String username, String discriminator) {
        String src = cfg.fileName;
        System.out.println("[EntrySong] registry config found: " + src +
                ", startSec=" + cfg.startSec + ", durationSec=" + cfg.durationSec);

        if (src.startsWith("http://") || src.startsWith("https://")) {
            System.out.println("[EntrySong] using URL entry song: " + src);
            return new EntrySongSource(src, true, cfg.startSec, cfg.durationSec);
        }

        Path candidatePath = Path.of("uploads", src);
        System.out.println("[EntrySong] registry file candidate = " + candidatePath.toAbsolutePath());

        if (Files.exists(candidatePath)) {
            System.out.println("[EntrySong] using registry file: " + candidatePath.toAbsolutePath());
            return new EntrySongSource(candidatePath.toString(), false, cfg.startSec, cfg.durationSec);
        }

        System.out.println("[EntrySong] registry file not found, fallback to username#disc.mp3");
        return findFallbackSource(username, discriminator);
    }

    private EntrySongSource findFallbackSource(String username, String discriminator) {
        String fileName = username + "#" + discriminator + ".mp3";
        Path candidatePath = Path.of("uploads", fileName);
        System.out.println("[EntrySong] fallback candidate path = " + candidatePath.toAbsolutePath());

        if (!Files.exists(candidatePath)) {
            System.out.println("[EntrySong] no fallback file for tag=" + username + "#" + discriminator + ", stop.");
            return null;
        }

        System.out.println("[EntrySong] using fallback entry song tag=" +
                username + "#" + discriminator +
                ", path=" + candidatePath.toAbsolutePath());

        return new EntrySongSource(candidatePath.toString(), false, 0, DEFAULT_ENTRY_DURATION_SEC);
    }

    private void prepareAudioConnection(Guild guild, 
                                       ServerMusicManager music,
                                       AudioChannel joinedChannel) {
        AudioManager audioManager = guild.getAudioManager();
        System.out.println("[EntrySong] audioManager connected? " + audioManager.isConnected());

        if (audioManager.getSendingHandler() == null) {
            System.out.println("[EntrySong] no sendingHandler, setting from ServerMusicManager.");
            audioManager.setSendingHandler(music.getSendHandler());
        }

        audioManager.openAudioConnection(joinedChannel);
    }

    private AtomicReference<AudioTrack> backupCurrentTrack(ServerMusicManager music) {
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

        return originalCloneRef;
    }

    private void loadAndPlayEntrySong(ServerMusicManager music,
                                     AtomicReference<AudioTrack> originalCloneRef,
                                     EntrySongSource sourceInfo) {
        if (sourceInfo.isUrl) {
            loadEntrySongFromUrl(music, originalCloneRef, sourceInfo);
        } else {
            loadEntrySongFromFile(music, originalCloneRef, sourceInfo);
        }
    }

    private void loadEntrySongFromUrl(ServerMusicManager music,
                                     AtomicReference<AudioTrack> originalCloneRef,
                                     EntrySongSource sourceInfo) {
        System.out.println("[EntrySong] loading URL via Lavaplayer: " + sourceInfo.source +
                " (start=" + sourceInfo.startSec + ", duration=" + sourceInfo.durationSec + ")");

        MusicManager.get().playerManager().loadItemOrdered(
                music,
                sourceInfo.source,
                createEntrySongHandler(music, originalCloneRef, sourceInfo.source, 
                        sourceInfo.startSec, sourceInfo.durationSec)
        );
    }

    private void loadEntrySongFromFile(ServerMusicManager music,
                                      AtomicReference<AudioTrack> originalCloneRef,
                                      EntrySongSource sourceInfo) {
        System.out.println("[EntrySong] loading file: " + Path.of(sourceInfo.source).toAbsolutePath() +
                " (start=" + sourceInfo.startSec + ", duration=" + sourceInfo.durationSec + ")");

        MusicManager.get().playerManager().loadItemOrdered(
                music,
                sourceInfo.source,
                createEntrySongHandler(music, originalCloneRef, sourceInfo.source,
                        sourceInfo.startSec, sourceInfo.durationSec)
        );
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