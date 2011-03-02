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
import java.util.Arrays;
import java.util.HashMap;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.ObjectService;

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
        
        setBreadCrumb();

        new LoadContentTask(this).execute();
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
    private static class LoadContentTask extends AsyncTask<Void, Integer, InputStream> {

        /** The target. */
        private WeakReference<FileViewerActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /** The show in browser. */
        private boolean showInBrowser;

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
        protected InputStream doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    FileViewerActivity activity = mTarget.get();
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    ObjectService objectService = factory.createObjectService();
    
                    // only show mimetype text/* and xml to WebView, else open
                    // default browser
                    if (activity.mMimeType.startsWith("text")
                            || activity.mMimeType.equals("application/xml")
                            || activity.mMimeType.equals("application/sh")
                            || activity.mMimeType.equals("application/xhtml+xml")) {
                        showInBrowser = false;
                        return objectService.getObjectContent(activity.mUserLogin, activity.mRepoName,
                                activity.mObjectSha);
                    }
                    else {
                        showInBrowser = true;
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(InputStream result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    if (showInBrowser) {
                        String url = "https://github.com/" + mTarget.get().mUserLogin + "/"
                                + mTarget.get().mRepoName + "/raw/" + mTarget.get().mBranchName + "/"
                                + mTarget.get().mPath;
                        mTarget.get().getApplicationContext().openBrowser(mTarget.get(), url);
                        mTarget.get().mLoadingDialog.dismiss();
                        mTarget.get().finish();
                    }
                    else {
                        mTarget.get().fillData(result);
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
    protected void fillData(InputStream is) {
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
        try {
            content = StringUtils.convertStreamToString(is);
            String highlighted = highlightSyntax(content);
            webView.setWebViewClient(webViewClient);
            webView.loadDataWithBaseURL("file:///android_asset/", highlighted, "text/html", "", "");
        }
        catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            showError();
        }
    }

    /**
     * Highlight syntax.
     * 
     * @param data the data
     * @return the string
     */
    private String highlightSyntax(String data) {
        String ext = StringUtils.getFileExtension(mName);
        
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        if (!Arrays.asList(Constants.SKIP_PRETTIFY_EXT).contains(ext)) {
            data = TextUtils.htmlEncode(data).replace("\n", "<br>");
            content.append("<link href='file:///android_asset/prettify.css' rel='stylesheet' type='text/css'/>");
            content.append("<script src='file:///android_asset/prettify.js' type='text/javascript'></script>");
            content.append("</head>");
            content.append("<body onload='prettyPrint()'>");
            content.append("<pre class='prettyprint linenums'>");
        }
        else if ("markdown".equals(ext) 
                || "md".equals(ext)
                || "mdown".equals(ext)){
            content.append("<script src='file:///android_asset/showdown.js' type='text/javascript'></script>");
            content.append("<style type='text/css'>");
            content.append("html,body {");
            content.append("margin:5px;");
            content.append("padding:0;");
            content.append("font-family: Helvetica, Arial, Verdana, sans-serif;");
            content.append("}");
            content.append("pre {");
            content.append("display: block;");
            content.append("background: #F0F0F0;");
            content.append("padding:5px;");
            content.append("}");
            content.append("</style>");
            content.append("</head>");
            content.append("<body>");
            content.append("<div id='content'>");
        }
        else {
            data = TextUtils.htmlEncode(data).replace("\n", "<br>");
            content.append("</head>");
            content.append("<body>");
            content.append("<pre>");
        }
        
        content.append(data);
        
        if ("markdown".equals(ext) 
                || "md".equals(ext)
                || "mdown".equals(ext)){
            content.append("</div>");
            
            content.append("<script>");
            content.append("var text = document.getElementById('content').innerHTML;");
            content.append("var converter = new Showdown.converter();");
            content.append("var html = converter.makeHtml(text);");
            content.append("document.getElementById('content').innerHTML = html;");
            content.append("</script>");
        }
        else {
            content.append("</pre>");
        }
        
        content.append("</body></html>");

        return content.toString();

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
