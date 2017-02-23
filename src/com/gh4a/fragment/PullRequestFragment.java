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

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.EditPullRequestCommentActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitStatusLoader;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentListLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.IssueLabelSpan;
import com.gh4a.widget.StyleableTextView;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PullRequestFragment extends ListDataBaseFragment<IssueEventHolder> implements
        View.OnClickListener, IssueEventAdapter.OnCommentAction<IssueEventHolder>,
        CommentBoxFragment.Callback {
    private static final int REQUEST_EDIT = 1000;

    private View mListHeaderView;
    private PullRequest mPullRequest;
    private Issue mIssue;
    private String mRepoOwner;
    private String mRepoName;
    private long mInitialCommentId;
    private boolean mIsCollaborator;
    private boolean mListShown;
    private CommentBoxFragment mCommentFragment;
    private IssueEventAdapter mAdapter;
    private HttpImageGetter mImageGetter;

    private final LoaderCallbacks<List<CommitStatus>> mStatusCallback =
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

    public static PullRequestFragment newInstance(PullRequest pr, Issue issue,
            boolean isCollaborator, long initialCommentId) {
        PullRequestFragment f = new PullRequestFragment();

        Bundle args = new Bundle();
        args.putSerializable("pr", pr);
        args.putSerializable("issue", issue);
        args.putSerializable("collaborator", isCollaborator);
        args.putLong("initial_comment", initialCommentId);
        f.setArguments(args);

        return f;
    }

    public void update(PullRequest pr) {
        mPullRequest = pr;

        fillData();
        loadStatusIfOpen();
        // reload events
        super.onRefresh();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPullRequest = (PullRequest) getArguments().getSerializable("pr");
        mIssue = (Issue) getArguments().getSerializable("issue");
        mIsCollaborator = getArguments().getBoolean("collaborator");
        mInitialCommentId = getArguments().getLong("initial_comment", -1);

        Repository repo = mPullRequest.getBase().getRepo();
        mRepoOwner = repo.getOwner().getLogin();
        mRepoName = repo.getName();

        updateCommentLockState();
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
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);

        mListHeaderView = inflater.inflate(R.layout.issue_comment_list_header, view, false);
        mAdapter.setHeaderView(mListHeaderView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        FragmentManager fm = getChildFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);

        fillData();
        fillLabels(mIssue.getLabels());
        updateCommentLockState();

        super.onActivityCreated(savedInstanceState);

        loadStatusIfOpen();
    }

    @Override
    public void onRefresh() {
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
        mAdapter = new IssueEventAdapter(getActivity(), mRepoOwner, mRepoName,
                mPullRequest.getNumber(), this);
        mAdapter.setLocked(isLocked());
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<IssueEventHolder, ?> adapter, List<IssueEventHolder> data) {
        super.onAddData(adapter, data);
        if (mInitialCommentId >= 0) {
            for (int i = 0; i < data.size(); i++) {
                IssueEventHolder event = data.get(i);
                if (event.comment != null && event.comment.getId() == mInitialCommentId) {
                    getLayoutManager().scrollToPosition(i + 1 /* adjust for header view */);
                    break;
                }
            }
            mInitialCommentId = -1;
        }
        updateMentionUsers();
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }

    @Override
    protected void updateEmptyState() {
        // we're never empty -> don't call super
    }

    @Override
    protected void setContentShown(boolean shown) {
        super.setContentShown(shown);
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
        if (ApiHelpers.IssueState.OPEN.equals(mPullRequest.getState())) {
            getLoaderManager().initLoader(2, null, mStatusCallback);
        }
    }

    private boolean isLocked() {
        return mPullRequest.isLocked() && !mIsCollaborator;
    }

    private void updateMentionUsers() {
        Set<User> users = mAdapter.getUsers();
        users.add(mPullRequest.getUser());
        mCommentFragment.setMentionUsers(users);
    }

    private void updateCommentLockState() {
        if (mCommentFragment != null) {
            mCommentFragment.setLocked(isLocked());
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

            if (!isLocked()) {
                descriptionView.setCustomSelectionActionModeCallback(
                        new UiUtils.QuoteActionModeCallback(descriptionView) {
                    @Override
                    public void onTextQuoted(CharSequence text) {
                        quoteText(text);
                    }
                });
            } else {
                descriptionView.setCustomSelectionActionModeCallback(null);
            }
        } else {
            SpannableString noDescriptionString = new SpannableString(getString(R.string.issue_no_description));
            noDescriptionString.setSpan(new StyleSpan(Typeface.ITALIC), 0, noDescriptionString.length(), 0);
            descriptionView.setText(noDescriptionString);
        }

        View branchGroup = mListHeaderView.findViewById(R.id.pr_container);
        branchGroup.setVisibility(View.VISIBLE);

        StyleableTextView fromBranch = (StyleableTextView) branchGroup.findViewById(R.id.tv_pr_from);
        StringUtils.applyBoldTagsAndSetText(fromBranch, getString(R.string.pull_request_from,
                mPullRequest.getHead().getLabel()));

        StyleableTextView toBranch = (StyleableTextView) branchGroup.findViewById(R.id.tv_pr_to);
        StringUtils.applyBoldTagsAndSetText(toBranch, getString(R.string.pull_request_to,
                mPullRequest.getBase().getLabel()));

        View milestoneGroup = mListHeaderView.findViewById(R.id.milestone_container);
        if (mPullRequest.getMilestone() != null) {
            TextView tvMilestone = (TextView) mListHeaderView.findViewById(R.id.tv_milestone);
            tvMilestone.setText(mPullRequest.getMilestone().getTitle());
            milestoneGroup.setVisibility(View.VISIBLE);
        } else {
            milestoneGroup.setVisibility(View.GONE);
        }

        View assigneeGroup = mListHeaderView.findViewById(R.id.assignee_container);
        List<User> assignees = mPullRequest.getAssignees();
        if (assignees != null && !assignees.isEmpty()) {
            ViewGroup assigneeContainer = (ViewGroup) mListHeaderView.findViewById(R.id.assignee_list);
            LayoutInflater inflater = getLayoutInflater(null);
            assigneeContainer.removeAllViews();
            for (User assignee : assignees) {
                View row = inflater.inflate(R.layout.row_assignee, assigneeContainer, false);
                TextView tvAssignee = (TextView) row.findViewById(R.id.tv_assignee);
                tvAssignee.setText(ApiHelpers.getUserLogin(getActivity(), assignee));

                ImageView ivAssignee = (ImageView) row.findViewById(R.id.iv_assignee);
                AvatarHandler.assignAvatar(ivAssignee, assignee);
                ivAssignee.setTag(assignee);
                ivAssignee.setOnClickListener(this);

                assigneeContainer.addView(row);
            }
            assigneeGroup.setVisibility(View.VISIBLE);
        } else {
            assigneeGroup.setVisibility(View.GONE);
        }

        if (mPullRequest.isMerged()) {
            setHighlightColors(R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark);
        } else if (ApiHelpers.IssueState.CLOSED.equals(mPullRequest.getState())) {
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

    @Override
    public void onClick(View v) {
        Intent intent = UserActivity.makeIntent(getActivity(), (User) v.getTag());
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
    public void editComment(IssueEventHolder item) {
        Intent intent = item.comment instanceof CommitComment
                ? EditPullRequestCommentActivity.makeIntent(getActivity(),
                        mRepoOwner, mRepoName, (CommitComment) item.comment)
                : EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName, item.comment);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void quoteText(CharSequence text) {
        mCommentFragment.addQuote(text);
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
        if (isAdded()) {
            super.onRefresh();
        }
    }

    public void refreshComments() {
        // no need to refresh pull request and collaborator status in that case
        super.onRefresh();
    }
}
