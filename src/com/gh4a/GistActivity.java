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
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Gist;
import com.github.api.v2.services.GistService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

/**
 * The Gist activity.
 */
public class GistActivity extends BaseActivity {

    /** The gist id. */
    private String mGistId;
    
    /** The loading dialog. */
    private LoadingDialog mLoadingDialog;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gist);
        setUpActionBar();
        
        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);
        
        new LoadGistTask(this).execute(mGistId);
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to load gist.
     */
    private static class LoadGistTask extends AsyncTask<String, Void, Gist> {

        /** The target. */
        private WeakReference<GistActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public LoadGistTask(GistActivity activity) {
            mTarget = new WeakReference<GistActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Gist doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    GistService service = factory.createGistService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    service.setAuthentication(auth);
                    return service.getGist(params[0]);                    
                }
                catch (GitHubException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Gist result) {
            if (mTarget.get() != null) {
                GistActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }
    
    /**
     * Fill data into UI components.
     *
     * @param gist the gist
     */
    private void fillData(final Gist gist) {
        TextView tvName = (TextView) findViewById(R.id.tv_name);
        tvName.setText(gist.getOwner());
        tvName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getApplicationContext().openUserInfoActivity(GistActivity.this,
                        gist.getOwner(), null);
            }
        });
        
        TextView tvGistId = (TextView) findViewById(R.id.tv_gist_id);
        tvGistId.setText("Gist : " + gist.getRepo());
        
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
        
        List<String> files = gist.getFiles();
        if (files != null) {
            for (final String filename : files) {
                TextView tvFilename = new TextView(getApplicationContext());
                SpannableString content = new SpannableString(filename);
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tvFilename.setText(content);
                tvFilename.setTextAppearance(getApplicationContext(),
                        R.style.default_text_medium_url);
                tvFilename.setBackgroundResource(R.drawable.default_link);
                tvFilename.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(GistActivity.this,
                                GistViewerActivity.class);
                        intent.putExtra(Constants.User.USER_LOGIN, gist.getOwner());
                        intent.putExtra(Constants.Gist.FILENAME, filename);
                        intent.putExtra(Constants.Gist.ID, gist.getRepo());
                        startActivity(intent);
                    }
                });
                llFiles.addView(tvFilename);
            }
        }
    }
}
