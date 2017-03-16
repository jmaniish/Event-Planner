package eventplanner.contrivers.com.eventplanner.model;

import java.util.HashMap;
import java.util.List;

public class Step {
    private List<HashMap<String, String>> path;
    private Integer duration;
    private Integer distance;

    public Step() {

    }

    public Step(List<HashMap<String, String>> path, Integer distance, Integer duration) {
        this.path = path;
        this.duration = duration;
        this.distance = distance;
    }

    public List<HashMap<String, String>> getPath() {
        return path;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getDistance() {
        return distance;
    }
}
