package com.github.yamill.orientation;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Surface;

import com.facebook.common.logging.FLog;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final BroadcastReceiver receiver;

    public static final String ORIENTATION_STR_LANDSCAPE = "LANDSCAPE";
    public static final String ORIENTATION_STR_LANDSCAPE_LEFT = "LANDSCAPE-LEFT";
    public static final String ORIENTATION_STR_LANDSCAPE_RIGHT = "LANDSCAPE-RIGHT";
    public static final String ORIENTATION_STR_PORTRAIT = "PORTRAIT";
    public static final String ORIENTATION_STR_PORTRAIT_UPSIDE_DOWN = "PORTRAITUPSIDEDOWN";
    public static final String ORIENTATION_STR_UNKNOWN = "UNKNOWN";

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        final ReactApplicationContext ctx = reactContext;

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!ctx.hasActiveCatalystInstance()) {
                    return;
                }

                Configuration newConfig = intent.getParcelableExtra("newConfig");
                Log.d("receiver", String.valueOf(newConfig.orientation));

                String orientationValue = getSpecificOrientationString();

                final DeviceEventManagerModule.RCTDeviceEventEmitter deviceEventEmitter = ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

                // NOTE: only specific orientation event emitter works

                WritableMap params = Arguments.createMap();
                params.putString("specificOrientation", orientationValue);

                deviceEventEmitter.emit("specificOrientationDidChange", params);
            }
        };
        ctx.addLifecycleEventListener(this);
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void getOrientation(Callback callback) {
        final int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);

        if (orientation == "null") {
            callback.invoke(orientationInt, null);
        } else {
            callback.invoke(null, orientation);
        }
    }

    @ReactMethod
    public void getSpecificOrientation(Callback callback) {
        String orientation = getSpecificOrientationString();
        callback.invoke(null, orientation);
    }

    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public @Nullable Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();
        int orientationInt = getReactApplicationContext().getResources().getConfiguration().orientation;

        String orientation = this.getOrientationString(orientationInt);
        if (orientation == "null") {
            constants.put("initialOrientation", null);
        } else {
            constants.put("initialOrientation", orientation);
        }

        return constants;
    }

    // Based on http://stackoverflow.com/a/10383164/1573638
    private String getSpecificOrientationString() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return ORIENTATION_STR_UNKNOWN;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics  dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        String orientation;
        // If device's natural orientation is portrait
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
            && height > width
            || (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
            && width > height
        ) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ORIENTATION_STR_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ORIENTATION_STR_LANDSCAPE_LEFT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ORIENTATION_STR_PORTRAIT_UPSIDE_DOWN;
                    break;
                case Surface.ROTATION_270:
                    orientation = ORIENTATION_STR_LANDSCAPE_RIGHT;
                    break;
                default:
                    orientation = ORIENTATION_STR_UNKNOWN;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ORIENTATION_STR_LANDSCAPE_LEFT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ORIENTATION_STR_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ORIENTATION_STR_LANDSCAPE_RIGHT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ORIENTATION_STR_PORTRAIT_UPSIDE_DOWN;
                    break;
                default:
                    orientation = ORIENTATION_STR_UNKNOWN;
            }
        }

        return orientation;
    }

    private String getOrientationString(int orientation) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return "LANDSCAPE";
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return "PORTRAIT";
        } else if (orientation == Configuration.ORIENTATION_UNDEFINED) {
            return "UNKNOWN";
        } else {
            return "null";
        }
    }

    @Override
    public void onHostResume() {
        final Activity activity = getCurrentActivity();

        assert activity != null;
        activity.registerReceiver(receiver, new IntentFilter("onConfigurationChanged"));
    }

    @Override
    public void onHostPause() {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        try
        {
            activity.unregisterReceiver(receiver);
        }
        catch (java.lang.IllegalArgumentException e) {
            FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
        }
    }

    @Override
    public void onHostDestroy() {
        final Activity activity = getCurrentActivity();
        if (activity == null) return;
        try
        {
            activity.unregisterReceiver(receiver);
        }
        catch (java.lang.IllegalArgumentException e) {
            FLog.e(ReactConstants.TAG, "receiver already unregistered", e);
        }
    }

}
