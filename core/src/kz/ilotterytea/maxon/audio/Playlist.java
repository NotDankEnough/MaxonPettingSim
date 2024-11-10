package kz.ilotterytea.maxon.audio;

import com.badlogic.gdx.audio.Music;
import kz.ilotterytea.maxon.utils.math.Math;

import java.util.Arrays;
import java.util.List;

public class Playlist {
    private List<Music> music;
    private Music playingNow;
    private int index;

    private boolean shuffleMode;
    private float volume;

    public Playlist(Music... music) {
        this.music = Arrays.asList(music);

        this.playingNow = this.music.get(0);
        this.index = 0;
        this.shuffleMode = false;
        this.volume = 1f;

        this.playingNow.setVolume(this.volume);
    }

    public void next() {
        if (playingNow.isPlaying()) playingNow.stop();

        if (shuffleMode) {
            index = Math.getRandomNumber(0, music.size() - 1);
            playingNow = music.get(index);
        } else {
            index++;
            if (index > music.size() - 1) index = 0;

            playingNow = music.get(index);
        }

        playingNow.setVolume(volume);
        playingNow.play();
    }

    public void setVolume(float volume) {
        this.volume = volume;
        playingNow.setVolume(volume);
    }

    public void setShuffleMode(boolean shuffleMode) {
        this.shuffleMode = shuffleMode;
    }

    public Music getPlayingNow() {
        return playingNow;
    }
}
