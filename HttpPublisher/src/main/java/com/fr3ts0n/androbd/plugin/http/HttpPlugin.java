package com.fr3ts0n.androbd.plugin.http;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.fr3ts0n.androbd.plugin.Plugin;
import com.fr3ts0n.androbd.plugin.PluginInfo;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class HttpPlugin
        extends Plugin
        implements
        LocationListener,
        Plugin.ConfigurationHandler,
        Plugin.ActionHandler,
        Plugin.DataReceiver,
        SharedPreferences.OnSharedPreferenceChangeListener {
    static final PluginInfo myInfo = new PluginInfo("HttpPublisher",
            HttpPlugin.class,
            "Http publish AndrOBD measurements",
            "Copyright (C) 2017 by fr3ts0n",
            "GPLV3+",
            "https://github.com/fr3ts0n/AndrOBD-Plugin"
    );

    LocationManager locationManager;

    /**
     * Preference keys
     */
    static final String HTTP_PROTOCOL = "http_protocol";
    static final String HTTP_HOSTNAME = "http_hostname";
    static final String HTTP_PORT = "http_port";
    static final String HTTP_PATH = "http_path";
    static final String HTTP_TOKEN = "http_token";
    //
    static final String BATCH_SIZE = "batch_size";
    static final String PERIOD = "period";

    /**
     * http
     */
    String protocol;
    String hostname;
    String port;
    String path;
    String token;
    //
    Integer batchSize = 1;
    Integer period = 5;

    //
    HashSet<String> mSelectedItems = new HashSet<>();
    /**
     * The data collection
     */
    static final HashMap<String, String> valueMap = new HashMap<>();

    SharedPreferences prefs;
    static final String ITEMS_SELECTED = "data_items";
    static final String ITEMS_KNOWN = "known_items";

    /**
     * set of items which are known to the plugin
     */
    protected static HashSet<String> mKnownItems = new HashSet<>();

    Thread updateThread = new Thread()
    {
        public void run()
        {
            Log.i("HTTP", "Thread - started");
            try
            {
                while (!interrupted())
                {
                    sleep(period * 10000);
                    Log.i("Http", "Thread - Http Publish data");
                    performAction();
                }
            }
            catch (InterruptedException ignored)
            {
                Log.i("HTTP", "Thread - error = " + ignored);
            }
            Log.i("HTTP", "Thread - finished");
        }
    };



    @Override
    public void onCreate() {
        {
            super.onCreate();
            // get preferences
            prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            prefs.registerOnSharedPreferenceChangeListener(this);
            // get all shared preference values
            onSharedPreferenceChanged(prefs, null);

            // GPS
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED &&
                        checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                PackageManager.PERMISSION_GRANTED)
                {
                    Log.e(getPackageName(), "Location permissions missing");
                    return;
                }
            }
            // Register the listener with the Location Manager to receive location updates
            try
            {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
            catch (Exception ex)
            {
                Log.e(getPackageName(), ex.toString());
            }
            try
            {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            }
            catch (Exception ex)
            {
                Log.e(getPackageName(), ex.toString());
            }
            //

            // 인증 권한 추가
            updateThread.start();
        }
    }

    @Override
    public void onDestroy()
    {
        // interrupt cyclic thread
        updateThread.interrupt();

        // forget about settings changes
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        // GPS
        locationManager.removeUpdates(this);
        super.onDestroy();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key == null || HTTP_PROTOCOL.equals(key)) {
            protocol = sharedPreferences.getString(HTTP_PROTOCOL,getString(R.string.http_protocol_https));
        }

        if (key == null || HTTP_HOSTNAME.equals(key)) {
            hostname = sharedPreferences.getString(HTTP_HOSTNAME,"");
        }

        if (key == null || HTTP_PORT.equals(key)) {
            port = sharedPreferences.getString(HTTP_PORT,"80");
        }

        if (key == null || HTTP_PATH.equals(key)) {
            path = sharedPreferences.getString(HTTP_PATH,"/");
        }

        if (key == null || HTTP_TOKEN.equals(key)) {
            token = sharedPreferences.getString(HTTP_TOKEN,"");
        }

        if (key == null || BATCH_SIZE.equals(key)) {
            batchSize = Integer.parseInt(sharedPreferences.getString(BATCH_SIZE,"1"));
        }

        if (key == null || PERIOD.equals(key)) {
            period = Integer.parseInt(sharedPreferences.getString(PERIOD,"1"));
        }

        if (key == null || ITEMS_SELECTED.equals(key))
        {
            mSelectedItems =
                    (HashSet<String>) sharedPreferences.getStringSet(ITEMS_SELECTED, mSelectedItems);
        }

        if (key == null || ITEMS_KNOWN.equals(key))
        {
            mKnownItems =
                    (HashSet<String>) sharedPreferences.getStringSet(ITEMS_KNOWN, mKnownItems);
        }

        final String http = protocol + hostname + ":" + port + path;
        Log.i("HTTP", "Connect: " + http);

    }

    @Override
    public PluginInfo getPluginInfo() {
        return myInfo;
    }

    @Override
    public void performAction() {
        final String http = protocol + hostname + ":" + port + path;
        Log.i("HTTP", "Connect: " + http);
        String http_test = "https://yvzguxmx8f.execute-api.ap-northeast-2.amazonaws.com/";
        Log.i("HTTP", "Connect: " + http_test);

        // Attempt to get fine GPS location ...
        // check permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED)
            {
                Log.e(getPackageName(), "Location permissions missing");
                return;
            }
        }
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        // If no fine location is available ...
        if (loc == null)
        {
            // Attempt to get coarse network location
            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        Location finalLoc = loc;;
        new Thread(()->{
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();

            try {
                Log.i("HTTP", "GPS" + finalLoc.toString());
                HttpPost httpPost = new HttpPost("https://yvzguxmx8f.execute-api.ap-northeast-2.amazonaws.com/obd-data");
                valueMap.put("GPS_LATITUDE", String.valueOf(finalLoc.getLatitude()));
                valueMap.put("GPS_LONGITUDE", String.valueOf(finalLoc.getLongitude()));
                valueMap.put("GPS_ALTITUDE", String.valueOf(finalLoc.getAltitude()));
                valueMap.put("GPS_BEARING", String.valueOf(finalLoc.getBearing()));
                valueMap.put("GPS_SPEED", String.valueOf(finalLoc.getSpeed()));
                JSONObject jsonObject = new JSONObject(valueMap);
                Log.i("HTTP", "DATA = " + valueMap);
                httpPost.setEntity(new StringEntity(String.valueOf(jsonObject), ContentType.APPLICATION_JSON));
                httpClient.execute(httpPost,
                        response -> {
                            Log.i("HTTP", "resonse: " + response.toString());
                            return null;
                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

    }

    @Override
    public void performConfigure() {
        Intent cfgIntent = new Intent(getApplicationContext(), SettingsActivity.class);
        cfgIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(cfgIntent);
    }

    @Override
    public void onDataListUpdate(String csvString) {
        Log.i("HTTP", "onDataListUpdate");
        synchronized (mKnownItems)
        {
            for (String csvLine : csvString.split("\n"))
            {
                String[] fields = csvLine.split(";");
                if (fields.length > 0)
                {
                    mKnownItems.add(fields[0]);
                }
            }
            // store known items as preference
            Log.i("HTTP", "ITEMS_KNOWN" + ITEMS_KNOWN);
            Log.i("HTTP", "mKnownItems=" + mKnownItems.toString());
            // prefs.edit().putStringSet(ITEMS_KNOWN, mKnownItems).apply();
        }
        // clear the map of received values
        synchronized (valueMap)
        {
            valueMap.clear();
        }

    }

    @Override
    public void onDataUpdate(String key, String value) {
        synchronized (valueMap)
        {
            // add value if it is to be published ...
            if (mSelectedItems.size() == 0 || mSelectedItems.contains(key))
            {
                valueMap.put(key, value);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onLocationChanged(List<Location> locations) {
        LocationListener.super.onLocationChanged(locations);
    }

    @Override
    public void onProviderEnabled(String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }
}
