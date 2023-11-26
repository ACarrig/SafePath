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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flagging);

        Intent intent = getIntent();
        Double usrLat = intent.getDoubleExtra("latitude", 0);
        Double usrLon = intent.getDoubleExtra("longitude", 0);
        LatLng usrPos = new LatLng(usrLat, usrLon);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(googleMap -> {
            mMap = googleMap;

            // display & zoom in on marker
            mMap.addMarker(new MarkerOptions().position(usrPos).title("My Location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(usrPos, 10));

            mMap.getUiSettings().setZoomControlsEnabled(true);
            mMap.getUiSettings().setCompassEnabled(true);

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {

                    mMap.addMarker(new MarkerOptions().position(point).title("Touch Location"));
                    //addPulsatingEffect(point);

                    mMap.addCircle(new CircleOptions()
                            .center(point)
                            .radius(200)
                            .strokeColor(Color.RED));

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
        startActivity(intent);
    }



}