package com.codeminders.inotes.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.codeminders.inotes.Constants;
import com.codeminders.inotes.R;

public class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private CheckBox wifiCheckbox;
    private Spinner spinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        setTitle(R.string.sync_settings_title);

        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);
        wifiCheckbox = (CheckBox) findViewById(R.id.wifi_checkbox);

        String[] times = {
                getString(R.string.time_5_min),
                getString(R.string.time_15_min),
                getString(R.string.time_30_min),
                getString(R.string.time_1_hour),
                getString(R.string.time_2_hours),
                getString(R.string.time_4_hours),
                getString(R.string.time_8_hours),
                getString(R.string.time_12_hours),
                getString(R.string.time_24_hours),
        };
        spinner = (Spinner) findViewById(R.id.spinner1);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, times);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner .setAdapter(adapter);

        Button save = (Button) findViewById(R.id.prefs_save);
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(Constants.SYNC_INTERVAL, spinner.getSelectedItemPosition());
                editor.putBoolean(Constants.SYNC_TYPE, wifiCheckbox.isChecked());
                editor.commit();
                Toast.makeText(getApplicationContext(), R.string.conf_save, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Boolean onlyWifi = prefs.getBoolean(Constants.SYNC_TYPE, false);
        wifiCheckbox.setChecked(onlyWifi);

        int itemPosition = prefs.getInt(Constants.SYNC_INTERVAL, 4);
        spinner.setSelection(itemPosition);
    }

}
