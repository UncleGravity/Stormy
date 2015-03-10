package com.angelviera.stormy;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity implements LocationProvider.LocationCallback {

    public static final String TAG = MainActivity.class.getSimpleName();

    private LocationProvider mLocationProvider;

    private CurrentWeather mCurrentWeather;

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

   double mLatitude = 40.4313684;
   double mLongitude = -79.9877103;


    /** START HERE
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mLocationProvider = new LocationProvider(this, this);

        // LISTENER
        // Segue into AsyncTestActivity (debug)
        mDegreeImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AsyncTestActivity.class);
                //intent.putExtra(getString(R.string.key_name),name);
                startActivity(intent);
            }
        });


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

        //Request weather, and update UI
        //requestForecast();

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
                            mCurrentWeather = getCurrentDetails(jsonData);

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
        mTemperatureLabel.setText(mCurrentWeather.getTemperature() + "");
        mTimeLabel.setText("At " + mCurrentWeather.getFormattedTime() + " it will be");
        mHumidityValue.setText(mCurrentWeather.getHumidity() + "");
        mPrecipValue.setText(mCurrentWeather.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeather.getSummary());
        Drawable icon = getResources().getDrawable(mCurrentWeather.getIconId());
        mIconImageView.setImageDrawable(icon);
        mLocationLabel.setText(mCurrentWeather.getLocation());

    }

    /** Parse JSON data into the CurrentWeather object.
     */
    private CurrentWeather getCurrentDetails(String jsonData) throws JSONException {

        JSONObject forecast = new JSONObject(jsonData);

        //String timezone = forecast.getString("timezone");
        //Log.i(TAG,"From JSON: " + timezone);

        JSONObject currently = forecast.getJSONObject("currently");

        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setHumidity(currently.getDouble("humidity"));
        currentWeather.setTime(currently.getLong("time"));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeather.setSummary(currently.getString("summary"));
        currentWeather.setTemperature(currently.getDouble("temperature"));
        currentWeather.setTimeZone(forecast.getString("timezone"));
        String city = "Turtles";

        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        try {
            List<android.location.Address> addresses = gcd.getFromLocation(mLatitude, mLongitude, 1);
            if(addresses.size() > 0){
                city = addresses.get(0).getLocality();

                Log.d(TAG, addresses.get(0).getLocality());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentWeather.setLocation(city); //Dummy City

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

        requestForecast();
        //LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        Log.d(TAG, "Lat: " + mLatitude);
        Log.d(TAG, "Lon: " + mLongitude);

        mLocationProvider.disconnect();
    }
}
