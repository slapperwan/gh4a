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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
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
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.PullRequestCommentListLoader;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IssueLabelSpan;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class PullRequestFragment extends ListDataBaseFragment<IssueEventHolder> implements
        View.OnClickListener, IssueEventAdapter.OnEditComment, CommentBoxFragment.Callback {
    private static final int REQUEST_EDIT = 1000;

    public interface StateChangeListener {
        void onPullRequestStateChanged(PullRequest newState);
    }

    private View mListHeaderView;
    private PullRequest mPullRequest;
    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private boolean mIsCollaborator;
    private boolean mListShown;
    private CommentBoxFragment mCommentFragment;
    private IssueEventAdapter mAdapter;
    private HttpImageGetter mImageGetter;

    private LoaderCallbacks<List<CommitStatus>> mStatusCallback =
            new LoaderCallbacks<List<CommitStatus>>(this) {
        @Override
        protected Loader<LoaderResult<List<CommitStatus>>> onCreateLoader() {
            return new CommitStatusLoader(getActivity(), mRepoOwner, mRepoName,
                    mPullRequest.getHead().getSha());
        }

        @Override
        protected void onResultReady(List<CommitStatus> result) {
            fillStatus(result);
        }
    };

    private LoaderCallbacks<Boolean> mCollaboratorCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsCollaboratorLoader(getActivity(), mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsCollaborator = result;
            updateCommentLockState();
            getActivity().supportInvalidateOptionsMenu();
        }
    };

    public static PullRequestFragment newInstance(PullRequest pr, Issue issue) {
        PullRequestFragment f = new PullRequestFragment();

        Bundle args = new Bundle();
        args.putSerializable("PULL", pr);
        args.putSerializable("ISSUE", issue);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPullRequest = (PullRequest) getArguments().getSerializable("PULL");
        mIssue = (Issue) getArguments().getSerializable("ISSUE");

        Repository repo = mPullRequest.getBase().getRepo();
        mRepoOwner = repo.getOwner().getLogin();
        mRepoName = repo.getName();

        setHasOptionsMenu(true);
        updateCommentLockState();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.pullrequest_menu, menu);

        Gh4Application app = Gh4Application.get();
        boolean authorized = app.isAuthorized();

        boolean isCreator = ApiHelpers.loginEquals(mPullRequest.getUser(), app.getAuthLogin());
        boolean canOpenOrClose = authorized && (isCreator || mIsCollaborator);
        boolean canMerge = authorized && mIsCollaborator;

        if (!canOpenOrClose) {
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

        if (!canMerge) {
            menu.removeItem(R.id.pull_merge);
        } else if (mPullRequest.isMerged() || !mPullRequest.isMergeable()) {
            MenuItem mergeItem = menu.findItem(R.id.pull_merge);
            mergeItem.setEnabled(false);
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LayoutInflater inflater = getLayoutInflater(savedInstanceState);
        mListHeaderView = inflater.inflate(R.layout.issue_comment_list_header,
                getRecyclerView(), false);
        mAdapter.setHeaderView(mListHeaderView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        FragmentManager fm = getChildFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);

        fillData();
        updateCommentLockState();

        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(1, null, mCollaboratorCallback);
        loadStatusIfOpen();
    }

    @Override
    public void onRefresh() {
        mIsCollaborator = false;
        if (mListHeaderView != null) {
            getActivity().supportInvalidateOptionsMenu();
            fillLabels(new ArrayList<Label>());
            fillStatus(new ArrayList<CommitStatus>());
        }
        hideContentAndRestartLoaders(1, 2);
        super.onRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageGetter.resume();
        mAdapter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageGetter.pause();
        mAdapter.pause();
    }

    @Override
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return super.canChildScrollUp();
    }

    @Override
    protected RootAdapter<IssueEventHolder, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new IssueEventAdapter(getActivity(), mRepoOwner, mRepoName, this);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }

    @Override
    protected void updateEmptyState() {
        setContentEmpty(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pull_merge:
                showMergeDialog();
                break;
            case R.id.pull_close:
            case R.id.pull_reopen:
                new PullRequestOpenCloseTask(item.getItemId() == R.id.pull_reopen).schedule();
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
    public void setContentShown(boolean shown) {
        super.setContentShown(shown);
        mListShown = shown;
        updateCommentSectionVisibility(getView());
    }

    @Override
    public void setContentShownNoAnimation(boolean shown) {
        super.setContentShownNoAnimation(shown);
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

    private void loadStatusIfOpen() {
        if (Constants.Issue.STATE_OPEN.equals(mPullRequest.getState())) {
            getLoaderManager().initLoader(2, null, mStatusCallback);
        }
    }

    private void updateCommentLockState() {
        if (mCommentFragment != null) {
            boolean locked = mPullRequest.isLocked() && !mIsCollaborator;
            mCommentFragment.setLocked(locked);
        }
    }

    private void fillData() {
        ImageView ivGravatar = (ImageView) mListHeaderView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(ivGravatar, mPullRequest.getUser());
        ivGravatar.setTag(mPullRequest.getUser());
        ivGravatar.setOnClickListener(this);

        TextView tvExtra = (TextView) mListHeaderView.findViewById(R.id.tv_extra);
        tvExtra.setText(mPullRequest.getUser().getLogin());

        TextView tvTimestamp = (TextView) mListHeaderView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(getActivity(),
                mPullRequest.getCreatedAt(), true));

        String body = mPullRequest.getBodyHtml();
        TextView descriptionView = (TextView) mListHeaderView.findViewById(R.id.tv_desc);
        descriptionView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        if (!StringUtils.isBlank(body)) {
            body = HtmlUtils.format(body).toString();
            mImageGetter.bind(descriptionView, body, mPullRequest.getId());
        }

        View milestoneGroup = mListHeaderView.findViewById(R.id.milestone_container);
        if (mPullRequest.getMilestone() != null) {
            TextView tvMilestone = (TextView) mListHeaderView.findViewById(R.id.tv_milestone);
            tvMilestone.setText(mPullRequest.getMilestone().getTitle());
            milestoneGroup.setVisibility(View.VISIBLE);
        } else {
            milestoneGroup.setVisibility(View.GONE);
        }

        View assigneeGroup = mListHeaderView.findViewById(R.id.assignee_container);
        if (mPullRequest.getAssignee() != null) {
            TextView tvAssignee = (TextView) mListHeaderView.findViewById(R.id.tv_assignee);
            tvAssignee.setText(mPullRequest.getAssignee().getLogin());

            ImageView ivAssignee = (ImageView) mListHeaderView.findViewById(R.id.iv_assignee);
            AvatarHandler.assignAvatar(ivAssignee, mPullRequest.getAssignee());
            ivAssignee.setTag(mPullRequest.getAssignee());
            ivAssignee.setOnClickListener(this);
            assigneeGroup.setVisibility(View.VISIBLE);
        } else {
            assigneeGroup.setVisibility(View.GONE);
        }

        if (mPullRequest.isMerged()) {
            setHighlightColors(R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark);
        } else if (Constants.Issue.STATE_CLOSED.equals(mPullRequest.getState())) {
            setHighlightColors(R.attr.colorIssueClosed, R.attr.colorIssueClosedDark);
        } else {
            setHighlightColors(R.attr.colorIssueOpen, R.attr.colorIssueOpenDark);
        }
    }

    private void fillLabels(List<Label> labels) {
        View labelGroup = mListHeaderView.findViewById(R.id.label_container);
        if (labels != null && !labels.isEmpty()) {
            TextView labelView = (TextView) mListHeaderView.findViewById(R.id.labels);
            SpannableStringBuilder builder = new SpannableStringBuilder();

            for (Label label : labels) {
                int pos = builder.length();
                IssueLabelSpan span = new IssueLabelSpan(getActivity(), label, true);
                builder.append(label.getName());
                builder.setSpan(span, pos, pos + label.getName().length(), 0);
            }
            labelView.setText(builder);
            labelGroup.setVisibility(View.VISIBLE);
        } else {
            labelGroup.setVisibility(View.GONE);
        }
    }

    private void fillStatus(List<CommitStatus> statuses) {
        Map<String, CommitStatus> statusByContext = new HashMap<>();
        for (CommitStatus status : statuses) {
            if (!statusByContext.containsKey(status.getContext())) {
                statusByContext.put(status.getContext(), status);
            }
        }

        final int statusIconDrawableAttrId, statusLabelResId;
        if (PullRequest.MERGEABLE_STATE_CLEAN.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeOkIcon;
            statusLabelResId = R.string.pull_merge_status_clean;
        } else if (PullRequest.MERGEABLE_STATE_UNSTABLE.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeUnstableIcon;
            statusLabelResId = R.string.pull_merge_status_unstable;
        } else if (PullRequest.MERGEABLE_STATE_DIRTY.equals(mPullRequest.getMergeableState())) {
            statusIconDrawableAttrId = R.attr.pullRequestMergeDirtyIcon;
            statusLabelResId = R.string.pull_merge_status_dirty;
        } else if (statusByContext.isEmpty()) {
            // unknwon status, no commit statuses -> nothing to display
            return;
        } else {
            statusIconDrawableAttrId = R.attr.pullRequestMergeUnknownIcon;
            statusLabelResId = R.string.pull_merge_status_unknown;
        }

        ImageView statusIcon = (ImageView) mListHeaderView.findViewById(R.id.iv_merge_status_icon);
        statusIcon.setImageResource(UiUtils.resolveDrawable(getActivity(),
                statusIconDrawableAttrId));

        TextView statusLabel = (TextView) mListHeaderView.findViewById(R.id.merge_status_label);
        statusLabel.setText(statusLabelResId);

        ViewGroup statusContainer = (ViewGroup)
                mListHeaderView.findViewById(R.id.merge_commit_status_container);
        LayoutInflater inflater = getLayoutInflater(null);
        statusContainer.removeAllViews();
        for (CommitStatus status : statusByContext.values()) {
            View statusRow = inflater.inflate(R.layout.row_commit_status, statusContainer, false);

            String state = status.getState();
            final int iconDrawableAttrId;
            if (CommitStatus.STATE_ERROR.equals(state) || CommitStatus.STATE_FAILURE.equals(state)) {
                iconDrawableAttrId = R.attr.commitStatusFailIcon;
            } else if (CommitStatus.STATE_SUCCESS.equals(state)) {
                iconDrawableAttrId = R.attr.commitStatusOkIcon;
            } else {
                iconDrawableAttrId = R.attr.commitStatusUnknownIcon;
            }
            ImageView icon = (ImageView) statusRow.findViewById(R.id.iv_status_icon);
            icon.setImageResource(UiUtils.resolveDrawable(getActivity(), iconDrawableAttrId));

            TextView context = (TextView) statusRow.findViewById(R.id.tv_context);
            context.setText(status.getContext());

            TextView description = (TextView) statusRow.findViewById(R.id.tv_desc);
            description.setText(status.getDescription());

            statusContainer.addView(statusRow);
        }
        mListHeaderView.findViewById(R.id.merge_commit_no_status).setVisibility(
                statusByContext.isEmpty() ? View.VISIBLE : View.GONE);

        mListHeaderView.findViewById(R.id.merge_status_container).setVisibility(View.VISIBLE);
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
                        new PullRequestMergeTask(text).schedule();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    @Override
    public void onClick(View v) {
        User user = (User) v.getTag();
        Intent intent = IntentUtils.getUserActivityIntent(getActivity(), user);
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public Loader<LoaderResult<List<IssueEventHolder>>> onCreateLoader() {
        return new PullRequestCommentListLoader(getActivity(),
                mRepoOwner, mRepoName, mPullRequest.getNumber());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                refreshComments();
                getActivity().setResult(Activity.RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onItemClick(IssueEventHolder event) {
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
        super.onRefresh();
    }

    public void refreshComments() {
        // no need to refresh pull request and collaborator status in that case
        super.onRefresh();
    }

    private class PullRequestOpenCloseTask extends ProgressDialogTask<PullRequest> {
        private boolean mOpen;

        public PullRequestOpenCloseTask(boolean open) {
            super(getBaseActivity(), 0, open ? R.string.opening_msg : R.string.closing_msg);
            mOpen = open;
        }

        @Override
        protected ProgressDialogTask<PullRequest> clone() {
            return new PullRequestOpenCloseTask(mOpen);
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

            fillData();
            loadStatusIfOpen();
            // reload events, the action will have triggered an additional one
            PullRequestFragment.super.onRefresh();

            BaseActivity activity = getBaseActivity();
            if (activity instanceof StateChangeListener) {
                ((StateChangeListener) activity).onPullRequestStateChanged(result);
            }
            activity.supportInvalidateOptionsMenu();
        }

        @Override
        protected String getErrorMessage() {
            int errorMessageResId =
                    mOpen ? R.string.issue_error_reopen : R.string.issue_error_close;
            return getContext().getString(errorMessageResId, mPullRequest.getNumber());
        }
    }

    private class PullRequestMergeTask extends ProgressDialogTask<MergeStatus> {
        private String mCommitMessage;

        public PullRequestMergeTask(String commitMessage) {
            super(getBaseActivity(), 0, R.string.merging_msg);
            mCommitMessage = commitMessage;
        }

        @Override
        protected ProgressDialogTask<MergeStatus> clone() {
            return new PullRequestMergeTask(mCommitMessage);
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
            setContentShown(false);
            getLoaderManager().getLoader(1).onContentChanged();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.pull_error_merge, mPullRequest.getNumber());
        }
    }
}
