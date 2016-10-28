package de.htw_berlin.bischoff.daniel.wikiscout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.*;
import com.loopj.android.http.*;
import com.loopj.android.http.RequestParams;

import java.util.Iterator;

import cz.msebera.android.httpclient.Header;

import static de.htw_berlin.bischoff.daniel.wikiscout.R.id.map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;
    protected Location mLastMarkerUpdateLocation;
    protected LocationRequest mLocationRequest;
    protected GoogleMap mMap;
    protected boolean mapReady = false;
    protected boolean googleApiClientReady = false;

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 511;
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar Toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(Toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mapFragment.getMapAsync(this);

        buildGoogleApiClient();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mapReady = true;
        mMap = googleMap;

        mGoogleApiClient.connect();
        enableMyLocationIcon();
        // addPlaceholderMarkers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocationIcon();
                    startLocationUpdates();
                }
            }
        }
    }

    protected void addPlaceholderMarkers() {
        LatLng pos1 = new LatLng(52.3924, 13.480);
        Marker marker1 = mMap.addMarker(new MarkerOptions()
                .position(pos1)
                .title("Interessant!")
                .snippet("Hier wurden kürzlich 463234 Grashalme gezählt."));

        LatLng pos2 = new LatLng(52.3994, 13.490);
        Marker marker2 = mMap.addMarker(new MarkerOptions()
                .position(pos2)
                .title("Interessant!")
                .snippet("Hier wurden kürzlich 463234 Grashalme gezählt."));

        LatLng pos3 = new LatLng(52.4054, 13.5178);

        Marker marker3 = mMap.addMarker(new MarkerOptions()
                .position(pos3)
                .title("Interessant!")
                .snippet("Hier wurden kürzlich 463234 Grashalme gezählt."));
    }

    public void addMarker(double lat, double lon, String title, String description) {
        // System.out.println("New marker: " + title + " " + lat + " " + lon);
        // System.out.println("Map and google client ready: " + (mapReady && googleApiClientReady));

        LatLng pos = new LatLng(lat, lon);
        mMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(title)
                .snippet(description != null ? description: ""));
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void enableMyLocationIcon() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    private void moveToCurrentLocation() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 14));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        googleApiClientReady = true;
        createLocationRequest();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        if (mCurrentLocation != null) {
            moveToCurrentLocation();

            updateMarker();
        }

        startLocationUpdates();
    }

    protected void updateMarker() {
        try {
            getEntries(String.valueOf(mCurrentLocation.getLatitude()), String.valueOf(mCurrentLocation.getLongitude()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if ((mLastMarkerUpdateLocation != null) && (mLastMarkerUpdateLocation.distanceTo(location) >= 500)) {
            updateMarker();
        }

        mCurrentLocation = location;
        moveToCurrentLocation();
    }

    public void getEntries(String lat, String lon) throws JSONException {

        RequestParams params = new RequestParams();
        params.put("action", "query");
        params.put("prop", "coordinates|pageimages|pageterms");
        params.put("colimit", "50");
        params.put("piprop", "thumbnail");
        params.put("pithumbsize", "144");
        params.put("pilimit", "50");
        params.put("generator", "geosearch");
        params.put("ggscoord", lat + "|" + lon);
        params.put("ggsradius", "2000");
        params.put("ggslimit", "20");
        params.put("format", "json");
        params.put("wbptterms", "description");

        WikiRestClient.get("/", params, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray

                mLastMarkerUpdateLocation = mCurrentLocation;

                try {
                    JSONObject entries = response.getJSONObject("query").getJSONObject("pages");

                    System.out.println("Response: " + entries);
                    System.out.println("Entries: " + entries.names().length());
                    // System.out.println(entries.names());

                    for (int i = 0; i < entries.names().length(); i++) {
                        String key = entries.names().getString(i);
                        JSONObject entry = entries.getJSONObject(key);

                        System.out.println(entry);

                        System.out.println("lat: " + entry.getJSONArray("coordinates").getJSONObject(0).getDouble("lat"));
                        System.out.println("lon: " + entry.getJSONArray("coordinates").getJSONObject(0).getDouble("lon"));
                        System.out.println("title: " + entry.getString("title"));
                        // System.out.println("description: " + entry.getJSONObject("terms").getJSONArray("description").getString(0));

                        double lat = entry.getJSONArray("coordinates").getJSONObject(0).getDouble("lat");
                        double lon = entry.getJSONArray("coordinates").getJSONObject(0).getDouble("lon");
                        String title = entry.getString("title");
                        String description = entry.getJSONObject("terms").getJSONArray("description").getString(0);

                        System.out.println("Values: " + lat + " " + lon + " " + title + " " + description);

                        addMarker(lat, lon, title, description != null ? description : null);
                        System.out.println(entry);
                    }
                } catch (JSONException e) {
                    // say something
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                System.out.println("status code: " + statusCode);
                System.out.println("headers: " + headers);
                System.out.println("json: " + errorResponse);
            }
        });
    }
}
