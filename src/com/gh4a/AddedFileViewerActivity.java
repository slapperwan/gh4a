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
import java.util.HashMap;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Blob;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.ObjectService;

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

        Button btnHistoryFile = (Button) findViewById(R.id.btn_view);
        btnHistoryFile.setText(getResources().getString(R.string.object_view_history));
        btnHistoryFile.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                Intent intent = new Intent().setClass(AddedFileViewerActivity.this, AddedFileViewerActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
                intent.putExtra(Constants.Object.TREE_SHA, mTreeSha);
                intent.putExtra(Constants.Object.PATH, mFilePath);
                
                startActivity(intent);
            }
        });
        
        setBreadCrumb();

        new LoadContentTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;
        
        createBreadcrumb(mFilePath, breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load content.
     */
    private static class LoadContentTask extends AsyncTask<Void, Integer, Blob> {

        /** The target. */
        private WeakReference<AddedFileViewerActivity> mTarget;

        /** The exception. */
        private boolean mException;

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
        protected Blob doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    AddedFileViewerActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    ObjectService objectService = factory.createObjectService();
                    String filepath = activity.mFilePath;
                    filepath = filepath.replaceAll(" ", "%20");
                    return objectService.getBlob(activity.mUserLogin,
                                activity.mRepoName,
                                activity.mTreeSha, 
                                filepath);
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
                    if (result.getMimeType().startsWith("text")
                            || result.getMimeType().equals("application/xml")
                            || result.getMimeType().equals("application/sh")
                            || result.getMimeType().equals("application/xhtml+xml")) {
                        mTarget.get().fillData(result);
                    }
                    else {
                        String url = "https://github.com/" + mTarget.get().mUserLogin + "/"
                                + mTarget.get().mRepoName + "/raw/" + mTarget.get().mObjectSha + "/"
                                + mTarget.get().mFilePath;
                        mTarget.get().getApplicationContext().openBrowser(mTarget.get(), url);
                        mTarget.get().mLoadingDialog.dismiss();
                        mTarget.get().finish();                    
                    }
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     *
     * @param blob the blob
     */
    protected void fillData(Blob blob) {
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
        
        String highlighted = highlightSyntax(blob.getData());
        webView.setWebViewClient(webViewClient);
        webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "", "");
    }

    /**
     * Highlight syntax.
     * 
     * @param data the data
     * @return the string
     */
    private String highlightSyntax(String data) {
        data = TextUtils.htmlEncode(data).replace("\n", "<br>");

        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        content.append("<link href='file:///android_asset/prettify.css' rel='stylesheet' type='text/css'/>");
        content.append("<script src='file:///android_asset/prettify.js' type='text/javascript'></script>");
        content.append("</head><body onload='prettyPrint()'><pre class='prettyprint linenums'>");
        content.append(data);
        content.append("</pre></body></html>");

        return content.toString();

    }
    
    /** The web view client. */
    private WebViewClient webViewClient = new WebViewClient() {

        @Override
        public void onPageFinished(WebView webView, String url) {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
        }
    };

}
