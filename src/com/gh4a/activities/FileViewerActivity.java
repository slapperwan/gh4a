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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileViewerActivity extends LoadingFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    private String mPath;
    private String mRef;
    private String mSha;
    private String mDiff;
    private boolean mInDiffMode;
    private WebView mWebView;
    private Map<Integer, String> positionLineMap = new HashMap<Integer, String>();
    private Map<Integer, List<CommitComment>> positionListCommitCommentsMap = new HashMap<Integer, List<CommitComment>>();
    private Map<Long, CommitComment> positionCommitCommentMap = new HashMap<Long, CommitComment>();

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            setContentShown(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (!StringUtils.isBlank(url)) {
                if (url.startsWith("http://add-comment")) {
                    int positionIdx = url.lastIndexOf("?position=");
                    String position = url.substring(positionIdx + 10, url.length());
                    String line = positionLineMap.get(Integer.parseInt(position));
                    line = Html.fromHtml(line).toString();
                    openCommentDialog(0L, line, Integer.parseInt(position));
                } else if (url.startsWith("http://edit-comment")) {
                    int positionIdx = url.lastIndexOf("?position=");
                    int idIdx = url.lastIndexOf("&id=");
                    String position = url.substring(positionIdx + 10, idIdx);
                    String id = url.substring(idIdx + 4, url.length());
                    String line = positionLineMap.get(Integer.parseInt(position));
                    line = Html.fromHtml(line).toString();
                    openCommentDialog(Long.parseLong(id), line, Integer.parseInt(position));
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
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

    private LoaderCallbacks<List<CommitComment>> mCommitCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
                @Override
                public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
                    return new CommitCommentListLoader(FileViewerActivity.this, mRepoOwner, mRepoName, mSha);
                }

                @Override
                public void onResultReady(LoaderResult<List<CommitComment>> result) {
                    setContentEmpty(true);
                    if (!result.handleError(FileViewerActivity.this)) {
                        List<CommitComment> data = result.getData();
                        for (CommitComment commitComment : data) {
                            if (positionListCommitCommentsMap.containsKey(commitComment.getPosition())) {
                                List<CommitComment> commitComments = positionListCommitCommentsMap.get(commitComment.getPosition());
                                commitComments.add(commitComment);
                            } else {
                                List<CommitComment> commitComments = new ArrayList<CommitComment>();
                                commitComments.add(commitComment);
                                positionListCommitCommentsMap.put(commitComment.getPosition(), commitComments);
                            }

                        }
                        showDiff();
                        setContentEmpty(false);
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
            getSupportLoaderManager().initLoader(1, null, mCommitCommentCallback);
            setContentShown(false);
        } else {
            getSupportLoaderManager().initLoader(0, null, mFileCallback);
            setContentShown(false);
        }
    }

    @SuppressWarnings("deprecation")
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

        mWebView.setBackgroundColor(ThemeUtils.getWebViewBackgroundColor(Gh4Application.THEME));
        mWebView.setWebViewClient(mWebViewClient);
    }

    private void showDiff() {
        StringBuilder content = new StringBuilder();

        content.append("<html><head><title></title>");
        content.append("<link href='file:///android_asset/text-");
        content.append(ThemeUtils.getCssTheme(Gh4Application.THEME));
        content.append(".css' rel='stylesheet' type='text/css'/>");
        content.append("</head><body><pre>");

        String encoded = TextUtils.htmlEncode(mDiff);
        String[] lines = encoded.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String cssClass = null;
            if (line.startsWith("@@")) {
                cssClass = "change";
            } else if (line.startsWith("+")) {
                cssClass = "add";
            } else if (line.startsWith("-")) {
                cssClass = "remove";
            }

            positionLineMap.put(i, line);

            line = "<div class=\"" + cssClass + "\" "
                    + "onclick=\"javascript:location.href='http://add-comment/"
                    + "?position=" + i + "'\">"
                    + line + "</div>";

            if (positionListCommitCommentsMap.containsKey(i)) {
                List<CommitComment> commitComments = positionListCommitCommentsMap.get(i);
                for (CommitComment commitComment : commitComments) {
                    positionCommitCommentMap.put(commitComment.getId(), commitComment);
                    String commentHtml = "<div style=\"border:1px solid; padding: 2px; margin-top: 2px;\" ";
                    commentHtml += "onclick=\"javascript:location.href='http://edit-comment/";
                    commentHtml += "?position=" + i + "&id=" + commitComment.getId() + "'\">";
                    commentHtml += commitComment.getUser().getLogin() + " added a note ";
                    commentHtml += StringUtils.formatRelativeTime(FileViewerActivity.this, commitComment.getCreatedAt(), true) + ".<br/>";
                    commentHtml += commitComment.getBodyHtml();
                    commentHtml += "</div>";

                    line += commentHtml;
                }
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
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
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

    @SuppressWarnings("deprecation")
    @TargetApi(11)
    private void doSearch() {
        if (mWebView != null) {
            mWebView.showFindDialog(null, true);
        }
    }

    private void openCommentDialog(final long id, String line, final int position) {
        final boolean isEdit = id != 0L ? true : false;
        LayoutInflater li = LayoutInflater.from(this);
        View commentDialog = li.inflate(R.layout.commit_comment_dialog, null);

        AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.commit_comment_dialog_title);
        builder.setView(commentDialog);

        final TextView code = (TextView) commentDialog.findViewById(R.id.line);
        code.setText(line);

        final EditText body = (EditText) commentDialog.findViewById(R.id.body);
        if (isEdit) {
            CommitComment commitComment = positionCommitCommentMap.get(id);
            body.setText(commitComment.getBody());
        }

        builder.setPositiveButton(R.string.issue_comment_title, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (!StringUtils.isBlank(body.getText().toString())) {
                    new CommentTask(id, body.getText().toString(), position).execute();
                } else {
                    ToastUtils.showMessage(FileViewerActivity.this, R.string.commit_comment_error_body);
                }
            }
        });

        builder.setNegativeButton(isEdit ? R.string.delete : R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (isEdit) {
                    new DeleteCommentTask(id).execute();
                }
            }
        });

        builder.show();
    }

    private void refresh() {
        positionListCommitCommentsMap = new HashMap<Integer, List<CommitComment>>();
        positionCommitCommentMap = new HashMap<Long, CommitComment>();
        getSupportLoaderManager().restartLoader(1, null, mCommitCommentCallback);
        setContentShown(false);
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mBody;
        private int mPosition;
        private long mId;

        public CommentTask(long id, String body, int position) {
            super(FileViewerActivity.this, 0, R.string.issue_comment_hint);
            mBody = body;
            mPosition = position;
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            boolean isEdit = mId != 0L ? true : false;
            CommitComment commitComment = new CommitComment();
            if (isEdit) {// edit mode
                commitComment.setId(mId);
            }
            commitComment.setPosition(mPosition);
            commitComment.setCommitId(mSha);
            commitComment.setPath(mPath);
            commitComment.setBody(mBody);

            CommitService commitService = (CommitService)
                    Gh4Application.get(mContext).getService(Gh4Application.COMMIT_SERVICE);

            if (isEdit) {
                commitService.editComment(new RepositoryId(mRepoOwner, mRepoName), commitComment);
            } else {
                commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mSha, commitComment);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(FileViewerActivity.this, 0, R.string.issue_comment_hint);
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            CommitComment commitComment = new CommitComment();
            commitComment.setId(mId);

            CommitService commitService = (CommitService)
                    Gh4Application.get(mContext).getService(Gh4Application.COMMIT_SERVICE);

            commitService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }
}