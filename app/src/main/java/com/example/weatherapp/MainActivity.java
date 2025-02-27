package com.example.weatherapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    TextView txt_location, txt_degree, txt_humidity, txt_wind, txt_description;
    ImageView img_iconweather;
    private static final String API_KEY = "5177d42db8347ccd0f85969c6626c7b4";
    private LocationHelper locationHelper;
    List<String> permissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_location = findViewById(R.id.txt_location);
        txt_degree =findViewById(R.id.txt_degree);
        txt_humidity = findViewById(R.id.txt_humidity);
        txt_wind = findViewById(R.id.txt_wind);
        img_iconweather=findViewById(R.id.icon_weather);
        txt_description=findViewById(R.id.txt_description);
        locationHelper = new LocationHelper(this);
        // Kiểm tra quyền ACCESS_FINE_LOCATION
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // Kiểm tra quyền POST_NOTIFICATIONS (chỉ yêu cầu trên Android 13 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }

// Nếu có quyền nào cần yêu cầu, gọi requestPermissions
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]),
                    1000);
        } else {
            // Nếu đã có đủ quyền, thực hiện getLocation
            getLocation();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation();
        }
    }


    private void getLocation() {
        locationHelper.getCurrentLocation(new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    Log.d("Location", "Lat: " + latitude + ", Lon: " + longitude);
                    getCityName(latitude, longitude);
                }
            }
        });
    }
    public String getCityName(double latitude, double longitude) {
        String cityName = "Unknown";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                cityName = addresses.get(0).getLocality();
                if (cityName == null) {
                    cityName = addresses.get(0).getAdminArea(); // Thử lấy tên tỉnh/thành phố
                }
                if (cityName == null) {
                    cityName = addresses.get(0).getSubAdminArea(); // Thử lấy tên quận/huyện
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("CityName", "City: " + cityName);
        FetchWeatherData(cityName);
        txt_location.setText(cityName);
        return cityName;
    }
    private void FetchWeatherData(String cityName) {

        String url = "https://api.openweathermap.org/data/2.5/weather?q=" + cityName + "&appid=" + API_KEY + "&units=metric";
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() ->
                {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    try {
                        Response response = client.newCall(request).execute();
                        String result = response.body().string();
                        runOnUiThread(() -> updateUI(result));
                    } catch (IOException e)
                    {
                        e.printStackTrace();;
                    }
                }
        );
    }
    private void updateUI(String result)
    {
        if(result != null)
        {
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONObject main =  jsonObject.getJSONObject("main");
                double temperature = main.getDouble("temp");
                double humidity = main.getDouble("humidity");
                double windSpeed = jsonObject.getJSONObject("wind").getDouble("speed");

                String description = jsonObject.getJSONArray("weather").getJSONObject(0).getString("description");
                String iconCode = jsonObject.getJSONArray("weather").getJSONObject(0).getString("icon");
                String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

                if (!isFinishing() && !isDestroyed()) {
                    Glide.with(this)
                            .load(iconUrl)
                            .into(img_iconweather);
                }

                txt_degree.setText(String.format("%.0f", temperature));
                txt_humidity.setText(String.format("%.0f%%", humidity));
                txt_wind.setText(String.format("%.0f km/h", windSpeed));
                txt_description.setText(description);

                // Lưu thông tin cũ
                SharedPreferences sharedPreferences = getSharedPreferences("WeatherData", MODE_PRIVATE);
                double oldTemp = Double.longBitsToDouble(sharedPreferences.getLong("temp", Double.doubleToLongBits(-999)));
                double oldHumidity = Double.longBitsToDouble(sharedPreferences.getLong("humidity", Double.doubleToLongBits(-999)));
                double oldWind = Double.longBitsToDouble(sharedPreferences.getLong("windSpeed", Double.doubleToLongBits(-999)));
                String oldDescription = sharedPreferences.getString("description", "");


                // Kiểm tra nếu có thay đổi thì mới hiển thị thông báo
                if (temperature != oldTemp || !description.equals(oldDescription) || oldHumidity!= humidity || windSpeed != oldWind) {
                    // Hiển thị thông báo
                    String title = "Thời tiết hiện tại";
                    String message = String.format("Nhiệt độ: %.0f°C - %s", temperature, description);
                    NotificationHelper notificationHelper = new NotificationHelper(this);
                    notificationHelper.showWeatherNotification(title, message);
                    // Lưu thông tin thời tiết mới nhất
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong("temp", Double.doubleToLongBits(temperature));
                    editor.putString("description", description);
                    editor.putLong("humidity", Double.doubleToLongBits(humidity));
                    editor.putLong("windSpeed", Double.doubleToLongBits(windSpeed));
                    editor.apply();
                }
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }
}