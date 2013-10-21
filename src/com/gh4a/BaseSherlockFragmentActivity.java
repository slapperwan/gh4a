/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import org.ocpsoft.pretty.time.PrettyTime;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.gh4a.Constants.LoaderResult;

/**
 * The Base activity.
 */
public class BaseSherlockFragmentActivity extends SherlockFragmentActivity {

    /** The Constant pt. */
    protected static final PrettyTime pt = new PrettyTime(new Locale(""));

    /* (non-Javadoc)
     * @see android.content.ContextWrapper#getApplicationContext()
     */
    @Override
    public Gh4Application getApplicationContext() {
        return (Gh4Application) super.getApplicationContext();
    }

    /**
     * Common function when device search button pressed, then open
     * SearchActivity.
     *
     * @return true, if successful
     */
    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent().setClass(getApplication(), SearchActivity.class);
        startActivity(intent);
        return true;
    }

    /**
     * Hide keyboard.
     *
     * @param binder the binder
     */
    public void hideKeyboard(IBinder binder) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu = setupOptionMenu(menu);
        if (!isAuthorized()) {
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.anon_menu, menu);
        }
        return true;        
    }
    
    public Menu setupOptionMenu(Menu menu) {
        return menu;
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.logout:
            if (isAuthorized()) {
                logout();
            }
            else {
                Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                        |Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
            return true;
        case R.id.login:
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                    |Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        case R.id.about:
            openAboutDialog();
            return true;
        case R.id.bookmarks:
            openBookmarkActivity();
            return true;
        default:
            return setMenuOptionItemSelected(item);
        }
    }

    public void logout() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null) != null){
                Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.commit();
                Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                        |Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                this.finish();
            }
        }
    }
    
    public void openBookmarkActivity() {
        //should be override at sub class
    }
    
    public boolean setMenuOptionItemSelected(MenuItem item) {
        return true;
    }
    
    public void openAboutDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.about_dialog);
        
        TextView tvCopyright = (TextView) dialog.findViewById(R.id.copyright);
        tvCopyright.setText("Copyright " + Calendar.getInstance().get(Calendar.YEAR) + " Azwan Adli");
        
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            dialog.setTitle(getResources().getString(R.string.app_name) + " v" + versionName);
        } 
        catch (PackageManager.NameNotFoundException e) {
            dialog.setTitle(getResources().getString(R.string.app_name));
        }
        
        Button btnByEmail = (Button) dialog.findViewById(R.id.btn_by_email);
        btnByEmail.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                sendEmail();
            }
        });
        
        Button btnByGh4a = (Button) dialog.findViewById(R.id.btn_by_gh4a);
        btnByGh4a.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(BaseSherlockFragmentActivity.this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, getResources().getString(R.string.my_username));
                    intent.putExtra(Constants.Repository.REPO_NAME, getResources().getString(R.string.my_repo));
                    startActivity(intent);
                }
                else {
                    showMessage(getString(R.string.login_prompt), false);
                }
            }
        });
        
        dialog.show();
    }

    private void sendEmail() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.my_email) });
        sendIntent.setType("message/rfc822");
        startActivity(Intent.createChooser(sendIntent, getString(R.string.send_email_title)));
    }

    public void openFeedbackDialog() {
        Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.feedback_dialog);
        dialog.setTitle(getResources().getString(R.string.feedback));
        
        Button btnByEmail = (Button) dialog.findViewById(R.id.btn_by_email);
        btnByEmail.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                sendEmail();
            }
        });
        
        Button btnByGh4a = (Button) dialog.findViewById(R.id.btn_by_gh4a);
        btnByGh4a.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(BaseSherlockFragmentActivity.this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, getResources().getString(R.string.my_username));
                    intent.putExtra(Constants.Repository.REPO_NAME, getResources().getString(R.string.my_repo));
                    startActivity(intent);
                }
                else {
                    showMessage(getString(R.string.login_prompt), false);
                }
            }
        });
        
        dialog.show();
    }
    
    public void connectionErrorDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setTitle(getResources().getString(R.string.donate));
        dialog.setContentView(R.layout.donate_dialog);
        Button btn = (Button) dialog.findViewById(R.id.btn_donate);
        btn.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                String url = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=CLFEUAAXKXLLU&lc=MY&item_name=Donate%20for%20Gh4a&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHosted";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);     
            }
        });
        dialog.show();
    }
    
    /**
     * Checks if is authenticated.
     *
     * @return true, if is authenticated
     */
