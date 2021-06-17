package com.example.cowin_booker;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.cowin_booker.Models.District;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import spencerstudios.com.bungeelib.Bungee;


public class MainActivity extends AppCompatActivity {
    int flag = 0;
    ArrayList<String> stateList = new ArrayList<>();
    ArrayList<String> districtList = new ArrayList<>();
    ArrayList<District> districtObjList = new ArrayList<>();
    private Spinner stateSpinner, districtSpinner;
    int districtId = -1;
    String urlString, TAG = "jhgj";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bookButton = findViewById(R.id.bookButton);
        stateSpinner = findViewById(R.id.spState);
        districtSpinner = findViewById(R.id.spDistrict);
        Button searchButton = findViewById(R.id.search);
        Button stopBtn = findViewById(R.id.stop);

        callAllStateFunctions();

        bookButton.setOnClickListener(view -> {
            //chrome customs tabs
            String url = "https://selfregistration.cowin.gov.in/";
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
            Bungee.slideLeft(MainActivity.this);
        });

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                String selectedItem = adapterView.getItemAtPosition(position).toString();
                if (selectedItem.equals("Select your State")) {
                    // it's default
                    callAllDistrictFunctions("Select your State");
                } else {
                    callAllDistrictFunctions(selectedItem);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(MainActivity.this, "Please Select something", Toast.LENGTH_LONG).show();
            }
        });

        districtSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                String selectedItem = adapterView.getItemAtPosition(position).toString();
                if (selectedItem.equals("Select your District") || selectedItem.equals("Select State First")) {
                    // it's default
                } else {
                    districtId = getDistrictId(selectedItem);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(MainActivity.this, "Please Select something", Toast.LENGTH_LONG).show();
            }
        });

        searchButton.setOnClickListener(view -> {

            if (districtId == -1) {
                Toast.makeText(MainActivity.this, "Please select State and District", Toast.LENGTH_SHORT).show();
            } else {
                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                String formattedDate = df.format(c);
                Toast.makeText(MainActivity.this, formattedDate, Toast.LENGTH_SHORT).show();

                urlString = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/findByDistrict?district_id=" + districtId + "&date=" + formattedDate + "";
                ComponentName componentName = new ComponentName(MainActivity.this, ForegroundService.class);
                PersistableBundle bundle = new PersistableBundle();
                bundle.putString("urlString", urlString);
                JobInfo info;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    info = new JobInfo.Builder(123, componentName)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPersisted(true)
                            .setMinimumLatency(5000)
                            .setExtras(bundle)
                            .build();
                } else {
                    info = new JobInfo.Builder(123, componentName)
                            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                            .setPersisted(true)
                            .setPeriodic(5000)
                            .setExtras(bundle)
                            .build();
                }

                JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
                int resultCode = scheduler.schedule(info);
                if (resultCode == JobScheduler.RESULT_SUCCESS) {
                    Log.d(TAG, "Job scheduled");
                } else {
                    Log.d(TAG, "Job scheduling failed");
                }
            }

        });

        stopBtn.setOnClickListener(v -> {
            JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            scheduler.cancel(123);
        });
    }

    // district functions
    public void callAllDistrictFunctions(String selectedItem) {
        districtList.clear();
        districtObjList.clear();
        districtObjectList(selectedItem);
        addToDistrictSpinner();
    }

    public int getDistrictId(String districtName) {
        for (District d : districtObjList) {
            if (d.getDistrictName().equals(districtName)) {
                return d.getDistrictId();
            }
        }

        return -1;
    }

    public String getDistrictJson(int stateId) {
        String json = null;
        try {
            InputStream is = getAssets().open(stateId + ".json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return json;
        }
        return json;
    }

    void districtObjectList(String selectedItem) {
        int stateId = getStateId(selectedItem);
        if (stateId == -1) {
            Toast.makeText(MainActivity.this, "Something went Wrong", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(getDistrictJson(stateId));
            JSONArray array = jsonObject.getJSONArray("districts");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String state = object.getString("district_name");
                districtList.add(state);
                District d = new District(object.getInt("district_id"), state);
                districtObjList.add(d);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void addToDistrictSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, districtList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        districtSpinner.setAdapter(adapter);
    }

    // state functions

    public void callAllStateFunctions() {
        stateObjectList();
        addToStateSpinner();
    }

    public String getStatesJson() {
        String json = null;
        try {
            InputStream is = getAssets().open("states.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return json;
        }
        return json;
    }

    public int getStateId(String stateName) {
        try {
            JSONObject jsonObject = new JSONObject(getStatesJson());
            JSONArray array = jsonObject.getJSONArray("states");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                if (stateName.equals(object.getString("state_name"))) {
                    return object.getInt("state_id");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return -1;
    }

    void stateObjectList() {
        try {
            JSONObject jsonObject = new JSONObject(getStatesJson());
            JSONArray array = jsonObject.getJSONArray("states");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String state = object.getString("state_name");
                stateList.add(state);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    void addToStateSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stateList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        stateSpinner.setAdapter(adapter);
    }
}