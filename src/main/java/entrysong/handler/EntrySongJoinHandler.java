package entrysong.handler;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import entrysong.service.EntrySongService;
import entrysong.service.EntrySongSource;
import music.service.MusicManager;
import music.service.ServerMusicManager;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * 특정 유저가 보이스 채널에 입장했을 때
 * 그 서버에 등록된 그 유저의 입장곡(로컬 파일 or URL)을 재생하는 핸들러입니다.
 */
public class EntrySongJoinHandler extends ListenerAdapter {

    private final EntrySongService entrySongService;
    private final MusicManager musicManager;

    public EntrySongJoinHandler(EntrySongService entrySongService, MusicManager musicManager) {
        this.entrySongService = entrySongService;
        this.musicManager = musicManager;
    }

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

        ServerMusicManager music = musicManager.of(event.getGuild());
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
