package com.cs407.safepath;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.List;

public class RoutesKVPair {

    private List<List<HashMap<String, String>>> routes = null; // Route object from parsing
    private int valid; // Int representing route validity (0 means valid route, 1 means not valid)
    private LatLng lastCoord; // The last coordinates of our route

    public RoutesKVPair(List<List<HashMap<String, String>>> routes, int valid, LatLng lastCoord) {
        this.routes = routes;
        this.valid = valid;
        this.lastCoord = lastCoord;
    }

    // This is our normal route object from before
    //     When we have a valid route, we will use
    //     this getRoutes method for getting the route
    //     needed to draw our polyLine
    public List<List<HashMap<String, String>>> getRoutes() {
        return this.routes;
    }

    // Returns 0 if valid route, 1 if not
    //     A valid route is a route that
    //     goes from start to finish
    //     w/o entering a danger zone
    public int isValidRoute() {
        return this.valid;
    }

    // Returns the last coordinates we get from our route
    //     This is useful for when we have an invalid route and
    //     Need to calculate a waypoint based on where we stopped
    public LatLng getLastCoord() {
        return this.lastCoord;
    }

}
