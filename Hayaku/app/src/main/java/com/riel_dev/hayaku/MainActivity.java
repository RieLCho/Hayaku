package com.riel_dev.hayaku;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.RemoteInput;

import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity;

import java.util.Random;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends AppCompatActivity{
    // Global Basic Types
    public static final int NOTIFICATION_ID = 1;
    public static final String KEY_TWEET = "key_tweet";
    public Boolean isAlreadyLoggedInToTwitter;
    public String profilePicUrl;

    // Global View Type Objects
    CardView accountCard;
    ImageView imageView;
    TextView textView;
    TextView textView2;
    SettingsFragment settingsFragment;
    NotificationCompat.Builder builder;
    Intent sendTwitterIntent;
    private AdView adView;

    // Global Twitter Type Objects
    RequestToken requestToken;
    ConfigurationBuilder configurationBuilder;
    TwitterFactory twitterFactory;
    Twitter twitter;
    RemoteInput remoteInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        @SuppressLint("HandlerLeak") Handler twitterDataLoadHandler = new Handler(){
            public void handleMessage(Message msg){
                setTwitterDataToViews();
            }
        };

        /* Bring Twitter Login Data with PreferenceManager then show into TextViews */
        isAlreadyLoggedInToTwitter = CustomPreferenceManager.getBoolean(getApplicationContext(),"login");
        if(isAlreadyLoggedInToTwitter){
            configurationBuilder = new ConfigurationBuilder();
            configurationBuilder.setOAuthConsumerKey(getString(R.string.consumer_key));
            configurationBuilder.setOAuthConsumerSecret(getString(R.string.consumer_key_secret));
            configurationBuilder.setOAuthAccessToken(CustomPreferenceManager.getString(getApplicationContext(), "access_token"));
            configurationBuilder.setOAuthAccessTokenSecret(CustomPreferenceManager.getString(getApplicationContext(), "access_secret"));
            twitterFactory = new TwitterFactory(configurationBuilder.build());
            twitter = twitterFactory.getInstance();

            sendTwitterIntent = new Intent(getApplicationContext(), SendTweetService.class);
            startService(sendTwitterIntent);

            Thread twitterDataLoadThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        loadTwitterData();
                        Message msg = twitterDataLoadHandler.obtainMessage();
                        twitterDataLoadHandler.sendMessage(msg);
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                }
            });
            twitterDataLoadThread.start();
        }
        Thread sleepThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
        sleepThread.start();
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        Spannable text = new SpannableString(actionBar.getTitle());
        text.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        actionBar.setTitle(text);
        actionBar.setElevation(0);
        loadSettingPreferences();

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* Connect Views with findViewById */
        accountCard = findViewById(R.id.twitterAccountCardView);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);

        /* Describe When User touches CardView (Twitter Account Information) */
        accountCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // If Application is not logged in to Twitter, go to LoginActivity
                if(!isAlreadyLoggedInToTwitter){
                    Thread twitterLoginThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final String consumer_key = getString(R.string.consumer_key);
                                final String consumer_key_secret = getString(R.string.consumer_key_secret);
                                ConfigurationBuilder builder = new ConfigurationBuilder();
                                builder.setOAuthConsumerKey(consumer_key);
                                builder.setOAuthConsumerSecret(consumer_key_secret);
                                Configuration configuration = builder.build();
                                TwitterFactory factory = new TwitterFactory(configuration);
                                twitter = factory.getInstance();
                                requestToken = twitter.getOAuthRequestToken();
                                // intent to LoginActivity
                                String twitterAuthURL = requestToken.getAuthorizationURL();
                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                intent.putExtra("url", twitterAuthURL);
                                intent.putExtra("twitter", twitter);
                                intent.putExtra("requestToken", requestToken);
                                startActivity(intent);
                                finish();
                            } catch (TwitterException exception) {
                                Toast.makeText(MainActivity.this, exception.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    twitterLoginThread.start();
                }else{
                    // If Application is already logged in, show logout dialog
                    showLogoutDialog();
                }
            }
        });
    }

    /* Dialog For Twitter Logout */
    public void showLogoutDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout?").setMessage("Do you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CustomPreferenceManager.clear(getApplicationContext());
                Toast.makeText(getApplicationContext(), R.string.logout_notification, Toast.LENGTH_SHORT).show();
                removeNotification();
                reload();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    /* ReLoad View for some reasons such as Twitter data load */
    public void reload(){
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
    private void show() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(this, "twitterId")
                .setSmallIcon(R.drawable.ic_twitter)
                .setContentTitle("Hayaku is running")
                .setContentIntent(resultPendingIntent)
                .setShowWhen(false)
                .setOngoing(true)
                .setContentText("Logged into " + CustomPreferenceManager.getString(getApplicationContext(), "twitterId"));
        Log.d("?????? ??????", "??????");
        remoteInput = new RemoteInput.Builder(KEY_TWEET)
                .setLabel("What's happening?")
                .build();
        int randomRequestCode = new Random().nextInt(54325);
        Intent resultIntent2 = new Intent(getApplicationContext(), SendTweetService.class);
        PendingIntent tweetPendingIntent =
                PendingIntent.getService(getApplicationContext(),randomRequestCode, resultIntent2, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action tweetAction = new NotificationCompat.Action.Builder(R.drawable.ic_edit, "Tweet", tweetPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        builder.addAction(tweetAction);
        Log.d("?????? ?????? ??????: ", "??????");
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("twitterId", KEY_TWEET, NotificationManager.IMPORTANCE_LOW));
        }
        notificationManager.notify(0, builder.build());

    }

    private void hide() {
        NotificationManagerCompat.from(this).cancel(0);
    }
    public void createNotification() {
        show();
    }
    public void removeNotification() {
        hide();
    }

    /* ??????????????? ?????? */
    private void loadSettingPreferences(){
        settingsFragment = (SettingsFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);

        // Open Source Notices
        Preference OssPreference = null;
        Preference tutorialPreference = null;
        Preference infoPreference = null;
        if (settingsFragment != null) {
            OssPreference = settingsFragment.findPreference("openSourceNotices");
            tutorialPreference = settingsFragment.findPreference("tutorial");
            infoPreference = settingsFragment.findPreference("information");
        }
        if (OssPreference != null) {
            OssPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    OssLicensesMenuActivity.setActivityTitle("Open Source Licenses");
                    startActivity(new Intent(getApplicationContext(), OssLicensesMenuActivity.class));
                    return true;
                }
            });
        }
        if (tutorialPreference != null) {
            tutorialPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // ???????????? ??????
                    startActivity(new Intent(getApplicationContext(), TutorialActivity.class));
                    return true;
                }
            });
        }
        if (infoPreference != null) {
            infoPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // ??????????????? ??????
                    startActivity(new Intent(getApplicationContext(),AboutActivity.class));
                    return true;
                }
            });
        }
    }

    private void loadTwitterData() throws TwitterException {
        User user = twitter.showUser(twitter.getId());
        profilePicUrl = user.getOriginalProfileImageURLHttps();
        CustomPreferenceManager.setString(getApplicationContext(), "profilePicUrl", profilePicUrl);
        Log.d("Profile Picture Url: ", profilePicUrl);
        String nickname = user.getName();
        CustomPreferenceManager.setString(getApplicationContext(), "nickname", nickname);
        Log.d("Twitter Nickname: ", nickname);
        String twitterId = user.getScreenName();
        Log.d("Twitter ID: ", twitterId);
        CustomPreferenceManager.setString(getApplicationContext(), "twitterId", "\u0040" + twitterId);
    }
    private void setTwitterDataToViews(){
        /* Load Twitter Profile Image into ImageView */
        if(CustomPreferenceManager.getString(getApplicationContext(), "profilePicUrl") != null) {
            Log.d("Profile Pic Url", CustomPreferenceManager.getString(getApplicationContext(), "profilePicUrl"));
            RequestOptions requestOptions = new RequestOptions();
            requestOptions = requestOptions.transform(new CenterCrop(), new RoundedCorners(30));
            Glide.with(MainActivity.this)
                    .load(Uri.parse(CustomPreferenceManager.getString(getApplicationContext(), "profilePicUrl")))
                    .apply(requestOptions)
                    .placeholder(R.drawable.egg)
                    .error(R.drawable.egg)
                    .into(imageView);
        }else{
            Glide.with(this).load(R.drawable.egg).into(imageView);
        }
        textView = findViewById(R.id.textView);
        textView.setText(CustomPreferenceManager.getString(getApplicationContext(), "nickname"));
        textView2 = findViewById(R.id.textView2);
        textView2.setText(CustomPreferenceManager.getString(getApplicationContext(), "twitterId"));
    }


}