/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * INDIserver is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with INDIserver.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2012 Alexander Tuschen <atuschen75 at gmail dot com>
 *
 */

package de.hallenbeck.indiserver.activities;

import java.util.List;

import de.hallenbeck.indiserver.R;
import de.hallenbeck.indiserver.server.INDIservice;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Main activity. This is where you could control the server, i.e. set the drivers.
 * Setting are saved in preferences.
 * There is also one button to start the background-service with the actual server
 * @author atuschen
 */

public class main extends PreferenceActivity {
    private Button startServer;
	public Context context;
	
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
   
        if (Build.VERSION.SDK_INT<13) {
        	// This is for Android < 4.0
        	addPreferencesFromResource(R.xml.preferences_old);
        	findPreference("start").setDefaultValue((Boolean) isServiceRunning());
        	findPreference("start").setOnPreferenceChangeListener(PrefListener);
        } else {
            // This is for Android >=4.0
        	if (hasHeaders()) {
        		startServer = new Button(this);
        		if (isServiceRunning()) {        			
        			startServer.setText("Stop Server");
        			startServer.setOnClickListener(stopListener);
        		} else {
        			startServer.setText("Start Server");
        			startServer.setOnClickListener(startListener);
        		}
        		setListFooter(startServer);
        	}
        }
	}
	
	

	/**
	 * Check if the Server is running
	 * @return
	 */
	private boolean isServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("de.hallenbeck.indiserver.server.INDIservice".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	// This is for Android < 4.0
	public OnPreferenceChangeListener PrefListener = new OnPreferenceChangeListener () {
		public boolean onPreferenceChange (Preference preference, Object newValue) {
			boolean val = (Boolean) newValue;
			if (val) {
				startService(new Intent(main.this, INDIservice.class));
			} else {
				stopService(new Intent(main.this, INDIservice.class));
			}
			return true;
		}
	};
	

	// This is for Android >= 4.0
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    // This is for Android >= 4.0
    public static class PrefsServerControl extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_control);
        }
    }
    
    // This is for Android >= 4.0
    public static class PrefsDeviceDrivers extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_devices);
        }
    }
	
	// This is for Android >= 4.0
	public OnClickListener startListener = new OnClickListener() {
		public void onClick(View v) {
			startService(new Intent(main.this, INDIservice.class));
			startServer.setText("Stop Server");
			startServer.setOnClickListener(stopListener);
		}
	};
	
	// This is for Android >= 4.0
	public OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v) {
			stopService(new Intent(main.this, INDIservice.class));
			startServer.setText("Start Server");
			startServer.setOnClickListener(startListener);
		}
	}; 
}




























