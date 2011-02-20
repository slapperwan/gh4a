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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

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

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences != null) {
            String username = sharedPreferences.getString(Constants.User.USER_LOGIN, null);
            if (username != null) {
                Intent intent = new Intent().setClass(getApplicationContext(),
                        DashboardActivity.class);
                startActivity(intent);
                return;
            }
        }
        setContentView(R.layout.main);

        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar_main);
        actionBar.addAction(new IntentAction(this, new Intent(getApplicationContext(),
                SearchActivity.class), R.drawable.ic_menu_search));

        mEtUserLogin = (EditText) findViewById(R.id.et_username_main);
        mEtPassword = (EditText) findViewById(R.id.et_password_main);

        Button btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                LoginPasswordAuthentication auth = new LoginPasswordAuthentication(mEtUserLogin
                        .getText().toString(), mEtPassword.getText().toString());
                SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                        Constants.PREF_NAME, MODE_PRIVATE);
                Editor editor = sharedPreferences.edit();
                editor.putString(Constants.User.USER_LOGIN, mEtUserLogin.getText().toString());
                editor.putString(Constants.User.USER_PASSWORD, mEtPassword.getText().toString());
                editor.commit();

                Intent intent = new Intent().setClass(getApplicationContext(),
                        DashboardActivity.class);
                startActivity(intent);
            }
        });
    }
}