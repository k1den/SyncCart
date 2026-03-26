package com.k1den.synccart_v20;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

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
import org.osmdroid.views.overlay.Polyline;

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

    private String currentFilter = "[shop]";

    // Переменные для маршрута
    private boolean isWaitingForRouteStart = false;
    private GeoPoint destinationPoint = null;
    private Marker startRouteMarker = null;
    private Polyline currentRoute = null;

    // 3 ПУНКТ: Элементы панели маршрута
    private View llRouteInfoPanel;
    private TextView tvRouteDistance, tvRouteTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName()); // Оставил твой агент

        setContentView(R.layout.activity_store_map);

        map = findViewById(R.id.map);
        map.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        map.setTileSource(TileSourceFactory.MAPNIK); // Оставил MAPNIK
        map.setMultiTouchControls(true);
        map.getController().setZoom(5.0);
        map.getController().setCenter(new GeoPoint(61.5240, 105.3188));

        // 3 ПУНКТ: Находим views панели маршрута
        llRouteInfoPanel = findViewById(R.id.llRouteInfoPanel);
        tvRouteDistance = findViewById(R.id.tvRouteDistance);
        tvRouteTime = findViewById(R.id.tvRouteTime);

        // Обработчик тапов по карте
        mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (isWaitingForRouteStart && destinationPoint != null) {
                    buildRouteFromPoint(p);
                    return true;
                }
                return false;
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

        // Кнопка отмены маршрута
        findViewById(R.id.btnClearRoute).setOnClickListener(v -> cancelRoute());

        // Настройка кнопок фильтров
        findViewById(R.id.btnFilterAll).setOnClickListener(v -> setFilterAndReload("[shop]"));
        findViewById(R.id.btnFilterSupermarket).setOnClickListener(v -> setFilterAndReload("[\"shop\"=\"supermarket\"]"));
        findViewById(R.id.btnFilterConvenience).setOnClickListener(v -> setFilterAndReload("[\"shop\"=\"convenience\"]"));
        findViewById(R.id.btnFilterPharmacy).setOnClickListener(v -> setFilterAndReload("[\"amenity\"=\"pharmacy\"]"));
    }

    private void cancelRoute() {
        if (currentRoute != null) {
            map.getOverlays().remove(currentRoute);
            currentRoute = null;
        }
        if (startRouteMarker != null) {
            map.getOverlays().remove(startRouteMarker);
            startRouteMarker = null;
        }

        isWaitingForRouteStart = false;
        destinationPoint = null;

        EditText etCitySearch = findViewById(R.id.etCitySearch);
        etCitySearch.setText("");
        etCitySearch.setHint("Введите город");

        // Скрываем все кнопки отмены и плашку инфо
        findViewById(R.id.btnClearRoute).setVisibility(View.GONE);
        llRouteInfoPanel.setVisibility(View.GONE); // 3 ПУНКТ: Скрываем плашку

        map.invalidate();
    }

    private void buildRouteFromPoint(GeoPoint startPoint) {
        isWaitingForRouteStart = false;
        EditText etCitySearch = findViewById(R.id.etCitySearch);
        etCitySearch.setHint("Введите город");

        if (startRouteMarker != null) map.getOverlays().remove(startRouteMarker);
        startRouteMarker = new Marker(map);
        startRouteMarker.setPosition(startPoint);
        startRouteMarker.setTitle("Отсюда");
        startRouteMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(startRouteMarker);

        fetchAndDrawRoute(startPoint, destinationPoint);
    }

    private void setFilterAndReload(String filterTag) {
        this.currentFilter = filterTag;
        GeoPoint center = (GeoPoint) map.getMapCenter();
        Toast.makeText(this, "Применяем фильтр...", Toast.LENGTH_SHORT).show();
        loadShopsNearby(center.getLatitude(), center.getLongitude());
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
                conn.setRequestProperty("User-Agent", getPackageName());
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
                        GeoPoint foundPoint = new GeoPoint(lat, lon);
                        map.getController().setCenter(foundPoint);
                        map.getController().setZoom(14.0);

                        if (isWaitingForRouteStart && destinationPoint != null) {
                            buildRouteFromPoint(foundPoint);
                        }
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                            "Город не найден", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(StoreMapActivity.this,
                        "Ошибка поиска: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } finally {
                if (reader != null) {
                    try { reader.close(); } catch (Exception ignored) {}
                }
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void loadShopsNearby(double lat, double lon) {
        executor.execute(() -> {
            String[] endpoints = {
                    "https://osm.hpi.de/overpass/api/interpreter",
                    "https://overpass-api.de/api/interpreter",
            };

            String query = "[out:json][timeout:25];node(around:5000," + lat + "," + lon + ")" + currentFilter + ";out;";
            String encodedQuery = null;
            try {
                encodedQuery = URLEncoder.encode(query, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            JSONArray elements = null;
            Exception lastException = null;

            for (String endpoint : endpoints) {
                HttpURLConnection conn = null;
                BufferedReader reader = null;
                try {
                    String urlStr = endpoint + "?data=" + encodedQuery;
                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("User-Agent", getPackageName());
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);

                    int responseCode = conn.getResponseCode();
                    if (responseCode != 200) {
                        lastException = new Exception("HTTP " + responseCode);
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
                            continue;
                        }
                    }

                    elements = jsonResponse.getJSONArray("elements");
                    break;

                } catch (Exception e) {
                    lastException = e;
                } finally {
                    if (reader != null) {
                        try { reader.close(); } catch (Exception ignored) {}
                    }
                    if (conn != null) conn.disconnect();
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
                    if (overlay instanceof Marker && overlay != startRouteMarker) {
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

                        Marker marker = new Marker(map);
                        marker.setPosition(new GeoPoint(shopLat, shopLon));
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        marker.setRelatedObject(tags);

                        marker.setOnMarkerClickListener((clickedMarker, mapView) -> {
                            mapView.getController().animateTo(clickedMarker.getPosition());
                            showBottomSheet((JSONObject) clickedMarker.getRelatedObject(), clickedMarker.getPosition());
                            return true;
                        });

                        map.getOverlays().add(marker);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                map.invalidate();
                Toast.makeText(StoreMapActivity.this,
                        "Найдено точек: " + finalElements.length(), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void showBottomSheet(JSONObject tags, GeoPoint storePos) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_bottom_sheet_store, null);

        TextView tvName = view.findViewById(R.id.tvStoreName);
        TextView tvType = view.findViewById(R.id.tvStoreType);
        TextView tvHours = view.findViewById(R.id.tvOpeningHours);
        TextView tvAddress = view.findViewById(R.id.tvAddress);
        Button btnBuildRoute = view.findViewById(R.id.btnBuildRoute);

        if (tags != null) {
            tvName.setText(tags.optString("name", tags.optString("brand", "Безымянный магазин")));

            String shopType = tags.optString("shop", tags.optString("amenity", "Точка"));
            tvType.setText("Категория: " + shopType);

            String hours = tags.optString("opening_hours", "Не указано");
            tvHours.setText("🕒 " + hours);

            String street = tags.optString("addr:street", "");
            String house = tags.optString("addr:housenumber", "");
            if (!street.isEmpty() || !house.isEmpty()) {
                tvAddress.setText("📍 " + street + " " + house);
            } else {
                tvAddress.setText("📍 Адрес не указан");
            }
        }

        btnBuildRoute.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            isWaitingForRouteStart = true;
            destinationPoint = storePos;

            EditText etCitySearch = findViewById(R.id.etCitySearch);
            etCitySearch.setText("");
            etCitySearch.setHint("Кликните на карту или введите адрес");

            // Показываем кнопку отмены
            findViewById(R.id.btnClearRoute).setVisibility(View.VISIBLE);

            Toast.makeText(this, "Укажите точку старта на карте", Toast.LENGTH_LONG).show();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }

    private void fetchAndDrawRoute(GeoPoint start, GeoPoint end) {
        Toast.makeText(this, "Строим маршрут...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            HttpURLConnection conn = null;
            BufferedReader reader = null;
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/" +
                        start.getLongitude() + "," + start.getLatitude() + ";" +
                        end.getLongitude() + "," + end.getLatitude() +
                        "?overview=full&geometries=geojson";

                URL url = new URL(urlStr);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", getPackageName());
                conn.setConnectTimeout(10000);

                if (conn.getResponseCode() != 200) return;

                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);

                JSONObject jsonResponse = new JSONObject(result.toString());
                JSONArray routes = jsonResponse.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject geometry = route.getJSONObject("geometry");
                    JSONArray coordinates = geometry.getJSONArray("coordinates");

                    // 3 ПУНКТ: Достаем дистанцию и время OSRM
                    double distanceKm = route.getDouble("distance") / 1000.0;
                    double durationSeconds = route.getDouble("duration");
                    int durationMinutes = (int) Math.round(durationSeconds / 60.0);

                    List<GeoPoint> routePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray coord = coordinates.getJSONArray(i);
                        routePoints.add(new GeoPoint(coord.getDouble(1), coord.getDouble(0)));
                    }

                    runOnUiThread(() -> {
                        if (currentRoute != null) {
                            map.getOverlays().remove(currentRoute);
                        }

                        currentRoute = new Polyline(map);
                        currentRoute.setPoints(routePoints);
                        currentRoute.setColor(Color.parseColor("#2196F3"));
                        currentRoute.setWidth(10.0f);

                        map.getOverlays().add(currentRoute);

                        // 3 ПУНКТ: Обновляем и показываем плашку инфо
                        tvRouteDistance.setText(String.format("%.1f км", distanceKm));
                        tvRouteTime.setText(durationMinutes + " мин");
                        llRouteInfoPanel.setVisibility(View.VISIBLE);

                        map.invalidate();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Не удалось построить маршрут", Toast.LENGTH_SHORT).show());
            } finally {
                if (reader != null) try { reader.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
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

    public void onBackPressed(View view) {
        finish();
    }
}