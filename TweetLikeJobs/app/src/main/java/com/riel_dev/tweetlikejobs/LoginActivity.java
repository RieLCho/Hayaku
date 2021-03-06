package com.riel_dev.tweetlikejobs;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class LoginActivity extends AppCompatActivity {

    // Global View Type Objects
    WebView webView;
    EditText editText;
    Button button;

    // Global Twitter Type Objects
    AccessToken accessToken;
    Twitter twitter;

    /* Action Bar Menu */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_menu, menu);
        return true;
    }

    /* When Action Bar Menu Selected */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.otherBroswer_button){
            Intent intent = getIntent();
            Bundle bundle = intent.getExtras();
            String twitterAuthURL = bundle.getString("url");
            Intent openBroswerIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(twitterAuthURL));
            startActivity(openBroswerIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle("Login");
        setContentView(R.layout.activity_login);

        /* Connect Views with findViewById */
        webView = findViewById(R.id.twitterLoginWebView);
        editText = findViewById(R.id.editTextNumberPassword2);
        button = findViewById(R.id.button);

        /* Get Intent From Main Activity */
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String twitterAuthURL = bundle.getString("url");
        RequestToken requestToken = (RequestToken)bundle.getSerializable("requestToken");
        Twitter twitter = (Twitter)bundle.getSerializable("twitter");

        /* Login with in app WebView Browser */
        webView.loadUrl(twitterAuthURL);
        webView.getSettings().setJavaScriptEnabled(true);

        /* When Next Button Clicked */
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread twitterLoginThread2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String pinNumber = editText.getText().toString();
                        Log.d("Pin", pinNumber);
                        try {
                            accessToken = twitter.getOAuthAccessToken(requestToken, pinNumber);
                        } catch (TwitterException e) {
                            if (401 == e.getStatusCode()) {
                                Toast.makeText(getApplicationContext(), "Unable to get the access token.", Toast.LENGTH_LONG).show();
                            }
                        }
                        PreferenceManager.setBoolean(getApplicationContext(), "login", true);
                        String access_token = accessToken.getToken();
                        String access_secret = accessToken.getTokenSecret();

                        PreferenceManager.setString(getApplicationContext(), "access_token", access_token);
                        PreferenceManager.setString(getApplicationContext(), "access_secret", access_secret);

                        Log.d("Access Token", access_token);
                        PreferenceManager.setBoolean(getApplicationContext(), "firstLogin", true);

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                });
                twitterLoginThread2.start();
            }
        });
    }
}
