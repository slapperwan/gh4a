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

import com.gh4a.utils.StringUtils;

/**
 * The GistViewer activity.
 */
public class GistViewerActivity extends BaseActivity {

    /** The user login. */
    private String mUserLogin;
    
    /** The filename. */
    private String mFilename;
    
    /** The gist id. */
    private String mGistId;
    
    /** The loading dialog. */
    private LoadingDialog mLoadingDialog;
    
    /** The data. */
    private String mData;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        mFilename = getIntent().getExtras().getString(Constants.Gist.FILENAME);
        mGistId = getIntent().getExtras().getString(Constants.Gist.ID);
        
        new LoadGistTask(this, true).execute(mGistId, mFilename);
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to load gist.
     */
    private static class LoadGistTask extends AsyncTask<String, Void, String> {

        /** The target. */
        private WeakReference<GistViewerActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /** The highlight. */
        private boolean mHighlight;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param highlight the highlight
         */
        public LoadGistTask(GistViewerActivity activity, boolean highlight) {
            mTarget = new WeakReference<GistViewerActivity>(activity);
            mHighlight = highlight;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
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
    
    /**
     * Fill data into UI components.
     *
     * @param is the is
     * @param highlight the highlight
     */
    private void fillData(String data, boolean highlight) {
        WebView webView = (WebView) findViewById(R.id.web_view);

        WebSettings s = webView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setUseWideViewPort(false);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setPluginsEnabled(false);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);

        webView.getSettings().setUseWideViewPort(true);

        mData = data;
        String highlighted = StringUtils.highlightSyntax(mData, highlight, mFilename);
        webView.setWebViewClient(webViewClient);
        webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "utf-8", "");
    }

    /** The web view client. */
    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView webView, String url) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };
}
