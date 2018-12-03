package zhao.felix.locationalarmapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static zhao.felix.locationalarmapp.ArrivedActivity.EXTRA_MESSAGE;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static double distance_limit = 0.1;

    Geocoder geocoder;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    private GoogleMap mMap;

    private Button _btnSet;

    private FusedLocationProviderClient mFusedLocationClient;

    private EditText mAddr;

    private Button mGoto;

    private TextView mDest;

    private TextView mCurr;

    private TextView mDis;

    /// destination
    private LatLng _dest;

    /// current position
    private LatLng _cur;

    /// is user enabled alarm?
    private boolean _start_alarm = false;

    /// is alarm already enabled?
    private boolean _alarm_already_started = false;

    /// check distance between destination with current position,
    /// if arrived jump to arrived page.
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Context context = getApplicationContext();

                    String info = "current location : lat:" + _cur.latitude + ",lon:" + _cur.longitude;
                    if(_dest != null && reachDestination(_cur, _dest) == true) {
                        if(_start_alarm == true) {
                            if(_alarm_already_started == false) {
                                Intent intent = new Intent(getApplicationContext(), ArrivedActivity.class);
                                intent.putExtra(EXTRA_MESSAGE, true);
                                startActivity(intent);

                                _alarm_already_started = true;
                            }else{
                                info = "Alarm Already Started! " + info;
                            }
                        }else{
                            info = "Alarm Not Start! " + info;
                        }



                        info = "arrived! " + info;
                    }else {
                        info = "Not reach. " + info;
                    }

                    Toast.makeText(context
                            , info
                            , Toast.LENGTH_LONG).show();
            }
        }
    };

    /// function to check arrived destination
    private boolean reachDestination(LatLng cur, LatLng dist){
        double distance = distance(cur.latitude, cur.longitude, dist.latitude, dist.longitude, "K");
        if(distance <= distance_limit){
            return true;
        }
        return false;
    }

    /// function get distance between two position
    /// where: 'M' is statute miles (default)
    /// 'K' is kilometers
    /// 'N' is nautical miles
    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        } // else is "M"

        return (dist);
    }

    /// This function converts decimal degrees to radians
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /// This function converts radians to decimal degrees
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    void setDistanceInfo(){
        double dist = 0.0;
        if(_cur != null && _dest != null) {
            dist = distance(_cur.latitude, _cur.longitude, _dest.latitude, _dest.longitude, "K");
        }
        mDis.setText("dist : " + String.format("%f",dist) + " KM");
    }

    void init() {
        setContentView(R.layout.activity_maps);

        geocoder = new Geocoder(this, Locale.getDefault());

        mDest = (TextView) findViewById(R.id.dest);
        mCurr = (TextView) findViewById(R.id.curr);
        mDis = (TextView) findViewById(R.id.distance);

        mAddr = (EditText) findViewById(R.id.address);
        mGoto = (Button) findViewById(R.id.go2);

        mDest.setText("");
        mCurr.setText("");
        mDis.setText("");

        _btnSet = (Button) findViewById(R.id.set_alarm);

        // User enable or disable alarm by click button
        _btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                if(_start_alarm == false) {
                    _btnSet.setText("Disable");
                    _start_alarm = true;
                    Toast.makeText(context
                            , "Alarm enabled."
                            , Toast.LENGTH_LONG).show();
                }else{
                    _btnSet.setText("Enable");
                    _start_alarm = false;
                    Toast.makeText(context
                            , "Alarm disabled."
                            , Toast.LENGTH_LONG).show();
                }
            }
        });

        mGoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = getApplicationContext();
                String addr = mAddr.getText().toString();
                List<Address> addresses = null;
                try {
                    addresses = geocoder.getFromLocationName(addr,1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(addresses != null && addresses.isEmpty() == false
                        && addresses.size() > 0){
                    Address add = addresses.get(0);
                    LatLng latLng = new LatLng(add.getLatitude(), add.getLongitude());
                    setDest(latLng);
                }

                // hide keyboard
                InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);



        if (Build.VERSION.SDK_INT > 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else {
            /// update user current position
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                try {



                                    //Move the camera to the user's location and zoom in!
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 12.0f));

                                    if (true) {
                                        mMap.setMyLocationEnabled(true);
                                        mMap.getUiSettings().setMyLocationButtonEnabled(true);
                                    }
                                } catch (Exception ex) {
                                    Log.d("Init Fail.", ex.getMessage(), ex);
                                }
                            }
                        }
                    });


            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(5 * 1000);
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) {
                        return;
                    }
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            _cur = new LatLng(location.getLatitude(), location.getLongitude());
                            mCurr.setText(String.format(Locale.US, "%6f , %6f", _cur.latitude, _cur.longitude));

                            /// sample for get address by location or address string
//                            List<Address> addresses = new ArrayList<Address>();
//                            List<Address> addressesLoc = new ArrayList<Address>();
//                            try {
//                                 addresses = geocoder.getFromLocation(
//                                        _cur.latitude,
//                                        _cur.longitude,
//                                        1);
//
//                                addressesLoc = geocoder.getFromLocationName("Massey University",1);
//
//
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }

                            Message message = new Message();
                            message.what = 0;
                            mHandler.sendMessage(message);
                        }
                    }
                }
            };




//            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

            // update current location
            UpdateLocation();

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT > 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else
            init();
    }

    /// handler user permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    init();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory( Intent.CATEGORY_HOME );
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(homeIntent);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    /// Now we use fuselocation, so this function is expired
    /// function update user current location
    private void UpdateLocation() {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (location != null) {
                    Log.d("location update", "lon:" + location.getLongitude());
                    _cur = new LatLng(location.getLatitude(), location.getLongitude());

                    Message message = new Message();
                    message.what = 0;
                    mHandler.sendMessage(message);
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };


        if (Build.VERSION.SDK_INT > 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else{
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);//.NETWORK_PROVIDER
    }
    }

    private void drawCircle(LatLng point){

        // Instantiating CircleOptions to draw a circle around the marker
        CircleOptions circleOptions = new CircleOptions();

        // Specifying the center of the circle
        circleOptions.center(point);

        // Radius of the circle
        circleOptions.radius(20);

        // Border color of the circle
        circleOptions.strokeColor(Color.CYAN);

        // Fill color of the circle
        circleOptions.fillColor(0x30ff0000);

        // Border width of the circle
        circleOptions.strokeWidth(2);

        // Adding the circle to the GoogleMap
        mMap.addCircle(circleOptions);

    }

    private void setDest(LatLng latLng){
        Log.i("Set Destination", "Destination : " +
                "Lat : " + latLng.latitude + " , "
                + "Long : " + latLng.longitude);
        _dest = latLng;


        mMap.clear();

        mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));

        mDest.setText(String.format(Locale.US, "%6f , %6f", _dest.latitude, _dest.longitude));

        setDistanceInfo();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                setDest(latLng);

                // draw circle around the location
                drawCircle(latLng);
            }
        });
    }
}
