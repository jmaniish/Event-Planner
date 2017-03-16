package eventplanner.contrivers.com.eventplanner.model;

import com.google.android.gms.maps.model.LatLng;

public class Route {
    private LatLng start;
    private LatLng end;
    private Integer distance;
    private Integer duration;

    public Route() {
    }

    public Route(LatLng start, LatLng end, Integer distance, Integer duration) {
        this.start = start;
        this.end = end;
        this.distance = distance;
        this.duration = duration;
    }

    public LatLng getStart() {
        return start;
    }

    public LatLng getEnd() {
        return end;
    }

    public Integer getDistance() {
        return distance;
    }

    public Integer getDuration() {
        return duration;
    }
}
