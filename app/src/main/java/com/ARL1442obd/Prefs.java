/*
T�RKAY B�L�YOR turkaybiliyor@hotmail.com
 */
package com.ARL1442obd;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.ARL1442obd.R;
public class Prefs extends PreferenceActivity {		
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        addPreferencesFromResource(R.xml.preference);
    }	 
}