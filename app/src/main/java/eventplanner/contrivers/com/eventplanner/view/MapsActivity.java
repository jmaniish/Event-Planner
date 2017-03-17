package eventplanner.contrivers.com.eventplanner.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        OnRouteChangedListener, ValueEventListener, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    public static final String MYNAME = "myname";
    private static final String REQUESTING_LOCATION_UPDATES_KEY = "update key";
    private static final String LOCATION_KEY = "locationkey";
    private GoogleMap mMap;
    private DatabaseReference reference;
    private Util util;
    private Plan plan;
    private GoogleApiClient mGoogleApiClient;
    private boolean addedLocation;
    private SharedPreferences preferences;
    private boolean mRequestingLocationUpdates;
    private String mCurrentLocation;
    private LocationRequest mLocationRequest;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUsername;


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

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
//            if (mFirebaseUser.getPhotoUrl() != null) {
//                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
//            }
        }
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
        updateValuesFromBundle(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getString(LOCATION_KEY);
            }
            addOwnLocation(plan);
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
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.7f)
                .snippet(distance + "km  " + duration + "min");
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
        // mMap.addPolyline(lineOptions);

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
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
    }

    private void addOwnLocation(Plan plan) {
        mCurrentLocation = getCurrentLocation();
        if (mCurrentLocation != null && plan != null) {
            plan.updateOrCreate(new Person(mCurrentLocation, preferences.getString(MYNAME, "Gopya")));
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
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                mRequestingLocationUpdates);
        savedInstanceState.putString(LOCATION_KEY, mCurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
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

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = util.getString(location);
        addOwnLocation(plan);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                mUsername = "any";
                startActivity(new Intent(this, SignInActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}