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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.utils.StringUtils;

public class GistActivity extends BaseSherlockFragmentActivity {

    private String mGistId;
    private String mUserLogin;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);
        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.gist);
        
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(getResources().getQuantityString(R.plurals.gist, 1) + " " + mGistId);
        mActionBar.setSubtitle(mUserLogin);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        new LoadGistTask(this).execute(mGistId);
    }
    
    private static class LoadGistTask extends AsyncTask<String, Void, Gist> {

        private WeakReference<GistActivity> mTarget;
        private boolean mException;
        
        public LoadGistTask(GistActivity activity) {
            mTarget = new WeakReference<GistActivity>(activity);
        }

        @Override
        protected Gist doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    GistService gistService = new GistService(client);
                    return gistService.getGist(params[0]);                    
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Gist result) {
            if (mTarget.get() != null) {
                GistActivity activity = mTarget.get();
                activity.hideLoading();
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }
    
    private void fillData(final Gist gist) {
        TextView tvDesc = (TextView) findViewById(R.id.tv_desc);
        if (StringUtils.isBlank(gist.getDescription())) {
            tvDesc.setVisibility(View.GONE);
        }
        else {
            tvDesc.setText(gist.getDescription());
            tvDesc.setVisibility(View.VISIBLE);
        }
        
        TextView tvCreatedAt = (TextView) findViewById(R.id.tv_created_at);
        tvCreatedAt.setText(pt.format(gist.getCreatedAt()));
        
        LinearLayout llFiles = (LinearLayout) findViewById(R.id.ll_files);

        Map<String, GistFile> files = gist.getFiles();
        if (files != null) {
            Iterator<String> iter = files.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                final GistFile gistFile = files.get(key);
                TextView tvFilename = new TextView(getApplicationContext());
                SpannableString content = new SpannableString(gistFile.getFilename());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextColor(Color.parseColor("#0099cc"));
                tvFilename.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
                tvFilename.setTextAppearance(this, android.R.attr.textAppearanceMedium);
                tvFilename.setPadding(0, 8, 0, 8);
                tvFilename.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(GistActivity.this,
                                GistViewerActivity.class);
                        intent.putExtra(Constants.User.USER_LOGIN, gist.getUser().getLogin());
                        intent.putExtra(Constants.Gist.FILENAME, gistFile.getFilename());
                        intent.putExtra(Constants.Gist.ID, gist.getId());
                        startActivity(intent);
                    }
                });
                llFiles.addView(tvFilename);
            }
        }
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent().setClass(this, GistListActivity.class);
                intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;     
            default:
                return true;
        }
    }
}
