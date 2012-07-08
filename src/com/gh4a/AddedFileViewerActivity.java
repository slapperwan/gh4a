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

import org.eclipse.egit.github.core.Blob;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.util.EncodingUtils;

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
 * The AddedFileViewer activity.
 */
public class AddedFileViewerActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The tree sha. */
    private String mTreeSha;
    
    /** The object sha. */
    private String mObjectSha;

    /** The path. */
    private String mFilePath;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    private Blob mBlob;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);
        setUpActionBar();
        
        mUserLogin = getIntent().getStringExtra(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getStringExtra(Constants.Repository.REPO_NAME);
        mTreeSha = getIntent().getStringExtra(Constants.Object.TREE_SHA);
        mObjectSha = getIntent().getStringExtra(Constants.Object.OBJECT_SHA);
        mFilePath = getIntent().getStringExtra(Constants.Object.PATH);

        new LoadContentTask(this).execute(true);
    }

    /**
     * An asynchronous task that runs on a background thread to load content.
     */
    private static class LoadContentTask extends AsyncTask<Boolean, Integer, Blob> {

        /** The target. */
        private WeakReference<AddedFileViewerActivity> mTarget;

        /** The exception. */
        private boolean mException;
        
        private boolean highlight;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadContentTask(AddedFileViewerActivity activity) {
            mTarget = new WeakReference<AddedFileViewerActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Blob doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                highlight = params[0];
                try {
                    AddedFileViewerActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    DataService dataService = new DataService(client);
                    
                    return dataService.getBlob(new RepositoryId(activity.mUserLogin,
                            activity.mRepoName), activity.mObjectSha);
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Blob result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
//                    if (result.getMimeType().startsWith("text")
//                            || result.getMimeType().equals("application/xml")
//                            || result.getMimeType().equals("application/sh")
//                            || result.getMimeType().equals("application/xhtml+xml")) {
                        mTarget.get().mBlob = result;
                        mTarget.get().fillData(result, highlight);
//                    }
//                    else {
//                        String url = "https://github.com/" + mTarget.get().mUserLogin + "/"
//                                + mTarget.get().mRepoName + "/raw/" + mTarget.get().mObjectSha + "/"
//                                + mTarget.get().mFilePath;
//                        mTarget.get().getApplicationContext().openBrowser(mTarget.get(), url);
//                        mTarget.get().mLoadingDialog.dismiss();
//                        mTarget.get().finish();                    
//                    }
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     *
     * @param blob the blob
     */
    protected void fillData(Blob blob, boolean highlight) {
        String data = new String(EncodingUtils.fromBase64(blob.getContent()));
        
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

        // webView.setWebViewClient(new WebChrome2());
        webView.getSettings().setUseWideViewPort(true);
        
        data = StringUtils.highlightSyntax(data, highlight, mFilePath);
        webView.setWebViewClient(webViewClient);
        webView.loadDataWithBaseURL("file:///android_asset/", data, "text/html", "utf-8", "");
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
