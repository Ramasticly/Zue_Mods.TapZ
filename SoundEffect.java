package com.namcobandaigames.dragonballtap.apk;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;
import android.media.AudioFormat;
import android.util.Log;
//import com.namcobandaigames.dragonballtap.apk.R.raw;
import java.util.HashMap;

public final class SoundEffect implements OnCompletionListener {

    private static SoundEffect Instance = null;
    private static final String TAG = "SoundEffect";
    private static String[] ars;
    private final int AUDIO_MAX = 3;
    private final int SE_MAX = 20;
    private AudioTrack[] at = null;
    private int audio_count = 0;
    private int audio_play_index = 0;
    private int[] audio_playno = new int[3];
    private int[] audio_status = new int[3];
    private audioThread[] audio_thread = new audioThread[3];
    private float audio_volume = 1.0f;
    private MediaPlayer mp = null;
    private int play_id = -1;
    private Thread[] play_thread = new Thread[3];
    private SoundPool soundPool = null;
    private HashMap soundPoolMap = null;
    private int sound_count = 0;
    private byte[][] wave = new byte[40][];

    class audioThread implements Runnable {

        private boolean _end = false;
        private int _index = -1;

        audioThread() {
        }

        public void setData(int index) {
            this._index = index;
        }

        public void End() {
            this._end = true;
        }

