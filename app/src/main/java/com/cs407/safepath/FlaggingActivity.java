package com.cs407.safepath;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class FlaggingActivity extends AppCompatActivity {

    private GoogleMap mMap;
    private Circle lastUserCircle;

    private int circleRadius;

    // Array with marker / danger zone center coordinates
    String[] markerArr = new String[100];
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flagging);

        Intent intent = getIntent();
        Double usrLat = intent.getDoubleExtra("latitude", 0);
        Double usrLon = intent.getDoubleExtra("longitude", 0);
        circleRadius = intent.getIntExtra("radius", 0);
        LatLng usrPos = new LatLng(usrLat, usrLon);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;

            // display & zoom in on user's location marker
            mMap.addMarker(new MarkerOptions().position(usrPos).title("My Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(usrPos, 10));

            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);

            // adding marker with a radius around it AKA "Danger Zone"
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {

                    mMap.addMarker(new MarkerOptions().position(point).title("Danger Zone"));
                    //addPulsatingEffect(point);

                    mMap.addCircle(new CircleOptions()
                            .center(point)
                            .radius(circleRadius)
                            .strokeColor(Color.RED));

                    // Adding the marker data to an array
                    String markerLatLng = point.toString(); // of form 'lat/lng: (37.338994748292315,-121.98496162891387)'
                    int leftP = markerLatLng.indexOf("(");
                    int rightP = markerLatLng.indexOf(")");
                    markerLatLng = markerLatLng.substring(leftP + 1, rightP); // just the numbers & comma
                    markerArr[count] = markerLatLng; // adding 'lat-coord,lon-coord' to array
                    count += 1;
                }
            });

        });

        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnToMainActivity();
            }
        });

    }

    public void returnToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("markerArr", markerArr);
        startActivity(intent);
    }

}