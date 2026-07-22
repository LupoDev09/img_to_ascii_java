package me.lupo;

import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import java.nio.ShortBuffer;

public class AudioPlayer {

    private SourceDataLine line;

    boolean playing = false;

    public boolean isPlaying() {
        return playing;
    }

    public void start(@NotNull Frame frame) throws LineUnavailableException {

        AudioFormat format = new AudioFormat(
                frame.sampleRate,
                16,
                frame.audioChannels,
                true,
                false
        );

        line = AudioSystem.getSourceDataLine(format);
        line.open(format);
        line.start();
        playing = true;
    }


    public void play(@NotNull Frame frame) {
        System.out.println(frame.samples[0].getClass());
        ShortBuffer samples = (ShortBuffer) frame.samples[0];

        byte[] audioBytes = new byte[samples.remaining() * 2];

        int index = 0;

        while(samples.hasRemaining()) {
            short value = samples.get();

            audioBytes[index++] = (byte) (value & 0xff);
            audioBytes[index++] = (byte) ((value >> 8) & 0xff);
        }

        line.write(audioBytes, 0, audioBytes.length);
    }


    public void stop() {
        if(line != null) {
            line.drain();
            line.close();
        }
        playing = false;
    }
}