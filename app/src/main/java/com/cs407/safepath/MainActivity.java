package com.cs407.safepath;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    SharedPreferences sp;

    private int mapType;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    PlacesClient placesClient;

    // For getting user location
    private final int FINE_PERMISSION_CODE = 1;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    // I made a google maps api key that has all the stuff enabled if needed -- Aidan
    private final String Api_Key = "AIzaSyCN5H7DS_wiFX29U-tgHTaZhToyi5VpfUU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Getting user's location / requesting location permission
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        // SETTINGS PREFERENCES SET UP
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("SETTINGS MAIN MESSAGE: ", String.valueOf(sp.getInt("radius", 0))); //example: gets Danger Radius value
        //mapType = Integer.valueOf(sp.getString("basemap", null));

        // Initializing Places AutoComplete Fragment
        if(!Places.isInitialized()) Places.initialize(getApplicationContext(), Api_Key);
        placesClient = Places.createClient(this);
        final AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autoCompleteFragment);

        // Setting up what properties we want when clicking on autocomplete results
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS));
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {

            // When we cannot find the selected place
            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, "Error with autocomplete thing", Toast.LENGTH_LONG).show();
            }

            // What we do with the selected location from the search bar
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                Log.i("PlacesAPI", "" + latLng.latitude + "\n" + latLng.longitude);

                mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
                //mMap.addPolyline(new PolylineOptions().add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), latLng));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15)); //todo: zoom changed based on convenience

                LatLng origin = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                //START DOWNLOADING ROUTES JSON DATA FROM GOOGLE
                String url = getDirectionsUrl(origin, latLng); //Form URL for google download
                DownloadTask downloadTask = new DownloadTask();
                downloadTask.execute(url);
            }
        });

        // Settings and Flagging buttons on the main screen
        FloatingActionButton fab1 = findViewById(R.id.settingsButton);
        Button fab2 = findViewById(R.id.flagButton);
        fab1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });

        fab2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFlaggingScreen();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("tag", "onMapReady called");
        mMap = googleMap;

        // Current Coordinates of User
        LatLng usrPos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        // Load in danger zones if any
        try {
            Intent intent = getIntent();
            String[] markerArr = intent.getStringArrayExtra("markerArr");
            for (int i = 0; i < markerArr.length; i++) {

                // Pull lat and lon from each element in the array
                double lat = Double.parseDouble(markerArr[i].substring(0, markerArr[i].indexOf(",")));
                double lon = Double.parseDouble(markerArr[i].substring(markerArr[i].indexOf(",") + 1));
                LatLng dZoneLatLng = new LatLng(lat, lon);

                // Draw the danger zone on the map
                mMap.addMarker(new MarkerOptions().position(dZoneLatLng).title("Danger Zone"));
                mMap.addCircle(new CircleOptions()
                        .center(dZoneLatLng)
                        .radius(200)
                        .strokeColor(Color.RED));
            }
        } catch (Exception e) {
            // no danger zone coord array yet
            Log.i("CATCH CALLED", e.toString());
        }

        // Setting the camera & marker
        mMap.addMarker(new MarkerOptions().position(usrPos).title("My Location"));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(usrPos, 14));

        //test
        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(usrPos, (float)0.5));
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);


        //enable preferences listener for settings -- ismail
        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (key.equals("basemap")){
                    mapType = Integer.valueOf(sp.getString("basemap", null));
                    mMap.setMapType(mapType);
                }
            }
        };
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    // Settings screen
    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    // Flagging screen
    public void openFlaggingScreen() {
        Intent intent = new Intent(this, FlaggingActivity.class);

        // Passing user's location to flagging activity
        intent.putExtra("latitude", currentLocation.getLatitude());
        intent.putExtra("longitude", currentLocation.getLongitude());

        startActivity(intent);
    }

    private void getLastLocation() {
        Log.i("tag", "getLastLocation called");
        // Permission check
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;
                    Log.i("Location Found", "Lat:" + currentLocation.getLatitude() + "Lng:" + currentLocation.getLongitude());
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
    }

    // Requesting Location
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                Toast.makeText(this, "Location Permission Denied \n " +
                        "Please allow this permission so that the app can function correctly", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Download Tasks and Routes Data
    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsParser parser = new DirectionsParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=walking";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters +
                "&key=" + "AIzaSyBPP0y_iqwO9CW0Meb83aGZQ_F6b6R2zzw";


        return url;
    }

    /**
     * Downloads JSON data from a given URL.
     *
     * @param strUrl The URL to download JSON data from.
     * @return A string containing the downloaded JSON data.
     * @throws IOException If an I/O exception occurs during the download.
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return data;
    }
}
