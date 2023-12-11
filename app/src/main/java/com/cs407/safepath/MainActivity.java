package com.cs407.safepath;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.location.Address;
import android.location.Geocoder;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    SharedPreferences sp;
    private int circleRadius;
    private int mapType;
    private boolean waypointaltRoute = false;
    private LatLng destination;
    private LatLng circleCenter;
    private LatLng topCenter;
    private LatLng topRight1;
    private LatLng topRight2;
    private LatLng Right;
    private LatLng bottomRight1;
    private LatLng bottomRight2;
    private LatLng Bottom;
    private LatLng bottomLeft1;
    private LatLng bottomLeft2;
    private LatLng Left;
    private LatLng topLeft1;
    private LatLng topLeft2;


    private DBHelper dbHelper;

    private Context context;

    private int failCount;
    private String[] rerouteMarkerArr;


    private SharedPreferences.OnSharedPreferenceChangeListener listener;
    PlacesClient placesClient;

    // For getting user location
    private final int FINE_PERMISSION_CODE = 1;
    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    // I made a google maps api key that has all the stuff enabled if needed -- Aidan
    private final String Api_Key = "AIzaSyCN5H7DS_wiFX29U-tgHTaZhToyi5VpfUU";

    // To hold center points for the danger zones
    LatLng[] latLngArr = new LatLng[100];
    private int dZoneCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize database for saving routes
        context = getApplicationContext();
        dbHelper = new DBHelper(context);
        if (dbHelper.getNotesCount() > 5 ) {
            dbHelper.deleteAllNotes();
        }

        // Getting user's location / requesting location permission
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();

        // SETTINGS PREFERENCES SET UP
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        circleRadius = sp.getInt("radius", 0);

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

                // Saving route to database -- ismail

                Geocoder gCoder = new Geocoder(context);
                String from = "";
                ArrayList<Address> addresses = null;
                try {
                    addresses = (ArrayList<Address>) gCoder.getFromLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), 1);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (addresses != null && addresses.size() > 0) {
                    from = addresses.get(0).getAddressLine(0);
                    if (from == null) {
                        from = "ERROR";
                    }
                }
                DateFormat dateFormat = new SimpleDateFormat("MM/DD/YYYY HH:mm:ss");
                String date = dateFormat.format(new Date());
                Note note1 = new Note(from, date, "x miles", place.getAddress());
                dbHelper.addNote(note1);
                //

                LatLng latLng = place.getLatLng();
                destination = place.getLatLng(); //save final destination
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

        // Load in danger zones, if any
        try {
            Intent intent = getIntent();
            String[] markerArr = intent.getStringArrayExtra("markerArr");
            rerouteMarkerArr = markerArr;
            for (int i = 0; i < markerArr.length; i++) {

                // Pull lat and lon from each element in the array
                double lat = Double.parseDouble(markerArr[i].substring(0, markerArr[i].indexOf(",")));
                double lon = Double.parseDouble(markerArr[i].substring(markerArr[i].indexOf(",") + 1));
                LatLng dZoneLatLng = new LatLng(lat, lon);
                circleCenter = new LatLng(dZoneLatLng.latitude, dZoneLatLng.longitude);

                // Draw the danger zone on the map
                mMap.addMarker(new MarkerOptions().position(dZoneLatLng).title("Danger Zone"));
                mMap.addCircle(new CircleOptions()
                        .center(dZoneLatLng)
                        .radius(circleRadius)
                        .strokeColor(Color.RED));

                // Add LatLng to array (for comparisons when drawing path)
                latLngArr[i] = dZoneLatLng;
                dZoneCount += 1;

                /** //  Calculate waypoints surrounding danger zone radius for possible need to reroute.
                double[][] waypoints = waypointCalculator(circleCenter.latitude, circleCenter.longitude, circleRadius);
                topCenter = new LatLng(waypoints[0][0],waypoints[0][1]);
                topRight1 = new LatLng(waypoints[1][0],waypoints[1][1]);
                topRight2 = new LatLng(waypoints[2][0],waypoints[2][1]);
                Right = new LatLng(waypoints[3][0],waypoints[3][1]);
                bottomRight1 = new LatLng(waypoints[4][0],waypoints[4][1]);
                bottomRight2 = new LatLng(waypoints[5][0],waypoints[5][1]);
                Bottom = new LatLng(waypoints[6][0],waypoints[6][1]);
                bottomLeft1 = new LatLng(waypoints[7][0],waypoints[7][1]);
                bottomLeft2 = new LatLng(waypoints[8][0],waypoints[8][1]);
                Left = new LatLng(waypoints[9][0],waypoints[9][1]);
                topLeft1 = new LatLng(waypoints[10][0],waypoints[10][1]);
                topLeft2 = new LatLng(waypoints[11][0],waypoints[11][1]); */

            }
        } catch (Exception e) {
            // no danger zone coord array yet (catch reached first time loading main screen)
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
                circleRadius = sp.getInt("radius", 0);
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
        intent.putExtra("radius", circleRadius);

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
        @SuppressLint("WrongThread")
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsParser parser = new DirectionsParser();

                // Find a route to our destination
                RoutesKVPair routeKVPair; // This is a new class I made to help w/ waypoint

                routeKVPair = parser.parse(jObject, latLngArr, dZoneCount, circleRadius);
                routes = routeKVPair.getRoutes();

                // If route is not valid (goes through danger zone)
                if (routeKVPair.isValidRoute() == 1) {
                    waypointaltRoute = true;

                    failCount += 1;

                    for(int d = 0; d < dZoneCount; d++) {
                        // Pull lat and lon from each element in the array
                        double lat = Double.parseDouble(rerouteMarkerArr[d].substring(0, rerouteMarkerArr[d].indexOf(",")));
                        double lon = Double.parseDouble(rerouteMarkerArr[d].substring(rerouteMarkerArr[d].indexOf(",") + 1));
                        LatLng dZoneLatLng = new LatLng(lat, lon);
                        circleCenter = new LatLng(dZoneLatLng.latitude, dZoneLatLng.longitude);

                        //  Calculate waypoints surrounding danger zone radius for possible need to reroute.
                        double[][] waypoints = waypointCalculator(circleCenter.latitude, circleCenter.longitude, circleRadius, failCount);
                        topCenter = new LatLng(waypoints[0][0],waypoints[0][1]);
                        topRight1 = new LatLng(waypoints[1][0],waypoints[1][1]);
                        topRight2 = new LatLng(waypoints[2][0],waypoints[2][1]);
                        Right = new LatLng(waypoints[3][0],waypoints[3][1]);
                        bottomRight1 = new LatLng(waypoints[4][0],waypoints[4][1]);
                        bottomRight2 = new LatLng(waypoints[5][0],waypoints[5][1]);
                        Bottom = new LatLng(waypoints[6][0],waypoints[6][1]);
                        bottomLeft1 = new LatLng(waypoints[7][0],waypoints[7][1]);
                        bottomLeft2 = new LatLng(waypoints[8][0],waypoints[8][1]);
                        Left = new LatLng(waypoints[9][0],waypoints[9][1]);
                        topLeft1 = new LatLng(waypoints[10][0],waypoints[10][1]);
                        topLeft2 = new LatLng(waypoints[11][0],waypoints[11][1]);
                    }

                    LatLng lastCoords = routeKVPair.getLastCoord();
                    Log.i("TESTING", "NOT VALID ROUTE FROM ROUTEKVPAIR (MAIN)" + lastCoords.toString());

                    //Calculates the needed waypoints around the perimeter of the circle depending on the situation
                    LatLng[] newWaypoint = calcWaypoint(lastCoords);

                    //Now need to re-call directions parsing/download with given waypoints
                    //START DOWNLOADING ROUTES JSON DATA FROM GOOGLE
                    String url = getDirectionsUrl(lastCoords, destination, newWaypoint); //Form URL for google download
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        public LatLng[] calcWaypoint(LatLng lastCoord) {
            //Check which way the route would be approaching the zone and make a waypoint accordingly
            LatLng[] waypoints = null;
            if (lastCoord.latitude < circleCenter.latitude) {
                //Coming from the left up into DZ
                if (lastCoord.longitude < circleCenter.longitude) {
                    //return set of waypoints - RA to Implement
                    waypoints = new LatLng[5];
                    waypoints[0] = Bottom;
                    waypoints[1] = bottomRight2;
                    waypoints[2] = bottomRight1;
                    waypoints[3] = Right;
                    waypoints[4] = topRight2;
                }
                //Coming from right up into DZ
                if (lastCoord.longitude > circleCenter.longitude) {
                    //return set of waypoints
                    waypoints = new LatLng[5];
                    waypoints[0] = Bottom;
                    waypoints[1] = bottomLeft1;
                    waypoints[2] = bottomLeft2;
                    waypoints[3] = Left;
                    waypoints[4] = topLeft1;
                }
            }
            if (lastCoord.latitude > circleCenter.latitude) {
                //Coming from top left into DZ
                if (lastCoord.longitude < circleCenter.longitude) {
                    //return set of waypoints
                    waypoints = new LatLng[5];
                    waypoints[0] = topCenter;
                    waypoints[1] = topRight1;
                    waypoints[2] = topRight2;
                    waypoints[3] = Right;
                    waypoints[4] = bottomRight1;
                }
                //Coming from top right into DZ
                if (lastCoord.longitude > circleCenter.longitude) {
                    //return set of waypoints
                    waypoints = new LatLng[5];
                    waypoints[0] = topCenter;
                    waypoints[1] = topLeft2;
                    waypoints[2] = topLeft1;
                    waypoints[3] = Left;
                    waypoints[4] = bottomLeft2;
                }
                }
            return waypoints;
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

                    if (j == path.size() - 1) {
                        notifyArrival();
                    }
                    // Loop over all danger zones
                    for (int q = 0; q < dZoneCount; q++) {
                        if(distance(latLngArr[q].latitude, latLngArr[q].longitude, lat, lng) > circleRadius) {
                            // Only add the points that aren't in the dangerous area
                            points.add(position);
                        }
                        else if (distance(latLngArr[q].latitude, latLngArr[q].longitude, lat, lng) <= circleRadius) {
                            notifyDangerZone();
                        }
                    }
                    // Always add the point if no danger zone to worry about
                    if(dZoneCount == 0) points.add(position);
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
    private String getDirectionsUrl(LatLng origin, LatLng dest, LatLng[] waypoint) { //copy of class to accept waypoints

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        //Waypoint to avoid the marker
       // String way_pt = "waypoints=" + waypoint.latitude + "," + waypoint.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=walking";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + "waypoints=optimize:true|" +
                waypoint[0].latitude + "," + waypoint[0].longitude + "|" + waypoint[1].latitude + "," + waypoint[1].longitude + "|" +
                waypoint[2].latitude + "," + waypoint[2].longitude + "|" + waypoint[3].latitude + "," + waypoint[3].longitude + "|" +
                waypoint[4].latitude + "," + waypoint[4].longitude + "|&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters +
                "&key=" + "AIzaSyBPP0y_iqwO9CW0Meb83aGZQ_F6b6R2zzw";

        Log.i("Route with Waypoints", url);


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

    // Find the distance (in meters) between two lat-lon pairs
    public static double distance(double lat1, double lon1, double lat2, double lon2) {

        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return distance;
    }
    private static final double EARTH_RADIUS_M = 6371000.00;

    /**
     * Calculates the 8 perimeter waypoints around the given danger zone.
     * Returns them in an array in a clockwise order starting from the top of the circle.
     *
     * @param centerLat
     * @param centerLon
     * @param radiusInMeters
     * @return waypoints - the 2d array holding all 12 waypoints.
     */
    public static double[][] waypointCalculator(double centerLat, double centerLon, double radiusInMeters, int failCount) {
        // Convert center coordinates to radians
        double centerLatRad = Math.toRadians(centerLat);
        double centerLonRad = Math.toRadians(centerLon);

        // Calculate angular distance in radians
        double angularDistance = (radiusInMeters + (50 * failCount)) / EARTH_RADIUS_M;

        // Calculate coordinates for 12 points on the perimeter
        double[][] waypoints = new double[12][2];

        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30.0); // Points spaced evenly at 30-degree intervals

            double newLatRad = centerLatRad + angularDistance * Math.sin(angle);
            double newLonRad = centerLonRad + angularDistance * Math.cos(angle) / Math.cos(centerLatRad);

            // Convert back to degrees
            waypoints[i][0] = Math.toDegrees(newLatRad);
            waypoints[i][1] = Math.toDegrees(newLonRad);
        }

        return waypoints;
    }
    private void notifyArrival() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "You have arrived at your destination!", Toast.LENGTH_SHORT).show());
    }

    private void notifyDangerZone() {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "You are close to a danger zone!", Toast.LENGTH_SHORT).show());
    }

}
