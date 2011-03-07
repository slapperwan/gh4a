package com.gh4a;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;
import android.widget.Toast;

public class AppPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Toast.makeText(this, "Please reload your profile page by pressing \'Home\' button at the top left for the changes to take effect", Toast.LENGTH_LONG).show();
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
