package com.cs407.safepath;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SeekBarPreference;

public class SettingsActivity extends AppCompatActivity {

    SharedPreferences sp;
    static int radius1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        radius1 = sp.getInt("radius", 0);

    }


    @SuppressLint("ValidFragment")
    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SeekBarPreference seekBarPreference = findPreference("radius");
            if (seekBarPreference != null) {
                if (seekBarPreference.getMax() != 500) {
                    seekBarPreference.setMax(500);
                }
                seekBarPreference.setValue(radius1);
            }

            Preference myPref = (Preference) findPreference("bookmarks");
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), NotesActivity.class);
                    startActivity(intent);
                    return true;
                }
            });

        }



    }
}