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
package com.gh4a.fragment;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditCommentActivity;
import com.gh4a.adapter.CommentAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueCommentsLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class PullRequestFragment extends ListDataBaseFragment<Comment> implements
        CommentAdapter.OnEditComment, OnClickListener {
    private static final int REQUEST_EDIT = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private LinearLayout mHeader;
    private PullRequest mPullRequest;
    private boolean mIsCollaborator;

    private LoaderCallbacks<PullRequest> mPullRequestCallback = new LoaderCallbacks<PullRequest>() {
        @Override
        public Loader<LoaderResult<PullRequest>> onCreateLoader(int id, Bundle args) {
            return new PullRequestLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }
        @Override
        public void onResultReady(LoaderResult<PullRequest> result) {
            setListShown(true);
            if (!result.handleError(getActivity())) {
                mPullRequest = result.getData();
                fillData();
                invalidateOptionsMenu();
            }
        }
    };

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(getActivity(), mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!result.handleError(getActivity())) {
                mIsCollaborator = result.getData();
                invalidateOptionsMenu();
            }
        }
    };

    public static PullRequestFragment newInstance(String repoOwner, String repoName, int pullRequestNumber) {
        PullRequestFragment f = new PullRequestFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putInt(Constants.Issue.NUMBER, pullRequestNumber);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mPullRequestNumber = getArguments().getInt(Constants.Issue.NUMBER);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (Gh4Application.get(getActivity()).isAuthorized()) {
            inflater.inflate(R.menu.pullrequest_menu, menu);

            String state = mPullRequest != null ? mPullRequest.getState() : null;
            if (!mIsCollaborator || state == null || state.equals(Constants.Issue.STATE_CLOSED)) {
                menu.removeItem(R.id.pull_close);
            }
            if (!mIsCollaborator || state == null || state.equals(Constants.Issue.STATE_OPEN)) {
                menu.removeItem(R.id.pull_reopen);
            }
            if (!mIsCollaborator || mPullRequest == null || mPullRequest.isMerged()) {
                menu.removeItem(R.id.pull_merge);
            } else if (!mPullRequest.isMergeable()) {
                menu.findItem(R.id.pull_merge).setEnabled(false);
            }
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.pull_request, container, false);

        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listContent);

        if (!Gh4Application.get(getActivity()).isAuthorized()) {
            v.findViewById(R.id.comment).setVisibility(View.GONE);
            v.findViewById(R.id.divider).setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        mHeader = (LinearLayout) getLayoutInflater(savedInstanceState).inflate(
                R.layout.issue_header, getListView(), false);
        mHeader.setClickable(false);
        mHeader.findViewById(R.id.info_box).setVisibility(View.GONE);
        getListView().addHeaderView(mHeader, null, true);

        UiUtils.assignTypeface(mHeader, Gh4Application.get(getActivity()).boldCondensed, new int[] {
            R.id.comment_title, R.id.tv_title, R.id.desc_title
        });

        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(1, null, mPullRequestCallback);
        getLoaderManager().initLoader(2, null, mCollaboratorCallback);
    }

    @Override
    public void refresh() {
        super.refresh();
        getLoaderManager().getLoader(1).onContentChanged();
        getLoaderManager().getLoader(2).onContentChanged();
    }

    @Override
    protected RootAdapter<Comment> onCreateAdapter() {
        return new CommentAdapter(getActivity(), mRepoOwner, this);
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pull_merge:
                showMergeDialog();
                break;
            case R.id.pull_close:
            case R.id.pull_reopen:
                new PullRequestOpenCloseTask(item.getItemId() == R.id.issue_reopen).execute();
                break;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pull_subject,
                        mPullRequestNumber, mPullRequest.getTitle(), mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  mPullRequest.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                break;
            case R.id.refresh:
                refresh();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fillData() {
        final Gh4Application app = Gh4Application.get(getActivity());
        View v = getView();

        User user = mPullRequest.getUser();
        ImageView gravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        gravatar.setOnClickListener(this);
        gravatar.setTag(user);
        GravatarHandler.assignGravatar(gravatar, user);

        TextView tvCommentTitle = (TextView) mHeader.findViewById(R.id.comment_title);
        tvCommentTitle.setText(getString(R.string.issue_comments) + " (" + mPullRequest.getComments() + ")");

        TextView tvState = (TextView) mHeader.findViewById(R.id.tv_state);
        if (Constants.Issue.STATE_CLOSED.equals(mPullRequest.getState())) {
            tvState.setBackgroundResource(R.drawable.default_red_box);
            tvState.setText(getString(R.string.closed).toUpperCase(Locale.getDefault()));
        } else {
            tvState.setBackgroundResource(R.drawable.default_green_box);
            tvState.setText(getString(R.string.open).toUpperCase(Locale.getDefault()));
        }

        TextView tvExtra = (TextView) mHeader.findViewById(R.id.tv_extra);
        tvExtra.setText(user.getLogin() + "\n"
                + StringUtils.formatRelativeTime(getActivity(), mPullRequest.getCreatedAt(), true));

        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        tvTitle.setText(mPullRequest.getTitle());
        tvTitle.setTypeface(app.boldCondensed);

        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        String body = mPullRequest.getBodyHtml();
        tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
        if (!StringUtils.isBlank(body)) {
            HttpImageGetter imageGetter = new HttpImageGetter(getActivity());
            body = HtmlUtils.format(body).toString();
            imageGetter.bind(tvDesc, body, mPullRequest.getId());
        }

        v.findViewById(R.id.iv_comment).setOnClickListener(this);
    }

    private void showMergeDialog() {
        Repository fromRepo = mPullRequest.getHead().getRepo();
        String defaultMessage = getString(R.string.pull_merge_message_default,
                mPullRequestNumber, fromRepo.getOwner().getLogin() + "/" + fromRepo.getName());
        String title = getString(R.string.pull_message_dialog_title, mPullRequestNumber);

        View view = getLayoutInflater(null).inflate(R.layout.pull_merge_message_dialog, null);
        final EditText editor = (EditText) view.findViewById(R.id.et_commit_message);
        editor.setText(defaultMessage);

        UiUtils.createDialogBuilder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        new PullRequestMergeTask(text).execute();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.iv_gravatar) {
            User user = (User) v.getTag();
            IntentUtils.openUserInfoActivity(getActivity(), user);
        } else if (id == R.id.iv_comment) {
            EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
            String text = etComment.getText() == null ? null : etComment.getText().toString();
            if (!StringUtils.isBlank(text)) {
                new CommentIssueTask(text).execute();
            }
            UiUtils.hideImeForView(getActivity().getCurrentFocus());
        }
    }

    @Override
    public Loader<LoaderResult<List<Comment>>> onCreateLoader(int id, Bundle args) {
        return new IssueCommentsLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT && resultCode == Activity.RESULT_OK) {
            // no need to refresh pull request and collaborator status in that case
            super.refresh();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onItemClick(Comment comment) {
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent = new Intent(getActivity(), EditCommentActivity.class);

        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Comment.ID, comment.getId());
        intent.putExtra(Constants.Comment.BODY, comment.getBody());
        startActivityForResult(intent, REQUEST_EDIT);
    }

    private class CommentIssueTask extends ProgressDialogTask<Void> {
        private String mText;

        public CommentIssueTask(String text) {
            super(getActivity(), 0, R.string.loading_msg);
            mText = text;
        }

        @Override
        protected Void run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get(getActivity()).getService(Gh4Application.ISSUE_SERVICE);
            issueService.createComment(mRepoOwner, mRepoName, mPullRequestNumber, mText);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            ToastUtils.showMessage(mContext, R.string.issue_success_comment);
            //reload comments
            refresh();

            EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
            etComment.setText(null);
            etComment.clearFocus();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_comment);
        }
    }

    private class PullRequestOpenCloseTask extends ProgressDialogTask<PullRequest> {
        private boolean mOpen;

        public PullRequestOpenCloseTask(boolean open) {
            super(getActivity(), 0, open ? R.string.opening_msg : R.string.closing_msg);
            mOpen = open;
        }

        @Override
        protected PullRequest run() throws IOException {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            PullRequest pullRequest = pullService.getPullRequest(repoId, mPullRequestNumber);
            pullRequest.setState(mOpen ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED);

            return pullService.editPullRequest(repoId, pullRequest);
        }

        @Override
        protected void onSuccess(PullRequest result) {
            mPullRequest = result;
            ToastUtils.showMessage(mContext,
                    mOpen ? R.string.issue_success_reopen : R.string.issue_success_close);

            fillData();
            invalidateOptionsMenu();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_close);
        }
    }

    private class PullRequestMergeTask extends ProgressDialogTask<MergeStatus> {
        private String mCommitMessage;

        public PullRequestMergeTask(String commitMessage) {
            super(getActivity(), 0, R.string.merging_msg);
            mCommitMessage = commitMessage;
        }

        @Override
        protected MergeStatus run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            return pullService.merge(repoId, mPullRequestNumber, mCommitMessage);
        }

        @Override
        protected void onSuccess(MergeStatus result) {
            if (result.isMerged()) {
                setListShown(false);
                getLoaderManager().getLoader(1).onContentChanged();
            } else {
                ToastUtils.showMessage(mContext, R.string.pull_error_merge);
            }
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.pull_error_merge);
        }
    }
}
