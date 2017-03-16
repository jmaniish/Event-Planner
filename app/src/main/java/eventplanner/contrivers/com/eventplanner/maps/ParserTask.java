package eventplanner.contrivers.com.eventplanner.maps;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import eventplanner.contrivers.com.eventplanner.model.Route;
import eventplanner.contrivers.com.eventplanner.model.Step;

public class ParserTask extends AsyncTask<String, Integer, List<Step>> {
    private OnRouteChangedListener routeChangeListener;
    private LatLng planLocation;
    private String name;

    public ParserTask(OnRouteChangedListener routeChangeListener, LatLng planLocation, String name) {
        this.routeChangeListener = routeChangeListener;
        this.planLocation = planLocation;
        this.name = name;
    }

    // Parsing the data in non-ui thread
    @Override
    protected List<Step> doInBackground(String... jsonData) {

        JSONObject jObject;
        List<Step> routes = null;

        try {
            jObject = new JSONObject(jsonData[0]);
            DirectionsJSONParser parser = new DirectionsJSONParser();

            // Starts parsing data
            routes = parser.parse(jObject);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return routes;
    }

    // Executes in UI thread, after the parsing process
    @Override
    protected void onPostExecute(List<Step> result) {
        Log.i("name", "---------------------  " + name + "  ------------------------------");
        Integer distance = 0;
        Integer duration = 0;
        for (int i = 0; i < result.size(); i++) {
            List<Route> routes = new ArrayList<>();
            Step step = result.get(i);
            List<HashMap<String, String>> path = step.getPath();

            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);
                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);
                routes.add(new Route(position, planLocation, step.getDistance(), step.getDuration()));
            }
            distance += step.getDistance();
            duration += step.getDuration();
            routeChangeListener.onRouteChanged(routes);
        }
        routeChangeListener.onRouteChanged(name, distance, duration);
    }

}
