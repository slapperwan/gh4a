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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.FieldError;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.util.List;
import java.util.Locale;

public class FileViewerActivity extends WebViewerActivity {
    private String mRepoName;
    private String mRepoOwner;
    private String mPath;
    private String mRef;
    private int mHighlightStart;
    private int mHighlightEnd;

    private static final int MENU_ITEM_HISTORY = 10;
    private static final String RAW_URL_FORMAT = "https://raw.githubusercontent.com/%s/%s/%s/%s";

    private LoaderCallbacks<List<RepositoryContents>> mFileCallback =
            new LoaderCallbacks<List<RepositoryContents>>(this) {
        @Override
        protected Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader() {
            return new ContentLoader(FileViewerActivity.this, mRepoOwner, mRepoName, mPath, mRef);
        }

        @Override
        protected void onResultReady(List<RepositoryContents> result) {
            boolean dataLoaded = false;

            if (result != null && !result.isEmpty()) {
                loadContent(result.get(0));
                dataLoaded = true;
            }
            if (!dataLoaded) {
                setContentEmpty(true);
                setContentShown(true);
            }
        }

        @Override
        protected boolean onError(Exception e) {
            if (e instanceof RequestException) {
                RequestError error = ((RequestException) e).getError();
                List<FieldError> errors = error != null ? error.getErrors() : null;
                if (errors != null) {
                    for (FieldError fe : errors) {
                        if ("too_large".equals(fe.getCode())) {
                            openUnsuitableFileAndFinish();
                            return true;
                        }
                    }
                }
            }
            return super.onError(e);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String filename = FileUtils.getFileName(mPath);
        if (FileUtils.isBinaryFormat(filename) && !FileUtils.isImage(filename)) {
            openUnsuitableFileAndFinish();
        } else {
            getSupportLoaderManager().initLoader(0, null, mFileCallback);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(filename);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mPath = extras.getString(Constants.Object.PATH);
        mRef = extras.getString(Constants.Object.REF);
        mHighlightStart = extras.getInt(Constants.Object.HIGHLIGHT_START, -1);
        mHighlightEnd = extras.getInt(Constants.Object.HIGHLIGHT_END, -1);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        getSupportLoaderManager().getLoader(0).onContentChanged();
        setContentShown(false);
        setContentEmpty(false);
        super.onRefresh();
    }

    private void loadContent(RepositoryContents content) {
        String base64Data = content.getContent();
        if (base64Data != null && FileUtils.isImage(mPath)) {
            String imageUrl = "data:image/" + FileUtils.getFileExtension(mPath) +
                    ";base64," + base64Data;
            loadThemedHtml(highlightImage(imageUrl));
        } else {
            String data = base64Data != null ? new String(EncodingUtils.fromBase64(base64Data)) : "";
            loadCode(data, mPath, mRepoOwner, mRepoName, mRef, mHighlightStart, mHighlightEnd);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        if (FileUtils.isImage(mPath) || FileUtils.isMarkdown(mPath)) {
            menu.removeItem(R.id.wrap);
        }

        menu.removeItem(R.id.download);
        MenuItem item = menu.add(0, MENU_ITEM_HISTORY, Menu.NONE, R.string.history);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String url = String.format(Locale.US, "https://github.com/%s/%s/blob/%s/%s",
                mRepoOwner, mRepoName, mRef, mPath);

        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(url));
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_file_subject,
                        FileUtils.getFileName(mPath), mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case MENU_ITEM_HISTORY:
                Intent historyIntent = new Intent(this, CommitHistoryActivity.class);
                historyIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                historyIntent.putExtra(Constants.Repository.NAME, mRepoName);
                historyIntent.putExtra(Constants.Object.PATH, mPath);
                historyIntent.putExtra(Constants.Object.REF, mRef);
                startActivity(historyIntent);
                return true;
         }
         return super.onOptionsItemSelected(item);
     }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }

    private void openUnsuitableFileAndFinish() {
        String url = String.format(Locale.US, RAW_URL_FORMAT, mRepoOwner, mRepoName, mRef, mPath);
        String mime = FileUtils.getMimeTypeFor(FileUtils.getFileName(mPath));
        Intent intent = IntentUtils.createViewerOrBrowserIntent(this, Uri.parse(url), mime);
        if (intent == null) {
            handleLoadFailure(new ActivityNotFoundException());
            findViewById(R.id.retry_button).setVisibility(View.GONE);
        } else {
            startActivity(intent);
            finish();
        }
    }

    private static String highlightImage(String imageUrl) {
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        writeCssInclude(content, "text");
        content.append("</head><body><div class='image'>");
        content.append("<img src='").append(imageUrl).append("' />");
        content.append("</div></body></html>");
        return content.toString();
    }
}
