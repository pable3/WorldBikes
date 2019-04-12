package es.upv.pabgalm2.worldbikes.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

import es.upv.pabgalm2.worldbikes.R;
import es.upv.pabgalm2.worldbikes.pojo.Network;
import es.upv.pabgalm2.worldbikes.pojo.Station;
import es.upv.pabgalm2.worldbikes.services.CitybikAPI;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static int NETWORK_IMAGE_LEVEL = 8;
    private static int STATION_LEVEL = 11;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private GoogleMap mMap;
    private Marker currentNetwork;
    private ArrayList<Marker> networkMarkers;
    private CitybikAPI citybikAPI;
    private LatLng lastLocation;
    private Boolean mLocationPermissionsGranted = false;
    private BottomSheetBehavior mBottom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        currentNetwork = null;
        Intent myIntent = getIntent();

        if(myIntent.getBooleanExtra("locationService", false)) {
            getLocationPermission();
        } else {
            initMap();
        }

        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateToLocation();
            }
        });

        initializeBottomSheet();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        networkMarkers = new ArrayList<>();

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(53, 9)));
        mMap.setOnMarkerClickListener(this);
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_map));

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {

                if(mMap.getCameraPosition().zoom < STATION_LEVEL) {

                    unFocusNetwork();

                    if(mMap.getCameraPosition().zoom >= NETWORK_IMAGE_LEVEL) {
                        setImageMarkers();
                    } else {
                        setDotMarkers();
                    }
                }

            }
        });

        citybikAPI = new CitybikAPI(getApplicationContext(), mMap, networkMarkers);
        citybikAPI.getNetworksFromFile();

        if (mLocationPermissionsGranted) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);

        }

    }

    private void initMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        final Object tag = marker.getTag();

        if(tag instanceof Network) {

            Network network = (Network) tag;

            if(mMap.getCameraPosition().zoom < NETWORK_IMAGE_LEVEL) {

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), NETWORK_IMAGE_LEVEL), 500,null);

            } else {

                unFocusNetwork();

                currentNetwork = marker;

                currentNetwork.setVisible(false);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentNetwork.getPosition(), STATION_LEVEL));

                citybikAPI.getStationsAsync(network);
            }
        } else {

            final Network network = (Network) currentNetwork.getTag();
            final Station station = (Station) tag;

            GradientDrawable shape = station.getBitmap().getShape();
            shape.setColor(Color.parseColor("#b70096"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16), 500, new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    mBottom.setState(BottomSheetBehavior.STATE_EXPANDED);

                    ImageView imageView = findViewById(R.id.network_logo);
                    imageView.setImageBitmap(network.getNetworkImage());

                    TextView bikes = findViewById(R.id.text_cycles);
                    TextView slots = findViewById(R.id.text_slots);
                    bikes.setText(station.getBikes());
                    slots.setText(station.getSlots());

                    int color = station.getBitmap().getColor();

                    int newColor = Color.argb(Color.alpha(color), (int)Math.min((Color.red(color)*0.5), 255),  (int) Math.min((Color.green(color)*0.5),255),  (int) Math.min((Color.blue(color)*0.5),255));

                    CardView cardBikes = findViewById(R.id.card_view_cycles);
                    CardView cardSlots = findViewById(R.id.card_view_slots);
                    cardBikes.setCardBackgroundColor(newColor);
                    cardSlots.setCardBackgroundColor(newColor);

                }

                @Override
                public void onCancel() {

                }
            });

        }

        return true;
    }

    private void initializeBottomSheet() {

        View bottomSheet = findViewById(R.id.bottom_sheet);
        mBottom = BottomSheetBehavior.from(bottomSheet);
        mBottom.setHideable(true);
        mBottom.setState(BottomSheetBehavior.STATE_HIDDEN);

    }

    private void setNetworkMarkersVisible(Boolean visible) {

        for(Marker m:networkMarkers) {
            m.setVisible(visible);
        }

    }

    private void setDotMarkers(){

        for(Marker m:networkMarkers) {
            Bitmap dotBitmap = ((Network) m.getTag()).getDotImage();
            m.setIcon(BitmapDescriptorFactory.fromBitmap(dotBitmap));
        }

    }

    private void setImageMarkers(){

        for(Marker m:networkMarkers) {
            Bitmap networkImage = ((Network) m.getTag()).getNetworkImage();
            m.setIcon(BitmapDescriptorFactory.fromBitmap(networkImage));
        }

    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermissionsGranted = false;

        switch(requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length > 0){
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            initMap();
                            return;
                        }
                    }
                    mLocationPermissionsGranted = true;

                    initMap();
                }
            }
        }
    }

    private void animateToLocation(){

        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try{
            if(mLocationPermissionsGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();

                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            final Location currentLocation = (Location) task.getResult();

                            if(currentLocation != null) {
                                lastLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 16), 300, null);
                            } else {
                                animateToLocation();
                            }

                        }else{
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Toast.makeText(this, ("animateToLocation: SecurityException: " + e.getMessage() ), Toast.LENGTH_LONG).show();
        }
    }

    private void unFocusNetwork(){

        if(currentNetwork != null) {

            Network network = (Network) currentNetwork.getTag();

            for (Marker m : network.getStations()) {
                m.remove();
            }

            network.clearStations();

            Network aux = (Network) currentNetwork.getTag();
            currentNetwork.setVisible(true);
            currentNetwork = null;
        }

    }

}
