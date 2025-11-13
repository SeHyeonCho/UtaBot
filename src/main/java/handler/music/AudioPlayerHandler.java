package handler.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.nio.ByteBuffer;


/**
 * Lavaplayer의 AudioPlayer가 출력한 Opus 형식의 오디오를
 * Discord 음성 채널로 전달해주는 클래스 입니다.
 */
public class AudioPlayerHandler implements AudioSendHandler {
    private final AudioPlayer player;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);

    public AudioPlayerHandler(AudioPlayer player) {
        this.player = player;
    }

    /**
     * JDA로 보낼 오디오 확인 메소드 입니다.
     *
     * @return 프레임이 존재하면 true, 없으면 false
     */
    @Override
    public boolean canProvide() {
        AudioFrame frame = player.provide();
        if (frame == null) {
            return false;
        }
        byte[] bytes = frame.getData();
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();
        return true;
    }

    /**
     * JDA로 오디오 보낼때 호출되는 메소드 입니다.
     *
     * @return Opus 형식의 20ms 오디오 프레임
     */
    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer;
    }

    /**
     * Opus 형식인지 확인 하는 메소드 입니다.
     *
     * @return 거의 항상 true
     */
    @Override
    public boolean isOpus() {
        return true;
    }
}

