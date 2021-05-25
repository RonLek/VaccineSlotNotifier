package com.example.cowin_booker;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> stateList = new ArrayList<>();
    ArrayList<String> districtList = new ArrayList<>();
    ArrayList<District> districtObjList = new ArrayList<>();
    private ArrayList<VaccinationCenter> centerList;
    private Spinner stateSpinner, districtSpinner;
    private EditText datePicker;
    String selectedDate = "NULL";
    int districtId = -1;
    int year, month, date;
    private Button searchButton;
    private static ProgressDialog mProgressDialog;
    String urlString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        stateSpinner = (Spinner) findViewById(R.id.spState);
        districtSpinner = (Spinner) findViewById(R.id.spDistrict);
        datePicker = (EditText) findViewById(R.id.datePicker);
        searchButton = (Button) findViewById(R.id.search);

        callAllStateFunctions();
        callDatePickerFunctions();

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

            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedDate.equals("NULL")) {
                    Toast.makeText(MainActivity.this, "Please select Date", Toast.LENGTH_SHORT).show();
                } else if (districtId == -1) {
                    Toast.makeText(MainActivity.this, "Please select State and District", Toast.LENGTH_SHORT).show();
                } else {
                    urlString = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/findByDistrict?district_id=" + districtId + "&date=" + selectedDate + "";
                    retrieveJSON(urlString);
                    Intent intent = new Intent(MainActivity.this, SessionFinderService.class);
                    intent.putExtra("urlString", urlString);
                    startService(intent);
                }
            }
        });

    }

    // retrieving json
    private void retrieveJSON(String urlString) {

        showSimpleProgressDialog(this, "Loading...", "Fetching Json", false);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, urlString,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);
                        centerList = new ArrayList<>();
                        JSONArray dataArray = obj.getJSONArray("states");

                        for (int i = 0; i < dataArray.length(); i++) {
                            VaccinationCenter centerModel = new VaccinationCenter();
                            JSONObject dataObj = dataArray.getJSONObject(i);

                            centerModel.setCenterId(dataObj.getString("center_id"));
                            centerModel.setCenterName(dataObj.getString("name"));
                            centerModel.setAvailableCap(dataObj.getInt("available_capacity"));
                            centerModel.setAvailableCapD1(dataObj.getInt("available_capacity_dose1"));
                            centerModel.setAvailableCapD2(dataObj.getInt("available_capacity_dose2"));
                            centerModel.setMinAgeLimit(dataObj.getInt("min_age_limit"));
                            centerModel.setVaccinationType(dataObj.getString("vaccine"));
                            centerModel.setDate(dataObj.getString("date"));

                            centerList.add(centerModel);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        removeSimpleProgressDialog();
                    }
                },
                error -> {
                    Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                    removeSimpleProgressDialog();
                });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public static void removeSimpleProgressDialog() {
        try {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    public static void showSimpleProgressDialog(Context context, String title,
                                                String msg, boolean isCancelable) {
        try {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(context, title, msg);
                mProgressDialog.setCancelable(isCancelable);
            }

            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }

        } catch (Exception ie) {
            ie.printStackTrace();
        }
    }

    // datePicker functions
    private void callDatePickerFunctions() {
        selectDate();
    }

    public void selectDate() {
        Calendar calendar = Calendar.getInstance();
        year = calendar.get(Calendar.YEAR);
        month = calendar.get(Calendar.MONTH);
        date = calendar.get(Calendar.DAY_OF_MONTH);

        datePicker.setOnClickListener(view -> {
            closeKeyboard();
            DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker dp, int selectYear, int selectMonth, int selectDate) {
                            selectMonth += 1;
                            if (selectMonth < 10) {
                                selectedDate = selectDate + "-0" + selectMonth + "-" + selectYear;
                            } else {
                                selectedDate = selectDate + "-" + selectMonth + "-" + selectYear;
                            }
                            datePicker.setText(selectedDate);

                        }
                    }, year, month, date);

            datePickerDialog.show();
        });
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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