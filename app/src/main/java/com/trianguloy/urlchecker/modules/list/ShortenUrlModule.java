package com.trianguloy.urlchecker.modules.list;

import android.os.AsyncTask;
import android.util.Log;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.url.UrlData;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;

public interface ShortenUrlCallback {
    void onUrlShortened(String shortenedUrl);
}

public class ShortenUrlModule extends AModuleDialog {

    private Button shortenButton;
    private TextView shortenedUrlTextView;

    public ShortenUrlModule(MainDialog mainDialog) {
        super(mainDialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.module_shorten_url;
    }

    @Override
    public void onInitialize(View rootView) {
        shortenButton = rootView.findViewById(R.id.button_shorten_url);
        shortenedUrlTextView = rootView.findViewById(R.id.text_shortened_url);

        shortenButton.setOnClickListener(v -> {
            UrlData currentUrlData = mainDialog.getUrlData();
            String originalUrl = currentUrlData.url;
            if (originalUrl != null && !originalUrl.isEmpty()) {
                shortenAndCallback(originalUrl, shortenedUrl -> shortenedUrlTextView.setText(shortenedUrl));
            } else {
                shortenedUrlTextView.setText("No URL available to shorten.");
            }
        });
    }

    @Override
    public void onPrepareUrl(UrlData urlData) {
        // No specific preparation needed for this module
    }

    @Override
    public void onModifyUrl(UrlData urlData, SetNewUrlCallback setNewUrlCallback) {
        // This module doesn't modify the URL in the main loop
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        shortenedUrlTextView.setText(""); // Clear previous shortened URL
    }

    @Override
    public void onFinishUrl() {
        // No specific action needed when URL processing finishes
    }

    public void shortenAndCallback(String originalUrl, ShortenUrlCallback callback) {
        new ShortenUrlTask(callback).execute(originalUrl);
    }

    private class ShortenUrlTask extends AsyncTask<String, Void, String> {
        private final ShortenUrlCallback callback;
        public ShortenUrlTask(ShortenUrlCallback callback) {
            this.callback = callback;
        }

         @Override
        protected String doInBackground(String... urls) {
            String originalUrl = urls[0];
            try {
                String apiUrl = "https://zws.im/api?url=" + originalUrl;
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder content = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();
                    conn.disconnect();

                    // Parse the JSON response to extract the shortened URL
                    JSONObject jsonResponse = new JSONObject(content.toString());
                    String shortenedUrl = jsonResponse.getString("shortenedUrl"); // Corrected key
                    return shortenedUrl;
                } else {
                    return "HTTP Error: " + conn.getResponseCode();
                }
            } catch (IOException e) {
                Log.e("ShortenUrlModule", "IOException during HTTP request", e);
                return "Error: " + e.getMessage();
            } catch (JSONException e) {
                Log.e("ShortenUrlModule", "JSONException while parsing JSON", e);
                return "Error: Invalid JSON response";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(callback!=null){
                callback.onUrlShortened(result);
            }else if (result.startsWith("Error:") || result.startsWith("HTTP Error:")) {
                mainDialog.runOnUiThread(() -> {
                    shortenedUrlTextView.setText(result);
                });
            }
        }
    }
}
```
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="8dp">

    <Button
        android:id="@+id/button_shorten_url"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Shorten URL" />

    <TextView
        android:id="@+id/text_shortened_url"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text=""
        android:textColor="@android:color/black"
        android:textSize="14sp" />

</LinearLayout>