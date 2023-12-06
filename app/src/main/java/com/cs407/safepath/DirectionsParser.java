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
    public List<List<HashMap<String, String>>> parse(JSONObject jObject, LatLng[] latLngArr, int dZoneCount) {
        List<List<HashMap<String, String>>> routes = new ArrayList<>();

        JSONArray jRoutes = null;
        JSONArray jLegs = null;
        JSONArray jSteps = null;

        boolean next = false; // added to check when route passes through danger zone, marks if we should move to next route

        try {
            jRoutes = jObject.getJSONArray("routes");

            // Traversing all routes
            for (int i = 0; i < jRoutes.length(); i++) { // This loop only gets run once...only 1 route being created?
                next = false; //todo: del?

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
                            //hm.put("lat", Double.toString(list.get(l).latitude));
                            //hm.put("lng", Double.toString(list.get(l).longitude));

                            // todo: test distance func here -------------------------------------------------------------

                            // WHAT THIS SEEMS TO DO IS NOT ROUTE THROUGH THE DANGER ZONE, BUT
                            // IT WILL STILL CONNECT THE POLYLINE THROUGH THE DANGER ZONE IF
                            // THE ROUTE CONTINUES ON THE OTHER SIDE OF IT AS OPPOSED TO
                            // RE-ROUTING A COMPLETE PATH AROUND IT AS IT SHOULD

                            // Comparing the traversal points to our danger zones
                            if(dZoneCount == 0) path.add(hm);
                            for(int m = 0; m < dZoneCount; m++) {
                                double dzLat = latLngArr[m].latitude;
                                double dzLon = latLngArr[m].longitude;

                                if(distance(dzLat, dzLon, list.get(l).latitude, list.get(l).longitude) > 200) {
                                    hm.put("lat", Double.toString(list.get(l).latitude));
                                    hm.put("lng", Double.toString(list.get(l).longitude));
                                    path.add(hm);
                                } else {
                                    next = true;
                                    break;
                                }
                            }
                            //path.add(hm); //todo: uncomment ?---------------------------------------------------
                            if (next) break;
                        }
                        if (next) break;
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

}