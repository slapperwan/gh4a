/*
 * Copyright 2011 Azwan Adli Abdullah Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.api.v2.schema.Repository;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.UserService;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;

/**
 * The MineDroid activity.
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
            getApplicationContext().openUserInfoActivity(this, getAuthUsername(), null);
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

        Button btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                new LoginTask(Github4AndroidActivity.this).execute();
//                String username = mEtUserLogin.getText().toString();
//                String password = mEtPassword.getText().toString();
//
//                LoginPasswordAuthentication auth = new LoginPasswordAuthentication(username,
//                        password);
//
//                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
//                UserService userService = factory.createUserService();
//                userService.setAuthentication(auth);
//                progressDialog = LoadingDialog.show(Github4AndroidActivity.this, "Please wait",
//                        "Authenticating...", false, false);
//                try {
//                    userService.getKeys();// test auth
//
//                    SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
//                            Constants.PREF_NAME, MODE_PRIVATE);
//                    Editor editor = sharedPreferences.edit();
//                    editor.putString(Constants.User.USER_LOGIN, username.trim());
//                    editor.putString(Constants.User.USER_PASSWORD, password.trim());
//                    editor.commit();
//
//                    getApplicationContext().openUserInfoActivity(Github4AndroidActivity.this,
//                            username, null);
//                }
//                catch (GitHubException e) {
//                    if (e.getCause() != null
//                            && e.getCause().getMessage().equalsIgnoreCase(
//                                    "Received authentication challenge is null")) {
//                        Toast.makeText(Github4AndroidActivity.this,
//                                getResources().getString(R.string.invalid_login),
//                                Toast.LENGTH_SHORT).show();
//                        return;
//                    }
//                    Github4AndroidActivity.this.showError();
//                    return;
//                }
//                finally {
//                    progressDialog.dismiss();
//                }
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
        
        private boolean isAuthError; 

        /**
         * Instantiates a new load repository list task.
         * 
         * @param activity the activity
         */
        public LoginTask(Github4AndroidActivity activity) {
            mTarget = new WeakReference<Github4AndroidActivity>(activity);
        }

        /** The hide main view. */
        private boolean hideMainView;

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
                if (mException) {
                    Toast.makeText(activity,
                            activity.getResources().getString(R.string.invalid_login),
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    SharedPreferences sharedPreferences = activity.getSharedPreferences(
                            Constants.PREF_NAME, MODE_PRIVATE);
                    Editor editor = sharedPreferences.edit();
                    editor.putString(Constants.User.USER_LOGIN, activity.mEtUserLogin.getText().toString().trim());
                    editor.putString(Constants.User.USER_PASSWORD, activity.mEtPassword.getText().toString().trim());
                    editor.commit();

                    activity.getApplicationContext().openUserInfoActivity(activity, activity.mEtUserLogin.getText().toString().trim(),
                            null);
                }
            }
        }
    }
}