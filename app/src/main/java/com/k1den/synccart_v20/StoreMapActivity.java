package com.k1den.synccart_v20;

import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StoreMapActivity extends AppCompatActivity {
    private MapView map;
    private MapEventsOverlay mapEventsOverlay;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_store_map);

        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(5.0);
        map.getController().setCenter(new GeoPoint(61.5240, 105.3188));

        // Обработчик кликов по карте
        mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                loadShopsNearby(p.getLatitude(), p.getLongitude());
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        });
        map.getOverlays().add(mapEventsOverlay);

        // Поиск города
        EditText etCitySearch = findViewById(R.id.etCitySearch);
        etCitySearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchCity(etCitySearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void searchCity(String cityName) {
        if (cityName.trim().isEmpty()) return;

        executor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String urlStr = "https://nominatim.openstreetmap.org/search?q=" +
                        URLEncoder.encode(cityName, "UTF-8") +
                        "&format=json&limit=1";
                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "SyncCart_V20");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                            "Ошибка геокодирования: код " + responseCode, Toast.LENGTH_SHORT).show());
                    return;
                }

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                JSONArray results = new JSONArray(response.toString());
                if (results.length() > 0) {
                    JSONObject first = results.getJSONObject(0);
                    double lat = first.getDouble("lat");
                    double lon = first.getDouble("lon");

                    runOnUiThread(() -> {
                        map.getController().setCenter(new GeoPoint(lat, lon));
                        map.getController().setZoom(12.0);
                        loadShopsNearby(lat, lon);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                            "Город не найден", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                        "Ошибка поиска города: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception ignored) {
                    }
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private void loadShopsNearby(double lat, double lon) {
        executor.execute(() -> {
            String[] endpoints = {
                    "https://osm.hpi.de/overpass/api/interpreter",        // 🇩🇪 HPI Proxy
                    "https://overpass-api.de/api/interpreter",            // 🇩🇪 Основной (часто перегружен)
            };

            String query = "[out:json][timeout:25];node(around:5000," + lat + "," + lon + ")[shop];out;";
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            JSONArray elements = null;
            Exception lastException = null;
            String failedEndpoint = "";

            for (String endpoint : endpoints) {
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    String urlStr = endpoint + "?data=" + encodedQuery;
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", "SyncCart_V20/1.0 (https://github.com/yourapp)");
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        lastException = new Exception("HTTP " + responseCode);
                        failedEndpoint = endpoint;
                        continue;
                    }

                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }

                    JSONObject jsonResponse = new JSONObject(result.toString());

                    if (jsonResponse.has("remark")) {
                        String remark = jsonResponse.getString("remark");
                        if (remark.contains("timeout") || remark.contains("busy")) {
                            lastException = new Exception("Сервер перегружен: " + remark);
                            failedEndpoint = endpoint;
                            continue;
                        }
                    }

                    elements = jsonResponse.getJSONArray("elements");
                    break;

                } catch (Exception e) {
                    lastException = e;
                    failedEndpoint = endpoint;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception ignored) {
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            }

            if (elements == null) {
                String errorMsg = lastException != null ? lastException.getMessage() : "Не удалось загрузить данные";
                runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                        "Ошибка: " + errorMsg + "\nПопробуйте позже", Toast.LENGTH_LONG).show());
                return;
            }

            JSONArray finalElements = elements;
            runOnUiThread(() -> {
                List<Overlay> toRemove = new ArrayList<>();
                for (Overlay overlay : map.getOverlays()) {
                    if (overlay instanceof Marker) {
                        toRemove.add(overlay);
                    }
                }
                map.getOverlays().removeAll(toRemove);

                for (int i = 0; i < Math.min(finalElements.length(), 50); i++) {
                    try {
                        JSONObject obj = finalElements.getJSONObject(i);
                        double shopLat = obj.getDouble("lat");
                        double shopLon = obj.getDouble("lon");
                        JSONObject tags = obj.optJSONObject("tags");
                        String name = tags != null ? tags.optString("name", "Магазин") : "Магазин";

                        Marker marker = new Marker(map);
                        marker.setPosition(new GeoPoint(shopLat, shopLon));
                        marker.setTitle(name);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        map.getOverlays().add(marker);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                map.invalidate();
                Toast.makeText(StoreMapActivity.this,
                        "Найдено магазинов: " + finalElements.length(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    public void onBackPressed(android.view.View view) {
        finish();
    }
}