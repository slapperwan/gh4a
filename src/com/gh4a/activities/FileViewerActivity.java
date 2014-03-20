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

import java.util.List;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class FileViewerActivity extends LoadingFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    private String mPath;
    private String mRef;
    private String mSha;
    private String mDiff;
    private boolean mInDiffMode;

    private WebView mWebView;

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            setContentShown(true);
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
    };

    private LoaderCallbacks<List<RepositoryContents>> mFileCallback =
            new LoaderCallbacks<List<RepositoryContents>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader(int id, Bundle args) {
            return new ContentLoader(FileViewerActivity.this, mRepoOwner, mRepoName, mPath, mRef);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryContents>> result) {
            setContentEmpty(true);
            if (!result.handleError(FileViewerActivity.this)) {
                List<RepositoryContents> data = result.getData();
                if (data != null && !data.isEmpty()) {
                    loadContent(data.get(0));
                    setContentEmpty(false);
                }
            }
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mPath = data.getString(Constants.Object.PATH);
        mRef = data.getString(Constants.Object.REF);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mDiff = data.getString(Constants.Commit.DIFF);
        mInDiffMode = data.getString(Constants.Object.TREE_SHA) != null;

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.web_viewer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(FileUtils.getFileName(mPath));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (mDiff != null) {
            showDiff();
        } else {
            getSupportLoaderManager().initLoader(0, null, mFileCallback);
            setContentShown(false);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        mWebView = (WebView) findViewById(R.id.web_view);

        WebSettings s = mWebView.getSettings();
        s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        s.setAllowFileAccess(true);
        s.setBuiltInZoomControls(true);
        s.setLightTouchEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setSupportZoom(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptEnabled(true);
        s.setUseWideViewPort(true);

        mWebView.setWebViewClient(mWebViewClient);
    }

    private void showDiff() {
        StringBuilder content = new StringBuilder();
        content.append("<html><head><title></title>");
        content.append("</head><body><pre>");

        String encoded = TextUtils.htmlEncode(mDiff);
        String[] lines = encoded.split("\n");
        for (String line : lines) {
            if (line.startsWith("@@")) {
                line = "<div style=\"background-color: #EAF2F5;\">" + line + "</div>";
            } else if (line.startsWith("+")) {
                line = "<div style=\"background-color: #DDFFDD; border-color: #00AA00;\">"
                        + line + "</div>";
            } else if (line.startsWith("-")) {
                line = "<div style=\"background-color: #FFDDDD; border-color: #CC0000;\">"
                        + line + "</div>";
            } else {
                line = "<div>" + line + "</div>";
            }
            content.append(line);
        }
        content.append("</pre></body></html>");

        setupWebView();
        mWebView.loadDataWithBaseURL("file:///android_asset/", content.toString(), null, "utf-8", null);
    }

    private void loadContent(RepositoryContents content) {
        setupWebView();

        String base64Data = content.getContent();
        if (base64Data != null && FileUtils.isImage(mPath)) {
            String imageUrl = "data:image/" + FileUtils.getFileExtension(mPath) + ";base64," + base64Data;
            String htmlImage = StringUtils.highlightImage(imageUrl);
            mWebView.loadDataWithBaseURL("file:///android_asset/", htmlImage, null, "utf-8", null);
        } else {
            String data = base64Data != null ? new String(EncodingUtils.fromBase64(base64Data)) : "";
            String highlighted = StringUtils.highlightSyntax(data, true, mPath, mRepoOwner, mRepoName, mRef);
            mWebView.loadDataWithBaseURL("file:///android_asset/", highlighted, null, "utf-8", null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menu.removeItem(R.id.search);
        }

        if (mDiff != null) {
            menu.add(0, 11, Menu.NONE, getString(R.string.object_view_file_at, mSha.substring(0, 7)))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        } else if (!mInDiffMode) {
            menu.add(0, 10, Menu.NONE, getString(R.string.history))
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        if (mInDiffMode) {
            IntentUtils.openCommitInfoActivity(this, mRepoOwner, mRepoName,
                    mSha, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            IntentUtils.openRepositoryInfoActivity(this, mRepoOwner, mRepoName,
                    null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String urlBase = "https://github.com/" + mRepoOwner + "/" + mRepoName;
        String url = mDiff != null ? urlBase + "/commit/" + mSha : urlBase + "/blob/" + mRef + "/" + mPath;

        switch (item.getItemId()) {
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                if (mDiff != null) {
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_commit_subject,
                            mSha.substring(0, 7), mRepoOwner + "/" + mRepoName));
                } else {
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file_subject,
                            FileUtils.getFileName(mPath), mRepoOwner + "/" + mRepoName));
                }
                shareIntent.putExtra(Intent.EXTRA_TEXT,  url);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.search:
                doSearch();
                return true;
            case 10:
                Intent historyIntent = new Intent(this, CommitHistoryActivity.class);
                historyIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                historyIntent.putExtra(Constants.Repository.NAME, mRepoName);
                historyIntent.putExtra(Constants.Object.PATH, mPath);
                historyIntent.putExtra(Constants.Object.REF, mRef);
                historyIntent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(historyIntent);
                return true;
            case 11:
                Intent viewIntent = new Intent(this, FileViewerActivity.class);
                viewIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                viewIntent.putExtra(Constants.Repository.NAME, mRepoName);
                viewIntent.putExtra(Constants.Object.PATH, mPath);
                viewIntent.putExtra(Constants.Object.REF, mSha);
                viewIntent.putExtra(Constants.Object.OBJECT_SHA, mSha);
                startActivity(viewIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @TargetApi(11)
    private void doSearch() {
        if (mWebView != null) {
            mWebView.showFindDialog(null, true);
        }
    }
}