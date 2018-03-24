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
import android.support.annotation.Nullable;
import android.support.v4.print.PrintHelper;
import android.support.v7.widget.PopupMenu;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.ApiRequestException;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.ClientErrorResponse;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.TextMatch;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;

import java.util.List;
import java.util.Locale;

import io.reactivex.Single;

public class FileViewerActivity extends WebViewerActivity
        implements PopupMenu.OnMenuItemClickListener {

    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_REF = "ref";
    private static final String EXTRA_HIGHLIGHT_START = "highlight_start";
    private static final String EXTRA_HIGHLIGHT_END = "highlight_end";
    private static final String EXTRA_TEXT_MATCH = "text_match";

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

    private static Intent makeIntent(Context context, String repoOwner, String repoName, String ref,
            String fullPath, int highlightStart, int highlightEnd, TextMatch textMatch) {
        return new Intent(context, FileViewerActivity.class)
                .putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_PATH, fullPath)
                .putExtra(EXTRA_REF, ref)
                .putExtra(EXTRA_HIGHLIGHT_START, highlightStart)
                .putExtra(EXTRA_HIGHLIGHT_END, highlightEnd)
                .putExtra(EXTRA_TEXT_MATCH, textMatch);
    }

    private String mRepoName;
    private String mRepoOwner;
    private String mPath;
    private String mRef;
    private int mHighlightStart;
    private int mHighlightEnd;
    private TextMatch mTextMatch;
    private Content mContent;
    private int mLastTouchedLine = 0;
    private boolean mViewRawText;

    private static final int ID_LOADER_FILE = 0;
    private static final int MENU_ITEM_HISTORY = 10;
    private static final String RAW_URL_FORMAT = "https://raw.githubusercontent.com/%s/%s/%s/%s";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String filename = FileUtils.getFileName(mPath);
        if (FileUtils.isBinaryFormat(filename) && !FileUtils.isImage(filename)) {
            openUnsuitableFileAndFinish();
        } else {
            loadFile(false);
        }
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return FileUtils.getFileName(mPath);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(EXTRA_OWNER);
        mRepoName = extras.getString(EXTRA_REPO);
        mPath = extras.getString(EXTRA_PATH);
        mRef = extras.getString(EXTRA_REF);
        mHighlightStart = extras.getInt(EXTRA_HIGHLIGHT_START, -1);
        mHighlightEnd = extras.getInt(EXTRA_HIGHLIGHT_END, -1);
        mTextMatch = extras.getParcelable(EXTRA_TEXT_MATCH);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        setContentShown(false);
        loadFile(true);
        super.onRefresh();
    }

    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        String base64Data = mContent.content();
        if (base64Data != null && FileUtils.isImage(mPath)) {
            String title = addTitleHeader ? getDocumentTitle() : null;
            String imageUrl = "data:" + FileUtils.getMimeTypeFor(mPath) +
                    ";base64," + base64Data;
            return highlightImage(imageUrl, cssTheme, title);
        } else if (base64Data != null && FileUtils.isMarkdown(mPath) && !mViewRawText) {
            return generateMarkdownHtml(base64Data,
                    mRepoOwner, mRepoName, mRef, cssTheme, addTitleHeader);
        } else {
            String data = base64Data != null ? StringUtils.fromBase64(base64Data) : "";
            findMatchingLines(data);
            return generateCodeHtml(data, mPath,
                    mHighlightStart, mHighlightEnd, cssTheme, addTitleHeader);
        }
    }

    private void findMatchingLines(String data) {
        if (mTextMatch == null) {
            return;
        }

        int[] matchingLines = StringUtils.findMatchingLines(data, mTextMatch.fragment());
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
        String base64Data = mContent != null ? mContent.content() : null;
        if (base64Data == null) {
            return false;
        }
        byte[] decodedData = Base64.decode(base64Data, Base64.DEFAULT);
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

        boolean isMarkdown = FileUtils.isMarkdown(mPath);
        if (FileUtils.isImage(mPath) || (isMarkdown && !mViewRawText)) {
            menu.removeItem(R.id.wrap);
        }
        if (isMarkdown) {
            MenuItem viewRawItem = menu.findItem(R.id.view_raw);
            viewRawItem.setChecked(mViewRawText);
            viewRawItem.setVisible(true);
        }

        menu.add(0, MENU_ITEM_HISTORY, Menu.NONE, R.string.history)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);

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
                        mRepoOwner, mRepoName, mRef, mPath, false));
                return true;
            case R.id.view_raw:
                mViewRawText = !mViewRawText;
                item.setChecked(mViewRawText);
                onRefresh();
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
        if (!isFinishing()) {
            PopupMenu popupMenu = new PopupMenu(this, anchor);
            popupMenu.getMenuInflater().inflate(R.menu.file_line_menu, popupMenu.getMenu());
            popupMenu.show();
            popupMenu.setOnMenuItemClickListener(this);
        }
    }

    @Override
    protected boolean shouldWrapLines() {
        boolean displayingMarkdown = FileUtils.isMarkdown(mPath) && !mViewRawText;
        return !displayingMarkdown && super.shouldWrapLines();
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

    private void loadFile(boolean force) {
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, force);
        service.getContents(mRepoOwner, mRepoName, mPath, mRef)
                .map(ApiHelpers::throwOnFailure)
                .map(Optional::of)
                .onErrorResumeNext(error -> {
                    if (error instanceof ApiRequestException) {
                        ClientErrorResponse response = ((ApiRequestException) error).getResponse();
                        List<ClientErrorResponse.FieldError> errors =
                                response != null ? response.errors() : null;
                        if (errors != null) {
                            for (ClientErrorResponse.FieldError fe : errors) {
                                if (fe.reason() == ClientErrorResponse.FieldError.Reason.TooLarge) {
                                    openUnsuitableFileAndFinish();
                                    return Single.just(Optional.absent());
                                }
                            }
                        }
                    }
                    return Single.error(error);
                })
                .compose(makeLoaderSingle(ID_LOADER_FILE, force))
                .subscribe(result -> {
                    if (result.isPresent()) {
                        mContent = result.get();
                        onDataReady();
                        setContentEmpty(false);
                    } else {
                        setContentEmpty(true);
                        setContentShown(true);
                    }
                }, this::handleLoadFailure);
    }
}
