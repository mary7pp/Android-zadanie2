package com.example.zaddom2;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    List<Marker> markerList;
    List<Markers> saveMarkerList;

    private FloatingActionButton getDataButton;
    private FloatingActionButton hideButton;
    private TextView displayData;

    private SensorManager sensorManager;
    private Sensor sensor;private boolean sensorWorks = false;

    private Button memClearButton;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Create an instance of FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        markerList = new ArrayList<>(); // Initialize markerList
        saveMarkerList = new ArrayList<>();
        getDataButton = findViewById(R.id.getDataButton);
        hideButton = findViewById(R.id.hideButton);
        displayData = findViewById(R.id.displayData);
        memClearButton = findViewById(R.id.memClearButton);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        getDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorWorks = !sensorWorks;
                startsensor(sensorWorks);
            }
        });

        hideButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataButton.animate().alpha(0.0f).translationY(30.0f).setDuration(1000);
                hideButton.animate().alpha(0.0f).translationY(30.0f).setDuration(1000);
                if(sensorWorks) {
                    sensorWorks = !sensorWorks;
                    startsensor(sensorWorks);
                }
            }
        });

        memClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markerList.clear();
                saveMarkerList.clear();
                mMap.clear();
                saveToJson();
            }
        });
    }

    private void startsensor(boolean sensorWorks){
        if (sensorWorks) {
            displayData.setVisibility(View.VISIBLE);
            sensorManager.registerListener(this, sensor, 100000);
        }
        else {
            displayData.setVisibility(View.INVISIBLE);
            sensorManager.unregisterListener(this, sensor);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJson();

    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request the missing permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();

        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {}
        });

        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        //Add a custom marker at the position of the long click
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .icon(bitmapDescriptorFromVector(this, R.drawable.marker))
                .alpha(0.8f)
                .title(String.format("Position: (%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        //Add the marker to the list
        markerList.add(marker);
        saveMarkerList.add(new Markers(latLng.latitude, latLng.longitude));
        saveToJson();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        //Zoom the map on the marker
        CameraPosition cameraPos = mMap.getCameraPosition();

        getDataButton.animate().alpha(1.0f).translationY(0.0f).setDuration(1000);
        hideButton.animate().alpha(1.0f).translationY(0.0f).setDuration(1000);
        return false;
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        //Request location updates with mLocationRequest and locationCallback
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void createLocationCallback(){
        //create the locationCallback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                //Code executed when user's location changes
                if(locationResult != null){
                    //Remove the last reported location
                    if(gpsMarker != null)
                        gpsMarker.remove();
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        if (sensor != null)
            sensorManager.unregisterListener(this, sensor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveToJson();
    }

    private void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void zoomInClick(View v) {
        //Zoom in the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v) {
        //Zoom out the map by 1
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        displayData.setText("Acceleration\n x:"+event.values[0]+" y:"+event.values[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void saveToJson() {
        Gson gson = new Gson();
        String listJson = gson.toJson(saveMarkerList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput("plik.json", MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput("plik.json");
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char [DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<Markers>>() {}.getType();
            List<Markers> o = gson.fromJson(readJson, collectionType);
            if (o != null) {
                saveMarkerList.clear();
                for (Markers markers : o) {
                    saveMarkerList.add(markers);
                    //Add a custom marker at the position of the long click
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(markers.latitude, markers.longitude))
                            .icon(bitmapDescriptorFromVector(this, R.drawable.marker))
                            .alpha(0.8f)
                            .title(String.format("Position: (%.2f, %.2f)", markers.latitude, markers.longitude)));
                    //Add the marker to the list
                    markerList.add(marker);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
