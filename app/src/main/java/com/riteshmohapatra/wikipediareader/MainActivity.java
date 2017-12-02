package com.riteshmohapatra.wikipediareader;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RequestQueue queue;
    private TextToSpeech tts;

    // viewer
    private TextView textView;
    private ProgressBar progress;
    private View textViewer;

    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.queue = Volley.newRequestQueue(MainActivity.this);

        // Initialize TextToSpeech engine.
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                // noop
            }
        });
        tts.setLanguage(Locale.getDefault());

        setContentView(R.layout.activity_main);

        // Initialize the views
        textView = (TextView) findViewById(R.id.textView);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        textViewer = findViewById(R.id.textViewer);
        fab = (FloatingActionButton) findViewById(R.id.volume);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // set toolbar as the ActionBar

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tts.isSpeaking()) {
                    tts.stop();
                    fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow));
                } else {
                    fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stop));
                    tts.speak(textView.getText().toString(), TextToSpeech.QUEUE_FLUSH, null);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.tts.stop();
        this.queue.stop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        this.queue.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.tts.shutdown();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);

        final MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView)searchMenuItem.getActionView();
        searchView.setQueryHint("Search Wikipedia");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override       // what happens when the user submits a query
            public boolean onQueryTextSubmit(String query) {
                searchMenuItem.collapseActionView(); // collapse the search box
                if (tts.isSpeaking()) {     // if tts is speaking, stop it
                    tts.stop();
                    fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow));
                }
                search(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // do nothing
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener(new OnFocusChangeListener() {
            @Override       // hide the search box if the user focuses on something else
            public void onFocusChange(View view, boolean queryTextFocused) {
                if(!queryTextFocused) {
                    searchView.setIconified(true);
                    searchMenuItem.collapseActionView();
                }
            }
        });

        return true;
    }

    private void search(String query) {        // fetches the article and loads it into the viewer.
        textViewer.requestFocus();
        String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + encodeURIComponent(query);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {       // response received
                        progress.setVisibility(View.INVISIBLE);         // hide the progress bar
                        try {
                            JSONObject pages = response.getJSONObject("query")
                                    .getJSONObject("pages");
                            String firstPage = pages.keys().next();         // extract the first result
                            String text = pages.getJSONObject(firstPage).getString("extract");
                            setTitle(pages.getJSONObject(firstPage).getString("title"));       // set the title to the article title.
                            textViewer.setVisibility(View.VISIBLE);         // make viewer visible
                            textView.setText(text);                         // load the content into the viewer.
                        } catch (JSONException ex) {                        // response could not be parsed.
                            Toast.makeText(MainActivity.this,"Error in parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {        // no response
                        progress.setVisibility(View.INVISIBLE);
                        // todo: view image
                        Toast.makeText(MainActivity.this,"Error in getting response", Toast.LENGTH_SHORT).show();

                    }
                });


        // Add the request to the RequestQueue.
        textViewer.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);           // make the progress bar visible
        MainActivity.this.queue.add(jsObjRequest);
    }

    private static String encodeURIComponent(String s)
    {
        String result;

        try
        {
            result = URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        }

        // This exception should never occur.
        catch (UnsupportedEncodingException e)
        {
            result = s;
        }

        return result;
    }

}