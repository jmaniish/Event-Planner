package eventplanner.contrivers.com.eventplanner.view;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import eventplanner.contrivers.com.eventplanner.R;
import eventplanner.contrivers.com.eventplanner.maps.DownloadTask;
import eventplanner.contrivers.com.eventplanner.maps.OnRouteChangedListener;
import eventplanner.contrivers.com.eventplanner.model.Person;
import eventplanner.contrivers.com.eventplanner.model.Plan;
import eventplanner.contrivers.com.eventplanner.model.Route;
import eventplanner.contrivers.com.eventplanner.utils.Util;

import static java.lang.Math.round;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        OnRouteChangedListener, ValueEventListener, ConnectionCallbacks, OnConnectionFailedListener {

    public static final String MYNAME = "myname";
    private GoogleMap mMap;
    private DatabaseReference reference;
    private Util util;
    private Plan plan;
    private GoogleApiClient mGoogleApiClient;
    private boolean addedLocation;
    private SharedPreferences preferences;
    ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        util = new Util();

        preferences = getSharedPreferences("myPreference", Context.MODE_PRIVATE);

        preferences.edit().putString("myname", Build.MODEL).apply();

        reference = FirebaseDatabase.getInstance().getReference("plan1");
        reference.keepSynced(true);
        reference.addValueEventListener(this);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        plan = dataSnapshot.getValue(Plan.class);
        if (!addedLocation) {
            addOwnLocation(plan);
            addedLocation = true;
        }
        mMap.clear();
        LatLng planLocation = util.createFromString(plan.location);

        setPlanDestination(planLocation);

        showDirectionsFor(plan.persons, planLocation);
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    private void showDirectionsFor(List<Person> persons, LatLng planLocation) {
        ArrayList<LatLng> positions = new ArrayList<>();
        for (Person person : persons) {
            LatLng personLocation = util.createFromString(person.location);
            showDirectionBetween(personLocation, planLocation, person.name);
            positions.add(personLocation);
        }
        positions.add(planLocation);
        setPointsInBound(positions);
    }

    private void addMarkerFor(final LatLng location, String name, final double distance, final double duration) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title(name)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .flat(true)
                .alpha(0.7f)
                .snippet(distance + "km  " + duration + "min")
                .anchor(0.5f, 0.5f);
        Marker marker = mMap.addMarker(markerOptions);
        marker.showInfoWindow();
    }


    private void setPointsInBound(List<LatLng> places) {
        final LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

        for (LatLng place : places) {
            boundsBuilder.include(place);
        }
        LatLngBounds bounds = boundsBuilder.build();
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 100);
        mMap.moveCamera(cu);
        mMap.animateCamera(cu);
    }

    private void showDirectionBetween(LatLng start, LatLng end, String name) {
        DownloadTask downloadTask = new DownloadTask(this, end, name);
        String url = downloadTask.getDirectionsUrl(start, end);
        downloadTask.execute(url);
    }

    public void setPlanDestination(LatLng latLng) {
        if (mMap == null) {
            return;
        }
        mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f));
    }

    @Override
    public void onRouteChanged(List<Route> routes) {
        PolylineOptions lineOptions = new PolylineOptions();
        lineOptions.addAll(util.getPointsOf(routes));
        lineOptions.width(8);
        lineOptions.color(Color.BLUE);
        mMap.addPolyline(lineOptions);

    }

    @Override
    public void onRouteChanged(String name, Integer distance, Integer duration) {
        for (Person person : plan.persons) {
            if (person.name.equalsIgnoreCase(name)) {
                addMarkerFor(util.createFromString(person.location), person.name, dist(distance), dur(duration));
            }
        }
    }

    private double dist(Integer distance) {
        return round(distance * 10 / 1000.0) / 10.0;
    }

    private double dur(Integer duration) {
        return round(duration * 10 / 60.0) / 10.0;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        addOwnLocation(plan);
    }

    private void addOwnLocation(Plan plan) {
        String currentLocation = getCurrentLocation();
        if (currentLocation != null && plan != null) {
            plan.updateOrCreate(new Person(currentLocation, preferences.getString(MYNAME, "Gopya")));
            reference.setValue(this.plan);
        }
    }

    private String getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        return util.getString(LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient));
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}