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
        tvName.setText(gist.getUser().getLogin());
        tvName.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                getApplicationContext().openUserInfoActivity(GistActivity.this,
                        gist.getUser().getLogin(), null);
            }
        });
        
        TextView tvGistId = (TextView) findViewById(R.id.tv_gist_id);
        tvGistId.setText("Gist : " + gist.getUser().getLogin());
        
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
                tvFilename.setTextAppearance(getApplicationContext(),
                        R.style.default_text_medium_url);
                tvFilename.setBackgroundResource(R.drawable.default_link);
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
}
