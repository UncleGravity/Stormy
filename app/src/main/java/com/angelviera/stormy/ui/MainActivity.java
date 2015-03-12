package com.angelviera.stormy.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.angelviera.stormy.R;
import com.angelviera.stormy.location.LocationProvider;
import com.angelviera.stormy.weather.Current;
import com.angelviera.stormy.weather.Day;
import com.angelviera.stormy.weather.Forecast;
import com.angelviera.stormy.weather.Hour;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity implements LocationProvider.LocationCallback {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final String DAILY_FORECAST = "DAILY_FORECAST";
    public static final String HOURLY_FORECAST = "HOURLY_FORECAST";

    private LocationProvider mLocationProvider;

    private Forecast mForecast;

    @InjectView(R.id.timeLabel) TextView mTimeLabel;
    @InjectView(R.id.temperatureLabel) TextView mTemperatureLabel;
    @InjectView(R.id.humidityValue) TextView mHumidityValue;
    @InjectView(R.id.precipValue) TextView mPrecipValue;
    @InjectView(R.id.summaryLabel) TextView mSummaryLabel;
    @InjectView(R.id.iconImageView) ImageView mIconImageView;
    @InjectView(R.id.locationLabel) TextView mLocationLabel;
    @InjectView(R.id.refreshImageView) ImageView mRefreshImageView;

    @InjectView(R.id.progressBar) ProgressBar mProgressBar;
    @InjectView(R.id.degreeImageView) ImageView mDegreeImageView;

   private double mLatitude = 40.4313684;
   private double mLongitude = -79.9877103;
   private String mCity;


    /** START HERE
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mLocationProvider = new LocationProvider(this, this);
        mProgressBar.setVisibility(View.INVISIBLE);

        // LISTENER
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //request Forecast here
                toggleRefresh();
                mLocationProvider.connect();
            }
        });

        // LISTENER
        // Segue into AsyncTestActivity (debug)
        mDegreeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AsyncTestActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        toggleRefresh();
        mLocationProvider.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationProvider.disconnect();
    }

    /** Verify is there is a network connection.  Requires ACCESS_NETWORK_STATE permission.
     */

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        boolean isAvailable = false;
        if(networkInfo != null && networkInfo.isConnected()){
            isAvailable = true;
        }
        return isAvailable;
    }


    /** Asynchronous request of the forecast.
     *  Updates the UI with new data.
     */

    private void requestForecast() {

        String apiKey = "4c70f5794678da3e752797b5c4a7bfdc";
        String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + mLatitude + "," + mLongitude;

        if(isNetworkAvailable()) {

            //toggleRefresh();

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();

            ////////////////////////////////////////////////////
            ///// START Asynchronous request
            //

            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });

                    try {
                        String jsonData = response.body().string();
                        if (response.isSuccessful()) {

                            mForecast = getForecastDetails(jsonData);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });

                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "IOException caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException caught: ", e);
                    }
                }
            });
        } else {
            Toast.makeText(this, getString(R.string.network_unavailable_message), Toast.LENGTH_LONG).show();
        }
        //
        ///// END Asynchronous request
        ////////////////////////////////////////////////////
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility() == View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    /** Update UI labels with new weather data.
     */
    private void updateDisplay() {
        Current current = mForecast.getCurrentWeather();

        mTemperatureLabel.setText(current.getTemperature() + "");
        mTimeLabel.setText("At " + current.getFormattedTime() + " it will be");
        mHumidityValue.setText(current.getHumidity() + "");
        mPrecipValue.setText(current.getPrecipChance() + "%");
        mSummaryLabel.setText(current.getSummary());
        Drawable icon = getResources().getDrawable(current.getIconId());
        mIconImageView.setImageDrawable(icon);
        mLocationLabel.setText(current.getLocation());

    }

    private Forecast getForecastDetails(String jsonData) throws JSONException, IOException {
        Forecast forecast = new Forecast();

        forecast.setCurrentWeather(getCurrentDetails(jsonData));
        forecast.setHourlyForecast(getHourlyForecast(jsonData));
        forecast.setDailyForecast(getDailyForecast(jsonData));

        return forecast;
    }

    private Day[] getDailyForecast(String jsonData) throws JSONException{
        JSONObject forecast = new JSONObject(jsonData); // Entire forecast.io data
        String timezone = forecast.getString("timezone"); // Timezone

        JSONObject daily = forecast.getJSONObject("daily"); // Hourly forecasts
        JSONArray data = daily.getJSONArray("data"); // Hourly forecasts (as JSONArray)

        Day[] days = new Day[data.length()];

        for(int i = 0; i < data.length(); i++){
            JSONObject jsonDay = data.getJSONObject(i);
            days[i] = new Day();

            days[i].setSummary(jsonDay.getString("summary"));
            days[i].setIcon(jsonDay.getString("icon"));
            days[i].setTemperatureMax(jsonDay.getLong("temperatureMax"));
            days[i].setTime(jsonDay.getLong("time"));
            days[i].setTimezone(timezone);
        }

        return days;
    }

    private Hour[] getHourlyForecast(String jsonData) throws JSONException {


        JSONObject forecast = new JSONObject(jsonData); // Entire forecast.io data
        String timezone = forecast.getString("timezone"); // Timezone
        JSONObject hourly = forecast.getJSONObject("hourly"); // Hourly forecasts
        JSONArray data = hourly.getJSONArray("data"); // Hourly forecasts (as JSONArray)

        Hour[] hours = new Hour[data.length()]; // Array to return

        for (int i = 0; i < data.length(); i++) {
            JSONObject jsonHour = data.getJSONObject(i);
            hours[i] = new Hour();

            hours[i].setTime(jsonHour.getLong("time"));
            hours[i].setIcon(jsonHour.getString("icon"));
            hours[i].setSummary(jsonHour.getString("summary"));
            hours[i].setTemperature(jsonHour.getDouble("temperature"));
            hours[i].setTimeZone(timezone);
        }

        return hours;
    }

    /** Parse JSON data into the Current object.
     */
    private Current getCurrentDetails(String jsonData) throws JSONException, IOException {

        JSONObject forecast = new JSONObject(jsonData);

        //String timezone = forecast.getString("timezone");

        JSONObject currently = forecast.getJSONObject("currently");

        Current currentWeather = new Current();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(forecast.getString("timezone"));
        //mCity = "Turtles"; // Dummy City

        currentWeather.setLocation(mCity);

        Log.d(TAG, currentWeather.getFormattedTime());
        return currentWeather;
    }

    /** Show alert when there is no connection.
     */
    private void alertUserAboutError() {
        AlertDialogFragment dialog = new AlertDialogFragment();
        dialog.show(getFragmentManager(),"error_dialog");
    }

    @Override
    public void handleNewLocation(Location location) {

        Log.d(TAG, "Location: " + location.toString());

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        // Get City from coordinates
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(mLatitude, mLongitude, 1);
            mCity = addresses.get(0).getLocality();
            Log.d(TAG, addresses.get(0).getLocality());
        } catch (IOException e) {
            e.printStackTrace();
        }

        requestForecast();
        //LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        Log.d(TAG, "Lat: " + mLatitude);
        Log.d(TAG, "Lon: " + mLongitude);

        mLocationProvider.disconnect();
    }

    @OnClick (R.id.dailyButton)
    public void startDailyActivity(View view){
        Intent intent = new Intent(this, DailyForecastActivity.class);
        intent.putExtra(DAILY_FORECAST, mForecast.getDailyForecast());
        intent.putExtra("city", mCity);
        startActivity(intent);
    }

    @OnClick (R.id.hourlyButton)
    public void startHourlyActivity(View view){
        Intent intent = new Intent(this, HourlyForecastActivity.class);
        intent.putExtra(HOURLY_FORECAST, mForecast.getHourlyForecast());
        startActivity(intent);
    }

}
