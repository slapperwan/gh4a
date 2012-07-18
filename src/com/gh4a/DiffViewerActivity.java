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

import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;

public class DiffViewerActivity extends BaseActivity {

    private String mRepoOwner;
    private String mRepoName;
    private String mSha;
    private String mDiff;
    private String mFilePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mDiff = data.getString(Constants.Commit.DIFF);
        mFilePath = data.getString(Constants.Object.PATH);

        String filename = FileUtils.getFileName(mFilePath);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(filename);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        WebView diffView = (WebView) findViewById(R.id.web_view);
        
        WebSettings s = diffView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setUseWideViewPort(true);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setPluginsEnabled(false);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);
        
        if (FileUtils.isImage(filename)) {
            String htmlImage = StringUtils.highlightImage("https://github.com/" + mRepoOwner + "/" + mRepoName + "/raw/" + mSha + "/" + mFilePath);
            diffView.loadDataWithBaseURL("file:///android_asset/", htmlImage, "text/html", "utf-8", "");
        }
        else {
            if (mDiff != null) {
                String formatted = highlightSyntax();
                diffView.loadDataWithBaseURL("file:///android_asset/", formatted, "text/html", "utf-8", "");
            }
            else {
                Toast.makeText(this, "Unable to view diff.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
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
