package com.tesis.datacollector;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {
    public final static String PHONE_NUMBER = "phone_number_preference";
    public final static String DESTINATION_NUMBER = "destination_phone_number_preference";
    public final static String GAP_BETWEEN_CALLS_IN_MSEC = "gap_between_calls_preference";
    public final static String SIGNAL_CHANGE_THRESHOLD = "signal_change_threshold";
    public final static String DISTANCE_THRESHOLD = "distance_threshold";
    public final static String SEND_SMS = "send_sms";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
    }
}
