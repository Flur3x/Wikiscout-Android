package de.htw_berlin.bischoff.daniel.wikiscout;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
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
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.Arrays;

import cz.msebera.android.httpclient.Header;

import static de.htw_berlin.bischoff.daniel.wikiscout.R.id.map;

public class MainActivity extends RuntimePermissionsActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, WikiEntryFragment.OnFragmentInteractionListener {

    private GoogleApiClient mGoogleApiClient;
    private Location mCurrentLocation;
    private Location mLastMarkerUpdateLocation;
    private GoogleMap mMap;
    private boolean mapReady;
    private FusedLocationProviderApi fusedLocationProviderApi;
    private LocationRequest mLocationRequest;

    private static final int DEFAULT_ZOOM = 15;
    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar Toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(Toolbar);

        WikiRestClient.setup();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mapFragment.getMapAsync(this);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
            }
        } else {
            buildGoogleApiClient();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mapReady = true;

        //enableMyLocationIcon
        //moveToCurrentLocation();
        //updateMarkers();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getLocation();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            getLocation();
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                System.out.println("CLICKED ON INFO WINDOW: " + marker.getTitle());

                String wikiPageTitle = marker.getTitle();

                if (findViewById(R.id.fragment_container) == null) {
                    System.out.println("Call activity");

                    Intent intent = new Intent(getApplicationContext(), WikiEntryActivity.class);

                    intent.putExtra("wikiPageTitle", wikiPageTitle);
                    startActivity(intent);
                } else {
                    System.out.println("Call fragment");

                    WikiEntryFragment fragment = new WikiEntryFragment();
                    Bundle bundle = new Bundle();

                    bundle.putString("wikiPageTitle", wikiPageTitle);
                    fragment.setArguments(bundle);

                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
                }
            }
        });
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        switch (requestCode) {
            case PermissionCodes.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }
                    mMap.setMyLocationEnabled(true);
                }
            }
        }
    }

    protected void addPlaceholderMarkers() {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.3924, 13.480))
                .title("Interessant!")
                .snippet("Hier wurden kürzlich 463234 Grashalme gezählt."));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.3994, 13.490))
                .title("Interessant!")
                .snippet("Hier wurden kürzlich 463234 Grashalme gezählt."));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(52.4054, 13.5178))
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
                .snippet(description != null ? description : ""));
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    protected void onStart() {
        super.onStart();
    }

    protected void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    protected void getLocation() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationProviderApi = LocationServices.FusedLocationApi;

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();
    }



    private void moveToCurrentLocation() {
        if (mCurrentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), DEFAULT_ZOOM));
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

            Location lastLocation = fusedLocationProviderApi.getLastLocation(mGoogleApiClient);

            if (lastLocation != null) {
                onLocationChanged(lastLocation);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PermissionCodes.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    protected void updateMarkers() {
        if (mCurrentLocation != null) {
            try {
                getEntries(String.valueOf(mCurrentLocation.getLatitude()), String.valueOf(mCurrentLocation.getLongitude()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Location unknown. Markers couldn't load. :(");
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
        mCurrentLocation = location;

        System.out.println("LOCATION: " + mCurrentLocation);

        if (mapReady) {
            if (mLastMarkerUpdateLocation == null || mLastMarkerUpdateLocation.distanceTo(location) >= 500) {
                updateMarkers();
            }

            // System.out.println("Distance: " + mLastMarkerUpdateLocation.distanceTo(location));

            moveToCurrentLocation();
        }
    }

    public void getEntries(String lat, String lon) throws JSONException {

        RequestParams params = new RequestParams();
        params.put("action", "query");
        params.put("prop", "coordinates|pageterms");
        params.put("colimit", "50");
        params.put("pithumbsize", "144");
        params.put("pilimit", "50");
        params.put("generator", "geosearch");
        params.put("ggscoord", lat + "|" + lon);
        params.put("ggsradius", "3000");
        params.put("ggslimit", "50");
        params.put("format", "json");
        params.put("wbptterms", "description");

        JsonHttpResponseHandler handler = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                mLastMarkerUpdateLocation = mCurrentLocation;

                try {
                    JSONObject entries = response.getJSONObject("query").getJSONObject("pages");

                    System.out.println("Response: " + entries);
                    System.out.println("Entries: " + entries.names().length());
                    // System.out.println(entries.names());

                    for (int i = 0; i < entries.names().length(); i++) {
                        String key = entries.names().getString(i);
                        JSONObject entry = entries.getJSONObject(key);
                        JSONObject terms = entry.optJSONObject("terms");
                        JSONArray coordinates = entry.optJSONArray("coordinates");

                        String wikiPageTitle;
                        double lat = 0.0;
                        double lon = 0.0;
                        String description = null;

                        wikiPageTitle = entry.optString("title");

                        if ((coordinates != null) && (coordinates.optJSONObject(0) != null)) {
                            lat = coordinates.getJSONObject(0).optDouble("lat");
                            lon = coordinates.getJSONObject(0).optDouble("lon");

                            System.out.println("lat: " + lat);
                            System.out.println("lon: " + lon);
                        }

                        if ((terms != null) && (terms.optJSONArray("description") != null)) {
                            description = terms.optJSONArray("description").optString(0);

                            System.out.println("description : " + description);
                        }

                        if (wikiPageTitle != null && (lat != 0.0) && (lon != 0.0)) {
                            addMarker(lat, lon, wikiPageTitle, description);
                            System.out.println(entry);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                System.out.println("status code: " + statusCode);
                System.out.println("headers: " + Arrays.toString(headers));
                System.out.println("json: " + errorResponse);
            }
        };

        if (lat != null && lon != null) {
            WikiRestClient.get("/", params, handler);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
