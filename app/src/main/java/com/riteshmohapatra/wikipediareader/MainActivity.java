package com.riteshmohapatra.wikipediareader;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

    // search
    private Toolbar toolbar;
    private MenuItem searchBtn;
    private boolean isSearchOpen;

    // viewer
    TextView textView;
    ProgressBar progress;
    View textViewer;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.queue = Volley.newRequestQueue(MainActivity.this);

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                // noop
            }
        });
        tts.setLanguage(Locale.getDefault());

        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.textView);
        progress = (ProgressBar) findViewById(R.id.progressBar);
        textViewer = findViewById(R.id.textViewer);
        fab = (FloatingActionButton) findViewById(R.id.volume);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        isSearchOpen = false;

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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        searchBtn = menu.findItem(R.id.search_btn);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search_btn)
            handleSearchButton();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(isSearchOpen)
            hideSearchBox();
        else
            super.onBackPressed();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

        }
        return super.dispatchTouchEvent(event);
    }

    private void search(String query) {
        textViewer.requestFocus();
        String url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&explaintext=&titles=" + encodeURIComponent(query);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        progress.setVisibility(View.INVISIBLE);
                        try {
                            JSONObject pages = response.getJSONObject("query")
                                    .getJSONObject("pages");
                            String firstPage = pages.keys().next();
                            String text = pages.getJSONObject(firstPage).getString("extract");
                            setTitle(pages.getJSONObject(firstPage).getString("title"));
                            textViewer.setVisibility(View.VISIBLE);
                            textView.setText(text);
                        } catch (JSONException ex) {
                            Toast.makeText(MainActivity.this,"Error in parsing response", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progress.setVisibility(View.INVISIBLE);
                        // todo: view image
                        Toast.makeText(MainActivity.this,"Error in getting response", Toast.LENGTH_SHORT).show();

                    }
                });


        // Add the request to the RequestQueue.
        textViewer.setVisibility(View.INVISIBLE);
        progress.setVisibility(View.VISIBLE);
        MainActivity.this.queue.add(jsObjRequest);
    }

    private void hideSearchBox() {
        ActionBar action = getSupportActionBar(); //get the actionbar

        action.setDisplayShowCustomEnabled(false); //disable a custom view inside the actionbar
        action.setDisplayShowTitleEnabled(true); //show the title in the action bar

        //hides the keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View view = this.getCurrentFocus();
        if (view != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);

        //add the search icon in the action bar
        searchBtn.setIcon(getResources().getDrawable(R.drawable.ic_open_search));

        isSearchOpen = false;
    }

    // Source: http://blog.rhesoft.com/2015/03/30/tutorial-android-actionbar-with-material-design-and-search-field/
    private void handleSearchButton() {
        ActionBar action = getSupportActionBar(); //get the actionbar

        if(isSearchOpen){ //test if the search is open
            EditText searchInput = (EditText)action.getCustomView().findViewById(R.id.search_input); //the text editor
            searchInput.getText().clear();
            searchInput.requestFocus();


            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);

        } else { //open the search entry
            System.out.println(R.layout.searchbox);
            action.setDisplayShowCustomEnabled(true); //enable it to display a
            // custom view in the action bar.
            action.setCustomView(R.layout.searchbox);//add the custom view
            action.setDisplayShowTitleEnabled(false); //hide the title

            EditText searchInput = (EditText)action.getCustomView().findViewById(R.id.search_input); //the text editor
            searchInput.requestFocus();

            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT);

            //this is a listener to do a search when the user clicks on search button
            searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        String query = v.getText().toString().trim();
                        hideSearchBox();
                        if (tts.isSpeaking()) {
                            tts.stop();
                            fab.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play_arrow));
                        }
                        search(query);
                        return true;
                    }
                    return false;
                }
            });

            searchInput.setOnFocusChangeListener(new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideSearchBox();
                    }
                }
            });

            //add the close icon
            searchBtn.setIcon(getResources().getDrawable(R.drawable.ic_cancel_search));

            isSearchOpen = true;
        }
    }

    private static String encodeURIComponent(String s)
    {
        String result = null;

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
