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
package com.gh4a.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OAuthService;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

/**
 * The Github4Android activity.
 */
public class Github4AndroidActivity extends BaseSherlockFragmentActivity {

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
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Gh4Application app = Gh4Application.get(this);
        if (app.isAuthorized()) {
            app.openUserInfoActivity(this, app.getAuthLogin(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
            return;
        }
        setContentView(R.layout.main);
        
        BugSenseHandler.setup(this, "6e1b031");
        
        mEtUserLogin = (EditText) findViewById(R.id.et_username_main);
        mEtPassword = (EditText) findViewById(R.id.et_password_main);

        final Button btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                UiUtils.hideImeForView(btnLogin);
                String username = mEtUserLogin.getText().toString();
                String password = mEtPassword.getText().toString();
                if (!StringUtils.checkEmail(username)) {
                    new LoginTask(username, password).execute();
                }
                else {
                    Toast.makeText(Github4AndroidActivity.this,
                            getString(R.string.enter_username_toast), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private class LoginTask extends ProgressDialogTask<Authorization> {
        private String mUserName;
        private String mPassword;
        
        /**
         * Instantiates a new load repository list task.
         */
        public LoginTask(String userName, String password) {
            super(Github4AndroidActivity.this, R.string.please_wait, R.string.authenticating);
            mUserName = userName;
            mPassword = password;
        }

        @Override
        protected Authorization run() throws IOException {
            GitHubClient client = new GitHubClient();
            client.setCredentials(mUserName, mPassword); 
            client.setUserAgent("Gh4a");
                    
            Authorization auth = null;
            OAuthService authService = new OAuthService(client);
            List<Authorization> auths = authService.getAuthorizations();
            for (Authorization authorization : auths) {
                if ("Gh4a".equals(authorization.getNote())) {
                    auth = authorization;
                    break;
                }
            }
                    
            if (auth == null) {
                auth = new Authorization();
                auth.setNote("Gh4a");
                auth.setUrl("http://github.com/slapperwan/gh4a");
                List<String> scopes = new ArrayList<String>();
                scopes.add("user");
                scopes.add("repo");
                scopes.add("gist");
                auth.setScopes(scopes);

                auth = authService.createAuthorization(auth);
            }
            return auth;
        }

        @Override
        protected void onSuccess(Authorization result) {
            SharedPreferences sharedPreferences = getSharedPreferences(
                    Constants.PREF_NAME, MODE_PRIVATE);
            Editor editor = sharedPreferences.edit();
            editor.putString(Constants.User.USER_AUTH_TOKEN, result.getToken());
            editor.putString(Constants.User.USER_LOGIN, mUserName);
            editor.commit();
            
            Gh4Application.get(mContext).openUserInfoActivity(mContext,
                    mUserName.trim(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
        }
    }
}