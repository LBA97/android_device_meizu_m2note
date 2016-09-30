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

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TouchscreenGestureSettings extends Activity {
    // M2Note gesture codes
    public static final int DOUBLE_TAP = 0xA0; //160
    public static final int SWIPE_X_LEFT = 0xB0; //176
    public static final int SWIPE_X_RIGHT = 0xB1;
    public static final int SWIPE_Y_UP = 0xB2;
    public static final int SWIPE_Y_DOWN = 0xB3;

    public static final int UNICODE_E = 0xC0; // 192
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

    private static final int[] supportedGesturesKeys = {
            DOUBLE_TAP,
            UNICODE_C,
            UNICODE_Z,
            SWIPE_X_LEFT,
            SWIPE_X_RIGHT,
            UNICODE_W,
            UNICODE_M,
            SWIPE_Y_DOWN
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getActionBar();
        //actionBar.setTitle(getString(R.string.appName));
        actionBar.setDisplayHomeAsUpEnabled(true);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PrefFragment())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    public static class PrefFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        public static final String TAG = PrefFragment.class.getSimpleName();
        public static final String CATEGORY_GESTURES = "category_gestures";
        public static PreferenceCategory gestureCat;

        private SharedPreferences sharedPrefs;
        private String[] actionTitles;
        private String[] actionValues;
        private List<String> actionTitlesList;
        private List<String> actionValuesList;
        private static final List<String> allowedSystemApps = new ArrayList<>();

        static {
            allowedSystemApps.add("com.android.dialer");
            allowedSystemApps.add("com.android.mms");
            allowedSystemApps.add("com.android.settings");
            allowedSystemApps.add("com.android.deskclock");
            allowedSystemApps.add("com.android.calculator2");
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.touchscreen_panel);
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            actionTitlesList = new ArrayList<>();
            actionValuesList = new ArrayList<>();
            actionTitles = this.getResources().getStringArray(R.array.gesture_action_titles);
            actionValues = this.getResources().getStringArray(R.array.gesture_action_values);
            actionTitlesList.addAll(Arrays.asList(actionTitles));
            actionValuesList.addAll(Arrays.asList(actionValues));

            List<ApplicationInfo> packages = getActivity().getPackageManager()
                    .getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInfo : packages) {
                if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        allowedSystemApps.indexOf(appInfo.packageName) == -1)
                    continue;
                actionTitlesList.add(appInfo.loadLabel(getActivity().getPackageManager()).toString());
                actionValuesList.add("launch$" + appInfo.packageName);
            }
            actionTitles = actionTitlesList.toArray(new String[actionTitlesList.size()]);
            actionValues = actionValuesList.toArray(new String[actionValuesList.size()]);
            for (int prefKey : supportedGesturesKeys) {
                ListPreference preference = (ListPreference) findPreference(String.valueOf(prefKey));
                preference.setEntries(actionTitles);
                preference.setEntryValues(actionValues);
                preference.setOnPreferenceChangeListener(this);
                String prefValue = sharedPrefs.getString(String.valueOf(prefKey), "disabled");
                int i = actionValuesList.indexOf(prefValue);
                if (i >= 0) {
                    preference.setSummary(actionTitles[i]);
                    preference.setValueIndex(i);
                }
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            gestureCat = (PreferenceCategory) findPreference(CATEGORY_GESTURES);
            if (gestureCat != null) {
                gestureCat.setEnabled(CMActionsSettings.areGesturesEnabled());
            }
        }

        @Override
        public void onStart() {
            super.onStart();
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            String newValue = (String) o;
            int i = actionValuesList.indexOf(newValue);
            if (i >= 0)
                preference.setSummary(actionTitles[i]);
            sharedPrefs.edit().putBoolean(preference.getKey() + "_enabled",
                    !"disabled".equals(newValue)).apply();
            return true;
        }
    }
}

