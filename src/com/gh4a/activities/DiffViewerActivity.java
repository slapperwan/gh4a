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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ThemeUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class DiffViewerActivity extends WebViewerActivity implements
        View.OnTouchListener {
    protected String mRepoOwner;
    protected String mRepoName;
    protected String mPath;
    protected String mSha;

    private String mDiff;
    private String[] mDiffLines;
    private SparseArray<List<CommitComment>> mCommitCommentsByPos = new SparseArray<>();
    private LongSparseArray<CommitComment> mCommitComments = new LongSparseArray<>();

    private Point mLastTouchDown = new Point();

    private static final int MENU_ITEM_VIEW = 10;

    private LoaderCallbacks<List<CommitComment>> mCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return createCommentLoader();
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            if (result.handleError(DiffViewerActivity.this)) {
                setContentEmpty(true);
                setContentShown(true);
                return;
            }

            addCommentsToMap(result.getData());
            showDiff();
        }
    };

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mPath = data.getString(Constants.Object.PATH);
        mSha = data.getString(Constants.Object.OBJECT_SHA);
        mDiff = data.getString(Constants.Commit.DIFF);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(FileUtils.getFileName(mPath));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mWebView.setOnTouchListener(this);

        List<CommitComment> comments =
                (ArrayList<CommitComment>) data.getSerializable(Constants.Commit.COMMENTS);

        if (comments != null) {
            addCommentsToMap(comments);
            showDiff();
        } else {
            getSupportLoaderManager().initLoader(0, null, mCommentCallback);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);

        menu.removeItem(R.id.download);

        String viewAtTitle = getString(R.string.object_view_file_at, mSha.substring(0, 7));
        MenuItem item = menu.add(0, MENU_ITEM_VIEW, Menu.NONE, viewAtTitle);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String url = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mSha;

        switch (item.getItemId()) {
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(url));
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_commit_subject,
                        mSha.substring(0, 7), mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT, url);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case MENU_ITEM_VIEW:
                startActivity(IntentUtils.getFileViewerActivityIntent(this,
                        mRepoOwner, mRepoName, mSha, mPath));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addCommentsToMap(List<CommitComment> comments) {
        for (CommitComment comment : comments) {
            if (!TextUtils.equals(comment.getPath(), mPath)) {
                continue;
            }
            int position = comment.getPosition();
            List<CommitComment> commentsByPos = mCommitCommentsByPos.get(position);
            if (commentsByPos == null) {
                commentsByPos = new ArrayList<>();
                mCommitCommentsByPos.put(position, commentsByPos);
            }
            commentsByPos.add(comment);
        }
    }

    protected void showDiff() {
        StringBuilder content = new StringBuilder();
        boolean authorized = Gh4Application.get().isAuthorized();

        content.append("<html><head><title></title>");
        content.append("<link href='file:///android_asset/text-");
        content.append(ThemeUtils.getCssTheme(Gh4Application.THEME));
        content.append(".css' rel='stylesheet' type='text/css'/>");
        content.append("<script src='file:///android_asset/wraphandler.js' type='text/javascript'></script>");
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

            content.append("<div ");
            if (cssClass != null) {
                content.append("class=\"").append(cssClass).append("\"");
            }
            if (authorized) {
                content.append(" onclick=\"javascript:location.href='comment://add");
                content.append("?position=").append(i).append("'\"");
            }
            content.append(">").append(line).append("</div>");

            List<CommitComment> comments = mCommitCommentsByPos.get(i);
            if (comments != null) {
                for (CommitComment comment : comments) {
                    mCommitComments.put(comment.getId(), comment);
                    content.append("<div class=\"comment\"");
                    if (authorized) {
                        content.append(" onclick=\"javascript:location.href='comment://edit");
                        content.append("?position=").append(i);
                        content.append("&id=").append(comment.getId()).append("'\"");
                    }
                    content.append("><div class=\"change\">");
                    content.append(getString(R.string.commit_comment_header,
                            "<b>" + comment.getUser().getLogin() + "</b>",
                            StringUtils.formatRelativeTime(DiffViewerActivity.this, comment.getCreatedAt(), true)));
                    content.append("</div>").append(comment.getBodyHtml()).append("</div>");
                }
            }
        }
        content.append("</pre></body></html>");
        loadThemedHtml(content.toString());
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
                    AsyncTaskCompat.executeParallel(new CommentTask(id, text, position));
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

        builder.show();
    }

    @Override
    protected boolean handleUrlLoad(String url) {
        if (!url.startsWith("comment://")) {
            return false;
        }

        Uri uri = Uri.parse(url);
        int line = Integer.parseInt(uri.getQueryParameter("position"));
        String lineText = Html.fromHtml(mDiffLines[line]).toString();
        String idParam = uri.getQueryParameter("id");
        long id = idParam != null ? Long.parseLong(idParam) : 0L;

        if (idParam == null) {
            openCommentDialog(id, lineText, line);
        } else {
            CommentActionPopup p = new CommentActionPopup(id, line, lineText,
                    mLastTouchDown.x, mLastTouchDown.y);
            p.show();
        }
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTouchDown.set((int) event.getX(), (int) event.getY());
        }
        return false;
    }

    protected void refresh() {
        mCommitComments.clear();
        mCommitCommentsByPos.clear();
        getSupportLoaderManager().restartLoader(0, null, mCommentCallback);
        setContentShown(false);
    }

    protected abstract Loader<LoaderResult<List<CommitComment>>> createCommentLoader();
    protected abstract void updateComment(long id, String body, int position) throws IOException;
    protected abstract void deleteComment(long id) throws IOException;

    private class CommentActionPopup extends ListPopupWindow implements
            AdapterView.OnItemClickListener {
        private long mId;
        private int mPosition;
        private String mLineText;

        public CommentActionPopup(long id, int position, String lineText, int x, int y) {
            super(DiffViewerActivity.this, null, R.attr.listPopupWindowStyle);

            mId = id;
            mPosition = position;
            mLineText = lineText;

            ArrayAdapter<String> adapter = new ArrayAdapter<>(DiffViewerActivity.this,
                    R.layout.popup_menu_item, populateChoices(isOwnComment(id)));
            setAdapter(adapter);
            setContentWidth(measureContentWidth(adapter));

            View anchor = findViewById(R.id.popup_helper);
            anchor.layout(x, y, x + 1, y + 1);
            setAnchorView(anchor);

            setOnItemClickListener(this);
            setModal(true);
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (position == 2) {
                new AlertDialog.Builder(DiffViewerActivity.this)
                        .setTitle(R.string.delete_comment_message)
                        .setMessage(R.string.confirmation)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                AsyncTaskCompat.executeParallel(new DeleteCommentTask(mId));
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                openCommentDialog(position == 0 ? 0 : mId, mLineText, mPosition);
            }
            dismiss();
        }

        private boolean isOwnComment(long id) {
            String login = Gh4Application.get().getAuthLogin();
            CommitComment comment = mCommitComments.get(id);
            User user = comment.getUser();
            return user != null && TextUtils.equals(login, comment.getUser().getLogin());
        }

        private String[] populateChoices(boolean ownComment) {
            String[] choices = new String[ownComment ? 3 : 1];
            choices[0] = getString(R.string.reply);
            if (ownComment) {
                choices[1] = getString(R.string.edit);
                choices[2] = getString(R.string.delete);
            }
            return choices;
        }

        private int measureContentWidth(ListAdapter adapter) {
            Context context = DiffViewerActivity.this;
            ViewGroup measureParent = new FrameLayout(context);
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int maxWidth = 0, count = adapter.getCount();
            View itemView = null;

            for (int i = 0; i < count; i++) {
                itemView = adapter.getView(i, itemView, measureParent);
                itemView.measure(measureSpec, measureSpec);
                maxWidth = Math.max(maxWidth, itemView.getMeasuredWidth());
            }
            return maxWidth;
        }
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mBody;
        private int mPosition;
        private long mId;

        public CommentTask(long id, String body, int position) {
            super(DiffViewerActivity.this, 0, R.string.saving_msg);
            mBody = body;
            mPosition = position;
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            updateComment(mId, mBody, mPosition);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
            setResult(RESULT_OK);
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(DiffViewerActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws IOException {
            deleteComment(mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
            setResult(RESULT_OK);
        }
    }
}
