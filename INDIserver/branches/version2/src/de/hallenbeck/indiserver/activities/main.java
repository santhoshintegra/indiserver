/*
 *
 * This file is part of INDIserver.
 *
 * INDIserver is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
/* import android.preference.PreferenceActivity; */
/* import android.preference.PreferenceFragment; */
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Main activity. This is where you could control the server, i.e. set the drivers.
 * Setting are saved in preferences.
 * There is also one button to start the background-service with the actual server
 * @author atuschen
 */
/* public class main extends PreferenceActivity { */

public class main extends Activity {
    private Button startServer;
	
	
	
	
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.main);

        startServer = (Button) findViewById(R.id.button1);
        startServer.setOnClickListener(startListener);
        
        // Works only on Android 4.0
        /*
        if (hasHeaders()) {
        	start = new Button(this);
            start.setText("Start Server");
            start.setOnClickListener(startListener);
            setListFooter(start);
            
        } */
    }

	/*
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

  
    public static class PrefsServerControl extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_control);
        }
    }

    public static class PrefsDeviceDrivers extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preference_devices);
        }
    }
	*/
	
	public OnClickListener startListener = new OnClickListener() {
		public void onClick(View v) {
			startService(new Intent(main.this, INDIservice.class));
			startServer.setText("Stop Server");
			startServer.setOnClickListener(stopListener);
		}
	};
	
	public OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v) {
			stopService(new Intent(main.this, INDIservice.class));
			startServer.setText("Start Server");
			startServer.setOnClickListener(startListener);
		}
	}; 
}




























