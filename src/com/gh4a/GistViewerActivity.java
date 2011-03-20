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
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.services.GistService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

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
        
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        tvViewRaw.setVisibility(View.VISIBLE);
        tvViewRaw.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                TextView tvViewRaw = (TextView) view;
                if ("Raw".equals(tvViewRaw.getText())) {
                    new LoadGistTask(GistViewerActivity.this, false).execute(mGistId, mFilename);
                }
                else {
                    new LoadGistTask(GistViewerActivity.this, true).execute(mGistId, mFilename);
                }
            }
        });
        
        TextView tvDownload = (TextView) findViewById(R.id.tv_download);
        tvDownload.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                boolean success = FileUtils.save(mFilename, mData);
                if (success) {
                    showMessage("File saved at " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/" + mFilename, false);
                }
                else {
                    showMessage("Unable to save the file", false);
                }
            }
        });
        
        setBreadCrumb();
        
        new LoadGistTask(this, true).execute(mGistId, mFilename);
    }
    
    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        createBreadcrumb(getResources().getString(R.string.gist_filename, mFilename), breadCrumbHolders);
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
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    GistService service = factory.createGistService();
                    Authentication auth = new LoginPasswordAuthentication(mTarget.get().getAuthUsername(),
                            mTarget.get().getAuthPassword());
                    service.setAuthentication(auth);
                    InputStream is = service.getGistContent(params[0], params[1]);
                    try {
                        return StringUtils.convertStreamToString(is);
                    }
                    catch (IOException e) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        mException = true;
                        return null;
                    }
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
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        if (highlight) {
            tvViewRaw.setText("Raw");
        }
        else {
            tvViewRaw.setText("Highlight");
        }
        
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
        webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "", "");
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
