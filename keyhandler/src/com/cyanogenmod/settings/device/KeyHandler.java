/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TorchManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.content.ContextWrapper;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private static final int GESTURE_REQUEST = 1;

    private static final String MEIZU_GESTURE_DATA =
            "/sys/devices/platform/mx-gs/gesture_data";

    private static final String KEY_GESTURE_HAPTIC_FEEDBACK =
            "touchscreen_gesture_haptic_feedback";

    private static final String TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK =
            "touchscreen_gesture_haptic_feedback";

    private static final String ACTION_DISMISS_KEYGUARD =
            "com.android.keyguard.action.DISMISS_KEYGUARD_SECURELY";

    // M2Note gesture codes
    public static final int DOUBLE_TAP = 0xA0;
    public static final int SWIPE_X_LEFT = 0xB0;
    public static final int SWIPE_X_RIGHT = 0xB1;
    public static final int SWIPE_Y_UP = 0xB2;
    public static final int SWIPE_Y_DOWN = 0xB3;

    public static final int UNICODE_E = 0xC0;
    public static final int UNICODE_C = 0xC1;
    public static final int UNICODE_W = 0xC2;
    public static final int UNICODE_M = 0xC3;
    public static final int UNICODE_O = 0xC4;
    public static final int UNICODE_S = 0xC5;
    public static final int UNICODE_V_UP = 0xC6;
    public static final int UNICODE_V_DOWN = 0xC7;
    public static final int UNICODE_V_L = 0xC8;
    public static final int UNICODE_V_R = 0xC9;
    public static final int UNICODE_Z = 0xCA;

    private static final int GESTURE_WAKELOCK_DURATION = 3000;

    private final Context mContext;

    private final PowerManager mPowerManager;
    private KeyguardManager mKeyguardManager;
    private EventHandler mEventHandler;
    private SensorManager mSensorManager;
    private TorchManager mTorchManager;
    private Sensor mProximitySensor;
    private Vibrator mVibrator;
    private WakeLock mProximityWakeLock;
    private WakeLock mGestureWakeLock;
    private int mProximityTimeOut;
    private boolean mProximityWakeSupported;
    private Instrumentation m_Instrumentation;
    private Context cmaContext = null;

    private boolean mNotificationSliderVibrate;

    public KeyHandler(Context context) {
        mContext = context;
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mEventHandler = new EventHandler();
        m_Instrumentation = new Instrumentation();
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");

        try {
            cmaContext = mContext.createPackageContext("com.cyanogenmod.settings.device", Context.CONTEXT_RESTRICTED);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final Resources resources = mContext.getResources();
        mProximityTimeOut = resources.getInteger(
                com.android.internal.R.integer.config_proximityCheckTimeout);
        mProximityWakeSupported = resources.getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);

        if (mProximityWakeSupported) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mProximityWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "ProximityWakeLock");
        }

        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    private void ensureKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager =
                    (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    private void ensureTorchManager() {
        if (mTorchManager == null) {
            mTorchManager = (TorchManager) mContext.getSystemService(Context.TORCH_SERVICE);
        }
    }

    private class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                int gestureData = (int) msg.obj;
                Log.i(TAG, "Handling gesture: " + gestureData);
                switch (gestureData) {
                    case DOUBLE_TAP:
                        mPowerManager.wakeUpWithProximityCheck(SystemClock.uptimeMillis());
                        doHapticFeedback();
                        break;
                    case UNICODE_C:
                        ensureKeyguardManager();
                        final String action;
                        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                        if (mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()) {
                            action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE;
                        } else {
                            mContext.sendBroadcastAsUser(new Intent(ACTION_DISMISS_KEYGUARD),
                                    UserHandle.CURRENT);
                            action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
                        }
                        mPowerManager.wakeUp(SystemClock.uptimeMillis());
                        Intent intent = new Intent(action, null);
                        startActivitySafely(intent);
                        doHapticFeedback();
                        break;
                    case UNICODE_Z:
                        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                        doHapticFeedback();
                        break;
                    case SWIPE_Y_DOWN:
                        ensureTorchManager();
                        mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                        mTorchManager.toggleTorch();
                        doHapticFeedback();
                        break;
                    case SWIPE_X_LEFT:
                        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                        doHapticFeedback();
                        break;
                    case SWIPE_X_RIGHT:
                        dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
                        doHapticFeedback();
                        break;
                    case UNICODE_S:
                    case UNICODE_W:
                    case UNICODE_M:
                        int zenMode = Global.ZEN_MODE_OFF;
                        if (gestureData == UNICODE_M) {
                            zenMode = Global.ZEN_MODE_NO_INTERRUPTIONS;
                        } else if (gestureData == UNICODE_S) {
                            zenMode = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
                        }
                        Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE,
                                zenMode);
                        if (mNotificationSliderVibrate) {
                            doHapticFeedback();
                        }
                        mNotificationSliderVibrate = true;
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Gesture EventHandler", e);
                //e.printStackTrace();
            }
        }
    }

    private boolean isLastPressHomeButton = false;

    public boolean handleKeyEvent(KeyEvent event) {
        boolean isHandled = false;

        switch (event.getScanCode()) {
            case 102: // Home button event
                if (event.getDeviceId() == 2 && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (isLastPressHomeButton) {
                        isHandled = true;
                    }
                }
                isLastPressHomeButton = event.getDeviceId() == 6;
                break;
            case 195: // Gesture event
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return true;

                try {
                    int gestureData = readGestureData();
                    if (gestureData == 0)
                        return true;

                    if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
                        Message msg = getGestureMessage(gestureData);
                        boolean defaultProximity = mContext.getResources().getBoolean(
                                com.android.internal.R.bool.config_proximityCheckOnWakeEnabledByDefault);
                        boolean proximityWakeCheckEnabled = Settings.System.getInt(mContext.getContentResolver(),
                                Settings.System.PROXIMITY_ON_WAKE, defaultProximity ? 1 : 0) == 1;
                        if (mProximityWakeSupported && proximityWakeCheckEnabled && mProximitySensor != null) {
                            mEventHandler.sendMessageDelayed(msg, mProximityTimeOut);
                            processEvent(event);
                        } else {
                            mEventHandler.sendMessage(msg);
                        }
                    }
                    return true;
                } catch (Exception e) {
                    // Gesture data read failed
                    return false;
                }
        }
        return isHandled;
    }

    private Message getGestureMessage(int gestureData) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = gestureData;
        return msg;
    }

    private Message getMessageForKeyEvent(KeyEvent keyEvent) {
        Message msg = mEventHandler.obtainMessage(GESTURE_REQUEST);
        msg.obj = keyEvent;
        return msg;
    }

    private void processEvent(final KeyEvent keyEvent) {
        mProximityWakeLock.acquire();
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                mProximityWakeLock.release();
                mSensorManager.unregisterListener(this);
                if (!mEventHandler.hasMessages(GESTURE_REQUEST)) {
                    // The sensor took to long, ignoring.
                    return;
                }
                mEventHandler.removeMessages(GESTURE_REQUEST);
                if (event.values[0] == mProximitySensor.getMaximumRange()) {
                    Message msg = getMessageForKeyEvent(keyEvent);
                    mEventHandler.sendMessage(msg);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

        }, mProximitySensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(int keycode) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper != null) {
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
            helper.sendMediaButtonEvent(event, true);
            event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
            helper.sendMediaButtonEvent(event, true);
        } else {
            Log.w(TAG, "Unable to send media key event");
        }
    }

    private void startActivitySafely(Intent intent) {
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            UserHandle user = new UserHandle(UserHandle.USER_CURRENT);
            mContext.startActivityAsUser(intent, null, user);
        } catch (ActivityNotFoundException e) {
            // Ignore
        }
    }

    // TODO implement it more graceful way
    private boolean getCMApref(String prefKey, boolean defVal) {
        SharedPreferences cmaPrefs = null;
        cmaPrefs = cmaContext.getSharedPreferences("com.cyanogenmod.settings.device_preferences", Context.MODE_MULTI_PROCESS);
        return cmaPrefs.getBoolean(prefKey, defVal);
    }

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
        boolean enabled = getCMApref(TOUCHSCREEN_GESTURE_HAPTIC_FEEDBACK, false);
        if (enabled) {
            mVibrator.vibrate(50);
        }
    }

    public String readSingleLine(File f) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            String line = reader.readLine();
            reader.close();
            return line;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private int readGestureData() throws Exception {
        File f = new File(MEIZU_GESTURE_DATA);
        return Integer.valueOf(readSingleLine(f));
    }
}