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

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.PullRequestCommentListLoader;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class PullRequestFragment extends ListDataBaseFragment<IssueEventHolder> implements
        IssueEventAdapter.OnEditComment, CommentBoxFragment.Callback {
    private static final int REQUEST_EDIT = 1000;

    private TextView mDescriptionView;
    private PullRequest mPullRequest;
    private String mRepoOwner;
    private String mRepoName;
    private boolean mIsCollaborator;
    private boolean mListShown;
    private CommentBoxFragment mCommentFragment;
    private IssueEventAdapter mAdapter;
    private HttpImageGetter mImageGetter;

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(getActivity(), mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!result.handleError(getActivity())) {
                mIsCollaborator = result.getData();
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    };

    public static PullRequestFragment newInstance(PullRequest pullRequest) {
        PullRequestFragment f = new PullRequestFragment();

        Bundle args = new Bundle();
        args.putSerializable("PULL", pullRequest);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPullRequest = (PullRequest) getArguments().getSerializable("PULL");

        Repository repo = mPullRequest.getBase().getRepo();
        mRepoOwner = repo.getOwner().getLogin();
        mRepoName = repo.getName();

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Gh4Application app = Gh4Application.get();
        if (app.isAuthorized()) {
            inflater.inflate(R.menu.pullrequest_menu, menu);

            boolean isCreator = mPullRequest.getUser().getLogin().equals(app.getAuthLogin());

            if (!mIsCollaborator && !isCreator) {
                menu.removeItem(R.id.pull_close);
                menu.removeItem(R.id.pull_reopen);
            } else if (Constants.Issue.STATE_CLOSED.equals(mPullRequest.getState())) {
                menu.removeItem(R.id.pull_close);
                if (mPullRequest.isMerged()) {
                    menu.findItem(R.id.pull_reopen).setEnabled(false);
                }
            } else {
                menu.removeItem(R.id.pull_reopen);
            }

            if (!mIsCollaborator) {
                menu.removeItem(R.id.pull_merge);
            } else if (mPullRequest.isMerged()) {
                MenuItem mergeItem = menu.findItem(R.id.pull_merge);
                mergeItem.setEnabled(false);
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

        mImageGetter = new HttpImageGetter(inflater.getContext());
        updateCommentSectionVisibility(v);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mImageGetter.destroy();
        mImageGetter = null;
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        LayoutInflater inflater = getLayoutInflater(savedInstanceState);
        mDescriptionView = (TextView) inflater.inflate(R.layout.issue_description,
                getListView(), false);
        getListView().addHeaderView(mDescriptionView, null, true);

        FragmentManager fm = getChildFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);

        final int stateColor;
        if (mPullRequest.isMerged()) {
            stateColor = R.attr.colorPullRequestMerged;
        } else if (Constants.Issue.STATE_CLOSED.equals(mPullRequest.getState())) {
            stateColor = R.attr.colorIssueClosed;
        } else {
            stateColor = R.attr.colorIssueOpen;
        }

        UiUtils.trySetListOverscrollColor(getListView(), stateColor);

        super.onActivityCreated(savedInstanceState);

        fillData();
        getLoaderManager().initLoader(1, null, mCollaboratorCallback);
    }

    @Override
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return super.canChildScrollUp();
    }

    @Override
    public void refresh() {
        super.refresh();
        getLoaderManager().getLoader(1).onContentChanged();
    }

    @Override
    protected RootAdapter<IssueEventHolder> onCreateAdapter() {
        mAdapter = new IssueEventAdapter(getActivity(), mRepoOwner, mRepoName, this);
        return mAdapter;
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
                AsyncTaskCompat.executeParallel(
                        new PullRequestOpenCloseTask(item.getItemId() == R.id.pull_reopen));
                break;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_pull_subject,
                        mPullRequest.getNumber(), mPullRequest.getTitle(),
                        mRepoOwner + "/" + mRepoName));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  mPullRequest.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setListShown(boolean shown) {
        super.setListShown(shown);
        mListShown = shown;
        updateCommentSectionVisibility(getView());
    }

    @Override
    public void setListShownNoAnimation(boolean shown) {
        super.setListShownNoAnimation(shown);
        mListShown = shown;
        updateCommentSectionVisibility(getView());
    }

    private void updateCommentSectionVisibility(View v) {
        if (v == null) {
            return;
        }

        int commentVisibility = mListShown && Gh4Application.get().isAuthorized()
                ? View.VISIBLE : View.GONE;
        v.findViewById(R.id.comment_box).setVisibility(commentVisibility);
    }

    private void fillData() {
        String body = mPullRequest.getBodyHtml();
        mDescriptionView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        if (!StringUtils.isBlank(body)) {
            body = HtmlUtils.format(body).toString();
            mImageGetter.bind(mDescriptionView, body, mPullRequest.getId());
        }
    }

    private void showMergeDialog() {
        String title = getString(R.string.pull_message_dialog_title, mPullRequest.getNumber());
        View view = getLayoutInflater(null).inflate(R.layout.pull_merge_message_dialog, null);

        final EditText editor = (EditText) view.findViewById(R.id.et_commit_message);
        editor.setText(mPullRequest.getTitle());

        new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(getString(R.string.pull_request_merge), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        AsyncTaskCompat.executeParallel(new PullRequestMergeTask(text));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public Loader<LoaderResult<List<IssueEventHolder>>> onCreateLoader(int id, Bundle args) {
        return new PullRequestCommentListLoader(getActivity(),
                mRepoOwner, mRepoName, mPullRequest.getNumber());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                refreshComments();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onItemClick(IssueEventHolder event) {
    }

    @Override
    public void editComment(Comment comment) {
        Intent intent = new Intent(getActivity(), comment instanceof CommitComment
                ? EditPullRequestCommentActivity.class : EditIssueCommentActivity.class);

        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Comment.ID, comment.getId());
        intent.putExtra(Constants.Comment.BODY, comment.getBody());
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.pull_request_comment_hint;
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        issueService.createComment(mRepoOwner, mRepoName, mPullRequest.getNumber(), comment);
    }

    @Override
    public void onCommentSent() {
        // reload comments
        super.refresh();
    }

    public void refreshComments() {
        // no need to refresh pull request and collaborator status in that case
        super.refresh();
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
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            PullRequest pullRequest = new PullRequest();
            pullRequest.setNumber(mPullRequest.getNumber());
            pullRequest.setState(mOpen ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED);

            return pullService.editPullRequest(repoId, pullRequest);
        }

        @Override
        protected void onSuccess(PullRequest result) {
            mPullRequest = result;
            ToastUtils.showMessage(mContext,
                    mOpen ? R.string.issue_success_reopen : R.string.issue_success_close);

            fillData();
            // reload events, the action will have triggered an additional one
            PullRequestFragment.super.refresh();
            getActivity().supportInvalidateOptionsMenu();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext,
                    mOpen ? R.string.issue_error_reopen : R.string.issue_error_close);
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
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

            return pullService.merge(repoId, mPullRequest.getNumber(), mCommitMessage);
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
