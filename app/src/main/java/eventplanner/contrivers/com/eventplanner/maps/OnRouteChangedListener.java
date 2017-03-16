package eventplanner.contrivers.com.eventplanner.maps;

import java.util.List;

import eventplanner.contrivers.com.eventplanner.model.Route;

public interface OnRouteChangedListener {
    void onRouteChanged(String name, Integer distance, Integer duration);

    void onRouteChanged(List<Route> routes);
}
