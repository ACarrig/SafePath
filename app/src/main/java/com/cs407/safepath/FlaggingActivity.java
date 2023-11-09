package com.cs407.safepath;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class FlaggingActivity extends AppCompatActivity {

    private GoogleMap mMap;

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
        });

    }

}