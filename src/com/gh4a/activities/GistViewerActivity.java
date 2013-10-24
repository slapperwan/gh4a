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
import java.lang.ref.WeakReference;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class GistViewerActivity extends BaseSherlockFragmentActivity {

    private String mUserLogin;
    private String mFilename;
    private String mGistId;
    private String mData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        mFilename = getIntent().getExtras().getString(Constants.Gist.FILENAME);
        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.web_viewer);
        
        ActionBar mActionBar = getSupportActionBar();
        mActionBar.setTitle(mFilename);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();
        new LoadGistTask(this, true).execute(mGistId, mFilename);
    }
    
    private static class LoadGistTask extends AsyncTask<String, Void, String> {

        private WeakReference<GistViewerActivity> mTarget;
        private boolean mException;
        private boolean mHighlight;
        
        public LoadGistTask(GistViewerActivity activity, boolean highlight) {
            mTarget = new WeakReference<GistViewerActivity>(activity);
            mHighlight = highlight;
        }

        @Override
        protected String doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    GistService gistService = new GistService(client);
                    return gistService.getGist(params[0]).getFiles().get(params[1]).getContent();
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
        protected void onPostExecute(String result) {
            if (mTarget.get() != null) {
                GistViewerActivity activity = mTarget.get();
                if (mException) {
                    activity.showError();
                }
                else {
                    mTarget.get().fillData(result, mHighlight);
                }
            }
        }
    }
    
    private void fillData(String data, boolean highlight) {
        WebView webView = (WebView) findViewById(R.id.web_view);

        WebSettings s = webView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(true);

        webView.setWebViewClient(webViewClient);

        mData = data;
        String highlighted = StringUtils.highlightSyntax(mData, highlight, mFilename);
        webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "utf-8", "");
    }

    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView webView, String url) {
            hideLoading();
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };
    
    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openGistActivity(this, mUserLogin, mGistId, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}