        public void run() {
            while (true) {
                boolean bPlay = false;
                if (!this._end) {
                    if (SoundEffect.this.audio_status[this._index] == 1) {
                        SoundEffect.this.at[this._index].setStereoVolume(SoundEffect.this.audio_volume, SoundEffect.this.audio_volume);
                        SoundEffect.this.at[this._index].play();
                        SoundEffect.this.at[this._index].write(
                                SoundEffect.this.wave[SoundEffect.this.audio_playno[this._index]], 44,
                                SoundEffect.this.wave[SoundEffect.this.audio_playno[this._index]].length - 44
                        );
                        SoundEffect.this.at[this._index].flush();
                        SoundEffect.this.at[this._index].stop();
                        bPlay = true;
                    }
                    try {
                        Thread.sleep(10);
                        if (bPlay) {
                            SoundEffect.this.audio_status[this._index] = 0;
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        }
    }

    public static SoundEffect getInstance() {
        if (Instance == null) {
            Instance = new SoundEffect();
        }
        return Instance;
    }

    public void dispose() {
        int i;
        if (this.soundPool != null) {
            this.soundPool.release();
            this.soundPool = null;
            this.soundPoolMap.clear();
            this.soundPoolMap = null;
        }
        this.sound_count = 0;
        this.play_id = -1;
        if (this.mp != null) {
            stopBgm();
            this.mp.release();
            this.mp = null;
        }
        for (i = 0; i < 3; i++) {
            try {
                if (this.play_thread[i] != null) {
                    this.audio_thread[i].End();
                    this.play_thread[i].join();
                }
            } catch (Exception e) {
            }
            this.audio_thread[i] = null;
            this.play_thread[i] = null;
        }
        try {
            Thread.sleep(20);
        } catch (InterruptedException e2) {
        }
        if (this.at != null) {
            stopAudio();
            for (i = 0; i < this.at.length; i++) {
                this.audio_status[i] = 0;
                if (this.at[i] != null) {
                    this.at[i].release();
                    this.at[i] = null;
                }
            }
            this.at = null;
        }
    }

    public int loadAudioTrack(Context _context, byte[] data) {
        try {
            if (this.at == null) {
                int i;
                int bufSize = AudioTrack.getMinBufferSize(44100, 2, 2);
                this.at = new AudioTrack[3];
                for (i = 0; i < this.at.length; i++) {
                    this.at[i] = new AudioTrack(3, 22050, 2, 2, bufSize, 1);
                }
                for (i = 0; i < 3; i++) {
                    if (this.audio_thread[i] == null) {
                        this.audio_thread[i] = new audioThread();
                    }
                    this.audio_thread[i].setData(i);
                    this.play_thread[i] = new Thread(this.audio_thread[i]);
                    this.play_thread[i].start();
                }
            }
            this.wave[this.audio_count] = data;
            return ++this.audio_count;
        } catch (Exception e) {
            Log.e(TAG, "233:" + e);
            return -1;
        }
    }

    public int load(Context _context, String name) {
//        Log.e("Era 4", name);
        if (this.sound_count >= 79) {
            return -1;
        }
        try {
            int no = this.sound_count;
            if (this.soundPool == null) {
                this.soundPool = new SoundPool(79, 3, 80);
            }
            if (this.soundPoolMap == null) {
                this.soundPoolMap = new HashMap();
            }
            AssetFileDescriptor hc = _context.getAssets().openFd(new StringBuilder(String.valueOf(name)).append(".ogg").toString());
            this.soundPoolMap.put(Integer.valueOf(this.sound_count), Integer.valueOf(this.soundPool.load(hc.getFileDescriptor(), hc.getStartOffset(), hc.getLength(), 1)));
            this.sound_count++;
            return no;
        } catch (Exception e) {
            e.printStackTrace();
//            Log.e(TAG, "388:[" + this.sound_count + "]" + e);
            return -1;
        }
    }

    public void releaseAudio() {
        for (int i = 0; i < this.wave.length; i++) {
            this.wave[i] = null;
        }
        this.audio_count = 0;
    }

    private int getAudioIndex() {
        if (this.at != null) {
            for (int i = 0; i < this.at.length; i++) {
                int index = (this.audio_play_index + i) % 3;
                if (this.at[index] != null && this.audio_status[index] == 0 && this.at[index].getPlayState() != 3 && this.at[index].getPlayState() == 1 && (this.at[index].getState() == 1 || this.at[index].getState() == 2)) {
                    this.audio_play_index++;
                    return index;
                }
            }
        }
        return -1;
    }

    public void playAudio(int no, float volume) {
        if (no < this.audio_count && this.wave[no] != null) {
            int index = getAudioIndex();
            if (index >= 0) {
                this.audio_volume = volume;
                this.audio_status[index] = 1;
                this.audio_playno[index] = no;
            }
        }
    }

    public synchronized void playBgm(Context context, String str, float f, boolean z) {
//        Log.e("Era 5", str);
        if (this.mp != null) {
            if (this.mp.isPlaying()) {
                this.mp.stop();
            }
            this.mp.release();
            this.mp = (MediaPlayer) null;
        }
        this.mp = new MediaPlayer();
        try {
            AssetFileDescriptor openFd = context.getAssets().openFd(new StringBuffer().append(str).append(".ogg").toString());
            this.mp.setDataSource(openFd.getFileDescriptor(), openFd.getStartOffset(), openFd.getLength());
            this.mp.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mp.setVolume(f, f);
        this.mp.setLooping(z);
        this.mp.start();
        this.mp.setOnCompletionListener(this);
    }

    public void stopAudio() {
        if (this.at != null) {
            int i = 0;
            while (i < this.at.length) {
                if (this.at[i] != null && this.at[i].getPlayState() == 3) {
                    this.at[i].stop();
                }
                i++;
            }
        }
    }

    public void playSE(int no, float volume) {
        if (no < this.sound_count && this.soundPool != null) {
            this.play_id = this.soundPool.play(((Integer) this.soundPoolMap.get(Integer.valueOf(no))).intValue(), volume, volume, 1, 0, 1.0f);
        }
    }

    public void stopSE() {
        if (this.soundPool != null && this.play_id != -1) {
            this.play_id = -1;
        }
    }

    public void stopBgm() {
        if (this.mp != null && this.mp.isPlaying()) {
            this.mp.stop();
        }
    }

    public void onCompletion(MediaPlayer mediaPlayer) {
    }

}
