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
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ListPopupWindow;
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

import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DiffViewerActivity extends WebViewerActivity implements
        View.OnTouchListener {
    protected static Intent fillInIntent(Intent baseIntent, String repoOwner, String repoName,
            String commitSha, String path, String diff, List<CommitComment> comments,
            int initialLine, int highlightStartLine, int highlightEndLine,
            boolean highlightisRight, IntentUtils.InitialCommentMarker initialComment) {
        return baseIntent.putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("sha", commitSha)
                .putExtra("path", path)
                .putExtra("diff", diff)
                .putExtra("comments", comments != null ? new ArrayList<>(comments) : null)
                .putExtra("initial_line", initialLine)
                .putExtra("highlight_start", highlightStartLine)
                .putExtra("highlight_end", highlightEndLine)
                .putExtra("highlight_right", highlightisRight)
                .putExtra("initial_comment", initialComment);
    }

    private static final Pattern HUNK_START_PATTERN =
            Pattern.compile("@@ -(\\d+),\\d+ \\+(\\d+),\\d+.*");
    private static final String COMMENT_ADD_URI_FORMAT = "comment://add?position=%d&l=%d&r=%d";
    private static final String COMMENT_EDIT_URI_FORMAT =
            "comment://edit?position=%d&l=%d&r=%d&id=%d";

    protected String mRepoOwner;
    protected String mRepoName;
    protected String mPath;
    protected String mSha;
    private int mInitialLine;
    private int mHighlightStartLine;
    private int mHighlightEndLine;
    private boolean mHighlightIsRight;
    private IntentUtils.InitialCommentMarker mInitialComment;

    private String mDiff;
    private String[] mDiffLines;
    private final SparseArray<List<CommitComment>> mCommitCommentsByPos = new SparseArray<>();
    private final LongSparseArray<CommitComment> mCommitComments = new LongSparseArray<>();

    private final Point mLastTouchDown = new Point();

    private static final int MENU_ITEM_VIEW = 10;

    private final LoaderCallbacks<List<CommitComment>> mCommentCallback =
            new LoaderCallbacks<List<CommitComment>>(this) {
        @Override
        protected Loader<LoaderResult<List<CommitComment>>> onCreateLoader() {
            return createCommentLoader();
        }

        @Override
        protected void onResultReady(List<CommitComment> result) {
            addCommentsToMap(result);
            onDataReady();
        }
    };

    @Override
    @SuppressWarnings("unchecked")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(FileUtils.getFileName(mPath));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mWebView.setOnTouchListener(this);

        List<CommitComment> comments = (ArrayList<CommitComment>)
                getIntent().getSerializableExtra("comments");

        if (comments != null) {
            addCommentsToMap(comments);
            onDataReady();
        } else {
            getSupportLoaderManager().initLoader(0, null, mCommentCallback);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mPath = extras.getString("path");
        mSha = extras.getString("sha");
        mDiff = extras.getString("diff");
        mInitialLine = extras.getInt("initial_line", -1);
        mHighlightStartLine = extras.getInt("highlight_start", -1);
        mHighlightEndLine = extras.getInt("highlight_end", -1);
        mHighlightIsRight = extras.getBoolean("highlight_right", false);
        mInitialComment = extras.getParcelable("initial_comment");
        extras.remove("initial_comment");
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // no need for pull-to-refresh if everything was passed in the intent extras
        return !getIntent().hasExtra("comments");
    }

    @Override
    public void onRefresh() {
        if (forceLoaderReload(0)) {
            mCommitCommentsByPos.clear();
            setContentShown(false);
        }
        super.onRefresh();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.file_viewer_menu, menu);

        String viewAtTitle = getString(R.string.object_view_file_at, mSha.substring(0, 7));
        MenuItem item = menu.add(0, MENU_ITEM_VIEW, Menu.NONE, viewAtTitle);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_NEVER);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected String generateHtml(String cssTheme, boolean addTitleHeader) {
        StringBuilder content = new StringBuilder();
        boolean authorized = Gh4Application.get().isAuthorized();
        String title = addTitleHeader ? getDocumentTitle() : null;

        content.append("<html><head><title>");
        if (title != null) {
            content.append(title);
        }
        content.append("</title>");
        writeCssInclude(content, "text", cssTheme);
        writeScriptInclude(content, "codeutils");
        content.append("</head><body");

        int highlightInsertPos = content.length();
        content.append(">");
        if (title != null) {
            content.append("<h2>").append(title).append("</h2>");
        }
        content.append("<pre>");

        mDiffLines = mDiff.split("\n");

        int highlightStartLine = -1, highlightEndLine = -1;
        int leftDiffPosition = -1, rightDiffPosition = -1;

        for (int i = 0; i < mDiffLines.length; i++) {
            String line = mDiffLines[i];
            String cssClass = null;
            if (line.startsWith("@@")) {
                Matcher matcher = HUNK_START_PATTERN.matcher(line);
                if (matcher.matches()) {
                    leftDiffPosition = Integer.parseInt(matcher.group(1)) - 1;
                    rightDiffPosition = Integer.parseInt(matcher.group(2)) - 1;
                }
                cssClass = "change";
            } else if (line.startsWith("+")) {
                ++rightDiffPosition;
                cssClass = "add";
            } else if (line.startsWith("-")) {
                ++leftDiffPosition;
                cssClass = "remove";
            } else {
                ++leftDiffPosition;
                ++rightDiffPosition;
            }

            int pos = mHighlightIsRight ? rightDiffPosition : leftDiffPosition;
            if (pos != -1 && pos == mHighlightStartLine) {
                highlightStartLine = i;
            }
            if (pos != -1 && pos == mHighlightEndLine) {
                highlightEndLine = i;
            }

            content.append("<div id=\"line").append(i).append("\"");
            if (cssClass != null) {
                content.append("class=\"").append(cssClass).append("\"");
            }
            if (authorized) {
                String uri = String.format(Locale.US, COMMENT_ADD_URI_FORMAT,
                        i, leftDiffPosition, rightDiffPosition);
                content.append(" onclick=\"javascript:location.href='");
                content.append(uri).append("'\"");
            }
            content.append(">").append(TextUtils.htmlEncode(line)).append("</div>");

            List<CommitComment> comments = mCommitCommentsByPos.get(i);
            if (comments != null) {
                for (CommitComment comment : comments) {
                    long id = comment.getId();
                    mCommitComments.put(id, comment);
                    content.append("<div ").append("id=\"comment").append(id).append("\"");
                    content.append(" class=\"comment");
                    if (mInitialComment != null && mInitialComment.matches(id, null)) {
                        content.append(" highlighted");
                    }
                    content.append("\"");
                    if (authorized) {
                        String uri = String.format(Locale.US, COMMENT_EDIT_URI_FORMAT,
                                i, leftDiffPosition, rightDiffPosition, id);
                        content.append(" onclick=\"javascript:location.href='");
                        content.append(uri).append("'\"");
                    }
                    content.append("><div class=\"change\">");
                    content.append(getString(R.string.commit_comment_header,
                            "<b>" + ApiHelpers.getUserLogin(this, comment.getUser()) + "</b>",
                            StringUtils.formatRelativeTime(DiffViewerActivity.this, comment.getCreatedAt(), true)));
                    content.append("</div>").append(comment.getBodyHtml()).append("</div>");
                }
            }
        }

        if (mInitialLine > 0) {
            content.insert(highlightInsertPos, " onload='scrollToElement(\"line"
                    + mInitialLine + "\")' onresize='scrollToHighlight();'");
        } else if (mInitialComment != null) {
            content.insert(highlightInsertPos, " onload='scrollToElement(\"comment"
                    + mInitialComment.commentId + "\")' onresize='scrollToHighlight();'");
        } else if (highlightStartLine != -1 && highlightEndLine != -1) {
            content.insert(highlightInsertPos, " onload='highlightDiffLines("
                    + highlightStartLine + "," + highlightEndLine
                    + ")' onresize='scrollToHighlight();'");
        }

        content.append("</pre></body></html>");
        return content.toString();
    }

    @Override
    protected String getDocumentTitle() {
        return getString(R.string.diff_print_document_title,
                FileUtils.getFileName(mPath), mSha.substring(0, 7), mRepoOwner, mRepoName);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String url = createUrl();

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
                startActivity(FileViewerActivity.makeIntent(this, mRepoOwner, mRepoName, mSha, mPath));
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

    private void openCommentDialog(final long id, final long replyToId, String line,
            final int position, final int leftLine, final int rightLine) {
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
                new CommentTask(id, replyToId, text, position).schedule();
            }
        };

        AlertDialog d = new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.commit_comment_dialog_title, leftLine, rightLine))
                .setView(commentDialog)
                .setPositiveButton(saveButtonResId, saveCb)
                .setNegativeButton(R.string.cancel, null)
                .show();

        body.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(
                body, d.getButton(DialogInterface.BUTTON_POSITIVE)));
    }

    @Override
    protected void handleUrlLoad(Uri uri) {
        if (!uri.getScheme().equals("comment")) {
            super.handleUrlLoad(uri);
            return;
        }

        int line = Integer.parseInt(uri.getQueryParameter("position"));
        int leftLine = Integer.parseInt(uri.getQueryParameter("l"));
        int rightLine = Integer.parseInt(uri.getQueryParameter("r"));
        String lineText = mDiffLines[line];
        String idParam = uri.getQueryParameter("id");
        long id = idParam != null ? Long.parseLong(idParam) : 0L;

        if (idParam == null) {
            openCommentDialog(id, 0L, lineText, line, leftLine, rightLine);
        } else {
            CommentActionPopup p = new CommentActionPopup(id, line, lineText, leftLine, rightLine,
                    mLastTouchDown.x, mLastTouchDown.y);
            p.show();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTouchDown.set((int) event.getX(), (int) event.getY());
        }
        return false;
    }

    private void refresh() {
        mCommitComments.clear();
        mCommitCommentsByPos.clear();
        getSupportLoaderManager().restartLoader(0, null, mCommentCallback);
        setContentShown(false);
    }

    protected abstract Loader<LoaderResult<List<CommitComment>>> createCommentLoader();
    protected abstract void createComment(CommitComment comment, long replyToCommentId) throws IOException;
    protected abstract void editComment(CommitComment comment) throws IOException;
    protected abstract void deleteComment(long id) throws IOException;
    protected abstract boolean canReply();
    protected abstract String createUrl();

    private class CommentActionPopup extends ListPopupWindow implements
            AdapterView.OnItemClickListener {
        private final long mId;
        private final int mPosition;
        private final int mLeftLine;
        private final int mRightLine;
        private final String mLineText;

        public CommentActionPopup(long id, int position, String lineText,
                int leftLine, int rightLine, int x, int y) {
            super(DiffViewerActivity.this, null, R.attr.listPopupWindowStyle);

            mId = id;
            mPosition = position;
            mLeftLine = leftLine;
            mRightLine = rightLine;
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
            int choiceCount = parent.getAdapter().getCount();
            if (choiceCount > 2 && position == choiceCount - 1) {
                new AlertDialog.Builder(DiffViewerActivity.this)
                        .setMessage(R.string.delete_comment_message)
                        .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new DeleteCommentTask(mId).schedule();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            } else {
                long replyToId = canReply() && position == 0 ? mId : 0L;
                long commentId = choiceCount > 2 && position == choiceCount - 2 ? mId : 0L;
                openCommentDialog(commentId, replyToId, mLineText,
                        mPosition, mLeftLine, mRightLine);
            }
            dismiss();
        }

        private boolean isOwnComment(long id) {
            String login = Gh4Application.get().getAuthLogin();
            CommitComment comment = mCommitComments.get(id);
            return ApiHelpers.loginEquals(comment.getUser(), login);
        }

        private String[] populateChoices(boolean ownComment) {
            int choiceCount = (ownComment ? 3 : 1) + (canReply() ? 1 : 0);
            String[] choices = new String[choiceCount];
            int index = 0;
            if (canReply()) {
                choices[index++] = getString(R.string.reply);
            }
            choices[index++] = getString(R.string.add_comment);
            if (ownComment) {
                choices[index++] = getString(R.string.edit);
                choices[index++] = getString(R.string.delete);
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
        private final CommitComment mComment;
        private final long mReplyToId;

        public CommentTask(long id, long replyToId, String body, int position) {
            super(DiffViewerActivity.this, R.string.saving_msg);
            mComment = new CommitComment();
            mComment.setBody(body);
            mComment.setId(id);
            mComment.setPosition(position);
            mReplyToId = replyToId;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new CommentTask(mComment.getId(), mReplyToId,
                    mComment.getBody(), mComment.getPosition());
        }

        @Override
        protected Void run() throws IOException {
            if (mComment.getId() == 0L) {
                createComment(mComment, mReplyToId);
            } else {
                editComment(mComment);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
            setResult(RESULT_OK);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_edit_commit_comment, mComment.getPosition());
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private final long mId;

        public DeleteCommentTask(long id) {
            super(DiffViewerActivity.this, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(mId);
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

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_commit_comment);
        }
    }
}
