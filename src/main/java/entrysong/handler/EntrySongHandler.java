package entrysong.handler;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import common.util.CommandUtils;
import entrysong.model.EntrySongConfig;
import entrysong.service.EntrySongService;
import entrysong.service.EntrySongSource;
import music.service.MusicManager;
import music.service.ServerMusicManager;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 입장곡 명령어와 입장 이벤트를 처리하는 핸들러입니다.
 *
 * 특정 유저가 보이스 채널에 입장했을 때
 * 그 서버에 등록된 그 유저의 입장곡(로컬 파일 or URL)을 재생합니다.
 */
public class EntrySongHandler extends ListenerAdapter {

    private final EntrySongService entrySongService;

    public EntrySongHandler(EntrySongService entrySongService) {
        this.entrySongService = entrySongService;
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
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        String url = event.getOption("url").getAsString();

        if (!EntrySongConfig.isUrl(url)) {
            event.reply("❌ 유효한 URL을 입력해주세요. (http/https)")
                    .setEphemeral(true).queue();
            return;
        }

        EntrySongConfig config = entrySongService.setUrl(
                event.getGuild().getIdLong(), event.getUser().getIdLong(), url);

        event.reply("🎵 입장곡을 해당 링크로 설정했어요!\n" +
                        "URL: `" + url + "`\n" +
                        "구간: " + config.startSec + "초 ~ " + (config.startSec + config.durationSec) + "초\n" +
                        "원하면 `/setentrytime` 명령어로 다시 구간을 설정할 수 있어요.")
                .queue();
    }

    private void handleSetEntryTime(SlashCommandInteractionEvent event) {
        if (!CommandUtils.requireGuild(event)) {
            return;
        }

        int start = event.getOption("start").getAsInt();
        int duration = event.getOption("duration").getAsInt();

        if (start < 0 || duration <= 0) {
            event.reply("⚠️ start ≥ 0, duration ≥ 1 이어야 합니다.")
                    .setEphemeral(true).queue();
            return;
        }

        EntrySongConfig config = entrySongService.setTime(
                event.getGuild().getIdLong(), event.getUser().getIdLong(), start, duration);

        if (config == null) {
            event.reply("❌ 이 서버에 설정된 입장곡이 없습니다.\n" +
                            "먼저 mp3 파일을 업로드하거나 `/setentrysong` 으로 URL을 설정해주세요.")
                    .setEphemeral(true).queue();
            return;
        }

        String sourceText = config.isUrl() ? "`" + config.source + "`" : "업로드한 mp3";
        event.reply("⏱️ 입장곡 재생 구간을 " +
                        start + "초 ~ " + (start + duration) + "초로 설정했습니다.\n" +
                        "(소스: " + sourceText + ")")
                .queue();
    }

    // ─────────────────────────────
    // Voice Update: 유저가 채널에 들어왔을 때
    // ─────────────────────────────
    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (!shouldProcessVoiceUpdate(event)) {
            return;
        }

        Member member = event.getMember();
        EntrySongSource source = entrySongService.findPlayableSource(
                event.getGuild().getIdLong(), member.getIdLong());
        if (source == null) {
            return;
        }

        ServerMusicManager music = MusicManager.get().of(event.getGuild());
        if (!moveToJoinedChannel(music, event.getChannelJoined())) {
            return;
        }

        loadAndPlayEntrySong(music, source);
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

    /**
     * 입장한 채널로 봇을 이동시킵니다.
     *
     * 봇이 다른 채널에서 음악을 재생 중이면 듣고 있는 사람들을 위해 이동하지 않습니다.
     *
     * @return 입장곡을 재생해도 되면 true
     */
    private boolean moveToJoinedChannel(ServerMusicManager music, AudioChannel joinedChannel) {
        AudioChannel current = music.getConnectedChannel();
        boolean sameChannel = current != null && current.getIdLong() == joinedChannel.getIdLong();

        if (sameChannel) {
            return true;
        }

        if (current != null && music.isPlaying()) {
            System.out.println("[EntrySong] skip: bot is playing in another channel (" + current.getName() + ")");
            return false;
        }

        music.connect(joinedChannel);
        return true;
    }

    private void loadAndPlayEntrySong(ServerMusicManager music, EntrySongSource source) {
        System.out.println("[EntrySong] loading: " + source.identifier +
                " (start=" + source.startSec + ", duration=" + source.durationSec + ")");

        music.load(source.identifier, createEntrySongHandler(music, source));
    }

    private AudioLoadResultHandler createEntrySongHandler(ServerMusicManager music,
                                                          EntrySongSource source) {
        return new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack entryTrack) {
                System.out.println("[EntrySong] trackLoaded OK: " + source.identifier +
                        ", trackDuration=" + entryTrack.getDuration() + "ms");

                long startPosMs = source.startSec * 1000L;
                if (startPosMs >= entryTrack.getDuration()) {
                    System.out.println("[EntrySong] start position is beyond track duration, skip entry song.");
                    return;
                }
                entryTrack.setPosition(startPosMs);

                music.playEntrySong(entryTrack, source.durationSec * 1000L);
                System.out.println("[EntrySong] entry track started.");
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                System.out.println("[EntrySong] playlistLoaded(예상치X): " + playlist.getName());
            }

            @Override
            public void noMatches() {
                System.out.println("[EntrySong] noMatches for source = " + source.identifier);
            }

            @Override
            public void loadFailed(FriendlyException e) {
                System.out.println("[EntrySong] loadFailed for source = " + source.identifier);
                e.printStackTrace();
            }
        };
    }
}
