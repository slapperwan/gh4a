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
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class DiffViewerActivity extends WebViewerActivity {
    private String mDiff;
    private boolean mInDiffMode;

    private String[] mDiffLines;
    protected SparseArray<List<CommitComment>> mCommitCommentsByPos =
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mDiff = data.getString(Constants.Commit.DIFF);

        doInitLoader();
    }

    public abstract void doInitLoader();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            menu.removeItem(R.id.search);
        }

        menu.add(0, 11, Menu.NONE, getString(R.string.object_view_file_at, mSha.substring(0, 7)))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void setWebViewClient() {
        mWebView.setWebViewClient(mWebViewClient);
    }

    protected void showDiff() {
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
                            StringUtils.formatRelativeTime(DiffViewerActivity.this, comment.getCreatedAt(), true)));
                    content.append("</div>").append(comment.getBodyHtml()).append("</div>");
                }
            }
        }
        content.append("</pre></body></html>");

        mWebView.loadDataWithBaseURL("file:///android_asset/", content.toString(), null, "utf-8", null);
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
                    updateComment(id, text, position);
                } else {
                    ToastUtils.showMessage(DiffViewerActivity.this, R.string.commit_comment_error_body);
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
                    new AlertDialog.Builder(DiffViewerActivity.this)
                            .setTitle(R.string.delete_comment_message)
                            .setMessage(R.string.confirmation)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    deleteComment(id);
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                }
            });
        }

        builder.show();
    }

    protected void refresh() {
        mCommitComments.clear();
        mCommitCommentsByPos.clear();
        doRestartLoader();
        setContentShown(false);
    }

    public abstract void updateComment(long id, String body, int position);

    public abstract void deleteComment(long id);

    public abstract void doRestartLoader();

    @Override
    public String getUrl() {
        return "/commit/" + mSha;
    }

    @Override
    public String getShareSubject() {
        return getString(R.string.share_commit_subject,
                mSha.substring(0, 7), mRepoOwner + "/" + mRepoName);
    }
}
