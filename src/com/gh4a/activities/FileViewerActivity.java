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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.util.List;

public class FileViewerActivity extends WebViewerActivity {
    protected WebViewClient mWebViewClient = new WebViewClient() {
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
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRef = data.getString(Constants.Object.REF);
        getSupportLoaderManager().initLoader(0, null, mFileCallback);
    }

    private void loadContent(RepositoryContents content) {
        String base64Data = content.getContent();
        if (base64Data != null && FileUtils.isImage(mPath)) {
            String imageUrl = "data:image/" + FileUtils.getFileExtension(mPath) + ";base64," + base64Data;
            String htmlImage = StringUtils.highlightImage(imageUrl);
            mWebView.loadDataWithBaseURL("file:///android_asset/", htmlImage, null, "utf-8", null);
        } else {
            String data = base64Data != null ? new String(EncodingUtils.fromBase64(base64Data)) : "";
            String highlightedText = StringUtils.highlightSyntax(data, true, mPath, mRepoOwner, mRepoName, mRef);

            mWebView.loadDataWithBaseURL("file:///android_asset/", highlightedText, null, "utf-8", null);
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

        menu.add(0, 10, Menu.NONE, getString(R.string.history))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openRepositoryInfoActivity(this, mRepoOwner, mRepoName,
                null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public void setWebViewClient() {
        mWebView.setWebViewClient(mWebViewClient);
    }

    @Override
    public String getUrl() {
        return "/blob/" + mRef + "/" + mPath;
    }

    @Override
    public String getShareSubject() {
        return getString(R.string.share_file_subject,
                FileUtils.getFileName(mPath), mRepoOwner + "/" + mRepoName);
    }
}