//    public boolean isAuthenticated() {
//        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
//                Constants.PREF_NAME, MODE_PRIVATE);
//        
//        if (sharedPreferences != null) {
//            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
//                    && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
//                return true;
//            }
//            else {
//                return false;
//            }
//        }
//        else {
//            return false;
//        }
//    }
    
    public boolean isAuthorized() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null) != null){
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return false;
        }
    }
    
    public String getAuthLogin() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null){
                return sharedPreferences.getString(Constants.User.USER_LOGIN, null);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    /**
     * Gets the auth password.
     *
     * @return the auth password
     */
//    public String getAuthPassword() {
//        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
//                Constants.PREF_NAME, MODE_PRIVATE);
//        
//        if (sharedPreferences != null) {
//            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
//                    && sharedPreferences.getString(Constants.User.USER_PASSWORD, null) != null){
//                return sharedPreferences.getString(Constants.User.USER_PASSWORD, null);
//            }
//            else {
//                return null;
//            }
//        }
//        else {
//            return null;
//        }
//    }
    
    /**
     * Show error.
     */
    public void showError() {
        showError(false);
    }

    /**
     * Show error.
     *
     * @param finishThisActivity the finish this activity
     */
    public void showError(boolean finishThisActivity) {
        showMessage(getString(R.string.error_toast), finishThisActivity);
    }
    
    public void showMessage(String message, boolean finishThisActivity) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
        if (finishThisActivity) {
            super.finish();
        }
    }
    
    public boolean isSettingEnabled(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(key, false);
    }
    
    public String getSettingStringValue(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, null);
    }
    
    public String getAuthToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null);
        return token;
    }
    
    public void unauthorized() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                    && sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null) != null){
                Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.commit();
                Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        }
    }
    
    public void showLoading() {
        if (findViewById(R.id.pager) != null) {
            findViewById(R.id.pager).setVisibility(View.INVISIBLE);
        }
        else if (findViewById(R.id.web_view) != null) {
            findViewById(R.id.web_view).setVisibility(View.INVISIBLE);
        }
        else if (findViewById(R.id.list_view) != null) {
            findViewById(R.id.list_view).setVisibility(View.INVISIBLE);
        }
        else if (findViewById(R.id.main_content) != null) {
            findViewById(R.id.main_content).setVisibility(View.INVISIBLE);
        }
        if (findViewById(R.id.pb) != null) {
            findViewById(R.id.pb).setVisibility(View.VISIBLE);
        }
    }
    
    public void hideLoading() {
        if (findViewById(R.id.pager) != null) {
            findViewById(R.id.pager).setVisibility(View.VISIBLE);
        }
        else if (findViewById(R.id.list_view) != null) {
            findViewById(R.id.list_view).setVisibility(View.VISIBLE);
        }
        else if (findViewById(R.id.main_content) != null) {
            findViewById(R.id.main_content).setVisibility(View.VISIBLE);
        }
        else if (findViewById(R.id.web_view) != null) {
            findViewById(R.id.web_view).setVisibility(View.VISIBLE);
        }
        
        if (findViewById(R.id.pb) != null) {
            findViewById(R.id.pb).setVisibility(View.GONE);
        }
    }
    
    public ProgressDialog showProgressDialog(String message, boolean cancelable) {
        return ProgressDialog.show(this, "", message, cancelable);
    }
    
    public void stopProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }
    
    public void setErrorView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.error);
        Button btnHome = (Button) findViewById(R.id.btn_home);
        btnHome.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (isAuthorized()) {
                    getApplicationContext().openUserInfoActivity(BaseSherlockFragmentActivity.this, 
                            getAuthLogin(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                else {
                    Intent intent = new Intent().setClass(BaseSherlockFragmentActivity.this, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });
    }
    
    public boolean isLoaderError(HashMap<Integer, Object> result) {
        if (result.get(LoaderResult.ERROR) != null
                && (Boolean) result.get(LoaderResult.ERROR)) {
            if (result.get(LoaderResult.AUTH_ERROR) != null
                    && (Boolean) result.get(LoaderResult.AUTH_ERROR)) {
                logout();
            }
            Toast.makeText(this, (String) result.get(LoaderResult.ERROR_MSG), Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            return false;
        }
    }

    public ViewPager setupPager(PagerAdapter adapter, int[] titleResIds) {
        final ActionBar actionBar = getSupportActionBar();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(adapter);

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {}

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                actionBar.getTabAt(position).select();
            }
        });

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < titleResIds.length; i++) {
            actionBar.addTab(actionBar.newTab()
                .setText(titleResIds[i])
                .setTabListener(new TabListener(this, i, pager)));
        }

        return pager;
    }
}