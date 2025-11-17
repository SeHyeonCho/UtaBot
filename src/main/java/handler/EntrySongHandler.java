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
import util.EntrySongRegistry;
import util.EntrySongConfig; // 레지스트리에서 쓰는 DTO (있다면)
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 특정 유저가 보이스 채널에 입장했을 때
 * - EntrySongRegistry 에 등록된 입장곡이 있으면 그걸 사용
 * - 없으면 서버의 uploads/{username}#{discriminator}.mp3 파일이 있으면 그걸 사용
 */
public class EntrySongHandler extends ListenerAdapter {

    // 레지스트리에 설정이 없고, username#discriminator.mp3 를 쓸 때 기본 재생 길이 (초)
    private static final int DEFAULT_ENTRY_DURATION_SEC = 7;

    // 파일명에 쓸 수 있도록 간단히 정제
    // (파일 저장할 때 사용한 sanitize 규칙과 맞춰주기)
    private String sanitize(String s) {
        return s.replaceAll("[^0-9A-Za-z가-힣_.\\-]", "_");
    }

    // ─────────────────────────────
    // Slash Command: /setentrytime
    // ─────────────────────────────
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "setentrytime" -> {
                User user = event.getUser();

                int start = event.getOption("start").getAsInt();
                int duration = event.getOption("duration").getAsInt();

                if (start < 0 || duration <= 0) {
                    event.reply("⚠️ start ≥ 0, duration ≥ 1 이어야 합니다.")
                            .setEphemeral(true).queue();
                    return;
                }

                // username#disc 기반 키
                String username = sanitize(user.getName());
                String discriminator = sanitize(user.getDiscriminator());

                // 1) 먼저 레지스트리에 설정된 입장곡 있는지 확인
                EntrySongConfig cfg = EntrySongRegistry.getSong(username, discriminator);

                String fileNameToUse = null;

                if (cfg != null) {
                    // 레지스트리 기반 → 그 파일명 그대로 사용
                    fileNameToUse = cfg.fileName;
                    System.out.println("[EntrySong] /setentrytime use registry fileName=" + fileNameToUse);
                } else {
                    // 2) 레지스트리에 없으면, 업로드된 username#disc.mp3 가 있는지 확인해서 그걸 사용
                    String fileName = username + "#" + discriminator + ".mp3";

                    Path candidatePath = Path.of("uploads", fileName);
                    System.out.println("[EntrySong] /setentrytime fallback candidate path=" + candidatePath.toAbsolutePath());

                    if (!Files.exists(candidatePath)) {
                        event.reply("❌ 설정된 입장곡이 없고, 서버에도 `" + fileName + "` 파일이 없습니다.\n" +
                                        "먼저 mp3 파일을 업로드하거나 서버의 `uploads/` 폴더에 `" + fileName + "` 를 넣어주세요.")
                                .setEphemeral(true).queue();
                        return;
                    }

                    fileNameToUse = fileName;
                    System.out.println("[EntrySong] /setentrytime use fallback fileName=" + fileNameToUse);
                }

                // 3) 최종적으로 결정된 fileName에 대해 시간 설정 저장 (username 기반)
                EntrySongRegistry.setSong(username, discriminator, fileNameToUse, start, duration);

                event.reply("⏱️ 입장곡 재생 구간을 " +
                                start + "초 ~ " + (start + duration) + "초로 설정했습니다.\n" +
                                "(파일: `" + fileNameToUse + "`)")
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
        String path = null;
        int startSec = 0;
        int durationSec = DEFAULT_ENTRY_DURATION_SEC;

        if (cfg != null) {
            String candidate = "uploads/" + cfg.fileName;
            System.out.println("[EntrySong] registry config found: fileName=" + cfg.fileName +
                    ", startSec=" + cfg.startSec + ", durationSec=" + cfg.durationSec +
                    ", candidatePath=" + Path.of(candidate).toAbsolutePath());

            if (Files.exists(Path.of(candidate))) {
                path = candidate;
                startSec = cfg.startSec;
                durationSec = cfg.durationSec;
                System.out.println("[EntrySong] using registry file: " + Path.of(path).toAbsolutePath());
            } else {
                System.out.println("[EntrySong] registry file not found, fallback to username#disc.mp3");
                path = null;
            }
        } else {
            System.out.println("[EntrySong] no registry config for tag=" +
                    username + "#" + discriminator + " → fallback to username#disc.mp3");
        }

        // 2) 레지스트리 기반이 없거나, 파일이 없으면 fallback:
        //    uploads/{username}#{discriminator}.mp3 를 입장곡으로 사용
        if (path == null) {
            String fileName = username + "#" + discriminator + ".mp3";

            Path candidatePath = Path.of("uploads", fileName);
            System.out.println("[EntrySong] fallback candidate path = " + candidatePath.toAbsolutePath());

            if (!Files.exists(candidatePath)) {
                System.out.println("[EntrySong] no fallback file for tag=" + username + "#" + discriminator + ", stop.");
                return;
            }

            path = candidatePath.toString();
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

        // ★ sendingHandler 강제 세팅
        if (audioManager.getSendingHandler() == null) {
            System.out.println("[EntrySong] no sendingHandler, setting from ServerMusicManager.");
            audioManager.setSendingHandler(music.getSendHandler());
        }

        // (채널 이동은 그대로: 이미 연결돼 있으면 새 채널로는 안 옮김)
        if (!audioManager.isConnected()) {
            System.out.println("[EntrySong] opening audio connection to " + joinedChannel.getName());
            audioManager.openAudioConnection(joinedChannel);
        }

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
        final String finalPath = path;
        final int finalStartSec = startSec;
        final int finalDurationSec = durationSec;

        System.out.println("[EntrySong] loading track: " + Path.of(finalPath).toAbsolutePath() +
                " (start=" + finalStartSec + ", duration=" + finalDurationSec + ")");

        MusicManager.get().playerManager().loadItemOrdered(
                music,
                finalPath,
                new AudioLoadResultHandler() {

                    @Override
                    public void trackLoaded(AudioTrack entryTrack) {
                        System.out.println("[EntrySong] trackLoaded OK: " + finalPath +
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
                        System.out.println("[EntrySong] noMatches for path = " + finalPath);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        System.out.println("[EntrySong] loadFailed for path = " + finalPath);
                        e.printStackTrace();
                    }
                }
        );
    }
}