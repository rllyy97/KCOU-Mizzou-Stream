package fm.kcou.kcoumizzoustream;
// Author: Riley Evans, started September 13 2017

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MyActivity";

    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
    String stream2 = "http://104.250.149.122:9106/stream?type=http&nocache=164758";
    String stream2Meta = "http://sc9.shoutcaststreaming.us:2199/recentfeed/c9106/json/";

    JSONObject jsonMeta;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting
    int currentStream = 1;

    protected ImageButton playButton;
    protected TextView status;
    protected TextView streamType;
    protected TextView silenceWarning;
    protected Visualizer audioOutput;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        playButton = (ImageButton) findViewById(R.id.playButton);
        status = (TextView) findViewById(R.id.statusTextView);
        streamType = (TextView) findViewById(R.id.streamType);
        silenceWarning = (TextView) findViewById(R.id.silenceWarning);
        if(!isNetworkAvailable()){
            status.setText(R.string.connection_warning);
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
        } else {
            status.setText(R.string.startup_message);
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);


    }

    public void playPauseStream(final View v) throws JSONException, IOException, ParseException {
        if(playing==1){
            stopStream();
        }
        else if(isNetworkAvailable()&&playing!=2){
            currentStream=whichStream();
            startStream();
        }
        else if(!isNetworkAvailable()){
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
            status.setText(R.string.connection_warning);
            playing = 0;
        }
    }


    /////     Android Make API Call for json : Now Playing

    private class AsyncMeta extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... url) {
            try {
                jsonMeta = readJsonFromUrl(url[0]);
                onPostExecute();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private Void onPostExecute() throws IOException, JSONException {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    try {
                        setStreamMeta();
                    } catch (JSONException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            return null;
        }

        private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
            try {
                InputStream is = new URL(url).openStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String jsonText = readAll(rd);
                JSONObject json = new JSONObject(jsonText);
                is.close();
                return json;
            } catch (IOException ex){
                return null;
            }
        }

        private String readAll(Reader rd) throws IOException {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {sb.append((char) cp);}
            return sb.toString();
        }

        private void setStreamMeta() throws JSONException, IOException {
            String[] data = new String[3];
            String fullTitle = jsonMeta.getJSONArray("items").getJSONObject(0).getString("title");
            String[] track = fullTitle.split(" - ");
            data[0] = track[1];
            data[1] = track[0];
            data[2] = jsonMeta.getJSONArray("items").getJSONObject(0).getString("description");

            LinearLayout trackInfo = (LinearLayout) findViewById(R.id.trackInfo);
            for(int k=0;k<3;k++) {
                TextView text = (TextView) trackInfo.getChildAt(k);
                text.setText(data[k]);
            }
        }
    }

    ///// Network Check

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /////

    private void stopStream(){
        mediaPlayer.stop();
        audioOutput.setEnabled(false);
        audioOutput.release();
        playing = 0;

                playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_radio_white_24px, null));
                streamType.setText("");
                LinearLayout trackInfo = (LinearLayout) findViewById(R.id.trackInfo);
                for (int k = 0; k < 3; k++) {
                    TextView text = (TextView) trackInfo.getChildAt(k);
                    text.setText("");
                }
                status.setText(R.string.startup_message);

        mediaPlayer.reset();
    }

    private void startStream() throws ParseException {
        playing = 2;
        currentStream=whichStream();
        final RotateAnimation rotateAnim = new RotateAnimation(
                0, 3240, playButton.getWidth()/2, playButton.getHeight()/2);
        rotateAnim.setDuration(2500);
        rotateAnim.setFillAfter(true);
        playButton.startAnimation(rotateAnim);

        try {
            if(currentStream==1) mediaPlayer.setDataSource(stream1);
            if(currentStream==2) mediaPlayer.setDataSource(stream2);

        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            public void onPrepared(MediaPlayer player){
                player.start();
                playing = 1;
                createVisualizer();

                rotateAnim.cancel();
                rotateAnim.reset();
                playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_white_24px, null));
                if(currentStream==1){
                    new AsyncMeta().execute(stream1Meta);
                    streamType.setText(R.string.stream_a);
                }
                if(currentStream==2){
                    new AsyncMeta().execute(stream2Meta);
                    streamType.setText(R.string.stream_b);
                }
                final Timer t = new Timer();
                t.scheduleAtFixedRate(new TimerTask(){
                    public void run() {
                        if(!isNetworkAvailable()){
                            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
                            status.setText(R.string.connection_warning);
                            t.purge();
                            cancel();
                        }
                        else if(playing==1) {
                            if(currentStream==1){
                                try {
                                    new AsyncMeta().execute(stream1Meta);
                                    if(whichStream()==2){
                                        switchStreams();
                                        t.purge();
                                        cancel();
                                    }

                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                            if(currentStream==2){
                                try {
                                    new AsyncMeta().execute(stream2Meta);
                                    if(whichStream()==1){
                                        switchStreams();
                                        t.purge();
                                        cancel();
                                    }
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else {
                            t.purge();
                            cancel();
                        }
                    }
                }, 0, 15000);
            }
        });
    }

    private void switchStreams() throws ParseException {
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
            try {
                stopStream();
                startStream();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        });
    }

    private int whichStream() throws ParseException {

        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        boolean early = currentHour >= 22;
        boolean late  = currentHour < 6;

        if(early || late){
            return 2;
        } else {
            return 1;
        }

//        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
//        int currentMin = rightNow.get(Calendar.MINUTE);
//        boolean trigger = currentMin >= 12;
//        Log.d(TAG,String.valueOf(currentMin));
//        if(trigger){
//            return 2;
//        } else {
//            return 1;
//        }

    }

    ///// Listener for silence on stream

    public float intensity = 0;
    public int silenceCount = 0;
    public int secondsQuietThreshold = 2*( 15 );

    private void createVisualizer(){
        int rate = 2000;
        audioOutput = new Visualizer(mediaPlayer.getAudioSessionId()); // get output audio stream
        audioOutput.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                intensity = ((float) waveform[0] + 128f) / 256;
                if(intensity<0.05){
                    silenceCount++;
                    if(silenceCount>secondsQuietThreshold){
                        silenceWarning.setText(R.string.silence_warning);
                        sendSilenceAlert(secondsQuietThreshold/2);
                    }
                } else {
                    silenceCount = 0;
                    silenceWarning.setText("");
                }
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {

            }
        },rate , true, false); // waveform not freq data

        audioOutput.setEnabled(true);
    }

    ///// Record Audio Permission Request

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();
    }

    protected void sendSilenceAlert(int seconds){
        try {
            URL url = new URL("https://api.groupme.com/v3/bots/post");
            HttpURLConnection client = (HttpURLConnection) url.openConnection();

            client.setRequestMethod("POST");
            client.setRequestProperty("bot_id","29652a058532ede37e2779a8be");
            client.setRequestProperty("text","Stream has been silent for " + seconds + " seconds.");
            client.setDoOutput(true);

            OutputStream outputPost = new BufferedOutputStream(client.getOutputStream());
            outputPost.flush();
            outputPost.close();
            client.setChunkedStreamingMode(0);
            client.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
