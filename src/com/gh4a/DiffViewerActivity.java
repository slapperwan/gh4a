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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;

public class DiffViewerActivity extends BaseActivity {

    private String mRepoOwner;
    private String mRepoName;
    private String mSha;
    private String mDiff;
    private String mFilePath;
    private String mFilename;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_viewer);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mDiff = data.getString(Constants.Commit.DIFF);
        mFilePath = data.getString(Constants.Object.PATH);

        mFilename = FileUtils.getFileName(mFilePath);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mFilename);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        WebView diffView = (WebView) findViewById(R.id.web_view);
        hideLoading();

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
        
        if (FileUtils.isImage(mFilename)) {
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
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.download_dark);
            menu.getItem(1).setIcon(R.drawable.web_site_dark);
            menu.getItem(2).setIcon(R.drawable.social_share_dark);
        }
        
        menu.removeItem(R.id.download);
        
        menu.add(0, 10, Menu.NONE, "View file @" + mSha.substring(0, 7))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        String diffUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mSha;
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openCommitInfoActivity(this, mRepoOwner, mRepoName, mSha, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;     
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(diffUrl));
                startActivity(browserIntent);
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Commit #" + mSha.substring(0, 7) + " at " + mRepoOwner + "/" + mRepoName);
                shareIntent.putExtra(Intent.EXTRA_TEXT,  "Commit #" + mSha.substring(0, 7) 
                        + " at " + mRepoOwner + "/" + mRepoName + " " + diffUrl);
                shareIntent = Intent.createChooser(shareIntent, "Share");
                startActivity(shareIntent);
                return true;
            case 10:
                Intent intent = new Intent().setClass(this, FileViewerActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                intent.putExtra(Constants.Object.PATH, mFilePath);
                intent.putExtra(Constants.Object.REF, mSha);
                intent.putExtra(Constants.Object.NAME, mFilename);
                intent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(intent);
                
                return true;
            default:
                return true;
        }
    }
}
