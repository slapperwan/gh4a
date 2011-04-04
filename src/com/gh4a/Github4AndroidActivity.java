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

import java.lang.ref.WeakReference;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AutoCompleteTextView.Validator;

import com.gh4a.utils.StringUtils;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.UserService;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;

/**
 * The Github4Android activity.
 */
public class Github4AndroidActivity extends BaseActivity {

    /** The EditText for user login. */
    protected EditText mEtUserLogin;

    /** The EditText for password. */
    protected EditText mEtPassword;

    protected ProgressDialog mProgressDialog;

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isAuthenticated()) {
            getApplicationContext().openUserInfoActivity(this, getAuthUsername(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            return;
        }
        setContentView(R.layout.main);

        // setup actionbar
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar_main);
        actionBar.addAction(new IntentAction(this, new Intent(getApplicationContext(),
                SearchActivity.class), R.drawable.ic_search));

        // setup title breadcrumb
        createBreadcrumb("Login", null);
        
        mEtUserLogin = (EditText) findViewById(R.id.et_username_main);
        mEtPassword = (EditText) findViewById(R.id.et_password_main);

        final Button btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                hideKeyboard(btnLogin.getWindowToken());
                if (!StringUtils.checkEmail(mEtUserLogin.getText().toString())) {
                    new LoginTask(Github4AndroidActivity.this).execute();    
                }
                else {
                    Toast.makeText(Github4AndroidActivity.this, "Please enter username instead of email", Toast.LENGTH_LONG).show();
                }
            }
        });

        TextView tvExplore = (TextView) findViewById(R.id.tv_explore);
        tvExplore.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent().setClass(Github4AndroidActivity.this,
                        SearchActivity.class);
                startActivity(intent);
            }
        });
    }

    private static class LoginTask extends AsyncTask<Void, Void, Void> {

        /** The target. */
        private WeakReference<Github4AndroidActivity> mTarget;

        /** The exception. */
        private boolean mException;
        
        /** The is auth error. */
        private boolean isAuthError; 
        
        /** The exception msg. */
        private String mExceptionMsg;

        /**
         * Instantiates a new load repository list task.
         * 
         * @param activity the activity
         */
        public LoginTask(Github4AndroidActivity activity) {
            mTarget = new WeakReference<Github4AndroidActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void...params) {
            if (mTarget.get() != null) {
                try {
                    String username = mTarget.get().mEtUserLogin.getText().toString();
                    String password = mTarget.get().mEtPassword.getText().toString();

                    LoginPasswordAuthentication auth = new LoginPasswordAuthentication(username,
                            password);
                    
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    UserService userService = factory.createUserService();
                    userService.setAuthentication(auth);
                    userService.getKeys();// test auth
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    if (e.getCause() != null
                            && e.getCause().getMessage().equalsIgnoreCase(
                                    "Received authentication challenge is null")) {
                        isAuthError = true;
                    }
                    mException = true;
                    mExceptionMsg = e.getMessage();
                    if (e.getCause() != null) {
                        mExceptionMsg += ", " + e.getCause().getMessage();
                    }
                }
                
                return null;
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mProgressDialog = ProgressDialog.show(mTarget.get(), "Please wait",
                        "Authenticating...", false, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                Github4AndroidActivity activity = mTarget.get();
                activity.mProgressDialog.dismiss();
                if (mException && isAuthError) {
                    Toast.makeText(activity,
                            activity.getResources().getString(R.string.invalid_login),
                            Toast.LENGTH_SHORT).show();
                }
                else if (mException) {
                    Toast.makeText(activity, mExceptionMsg, Toast.LENGTH_LONG).show();
                }
                else {
                    SharedPreferences sharedPreferences = activity.getSharedPreferences(
                            Constants.PREF_NAME, MODE_PRIVATE);
                    Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.User.USER_LOGIN, activity.mEtUserLogin.getText().toString().trim());
                    editor.putString(Constants.User.USER_PASSWORD, activity.mEtPassword.getText().toString().trim());
                    editor.commit();
                    activity.getApplicationContext().openUserInfoActivity(activity, activity.mEtUserLogin.getText().toString().trim(),
                            null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    activity.finish();
                    
                }
            }
        }
    }
}