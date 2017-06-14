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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.print.PrintHelper;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.R;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.FieldError;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.TextMatch;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.util.List;
import java.util.Locale;

public class FileViewerActivity extends WebViewerActivity
        implements PopupMenu.OnMenuItemClickListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String ref, String fullPath) {
        return makeIntent(context, repoOwner, repoName, ref, fullPath, -1, -1, null);
    }

    public static Intent makeIntentWithHighlight(Context context, String repoOwner, String repoName,
            String ref, String fullPath, int highlightStart, int highlightEnd) {
        return makeIntent(context, repoOwner, repoName, ref, fullPath, highlightStart, highlightEnd,
                null);
    }

    public static Intent makeIntentWithSearchMatch(Context context, String repoOwner,
            String repoName, String ref, String fullPath, TextMatch textMatch) {
        return makeIntent(context, repoOwner, repoName, ref, fullPath, -1, -1, textMatch);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName, String ref,
            String fullPath, int highlightStart, int highlightEnd, TextMatch textMatch) {
        return new Intent(context, FileViewerActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("path", fullPath)
                .putExtra("ref", ref)
                .putExtra("highlight_start", highlightStart)
                .putExtra("highlight_end", highlightEnd)
                .putExtra("text_match", textMatch);
    }

    private String mRepoName;
    private String mRepoOwner;
    private String mPath;
    private String mRef;
    private int mHighlightStart;
    private int mHighlightEnd;
    private TextMatch mTextMatch;
    private RepositoryContents mContent;
    private int mLastTouchedLine = 0;

    private static final int MENU_ITEM_HISTORY = 10;
    private static final String RAW_URL_FORMAT = "https://raw.githubusercontent.com/%s/%s/%s/%s";

    private final LoaderCallbacks<List<RepositoryContents>> mFileCallback =
            new LoaderCallbacks<List<RepositoryContents>>(this) {
        @Override
        protected Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader() {
            return new ContentLoader(FileViewerActivity.this, mRepoOwner, mRepoName, mPath, mRef);
        }

        @Override
        protected void onResultReady(List<RepositoryContents> result) {
            if (result != null && !result.isEmpty()) {
                mContent = result.get(0);
                onDataReady();
                setContentEmpty(false);
            } else {
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
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mPath = extras.getString("path");
        mRef = extras.getString("ref");
        mHighlightStart = extras.getInt("highlight_start", -1);
        mHighlightEnd = extras.getInt("highlight_end", -1);
        mTextMatch = (TextMatch) extras.getSerializable("text_match");
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        forceLoaderReload(0);
        setContentShown(false);
        super.onRefresh();
    }

    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        String base64Data = mContent.getContent();
        if (base64Data != null && FileUtils.isImage(mPath)) {
            String title = addTitleHeader ? getDocumentTitle() : null;
            String imageUrl = "data:image/" + FileUtils.getFileExtension(mPath) +
                    ";base64," + base64Data;
            return highlightImage(imageUrl, cssTheme, title);
        } else if (base64Data != null && FileUtils.isMarkdown(mPath)) {
            return generateMarkdownHtml(base64Data,
                    mRepoOwner, mRepoName, mRef, cssTheme, addTitleHeader);
        } else {
            String data = base64Data != null ? new String(EncodingUtils.fromBase64(base64Data)) : "";
            findMatchingLines(data);
            return generateCodeHtml(data, mPath,
                    mHighlightStart, mHighlightEnd, cssTheme, addTitleHeader);
        }
    }

    private void findMatchingLines(String data) {
        if (mTextMatch == null) {
            return;
        }

        int[] matchingLines = StringUtils.findMatchingLines(data, mTextMatch.getFragment());
        if (matchingLines != null) {
            mHighlightStart = matchingLines[0];
            mHighlightEnd = matchingLines[1];
        }
    }

    @Override
    protected String getDocumentTitle() {
        return getString(R.string.file_print_document_title, FileUtils.getFileName(mPath),
                mRepoOwner, mRepoName);
    }

    @Override
    protected boolean handlePrintRequest() {
        if (!FileUtils.isImage(mPath)) {
            return false;
        }
        String base64Data = mContent != null ? mContent.getContent() : null;
        if (base64Data == null) {
            return false;
        }
        byte[] decodedData = EncodingUtils.fromBase64(base64Data);
        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedData, 0, decodedData.length);

        PrintHelper printHelper = new PrintHelper(this);
        printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
        printHelper.printBitmap(getDocumentTitle(), bitmap);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_viewer_menu, menu);

        if (FileUtils.isImage(mPath) || FileUtils.isMarkdown(mPath)) {
            menu.removeItem(R.id.wrap);
        }

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
                IntentUtils.share(this, getString(R.string.share_file_subject,
                        FileUtils.getFileName(mPath), mRepoOwner + "/" + mRepoName), url);
                return true;
            case MENU_ITEM_HISTORY:
                startActivity(CommitHistoryActivity.makeIntent(this,
                        mRepoOwner, mRepoName, mRef, mPath));
                return true;
         }
         return super.onOptionsItemSelected(item);
     }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                if (mLastTouchedLine > 0) {
                    String subject = getString(R.string.share_line_subject, mLastTouchedLine, mPath,
                            mRepoOwner + "/" + mRepoName);
                    IntentUtils.share(this, subject, createUrl());
                }
                return true;
        }
        return false;
    }

    @Override
    protected void onLineTouched(int line, int x, int y) {
        super.onLineTouched(line, x, y);

        mLastTouchedLine = line;

        View anchor = findViewById(R.id.popup_helper);
        anchor.layout(x, y, x + 1, y + 1);
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.file_line_menu, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(this);
    }

    private String createUrl() {
        return "https://github.com/" + mRepoOwner + "/" + mRepoName + "/blob/" + mRef + "/" +
                mPath + "#L" + mLastTouchedLine;
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

    private static String highlightImage(String imageUrl, String cssTheme, String title) {
        StringBuilder content = new StringBuilder();
        content.append("<html><head>");
        writeCssInclude(content, "text", cssTheme);
        content.append("</head><body>");
        if (title != null) {
            content.append("<h2>").append(title).append("</h2>");
        }
        content.append("<div class='image'>");
        content.append("<img src='").append(imageUrl).append("' />");
        content.append("</div></body></html>");
        return content.toString();
    }
}
