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
import com.github.api.v2.schema.Blob;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.ObjectService;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;

/**
 * The DiffViewer activity.
 */
public class FileViewerActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The object sha. */
    protected String mObjectSha;

    /** The tree sha. */
    private String mTreeSha;

    /** The name. */
    protected String mName;

    /** The mime type. */
    private String mMimeType;

    /** The path. */
    private String mPath;

    /** The branch name. */
    private String mBranchName;

    /** The from btn id. */
    private int mFromBtnId;

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
        mObjectSha = getIntent().getStringExtra(Constants.Object.OBJECT_SHA);
        mTreeSha = getIntent().getStringExtra(Constants.Object.TREE_SHA);
        mName = getIntent().getStringExtra(Constants.Object.NAME);
        mMimeType = getIntent().getStringExtra(Constants.Object.MIME_TYPE);
        mPath = getIntent().getStringExtra(Constants.Object.PATH);
        mBranchName = getIntent().getStringExtra(Constants.Repository.REPO_BRANCH);
        mFromBtnId = getIntent().getExtras().getInt(Constants.VIEW_ID);

        TextView tvViewInBrowser = (TextView) findViewById(R.id.tv_in_browser);
        tvViewInBrowser.setVisibility(View.GONE);
        
        TextView tvHistoryFile = (TextView) findViewById(R.id.tv_view);
        tvHistoryFile.setText(getResources().getString(R.string.object_view_history));
        tvHistoryFile.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                Intent intent = new Intent().setClass(FileViewerActivity.this, CommitHistoryActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.OBJECT_SHA, mBranchName);
                intent.putExtra(Constants.Object.PATH, mPath);
                
                startActivity(intent);
            }
        });
        
        
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        tvViewRaw.setVisibility(View.VISIBLE);
        tvViewRaw.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                TextView tvViewRaw = (TextView) view;
                if ("Raw".equals(tvViewRaw.getText())) {
                    new LoadContentTask(FileViewerActivity.this).execute(false);
                }
                else {
                    new LoadContentTask(FileViewerActivity.this).execute(true);
                }
            }
        });
        
        TextView tvDownload = (TextView) findViewById(R.id.tv_download);
        tvDownload.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                String filename = mBlob.getName();
                int idx = filename.lastIndexOf("/");
                
                if (idx != -1) {
                    filename = filename.substring(filename.lastIndexOf("/") + 1, filename.length());
                }

                boolean success = FileUtils.save(filename, mBlob.getData());
                if (success) {
                    showMessage("File saved at " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/download/" + filename, false);
                }
                else {
                    showMessage("Unable to save the file", false);
                }
            }
        });
        
        setBreadCrumb();

        new LoadContentTask(this).execute(true);
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders;
        if (!mPath.equals(mBranchName)) {
            breadCrumbHolders = new BreadCrumbHolder[4];
        }
        else {
            breadCrumbHolders = new BreadCrumbHolder[3];
        }

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

        // branches/tags
        b = new BreadCrumbHolder();
        if (R.id.btn_tags == mFromBtnId) {
            b.setLabel(getResources().getString(R.string.repo_tag));
            b.setTag(Constants.Object.TAGS);
        }
        else {
            b.setTag(Constants.Object.BRANCHES);
            b.setLabel(getResources().getString(R.string.repo_branch));
        }
        b.setData(data);
        breadCrumbHolders[2] = b;

        // branch name
        if (!mPath.equals(mBranchName)) {
            b = new BreadCrumbHolder();
            b.setLabel(mBranchName);
            b.setTag(Constants.Repository.REPO_BRANCH);
            data.put(Constants.Object.TREE_SHA, mTreeSha);
            data.put(Constants.Repository.REPO_BRANCH, mBranchName);
            data.put(Constants.Object.PATH, mPath);
            data.put(Constants.VIEW_ID, String.valueOf(mFromBtnId));
            b.setData(data);
            breadCrumbHolders[3] = b;
        }
        if (!mPath.equals("Tree")) {
            mPath = mPath.replaceFirst("Tree/", "");
        }
        createBreadcrumb(mPath, breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load tree list.
     */
    private static class LoadContentTask extends AsyncTask<Boolean, Integer, Blob> {

        /** The target. */
        private WeakReference<FileViewerActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /** The show in browser. */
        private boolean mShowInBrowser;
        
        private boolean mHighlight;

        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadContentTask(FileViewerActivity activity) {
            mTarget = new WeakReference<FileViewerActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Blob doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                mHighlight = params[0];
                try {
                    FileViewerActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    ObjectService objectService = factory.createObjectService();
    
                    Authentication auth = new LoginPasswordAuthentication(activity.getAuthUsername(), activity.getAuthPassword());
                    objectService.setAuthentication(auth);
                    
                    // only show mimetype text/* and xml to WebView, else open
                    // default browser
//                    if (activity.mMimeType.startsWith("text")
//                            || activity.mMimeType.equals("application/xml")
//                            || activity.mMimeType.equals("application/sh")
//                            || activity.mMimeType.equals("application/xhtml+xml")) {
                        mShowInBrowser = false;
                        return objectService.getBlob(activity.mUserLogin,
                                activity.mRepoName,
                                activity.mTreeSha, 
                                activity.mPath);
//                        return objectService.getObjectContent(activity.mUserLogin, activity.mRepoName,
//                                activity.mObjectSha);
//                    }
//                    else {
//                        mShowInBrowser = true;
//                        return null;
//                    }
    
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
                    if (mShowInBrowser) {
                        String url = "https://github.com/" + mTarget.get().mUserLogin + "/"
                                + mTarget.get().mRepoName + "/raw/" + mTarget.get().mBranchName + "/"
                                + mTarget.get().mPath;
                        mTarget.get().getApplicationContext().openBrowser(mTarget.get(), url);
                        mTarget.get().mLoadingDialog.dismiss();
                        mTarget.get().finish();
                    }
                    else {
                        mTarget.get().mBlob = result;
                        mTarget.get().fillData(result, mHighlight);
                    }
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param is the is
     */
    protected void fillData(Blob blob, boolean highlight) {
        String data = blob.getData();
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

        // webView.setWebViewClient(new WebChrome2());
        webView.getSettings().setUseWideViewPort(true);

        String content;
        //try {
            //content = StringUtils.convertStreamToString(is);
            String highlighted = StringUtils.highlightSyntax(data, highlight, mName);
            webView.setWebViewClient(webViewClient);
            webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "utf-8", "");
        //}
        //catch (IOException e) {
        //    Log.e(Constants.LOG_TAG, e.getMessage(), e);
        //    showError();
        //}
    }

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
