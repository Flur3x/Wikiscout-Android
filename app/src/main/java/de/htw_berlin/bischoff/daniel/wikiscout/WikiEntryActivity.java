package de.htw_berlin.bischoff.daniel.wikiscout;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class WikiEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wiki_entry);

        Intent intent = getIntent();
        String title = intent.getStringExtra("title");

        try {
            getWikiText(title);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void getWikiText(String title) throws JSONException {

        RequestParams params = new RequestParams();
        params.put("action", "query");
        params.put("prop", "extracts|pageimages");
        params.put("piprop", "thumbnail");
        params.put("pithumbsize", "1200");
        params.put("format", "json");
        params.put("exintro", "1");
        params.put("titles", title);

        JsonHttpResponseHandler handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                System.out.println(response);

                String imageUrl = null;

                try {
                    JSONObject entries = response.getJSONObject("query").getJSONObject("pages");
                    String key = entries.names().getString(0);
                    JSONObject entry = entries.getJSONObject(key);
                    String text = entry.optString("extract");

                    JSONObject thumbnail = entry.optJSONObject("thumbnail");

                    if (thumbnail != null) {
                        imageUrl = thumbnail.optString("source");
                    }

                    // System.out.println("entry: " + entry);
                    System.out.println("image: " + imageUrl);

                    setText(text);

                    if (imageUrl != null) {
                        setImage(imageUrl);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                System.out.println("status code: " + statusCode);
                System.out.println("headers: " + headers);
                System.out.println("json: " + errorResponse);
            }
        };

        if (title != null) {
            WikiRestClient.get("/", params, handler);
        }
    }

    public void setText(String htmlString) {
        Spanned parsedHtml;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            parsedHtml = Html.fromHtml(htmlString,Html.FROM_HTML_MODE_LEGACY);
        } else {
            parsedHtml = Html.fromHtml(htmlString);
        }

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(parsedHtml);
    }

    public void setImage(String imageUrl) {
        ImageView imageView = (ImageView) findViewById(R.id.image);
        ImageLoader imageLoader = ImageLoader.getInstance();
        imageLoader.displayImage(imageUrl, imageView);
    }
}
