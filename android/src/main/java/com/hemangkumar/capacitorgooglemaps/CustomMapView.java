package com.hemangkumar.capacitorgooglemaps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.util.Consumer;

import com.getcapacitor.JSObject;
import com.google.android.libraries.maps.CameraUpdate;
import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.GoogleMapOptions;
import com.google.android.libraries.maps.MapView;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.UiSettings;
import com.google.android.libraries.maps.model.CameraPosition;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.GroundOverlay;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.android.libraries.maps.model.PointOfInterest;
import com.google.android.libraries.maps.model.Polygon;
import com.google.android.libraries.maps.model.Polyline;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomMapView
        implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnInfoWindowCloseListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnPolygonClickListener,
        GoogleMap.OnPolylineClickListener,
        GoogleMap.OnMyLocationClickListener,
        GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnPoiClickListener,
        GoogleMap.OnCameraMoveStartedListener,
        GoogleMap.OnCameraMoveListener,
        GoogleMap.OnCameraIdleListener,
        ClusterManager.OnClusterClickListener<CustomClusterItem>,
        ClusterManager.OnClusterInfoWindowClickListener<CustomClusterItem>,
        ClusterManager.OnClusterItemClickListener<CustomClusterItem>,
        ClusterManager.OnClusterItemInfoWindowClickListener<CustomClusterItem> {
    private final AppCompatActivity activity;
    private final CustomMapViewEvents customMapViewEvents;
    private final MapEventsListener mapEventsListener = new MapEventsListener();

    private final String id;

    MapView mapView;
    GoogleMap googleMap;

    private final Map<String, Marker> markers = new HashMap<>();
    private final Map<String, ShapePolygon> polygons = new HashMap<>();
    private final Map<String, ShapePolyline> polylines = new HashMap<>();
    private final Map<String, ShapeCircle> circles = new HashMap<>();
    private ClusterManager<CustomClusterItem> clusterManager;
    private CustomClusterRenderer clusterRenderer;
    private final Map<String, CustomClusterItem> clusterItems = new HashMap<>();
    private boolean shapesAboveMarkers = false;

    String savedCallbackIdForCreate;

    String savedCallbackIdForDidTapInfoWindow;

    String savedCallbackIdForDidCloseInfoWindow;

    String savedCallbackIdForDidTapMap;

    String savedCallbackIdForDidLongPressMap;

    String savedCallbackIdForDidTapMarker;
    Boolean preventDefaultForDidTapMarker = false;

    String savedCallbackIdForDidBeginDraggingMarker;

    String savedCallbackIdForDidDragMarker;
    String savedCallbackIdForDidTapPolygon;
    String savedCallbackIdForDidTapPolyline;

    String savedCallbackIdForDidEndDraggingMarker;

    String savedCallbackIdForDidTapMyLocationButton;
    Boolean preventDefaultForDidTapMyLocationButton = false;

    String savedCallbackIdForDidTapMyLocationDot;

    String savedCallbackIdForDidTapPoi;

    String savedCallbackIdForDidBeginMovingCamera;
    String savedCallbackIdForDidMoveCamera;
    String savedCallbackIdForDidEndMovingCamera;

    public static final String EVENT_DID_TAP_INFO_WINDOW = "didTapInfoWindow";
    public static final String EVENT_DID_CLOSE_INFO_WINDOW = "didCloseInfoWindow";
    public static final String EVENT_DID_TAP_MAP = "didTapMap";
    public static final String EVENT_DID_LONG_PRESS_MAP = "didLongPressMap";
    public static final String EVENT_DID_TAP_MARKER = "didTapMarker";
    public static final String EVENT_DID_TAP_POLYGON = "didTapPolygon";
    public static final String EVENT_DID_TAP_POLYLINE = "didTapPolyline";
    public static final String EVENT_DID_BEGIN_DRAGGING_MARKER = "didBeginDraggingMarker";
    public static final String EVENT_DID_DRAG_MARKER = "didDragMarker";
    public static final String EVENT_DID_END_DRAGGING_MARKER = "didEndDraggingMarker";
    public static final String EVENT_DID_TAP_MY_LOCATION_BUTTON = "didTapMyLocationButton";
    public static final String EVENT_DID_TAP_MY_LOCATION_DOT = "didTapMyLocationDot";
    public static final String EVENT_DID_TAP_POI = "didTapPoi";
    public static final String EVENT_DID_BEGIN_MOVING_CAMERA = "didBeginMovingCamera";
    public static final String EVENT_DID_MOVE_CAMERA = "didMoveCamera";
    public static final String EVENT_DID_END_MOVING_CAMERA = "didEndMovingCamera";

    public MapCameraPosition mapCameraPosition;
    public MapPreferences mapPreferences;
    private MarkerVisibilityCorrector markerVisibilityCorrector;

    public CustomMapView(@NonNull AppCompatActivity activity, CustomMapViewEvents customMapViewEvents) {
        this.activity = activity;
        this.customMapViewEvents = customMapViewEvents;
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    private boolean hasPermission() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        // populate `googleMap` variable for other methods to use
        this.googleMap = map;

        // set controls
        UiSettings googleMapUISettings = this.googleMap.getUiSettings();
        googleMapUISettings.setIndoorLevelPickerEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY));
        googleMapUISettings.setMyLocationButtonEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.MY_LOCATION_BUTTON_KEY));

        // set appearance
        this.googleMap.setMapStyle(this.mapPreferences.appearance.style);
        this.googleMap.setBuildingsEnabled(this.mapPreferences.appearance.isBuildingsShown);
        this.googleMap.setIndoorEnabled(this.mapPreferences.appearance.isIndoorShown);
        if (hasPermission()) {
            this.googleMap.setMyLocationEnabled(this.mapPreferences.appearance.isMyLocationDotShown);
        }
        this.googleMap.setTrafficEnabled(this.mapPreferences.appearance.isTrafficShown);


        // execute callback
        if (customMapViewEvents != null && savedCallbackIdForCreate != null) {
            JSObject result = getResultForMap();
            customMapViewEvents.onMapReady(savedCallbackIdForCreate, result);
        }

        assignProxyListenerToMap();
        CustomMarkerManager markerManager = new CustomMarkerManager(googleMap, mapEventsListener);
        markerVisibilityCorrector = new MarkerVisibilityCorrector(
                markerManager, markers, polygons, circles);
        clusterManager = new ClusterManager<>(activity, googleMap, markerManager);
        clusterRenderer = new CustomClusterRenderer(
                activity, googleMap, clusterManager, markerVisibilityCorrector);
        markerVisibilityCorrector.setClusterRenderer(clusterRenderer);
        clusterManager.setRenderer(clusterRenderer);
        mapEventsListener.addOnCameraIdleListener(clusterManager);
        clusterManager.setOnClusterClickListener(this);
        clusterManager.setOnClusterInfoWindowClickListener(this);
        clusterManager.setOnClusterItemClickListener(this);
        clusterManager.setOnClusterItemInfoWindowClickListener(this);
    }

    private void assignProxyListenerToMap() {
        googleMap.setOnCameraIdleListener(mapEventsListener);
        googleMap.setOnCameraMoveListener(mapEventsListener);
        googleMap.setOnMapClickListener(mapEventsListener);
        googleMap.setOnCameraMoveStartedListener(mapEventsListener);
        googleMap.setOnMapLongClickListener(mapEventsListener);
        googleMap.setOnInfoWindowClickListener(mapEventsListener);
        googleMap.setOnInfoWindowCloseListener(mapEventsListener);
        googleMap.setOnMarkerClickListener(mapEventsListener);
        googleMap.setOnMarkerDragListener(mapEventsListener);
        googleMap.setOnMyLocationClickListener(mapEventsListener);
        googleMap.setOnMyLocationButtonClickListener(mapEventsListener);
        googleMap.setOnPoiClickListener(mapEventsListener);
        googleMap.setOnPolygonClickListener(mapEventsListener);
        googleMap.setOnPolylineClickListener(mapEventsListener);
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapInfoWindow != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapInfoWindow, result);
        }
    }

    @Override
    public void onInfoWindowClose(Marker marker) {
        if (customMapViewEvents != null && savedCallbackIdForDidCloseInfoWindow != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidCloseInfoWindow, result);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMap != null) {
            JSObject result = getResultForPosition(latLng);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMap, result);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (customMapViewEvents != null && savedCallbackIdForDidLongPressMap != null) {
            JSObject result = getResultForPosition(latLng);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidLongPressMap, result);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (clusterRenderer.isItAClusterMarker(marker)) return false;
        if (customMapViewEvents != null && savedCallbackIdForDidTapMarker != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMarker, result);
        }
        return preventDefaultForDidTapMarker;
    }

    @Override
    public void onPolygonClick(Polygon polygon) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapPolygon != null) {
            CustomPolygon customPolygon = new CustomPolygon();
            JSObject result = customPolygon.getResultFor(new ShapePolygon(polygon), this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapPolygon, result);
        }
    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapPolyline != null) {
            CustomPolyline customPolyline = new CustomPolyline();
            JSObject result = customPolyline.getResultFor(new ShapePolyline(polyline), this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapPolyline, result);
        }
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        if (clusterRenderer.isItAClusterMarker(marker)) return;
        if (customMapViewEvents != null && savedCallbackIdForDidBeginDraggingMarker != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidBeginDraggingMarker, result);
        }
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        if (clusterRenderer.isItAClusterMarker(marker)) return;
        if (customMapViewEvents != null && savedCallbackIdForDidDragMarker != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidDragMarker, result);
        }
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        if (clusterRenderer.isItAClusterMarker(marker)) return;
        if (customMapViewEvents != null && savedCallbackIdForDidEndDraggingMarker != null) {
            JSObject result = CustomMarker.getResultForMarker(marker, this.id);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidEndDraggingMarker, result);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMyLocationButton != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMyLocationButton, null);
        }
        return preventDefaultForDidTapMyLocationButton;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapMyLocationDot != null) {
            JSObject result = getResultForPosition(location);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapMyLocationDot, result);
        }
    }

    @Override
    public void onPoiClick(PointOfInterest pointOfInterest) {
        if (customMapViewEvents != null && savedCallbackIdForDidTapPoi != null) {
            JSObject result = getResultForPoi(pointOfInterest);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidTapPoi, result);
        }
    }

    @Override
    public void onCameraMoveStarted(int i) {
        if (customMapViewEvents != null && savedCallbackIdForDidBeginMovingCamera != null) {
            int reason = 2;
            if (i == REASON_GESTURE) {
                // Camera motion initiated in response to user gestures on the map.
                // For example: pan, tilt, pinch to zoom, or rotate.
                reason = 1;
            }
            JSObject result = new JSObject();
            result.put("reason", reason);
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidBeginMovingCamera, result);
        }
    }

    @Override
    public void onCameraMove() {
        if (customMapViewEvents != null && savedCallbackIdForDidMoveCamera != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidMoveCamera, null);
        }
    }

    @Override
    public void onCameraIdle() {
        if (customMapViewEvents != null && savedCallbackIdForDidEndMovingCamera != null) {
            customMapViewEvents.resultForCallbackId(savedCallbackIdForDidEndMovingCamera, this.getResultForCameraPosition(new JSObject()));
        }
    }

    @Override
    public boolean onClusterClick(Cluster<CustomClusterItem> cluster) {
        return false;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<CustomClusterItem> cluster) {

    }

    @Override
    public boolean onClusterItemClick(CustomClusterItem item) {
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(CustomClusterItem item) {

    }

    protected void handleOnStart() {
        if (mapView != null) {
            mapView.onStart();
        }
    }

    protected void handleOnResume() {
        if (mapView != null) {
            mapView.onResume();
        }
    }

    protected void handleOnPause() {
        if (mapView != null) {
            mapView.onPause();
        }
    }

    protected void handleOnStop() {
        if (mapView != null) {
            mapView.onStop();
        }
    }

    protected void handleOnDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    public void setCallbackIdForEvent(String callbackId, String eventName, Boolean preventDefault) {
        if (callbackId != null && eventName != null) {
            switch (eventName) {
                case CustomMapView.EVENT_DID_TAP_INFO_WINDOW:
                    this.mapEventsListener.addOnInfoWindowClickListener(this);
                    savedCallbackIdForDidTapInfoWindow = callbackId;
                    break;
                case CustomMapView.EVENT_DID_CLOSE_INFO_WINDOW:
                    this.mapEventsListener.addOnInfoWindowCloseListener(this);
                    savedCallbackIdForDidCloseInfoWindow = callbackId;
                    break;
                case CustomMapView.EVENT_DID_TAP_MAP:
                    this.mapEventsListener.addOnMapClickListener(this);
                    savedCallbackIdForDidTapMap = callbackId;
                    break;
                case CustomMapView.EVENT_DID_LONG_PRESS_MAP:
                    this.mapEventsListener.addOnMapLongClickListener(this);
                    savedCallbackIdForDidLongPressMap = callbackId;
                    break;
                case CustomMapView.EVENT_DID_TAP_MARKER:
                    this.mapEventsListener.addOnMarkerClickListener(this);
                    savedCallbackIdForDidTapMarker = callbackId;
                    if (preventDefault == null) {
                        preventDefault = false;
                    }
                    preventDefaultForDidTapMarker = preventDefault;
                    break;
                case CustomMapView.EVENT_DID_TAP_POLYGON:
                    this.mapEventsListener.addOnPolygonClickListener(this);
                    savedCallbackIdForDidTapPolygon = callbackId;
                    break;
                case CustomMapView.EVENT_DID_TAP_POLYLINE:
                    this.mapEventsListener.addOnPolylineClickListener(this);
                    savedCallbackIdForDidTapPolyline = callbackId;
                    break;
                case CustomMapView.EVENT_DID_BEGIN_DRAGGING_MARKER:
                    this.mapEventsListener.addOnMarkerDragListener(this);
                    savedCallbackIdForDidBeginDraggingMarker = callbackId;
                    break;
                case CustomMapView.EVENT_DID_DRAG_MARKER:
                    this.mapEventsListener.addOnMarkerDragListener(this);
                    savedCallbackIdForDidDragMarker = callbackId;
                    break;
                case CustomMapView.EVENT_DID_END_DRAGGING_MARKER:
                    this.mapEventsListener.addOnMarkerDragListener(this);
                    savedCallbackIdForDidEndDraggingMarker = callbackId;
                    break;
                case CustomMapView.EVENT_DID_TAP_MY_LOCATION_BUTTON:
                    this.mapEventsListener.addOnMyLocationButtonClickListener(this);
                    savedCallbackIdForDidTapMyLocationButton = callbackId;
                    if (preventDefault == null) {
                        preventDefault = false;
                    }
                    preventDefaultForDidTapMyLocationButton = preventDefault;
                    break;
                case CustomMapView.EVENT_DID_TAP_MY_LOCATION_DOT:
                    this.mapEventsListener.addOnMyLocationClickListener(this);
                    savedCallbackIdForDidTapMyLocationDot = callbackId;
                    break;
                case (CustomMapView.EVENT_DID_TAP_POI):
                    this.mapEventsListener.addOnPoiClickListener(this);
                    savedCallbackIdForDidTapPoi = callbackId;
                    break;
                case (CustomMapView.EVENT_DID_BEGIN_MOVING_CAMERA):
                    this.mapEventsListener.addOnCameraMoveStartedListener(this);
                    savedCallbackIdForDidBeginMovingCamera = callbackId;
                    break;
                case (CustomMapView.EVENT_DID_MOVE_CAMERA):
                    this.mapEventsListener.addOnCameraMoveListener(this);
                    savedCallbackIdForDidMoveCamera = callbackId;
                    break;
                case (CustomMapView.EVENT_DID_END_MOVING_CAMERA):
                    this.mapEventsListener.addOnCameraIdleListener(this);
                    savedCallbackIdForDidEndMovingCamera = callbackId;
                    break;
            }
        }
    }

    public void createMap(String callbackId, BoundingRect boundingRect, MapCameraPosition mapCameraPosition, MapPreferences mapPreferences) {
        savedCallbackIdForCreate = callbackId;

        this.mapCameraPosition = mapCameraPosition;
        this.mapPreferences = mapPreferences;

        GoogleMapOptions googleMapOptions = this.mapPreferences.generateGoogleMapOptions();
        googleMapOptions.camera(this.mapCameraPosition.cameraPosition);

        mapView = new MapView(activity, googleMapOptions);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getScaledPixels(boundingRect.width), getScaledPixels(boundingRect.height));
        lp.topMargin = getScaledPixels(boundingRect.y);
        lp.leftMargin = getScaledPixels(boundingRect.x);

        mapView.setLayoutParams(lp);

        mapView.onCreate(null);
        mapView.onStart();
        mapView.getMapAsync(this);
    }

    @SuppressLint("MissingPermission")
    public JSObject invalidateMap() {
        if (this.googleMap == null) {
            return null;
        }

        UiSettings googleMapUISettings = this.googleMap.getUiSettings();

        // set gestures
        googleMapUISettings.setRotateGesturesEnabled(this.mapPreferences.gestures.getBoolean(MapPreferencesGestures.ROTATE_ALLOWED_KEY));
        googleMapUISettings.setScrollGesturesEnabled(this.mapPreferences.gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_KEY));
        googleMapUISettings.setScrollGesturesEnabledDuringRotateOrZoom(this.mapPreferences.gestures.getBoolean(MapPreferencesGestures.SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY));
        googleMapUISettings.setTiltGesturesEnabled(this.mapPreferences.gestures.getBoolean(MapPreferencesGestures.TILT_ALLOWED_KEY));
        googleMapUISettings.setZoomGesturesEnabled(this.mapPreferences.gestures.getBoolean(MapPreferencesGestures.ZOOM_ALLOWED_KEY));

        // set controls
        googleMapUISettings.setCompassEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.COMPASS_BUTTON_KEY));
        googleMapUISettings.setIndoorLevelPickerEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY));
        googleMapUISettings.setMapToolbarEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.MAP_TOOLBAR_KEY));
        googleMapUISettings.setMyLocationButtonEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.MY_LOCATION_BUTTON_KEY));
        googleMapUISettings.setZoomControlsEnabled(this.mapPreferences.controls.getBoolean(MapPreferencesControls.ZOOM_BUTTONS_KEY));

        // set appearance
        this.googleMap.setMapType(this.mapPreferences.appearance.type);
        this.googleMap.setMapStyle(this.mapPreferences.appearance.style);
        this.googleMap.setBuildingsEnabled(this.mapPreferences.appearance.isBuildingsShown);
        this.googleMap.setIndoorEnabled(this.mapPreferences.appearance.isIndoorShown);
        if (hasPermission()) {
            this.googleMap.setMyLocationEnabled(this.mapPreferences.appearance.isMyLocationDotShown);
        }
        this.googleMap.setTrafficEnabled(this.mapPreferences.appearance.isTrafficShown);

        return getResultForMap();
    }

    public JSObject getMap() {
        return getResultForMap();
    }

    public CameraPosition getCameraPosition() {
        if (this.googleMap != null) {
            return this.googleMap.getCameraPosition();
        }
        return null;
    }

    public void moveCamera(Integer duration) {
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(this.mapCameraPosition.cameraPosition);

        if (duration == null || duration <= 0) {
            googleMap.moveCamera(cameraUpdate);
        } else {
            googleMap.animateCamera(cameraUpdate, duration, null);
        }
    }

    private int getScaledPixels(float pixels) {
        // Get the screen's density scale
        final float scale = activity.getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    public void addToView(ViewGroup parent) {
        parent.addView(mapView);
    }

    public void removeFromView(ViewGroup parent) {
        parent.removeView(mapView);
    }

    public void clear() {
        clusterManager.clearItems();
        googleMap.clear();
        markers.clear();
        clusterItems.clear();
        polygons.clear();
        polylines.clear();
        circles.clear();
        clusterManager.cluster();
        shapesAboveMarkers = false;
        markerVisibilityCorrector.clear();
    }

    public void addMarker(CustomMarker customMarker, @Nullable Consumer<Marker> consumer) {
        // the only way to guarantee the visibility IS EFFECTIVE is to set it in MarkerOptions.
        // Otherwise ~1% of markers which are considered invisible will actually visible.
        MarkerOptions markerOptions = customMarker.getMarkerOptions();
        final boolean origVisibility = markerOptions.isVisible();
        final boolean shouldHide = markerVisibilityCorrector.isCoveredWithShape(
                markerOptions.getPosition());
        markerOptions.visible(!shouldHide);
        customMarker.addToMap(
                activity,
                googleMap,
                (marker) -> {
                    markerOptions.visible(origVisibility); // restore ORIGINAL visibility in options only!
                    markerVisibilityCorrector.updateVisibility(shouldHide, origVisibility, marker);
                    markers.put(customMarker.markerId, marker);
                    if (consumer != null) {
                        consumer.accept(marker);
                    }
                }
        );
    }

    public boolean removeMarker(String markerId) {
        Marker marker = markers.remove(markerId);
        if (marker != null) {
            marker.remove();
            markerVisibilityCorrector.remove(marker);
            return true;
        } else {
            CustomClusterItem item = clusterItems.remove(markerId);
            if (item != null) {
                if (clusterManager.removeItem(item)) {
                    clusterManager.cluster();
                    return true;
                }
            }
        }
        return false;
    }

    private int nIconsToLoad = 0;

    public void addCluster(
            Collection<CustomMarker> customMarkers,
            @Nullable IconDescriptor clusterIconDescriptor,
            @Nullable CaptionPreferences clusterCaptionPreferences,
            @Nullable Runnable complete) {
        asyncLoadClusterIcon(clusterIconDescriptor, clusterCaptionPreferences);
        final int n = customMarkers.size();
        nIconsToLoad += n;
        for (CustomMarker customMarker : customMarkers) {
            final CustomClusterItem item = new CustomClusterItem(customMarker);
            clusterManager.addItem(item);
            clusterItems.put(item.getCustomMarker().markerId, item);
            item.getCustomMarker().asyncLoadIcon(
                    activity,
                    () -> {
                        clusterManager.updateItem(item);
                        clusterManager.cluster();
                        if (--nIconsToLoad == 0 && complete != null) {
                            complete.run();
                        }
                    });
        }
    }

    public void addPolygon(CustomPolygon customPolygon, @Nullable Consumer<ShapePolygon> consumer) {
        customPolygon.addToMap(activity, googleMap, (shapePolygon) -> {
            polygons.put(customPolygon.id, shapePolygon);
            markerVisibilityCorrector.correctMarkerVisibility();
            if (consumer != null) {
                consumer.accept(shapePolygon);
            }
        });
    }

    public ShapePolygon getPolygon(String polygonId) {
        return polygons.get(polygonId);
    }

    public boolean updatePolygon(String polygonId, CustomPolygon customPolygon) {
        ShapePolygon polygon = polygons.get(polygonId);
        if (polygon != null) {
            customPolygon.updateShape(polygon);
            markerVisibilityCorrector.correctMarkerVisibility();
            return true;
        }
        return false;
    }

    public boolean removePolygon(String polygonId) {
        ShapePolygon polygon = polygons.remove(polygonId);
        if (polygon != null) {
            polygon.remove();
            markerVisibilityCorrector.correctMarkerVisibility();
            return true;
        }
        return false;
    }

    public ShapePolyline addPolyline(CustomPolyline customPolyline) {
        ShapePolyline polyline = customPolyline.addToMap(googleMap);
        polylines.put(customPolyline.id, polyline);
        return polyline;
    }

    public ShapePolyline getPolyline(String polylineId) {
        return polylines.get(polylineId);
    }

    public boolean updatePolyline(String polylineId, CustomPolyline customPolyline) {
        ShapePolyline polyline = polylines.get(polylineId);
        if (polyline != null) {
            customPolyline.updateShape(polyline);
            return true;
        }
        return false;
    }

    public boolean removePolyline(String polylineId) {
        ShapePolyline polyline = polylines.remove(polylineId);
        if (polyline != null) {
            polyline.remove();
            return true;
        }
        return false;
    }

    public ShapeCircle addCircle(CustomCircle customCircle) {
        ShapeCircle circle = customCircle.addToMap(googleMap);
        circles.put(customCircle.id, circle);
        markerVisibilityCorrector.correctMarkerVisibility();
        return circle;
    }

    public ShapeCircle getCircle(String circleId) {
        return circles.get(circleId);
    }

    public boolean updateCircle(String circleId, CustomCircle customCircle) {
        ShapeCircle circle = circles.get(circleId);
        if (circle != null) {
            customCircle.updateShape(circle);
            markerVisibilityCorrector.correctMarkerVisibility();
            return true;
        }
        return false;
    }

    public boolean removeCircle(String circleId) {
        ShapeCircle circle = circles.remove(circleId);
        if (circle != null) {
            circle.remove();
            markerVisibilityCorrector.correctMarkerVisibility();
            return true;
        }
        return false;
    }

    private void asyncLoadClusterIcon(
            @Nullable IconDescriptor clusterIconDescriptor,
            @Nullable CaptionPreferences captionPreferences) {
        if (clusterIconDescriptor == null) {
            return;
        }
        new AsyncIconLoader(clusterIconDescriptor, activity)
                .load((bitmap) -> {
                    clusterRenderer.setIcon(bitmap);
                    if (captionPreferences != null) {
                        clusterRenderer.setCaptionPreferences(captionPreferences);
                    }
                });
    }

    private JSObject getResultForMap() {
        if (this.mapView != null && this.googleMap != null) {
            // initialize JSObjects
            JSObject result = new JSObject();

            JSObject resultGoogleMap = new JSObject();
            result.put("googleMap", resultGoogleMap);

            JSObject resultPreferences = new JSObject();
            resultGoogleMap.put("preferences", resultPreferences);

            JSObject resultGestures = new JSObject();
            resultPreferences.put("gestures", resultGestures);

            JSObject resultControls = new JSObject();
            resultPreferences.put("controls", resultControls);

            JSObject resultAppearance = new JSObject();
            resultPreferences.put("appearance", resultAppearance);

            // get UISettings
            UiSettings googleMapUISettings = this.googleMap.getUiSettings();

            // return mapId
            resultGoogleMap.put("mapId", id);

            // return cameraPosition
            this.getResultForCameraPosition(resultGoogleMap);

            // return gestures
            resultGestures.put(MapPreferencesGestures.ROTATE_ALLOWED_KEY, googleMapUISettings.isRotateGesturesEnabled());
            resultGestures.put(MapPreferencesGestures.SCROLL_ALLOWED_KEY, googleMapUISettings.isScrollGesturesEnabled());
            resultGestures.put(MapPreferencesGestures.SCROLL_ALLOWED_DURING_ROTATE_OR_ZOOM_KEY, googleMapUISettings.isScrollGesturesEnabledDuringRotateOrZoom());
            resultGestures.put(MapPreferencesGestures.TILT_ALLOWED_KEY, googleMapUISettings.isTiltGesturesEnabled());
            resultGestures.put(MapPreferencesGestures.ZOOM_ALLOWED_KEY, googleMapUISettings.isZoomGesturesEnabled());

            // return controls
            resultControls.put(MapPreferencesControls.COMPASS_BUTTON_KEY, googleMapUISettings.isCompassEnabled());
            // resultControls.put(MapPreferencesControls.INDOOR_LEVEL_PICKER_KEY, googleMapUISettings.isIndoorLevelPickerEnabled());
            resultControls.put(MapPreferencesControls.MAP_TOOLBAR_KEY, googleMapUISettings.isMapToolbarEnabled());
            resultControls.put(MapPreferencesControls.MY_LOCATION_BUTTON_KEY, googleMapUISettings.isMyLocationButtonEnabled());
            resultControls.put(MapPreferencesControls.ZOOM_BUTTONS_KEY, googleMapUISettings.isZoomControlsEnabled());

            // return appearance
            resultAppearance.put(MapPreferencesAppearance.TYPE_KEY, this.googleMap.getMapType());
            resultAppearance.put(MapPreferencesAppearance.BUILDINGS_SHOWN_KEY, this.googleMap.isBuildingsEnabled());
            resultAppearance.put(MapPreferencesAppearance.INDOOR_SHOWN_KEY, this.googleMap.isIndoorEnabled());
            resultAppearance.put(MapPreferencesAppearance.MY_LOCATION_DOT_SHOWN_KEY, this.googleMap.isMyLocationEnabled());
            resultAppearance.put(MapPreferencesAppearance.TRAFFIC_SHOWN_KEY, this.googleMap.isTrafficEnabled());

            return result;
        }
        return null;
    }

    private JSObject getResultForCameraPosition(JSObject resultObjectToExtend) {
        JSObject resultCameraPosition = new JSObject();
        resultObjectToExtend.put("cameraPosition", resultCameraPosition);

        JSObject resultCameraPositionTarget = new JSObject();
        resultCameraPosition.put("target", resultCameraPositionTarget);

        // get CameraPosition
        CameraPosition cameraPosition = this.getCameraPosition();

        // return cameraPosition
        resultCameraPositionTarget.put("latitude", cameraPosition.target.latitude);
        resultCameraPositionTarget.put("longitude", cameraPosition.target.longitude);
        resultCameraPosition.put("bearing", cameraPosition.bearing);
        resultCameraPosition.put("tilt", cameraPosition.tilt);
        resultCameraPosition.put("zoom", cameraPosition.zoom);

        return resultObjectToExtend;
    }

    private JSObject getResultForPosition(Location location) {
        // initialize JSObjects to return
        JSObject result = new JSObject();
        JSObject positionResult = new JSObject();
        result.put("position", positionResult);

        // get position values
        positionResult.put("latitude", location.getLatitude());
        positionResult.put("longitude", location.getLongitude());

        // return result
        return result;
    }

    private JSObject getResultForPosition(LatLng latLng) {
        // initialize JSObjects to return
        JSObject result = new JSObject();
        JSObject positionResult = new JSObject();
        result.put("position", positionResult);

        // get position values
        positionResult.put("latitude", latLng.latitude);
        positionResult.put("longitude", latLng.longitude);

        // return result
        return result;
    }

    private JSObject getResultForPoi(PointOfInterest pointOfInterest) {
        // initialize JSObjects to return
        JSObject result = new JSObject();
        JSObject poiResult = new JSObject();
        JSObject positionResult = new JSObject();

        result.put("poi", poiResult);
        poiResult.put("position", positionResult);

        // get position values
        positionResult.put("latitude", pointOfInterest.latLng.latitude);
        positionResult.put("longitude", pointOfInterest.latLng.longitude);

        // get other values
        poiResult.put("name", pointOfInterest.name);
        poiResult.put("placeId", pointOfInterest.placeId);

        // return result
        return result;
    }
}
