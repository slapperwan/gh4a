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

import java.util.HashMap;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;

import com.gh4a.holder.BreadCrumbHolder;

/**
 * The DiffViewer activity.
 */
public class DiffViewerActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The sha. */
    protected String mSha;

    /** The diff. */
    protected String mDiff;

    /** The filename. */
    protected String mFilePath;
    
    /** The tree sha. */
    protected String mTreeSha;

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

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mTreeSha = data.getString(Constants.Object.TREE_SHA);
        mDiff = data.getString(Constants.Commit.DIFF);
        mFilePath = data.getString(Constants.Object.PATH);

        setBreadCrumb();
        
        TextView tvViewFile = (TextView) findViewById(R.id.tv_view);
        tvViewFile.setText(getResources().getString(R.string.object_view_file_at, mSha.substring(0, 7)));
        tvViewFile.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View view) {
                Intent intent = new Intent().setClass(DiffViewerActivity.this, AddedFileViewerActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                intent.putExtra(Constants.Object.TREE_SHA, mTreeSha);
                intent.putExtra(Constants.Object.PATH, mFilePath);
                
                startActivity(intent);
            }
        });
        
        TextView tvViewRaw = (TextView) findViewById(R.id.tv_view_raw);
        tvViewRaw.setVisibility(View.GONE);
        
        TextView tvDownload = (TextView) findViewById(R.id.tv_download);
        tvDownload.setVisibility(View.GONE);

        WebView diffView = (WebView) findViewById(R.id.web_view);
        String formatted = highlightSyntax();
        WebSettings s = diffView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setPluginsEnabled(false);
        s.setSupportZoom(true);
        s.setUseWideViewPort(true);
        diffView.loadDataWithBaseURL("file:///android_asset/", formatted, "text/html", "utf-8", "");
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[3];

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

        // Commit
        b = new BreadCrumbHolder();
        b.setLabel(String.format(getResources().getString(R.string.commit_sha, mSha.substring(0, 7))));
        b.setTag(Constants.Commit.COMMIT);
        data.put(Constants.Object.OBJECT_SHA, mSha);
        b.setData(data);
        breadCrumbHolders[2] = b;

        createBreadcrumb(mFilePath, breadCrumbHolders);
    }

    /**
     * Highlight syntax.
     * 
     * @return the string
     */
    private String highlightSyntax() {
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        content.append("</head><body><pre>");

        String encoded = TextUtils.htmlEncode(mDiff);
        String[] lines = encoded.split("\n");
        for (String line : lines) {
            if (line.startsWith("@@")) {
                line = "<div style=\"background-color: #EAF2F5;\">" + line + "</div>";
            }
            else if (line.startsWith("+")) {
                line = "<div style=\"background-color: #DDFFDD; border-color: #00AA00;\">" + line
                        + "</div>";
            }
            else if (line.startsWith("-")) {
                line = "<div style=\"background-color: #FFDDDD; border-color: #CC0000;\">" + line
                        + "</div>";
            }
            else {
                line = "<div>" + line + "</div>";
            }
            content.append(line);
        }
        content.append("</pre></body></html>");
        return content.toString();

    }
}
