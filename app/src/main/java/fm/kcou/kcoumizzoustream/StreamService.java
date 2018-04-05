package fm.kcou.kcoumizzoustream;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.res.ResourcesCompat;
import android.widget.ImageButton;

import java.io.IOException;

public class StreamService extends Service implements MediaPlayer.OnPreparedListener,MediaPlayer.OnErrorListener {

    MediaPlayer mMediaPlayer = null;
    int currentState = 0; // 0=paused, 1=playing 2=connecting
    WifiManager.WifiLock wifiLock;
    Context context;
    AudioManager am;
    AudioManager.OnAudioFocusChangeListener afChangeListener;
    ImageButton playButton;

    public void build(final Context context, ImageButton imageButton) {
        this.context = context;
        this.playButton = imageButton;
        mMediaPlayer = new MediaPlayer();

        am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            wifiLock = wm.createWifiLock(String.valueOf(WifiManager.WIFI_MODE_FULL));
            wifiLock.setReferenceCounted(true);
        }
        afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    stop();
                    playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_play_arrow_white_24dp, null));
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    mMediaPlayer.setVolume(0.2f,0.2f);
                }
            }
        };

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void initMediaPlayer(String streamURL) {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mMediaPlayer.setDataSource(streamURL);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaPlayer.setOnErrorListener(this);
    }

    boolean createFocus(){
        int result = am.requestAudioFocus(afChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    void play(String streamURL) {
        currentState=2;
        playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_hourglass_empty_white_24dp, null));
        initMediaPlayer(streamURL);
        mMediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
        wifiLock.acquire();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.prepareAsync();
//        onPrepared(mMediaPlayer);
        if(!createFocus()){stop();}
    }

    /** Called when MediaPlayer is ready */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_pause_white_24px, null));
        currentState=1;
    }

    void stop() {
        mMediaPlayer.stop();
        playButton.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.ic_play_arrow_white_24dp, null));
        mMediaPlayer.reset();
//        mMediaPlayer.release();
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
        currentState=0;
        am.abandonAudioFocus(afChangeListener);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if(wifiLock.isHeld()) {
            wifiLock.release();
        }
        return false;
    }

    public void onDestroy() {

        if (currentState == 1) stop();
        if (mMediaPlayer != null) mMediaPlayer.release();
    }

    int getState(){
        return currentState;
    }

//    MediaPlayer getMediaPlayer(){
//        return mMediaPlayer;
//    }

}