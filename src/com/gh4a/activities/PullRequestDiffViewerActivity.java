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
import android.util.Log;
import android.util.SparseArray;
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
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PullRequestDiffViewerActivity extends LoadingFragmentActivity {
    protected String mRepoOwner;
    protected String mRepoName;
    private int mPullRequestNumber;
    private String mPath;
    private String mDiff;
    private String mSha;
    private WebView mWebView;

    private String[] mDiffLines;
    private SparseArray<List<CommitComment>> mCommitCommentsByPos =
            new SparseArray<List<CommitComment>>();
    private HashMap<Long, CommitComment> mCommitComments = new HashMap<Long, CommitComment>();

    private WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView webView, String url) {
            setContentShown(true);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("comment://")) {
                Uri uri = Uri.parse(url);
                int line = Integer.parseInt(uri.getQueryParameter("position"));
                String lineText = Html.fromHtml(mDiffLines[line]).toString();
                String idParam = uri.getQueryParameter("id");
                long id = idParam != null ? Long.parseLong(idParam) : 0L;

                openCommentDialog(id, lineText, line);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }

            return true;
        }
    };

    private LoaderCallbacks<List<CommitComment>> mPullRequestCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new PullRequestCommentsLoader(PullRequestDiffViewerActivity.this, mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            setContentEmpty(true);
            if (!result.handleError(PullRequestDiffViewerActivity.this)) {
                for (CommitComment comment : result.getData()) {
                    if (!TextUtils.equals(comment.getPath(), mPath)) {
                        continue;
                    }
                    int position = comment.getPosition();
                    List<CommitComment> comments = mCommitCommentsByPos.get(position);
                    if (comments == null) {
                        comments = new ArrayList<CommitComment>();
                        mCommitCommentsByPos.put(position, comments);
                    }
                    comments.add(comment);
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
        mPullRequestNumber = data.getInt(Constants.PullRequest.NUMBER);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mPath = data.getString(Constants.Object.PATH);
        mDiff = data.getString(Constants.Commit.DIFF);

        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.web_viewer);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(FileUtils.getFileName(mPath));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentShown(false);

        getSupportLoaderManager().initLoader(0, null, mPullRequestCommentCallback);
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
        mDiffLines = encoded.split("\n");

        for (int i = 0; i < mDiffLines.length; i++) {
            String line = mDiffLines[i];
            String cssClass = null;
            if (line.startsWith("@@")) {
                cssClass = "change";
            } else if (line.startsWith("+")) {
                cssClass = "add";
            } else if (line.startsWith("-")) {
                cssClass = "remove";
            }

            content.append("<div class=\"").append(cssClass).append("\" ");
            content.append("onclick=\"javascript:location.href='comment://add");
            content.append("?position=").append(i).append("'\">").append(line).append("</div>");

            List<CommitComment> comments = mCommitCommentsByPos.get(i);
            if (comments != null) {
                for (CommitComment comment : comments) {
                    mCommitComments.put(comment.getId(), comment);
                    content.append("<div style=\"border:1px solid; padding: 2px; margin: 5px 0;\" ");
                    content.append("onclick=\"javascript:location.href='comment://edit");
                    content.append("?position=").append(i);
                    content.append("&id=").append(comment.getId()).append("'\">");
                    content.append("<div class=\"change\">");
                    content.append(getString(R.string.commit_comment_header,
                            comment.getUser().getLogin(),
                            StringUtils.formatRelativeTime(PullRequestDiffViewerActivity.this, comment.getCreatedAt(), true)));
                    content.append("</div>").append(comment.getBodyHtml()).append("</div>");
                }
            }
        }
        content.append("</pre></body></html>");

        setupWebView();

        mWebView.loadDataWithBaseURL("file:///android_asset/", content.toString(), null, "utf-8", null);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openPullRequestListActivity(this, mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    private void openCommentDialog(final long id, String line, final int position) {
        final boolean isEdit = id != 0L;
        LayoutInflater inflater = LayoutInflater.from(this);
        View commentDialog = inflater.inflate(R.layout.commit_comment_dialog, null);

        final TextView code = (TextView) commentDialog.findViewById(R.id.line);
        code.setText(line);

        final EditText body = (EditText) commentDialog.findViewById(R.id.body);
        if (isEdit) {
            body.setText(mCommitComments.get(id).getBody());
        }

        final int saveButtonResId = isEdit
                ? R.string.issue_comment_update_title : R.string.issue_comment_title;
        final DialogInterface.OnClickListener saveCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = body.getText().toString();
                if (!StringUtils.isBlank(text)) {
                    new CommentTask(id, text, position).execute();
                } else {
                    ToastUtils.showMessage(PullRequestDiffViewerActivity.this, R.string.commit_comment_error_body);
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.commit_comment_dialog_title, position))
                .setView(commentDialog)
                .setPositiveButton(saveButtonResId, saveCb)
                .setNegativeButton(R.string.cancel, null);

        if (isEdit) {
            builder.setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new AlertDialog.Builder(PullRequestDiffViewerActivity.this)
                            .setTitle(R.string.delete_comment_message)
                            .setMessage(R.string.confirmation)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new DeleteCommentTask(id).execute();
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            });
        }

        builder.show();
    }

    private void refresh() {
        mCommitComments.clear();
        mCommitCommentsByPos.clear();
        getSupportLoaderManager().restartLoader(0, null, mPullRequestCommentCallback);
        setContentShown(false);
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mBody;
        private int mPosition;
        private long mId;

        public CommentTask(long id, String body, int position) {
            super(PullRequestDiffViewerActivity.this, 0, R.string.saving_msg);
            mBody = body;
            mPosition = position;
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            boolean isEdit = mId != 0L;
            CommitComment commitComment = new CommitComment();

            if (isEdit) {
                commitComment.setId(mId);
            }
            commitComment.setPosition(mPosition);
            Log.d("XXX", mSha);
            commitComment.setCommitId(mSha);
            commitComment.setPath(mPath);
            commitComment.setBody(mBody);

            PullRequestService pullRequestService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            if (isEdit) {
                pullRequestService.editComment(new RepositoryId(mRepoOwner, mRepoName), commitComment);
            } else {
                pullRequestService.createComment(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber, commitComment);
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
            super(PullRequestDiffViewerActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            CommitComment commitComment = new CommitComment();
            commitComment.setId(mId);

            PullRequestService pullRequestService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            pullRequestService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
        }
    }
}