package fm.kcou.kcoumizzoustream;
// Author: Riley Evans, started September 13 2017

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

//    private static final String TAG = "MyActivity";

    String stream1 = "http://radio.kcou.fm:8180/stream";
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
    String stream2 = "http://104.250.149.122:9106/stream?type=http&nocache=164758";
    String stream2Meta = "http://sc9.shoutcaststreaming.us:2199/recentfeed/c9106/json/";

    JSONObject jsonMeta;

    MediaPlayer mediaPlayer = new MediaPlayer();
    int playing = 0; // 0=paused, 1=playing 2=connecting

    protected ImageButton playButton;
    protected TextView status;
    protected TextView streamType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        playButton = (ImageButton) findViewById(R.id.playButton);
        status = (TextView) findViewById(R.id.statusTextView);
        streamType = (TextView) findViewById(R.id.streamType);
        if(!isNetworkAvailable()){
            status.setText(R.string.connection_warning);
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
        } else {
            status.setText(R.string.startup_message);
        }
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


    }

    public void playPauseStream(final View v) throws JSONException, IOException, ParseException {
        if(playing==1){
            stopStream();
        }
        else if(isNetworkAvailable()&&playing!=2){
            startStream(whichStream());
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
        playing = 0;
        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_radio_white_24px, null));
        mediaPlayer.reset();
    }

    private void startStream(final int x){
        playing = 2;
        final RotateAnimation rotateAnim = new RotateAnimation(
                0, 3240, playButton.getWidth()/2, playButton.getHeight()/2);
        rotateAnim.setDuration(2500);
        rotateAnim.setFillAfter(true);
        playButton.startAnimation(rotateAnim);

        try {
            if(x==1){
                mediaPlayer.setDataSource(stream1);
                scheduleSwitchToB();
            }
            if(x==2){
                mediaPlayer.setDataSource(stream2);
                scheduleSwitchToA();
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
            public void onPrepared(MediaPlayer player){
                player.start();
                playing = 1;

                rotateAnim.cancel();
                rotateAnim.reset();
                playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_white_24px, null));
                if(x==1){
                    new AsyncMeta().execute(stream1Meta);
                    streamType.setText(R.string.stream_a);
                }
                if(x==2){
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
                        else if(playing!=0) {
                            if(x==1)new AsyncMeta().execute(stream1Meta);
                            if(x==2)new AsyncMeta().execute(stream2Meta);
                        }
                        else {
                            t.purge();
                            cancel();
                        }
                    }
                }, 0, 60000);
            }
        });
    }

    private void switchStreams(int x){
        if(playing==1){
            stopStream();
            startStream(x);
        }
    }

    private void scheduleSwitchToA() throws ParseException {
        SimpleDateFormat earlyFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        earlyFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date early = earlyFormat.parse("06:00");
        Timer t=new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if (playing == 1) {
                    try {
                        switchStreams(1);
                        scheduleSwitchToB();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, early);
    }

    private void scheduleSwitchToB() throws ParseException {
        SimpleDateFormat lateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
        lateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date late = lateFormat.parse("20:00");
        Timer t=new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                if(playing==1) {
                    try {
                        switchStreams(2);
                        scheduleSwitchToA();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, late);
    }

    private int whichStream() throws ParseException {
//        Date date = new Date();
//        SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse(date);
//        boolean early = new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse("06:00").after(currentTime);
//        boolean late  = new SimpleDateFormat("HH:mm", Locale.ENGLISH).parse("20:00").before(currentTime);
        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        boolean early = currentHour >= 20;
        boolean late  = currentHour < 6;

        if(early || late){
            return 2;
        } else {
            return 1;
        }
    }
}
