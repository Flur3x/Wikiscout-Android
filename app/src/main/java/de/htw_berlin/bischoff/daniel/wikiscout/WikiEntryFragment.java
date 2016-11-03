package de.htw_berlin.bischoff.daniel.wikiscout;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class WikiEntryFragment extends Fragment {

    private static final String ARG_TITLE = "wikiPageTitle";
    private String wikiPageTitle;
    private OnFragmentInteractionListener mListener;
    private TextView textView;
    private ImageView imageView;

    public WikiEntryFragment() {
        // Required empty public constructor
    }

    public static WikiEntryFragment newInstance(String wikiPageTitle) {
        WikiEntryFragment fragment = new WikiEntryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, wikiPageTitle);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            wikiPageTitle = getArguments().getString(ARG_TITLE);
        }

        try {
            getWikiText(wikiPageTitle);
        } catch (JSONException e) {
            e.printStackTrace();
        }
}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wiki_entry, container, false);

        textView = (TextView) rootView.findViewById(R.id.textView);
        imageView = (ImageView) rootView.findViewById(R.id.image);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
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

                    loadContent((imageUrl != null) ? imageUrl : null, text);
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

        textView.setText(parsedHtml);
    }

    public void loadContent(String imageUrl, final String text) {
        if (imageUrl != null) {
            ImageLoader imageLoader = ImageLoader.getInstance();

            imageLoader.displayImage(imageUrl, imageView, null, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {}

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    setText(text);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    setText(text);
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    setText(text);
                }

            }, new ImageLoadingProgressListener() {
                @Override
                public void onProgressUpdate(String imageUri, View view, int current, int total) {}
            });
        } else {
            setText(text);
        }
    }
}
