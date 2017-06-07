package com.example.smithj2058.mapsactivity;

import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Location loc;

    private EditText editSearch;

    private LocationManager locationManager;
    private boolean isGPSenabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 15;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 5.0f;
    private static final float MY_LOC_ZOOM_FACTOR = 15f;
    private boolean isTrack = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editSearch = (EditText) findViewById(R.id.editText_Search);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void toggleView(View view) {
        int holder = mMap.getMapType();
        if (holder == 1) {
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        LatLng birthplace = new LatLng(32.7157, -117.1611);
        mMap.addMarker(new MarkerOptions().position(birthplace).title("Born here"));
        editSearch = (EditText) findViewById(R.id.editText_Search);

    }

    public void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            isGPSenabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (isGPSenabled)
                Log.d("MapsActivity", "getLocation: GPS is enabled");

            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isNetworkEnabled)
                Log.d("MapsActivity", "getLocation: Network is enabled");

            if (!isGPSenabled && !isNetworkEnabled)
                Log.d("MapsActivity", "getLocation: No Provider is enabled");
            else {
                canGetLocation = true;
                if (isGPSenabled) {
                    Log.d("MapsActivity", "getLocation: GPS enabled - requesting location updates");
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListenerGPS);
                    Toast.makeText(this, "Using GPS", Toast.LENGTH_SHORT).show();;
                }
                if (isNetworkEnabled) {
                    Log.d("MapsActivity", "getLocation: Network enabled - requesting location updates");
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListenerNetwork);
                    Toast.makeText(this, "Using Network", Toast.LENGTH_SHORT).show();;

                }
            }
        } catch (Exception e) {

            Log.d("My Maps", "Caught an exception in getLocation");

        }
    }

    public void dropMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        if (isGPSenabled) {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            Log.d("MapsActivity", "Dropped GPS Marker");
        } else {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            Log.d("MapsActivity", "Dropped Network Marker" + location.getAccuracy());

        }
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, MY_LOC_ZOOM_FACTOR));
    }

    public void trackMe(View view) {
        if (isTrack) {
            isTrack = false;
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerGPS);
            locationManager.removeUpdates(locationListenerNetwork);
            Log.d("MapsActivity", "Tracking disabled");
            Toast.makeText(this.getApplicationContext(), "Tracking disabled", Toast.LENGTH_SHORT).show();;
        }
        else {
            getLocation();
            isTrack = true;
            Log.d("MapsActivity", "Tracking enabled");
            Toast.makeText(this.getApplicationContext(), "Tracking enabled", Toast.LENGTH_SHORT).show();;

        }
    }

    public void searchPOI(View view) throws IOException {
        Geocoder myGeo = new Geocoder(this.getApplicationContext());
         if(loc != null && editSearch.getText() !=null) {
            List<Address> holder = myGeo.getFromLocationName(editSearch.getText().toString(), 3, loc.getLatitude() - .07246, loc.getLongitude() - .07246, loc.getLatitude() + .07246, loc.getLongitude() + .07246);
            for (int i = 0; i < holder.size(); i++) {
                LatLng poi = new LatLng(holder.get(i).getLatitude(), holder.get(i).getLongitude());
                mMap.addMarker(new MarkerOptions().position(poi).title(holder.get(i).getAddressLine(0)));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(poi, MY_LOC_ZOOM_FACTOR));

            }
             Toast.makeText(this.getApplicationContext(), "Search Completed; Markers added", Toast.LENGTH_SHORT).show();
        }
    }
    public void clearMakers(View view){
        mMap.clear();
        LatLng birthplace = new LatLng(32.7157, -117.1611);
        mMap.addMarker(new MarkerOptions().position(birthplace).title("Born here"));
    }

    android.location.LocationListener locationListenerGPS = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            dropMarker(location);
            loc = location;
            if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerNetwork);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            switch (status) {

                case LocationProvider.AVAILABLE:
                    Log.d("MapsActivity", "Location Provider is available");

                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d("MapsActivity", "Location Provider is temp unavailable");
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListenerNetwork);
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d("MapsActivity", "Location Provider is out of service");
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListenerNetwork);
                    break;
                default:
                    Log.d("MapsActivity", "Default called");
                    if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES,
                            locationListenerNetwork);
                    break;

            }

         }

         @Override
         public void onProviderEnabled(String provider) {

         }

         @Override
         public void onProviderDisabled(String provider) {

         }

     };

    android.location.LocationListener locationListenerNetwork = new android.location.LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            dropMarker(location);
            loc = location;
            Log.d("MapsActivity", "locationListenerNetwork: location changed");
            if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListenerNetwork);
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
    };

}



