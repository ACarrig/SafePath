package com.cs407.safepath;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class parses JSON data from the Google Maps Directions API and extracts latitude
 * and longitude information to represent a route.
 */
public class DirectionsParser {

    /**
     * Receives a JSONObject and returns a list of lists containing latitude and longitude.
     */
    public RoutesKVPair parse(JSONObject jObject, LatLng[] latLngArr, int dZoneCount, int circleRadius) {
        List<List<HashMap<String, String>>> routes = new ArrayList<>();

        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        LatLng finalLL = null; // The last coordinates of our route

        try {
            jRoutes = jObject.getJSONArray("routes");

            // Traversing all routes
            for (int i = 0; i < jRoutes.length(); i++) { // This loop only gets run once...only 1 route being created?

                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List<HashMap<String, String>> path = new ArrayList<>();

                // Traversing all legs
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    // Traversing all steps
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        // Traversing all points
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<>();

                            // Comparing the traversal points to our danger zones
                            if(dZoneCount == 0) {
                                hm.put("lat", Double.toString(list.get(l).latitude));
                                hm.put("lng", Double.toString(list.get(l).longitude));
                                path.add(hm); // if no danger zones path is safe to add
                            }

                            // Loop over danger zones to check for all possible conflicts
                            for(int m = 0; m < dZoneCount; m++) {
                                // Coordinates for the center of the danger zone
                                double dzLat = latLngArr[m].latitude;
                                double dzLon = latLngArr[m].longitude;

                                // Points from the leg of the route we are checking
                                double lastLat = list.get(l).latitude;
                                double lastLon = list.get(l).longitude;

                                // checking if point in the path is outside of the dangerous area (distance > radius)
                                double distance = distance(dzLat, dzLon, lastLat, lastLon); //* 3.28084; // Convert to feet
                                if(distance > (circleRadius * 1.125)) {
                                    hm.put("lat", Double.toString(list.get(l).latitude));
                                    hm.put("lng", Double.toString(list.get(l).longitude));
                                    path.add(hm);

                                    // Current last point of our route
                                    finalLL = new LatLng(lastLat, lastLon);
                                } else { // Else when coords we are checking are too close to danger zone
                                    Log.i("TESTING", "Distance was less than boosted circle radius");
                                    routes.add(path);
                                    // If we never got any coords in our route before it failed
                                    // then we don't have any reference for our waypoint
                                    // todo: This case should not occur unless user starts in danger zone
                                    //  mostly just an edge case to prevent crashing
                                    if (finalLL == null) {
                                        Log.i("TESTING", "finalLL was null");
                                        finalLL = new LatLng(0, 0);
                                        return new RoutesKVPair(routes, 1, finalLL);

                                    } else { // we use whatever coords were last set to be finalLL
                                        Log.i("TESTING", "finalLL was not null");
                                        return new RoutesKVPair(routes, 1, finalLL);
                                    }
                                }
                            }
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // Handle other exceptions if necessary
        }
        Log.i("ROUTES LENGTH: ", "" + routes.size() + " routes");

        // If reached, we have a valid route and therefore a RouteKVPair with validity 0
        //     *Note: if everything works well and we get a route, won't need finalLL
        RoutesKVPair route = new RoutesKVPair(routes, 0, finalLL);
        return route;
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));

            poly.add(p);
        }

        return poly;
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

    public List<List<HashMap<String, String>>> parseOLD(JSONObject jObject, LatLng[] latLngArr, int dZoneCount, int circleRadius) {
        List<List<HashMap<String, String>>> routes = new ArrayList<>();

        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        try {
            jRoutes = jObject.getJSONArray("routes");

            // Traversing all routes
            for (int i = 0; i < jRoutes.length(); i++) { // This loop only gets run once...only 1 route being created?

                jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                List<HashMap<String, String>> path = new ArrayList<>();

                // Traversing all legs
                for (int j = 0; j < jLegs.length(); j++) {
                    jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                    // Traversing all steps
                    for (int k = 0; k < jSteps.length(); k++) {
                        String polyline = "";
                        polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePoly(polyline);

                        // Traversing all points
                        for (int l = 0; l < list.size(); l++) {
                            HashMap<String, String> hm = new HashMap<>();

                            // Comparing the traversal points to our danger zones
                            if(dZoneCount == 0) {
                                hm.put("lat", Double.toString(list.get(l).latitude));
                                hm.put("lng", Double.toString(list.get(l).longitude));
                                path.add(hm); // if no danger zones path is safe to add
                            }

                            // Loop over danger zones to check for all possible conflicts
                            for(int m = 0; m < dZoneCount; m++) {
                                // Coordinates for the center of the danger zone
                                double dzLat = latLngArr[m].latitude;
                                double dzLon = latLngArr[m].longitude;

                                // Current coords from the leg of the route we are checking
                                double lastLat = list.get(l).latitude;
                                double lastLon = list.get(l).longitude;

                                // checking if point in the path is outside of the dangerous area (distance > radius)
                                double distance = distance(dzLat, dzLon, lastLat, lastLon); //* 3.28084; // Convert to feet
                                if(distance > (circleRadius * 1.1)) {
                                    hm.put("lat", Double.toString(lastLat));
                                    hm.put("lng", Double.toString(lastLon));
                                    path.add(hm);
                                } else { // Else when coords we are checking are too close to danger zone
                                    return routes;
                                }
                            }
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // Handle other exceptions if necessary
        }
        Log.i("ROUTES LENGTH: ", "" + routes.size() + " routes");
        return routes;
    }

}