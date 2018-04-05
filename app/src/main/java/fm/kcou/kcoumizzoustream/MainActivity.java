package fm.kcou.kcoumizzoustream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    String stream1 = "http://sc7.shoutcaststreaming.us:8180/stream";
    String stream1Meta = "http://sc7.shoutcaststreaming.us:2199/recentfeed/c8180/json";
    String stream2 = "http://sc9.shoutcaststreaming.us:9106/stream";
    String stream2Meta = "http://sc9.shoutcaststreaming.us:2199/recentfeed/c9106/json/";

    JSONObject jsonMeta;

//    MediaPlayer mediaPlayer = new MediaPlayer();
    StreamService stream;
    int currentStream = 1;

    protected ImageButton playButton;
    protected TextView status;
    protected Switch streamSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        playButton = findViewById(R.id.playButton);
        status = findViewById(R.id.statusTextView);
        streamSwitch = findViewById(R.id.stream_switch);

        if(!isNetworkAvailable()){
            status.setText(R.string.connection_warning);
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
        } else {
            if(isSafe()) status.setText(R.string.startup_message);
            else status.setText(R.string.safe_warning);
        }
        stream = new StreamService();
        stream.build(getBaseContext(),playButton);

        whichStream();
        streamSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(stream.getState()==1 && isSafe()) switchStreams();
            }
        });
    }

    public void playPauseStream(final View v) {
        if(stream.getState() == 1){
            stopStream();
        }
        else if(isNetworkAvailable()&&stream.getState()!=2){
            startStream();
        }
        else if(!isNetworkAvailable()){
            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask(){
                public void run() {
                    if(!isNetworkAvailable()){
                        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
                    } else {
                        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_white_24dp, null));
                        t.cancel();
                        t.purge();
                    }
                }
            }, 5000, 5000);
        }
    }


    /////     Android Make API Call for json : Now Playing

    @SuppressLint("StaticFieldLeak")
    private class AsyncMeta extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... url) {
            try {
                jsonMeta = readJsonFromUrl(url[0]);
                onPostExecute();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        private void onPostExecute() {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    try {
                        setStreamMeta();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private JSONObject readJsonFromUrl(String url) throws JSONException {
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

        private void setStreamMeta() throws JSONException {
            String[] data = new String[3];

            if(jsonMeta == null)
                return;
            if(jsonMeta.getJSONArray("items") == null)
                return;

            String fullTitle = jsonMeta.getJSONArray("items").getJSONObject(0).getString("title");          // gives null pointer error
            String[] track = fullTitle.split(" - ");
            data[0] = track[1];
            data[1] = track[0];
            data[2] = jsonMeta.getJSONArray("items").getJSONObject(0).getString("description");             // catch this also, same as above

            LinearLayout trackInfo = findViewById(R.id.trackInfo);
            for(int k=0;k<3;k++) {
                TextView text = (TextView) trackInfo.getChildAt(k);
                text.setText(data[k]);
            }
        }
    }

    ///// Network Check

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connectivityManager != null;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /////

    private void stopStream(){
        stream.stop();
        playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_white_24dp, null));
        LinearLayout trackInfo = findViewById(R.id.trackInfo);
        for (int k = 0; k < 3; k++) {
            TextView text = (TextView) trackInfo.getChildAt(k);
            text.setText("");
        }
        if(isSafe()) status.setText(R.string.startup_message);
        else status.setText(R.string.safe_warning);
    }

    private void startStream() {
//        playing = 2;
        currentStream=whichStream();

        if(currentStream==1) stream.play(stream1);
        if(currentStream==2) stream.play(stream2);

        if(currentStream==1){
            new AsyncMeta().execute(stream1Meta);
        }
        if(currentStream==2){
            new AsyncMeta().execute(stream2Meta);
        }

        final Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask(){
            public void run() {
                if(!isNetworkAvailable()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
                            status.setText(R.string.connection_warning);
                        }
                    });
                    t.purge();
                    cancel();
                }
                else if(stream.getState()==1) {
                    if(currentStream==1){
                        new AsyncMeta().execute(stream1Meta);
                        if(whichStream()==2){
                            switchStreams();
                            t.purge();
                            cancel();
                        }

                    }
                    if(currentStream==2){
                        new AsyncMeta().execute(stream2Meta);
                        if(whichStream()==1){
                            switchStreams();
                            t.purge();
                            cancel();
                        }
                    }
                }
                else {
                    t.purge();
                    cancel();
                }
            }
        }, 0, 15000);

//        mediaPlayer.prepareAsync();
//        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
//            public void onPrepared(MediaPlayer player){
//                player.start();
//                playing = 1;
//
//                rotateAnim.cancel();
//                rotateAnim.reset();
//                playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_white_24px, null));
//                if(currentStream==1){
//                    new AsyncMeta().execute(stream1Meta);
//                }
//                if(currentStream==2){
//                    new AsyncMeta().execute(stream2Meta);
//                }
//                final Timer t = new Timer();
//                t.scheduleAtFixedRate(new TimerTask(){
//                    public void run() {
//                        if(!isNetworkAvailable()){
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    playButton.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_portable_wifi_off_white_24px, null));
//                                    status.setText(R.string.connection_warning);
//                                }
//                            });
//                            t.purge();
//                            cancel();
//                        }
//                        else if(playing==1) {
//                            if(currentStream==1){
//                                new AsyncMeta().execute(stream1Meta);
//                                if(whichStream()==2){
//                                    switchStreams();
//                                    t.purge();
//                                    cancel();
//                                }
//
//                            }
//                            if(currentStream==2){
//                                new AsyncMeta().execute(stream2Meta);
//                                if(whichStream()==1){
//                                    switchStreams();
//                                    t.purge();
//                                    cancel();
//                                }
//                            }
//                        }
//                        else {
//                            t.purge();
//                            cancel();
//                        }
//                    }
//                }, 0, 15000);
//            }
//        });
    }

    private void switchStreams() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopStream();
                startStream();
            }
        });
    }

    private boolean isSafe() {

        Calendar rightNow = Calendar.getInstance(TimeZone.getTimeZone("America/Chicago"));
        int currentHour = rightNow.get(Calendar.HOUR_OF_DAY);
        boolean early = currentHour >= 22;
        boolean late  = currentHour < 6;

        if(early || late){
            return false;
        } else {
            status.setText("");
            return true;
        }

    }

    private int whichStream() {
        if(isSafe()){
            if(!streamSwitch.isClickable()){
                streamSwitch.setClickable(true);
                streamSwitch.setAlpha((float)1);
                streamSwitch.setChecked(false);

            }
            if(!streamSwitch.isChecked()) return 1;
            else return 2;
        }else{
            streamSwitch.setClickable(false);
            streamSwitch.setAlpha((float)0.5);
            streamSwitch.setChecked(true);
            return 2;
        }
    }

}
