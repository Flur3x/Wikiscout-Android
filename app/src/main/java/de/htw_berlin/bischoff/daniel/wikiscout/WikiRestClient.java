package de.htw_berlin.bischoff.daniel.wikiscout;

import com.loopj.android.http.*;

import cz.msebera.android.httpclient.client.params.ClientPNames;

class WikiRestClient {
    private static final String BASE_URL = "https://de.wikipedia.org/w/api.php";

    private static AsyncHttpClient client = new AsyncHttpClient();

    static void setup() {
        client.getHttpClient().getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
    }

    static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}