package eventplanner.contrivers.com.eventplanner.utils;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import eventplanner.contrivers.com.eventplanner.model.Plan;
import eventplanner.contrivers.com.eventplanner.model.Route;

import static java.lang.Double.parseDouble;

public class Util {
    public LatLng createFromString(String location) {
        String[] locations = location.split(",");
        return new LatLng(parseDouble(locations[0]), parseDouble(locations[1]));
    }

    public List<LatLng> planDelta(Plan plan, Plan changedPlan) {
        List<LatLng> deltaLocation = new ArrayList<>();
        for (int i = 0; i < plan.persons.size(); i++) {
            if (!plan.persons.get(i).location.equalsIgnoreCase(changedPlan.persons.get(i).location)) {
                deltaLocation.add(createFromString(plan.name));
            }
        }
        return deltaLocation;
    }

    public List<LatLng> locationsOf(Plan plan) {
        List<LatLng> locations = new ArrayList<>();
        for (int i = 0; i < plan.persons.size(); i++) {
            locations.add(createFromString(plan.name));
        }
        return locations;
    }

    public Iterable<LatLng> getPointsOf(List<Route> routes) {
        List<LatLng> points = new ArrayList<>();
        for (Route route : routes) {
            points.add(route.getStart());
        }
        return points;
    }

    public String getString(Location location) {
        return "" + location.getLatitude() + "," + location.getLongitude();
    }
}
