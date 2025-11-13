package handler.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import java.nio.ByteBuffer;

public class AudioPlayerHandler implements AudioSendHandler {
    private final AudioPlayer player;
    private final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
    private final byte[] data = new byte[1024 * 1024];

    public AudioPlayerHandler(AudioPlayer player) { this.player = player; }

    @Override
    public boolean canProvide() {
        var frame = player.provide();
        if (frame == null) {
            return false;
        }
        byte[] bytes = frame.getData();
        buffer.clear();
        buffer.put(bytes);
        buffer.flip();
        return true;
    }

    @Override
    public ByteBuffer provide20MsAudio() {
        return buffer;
    }

    @Override
    public boolean isOpus() {
        return true;
    }
}

