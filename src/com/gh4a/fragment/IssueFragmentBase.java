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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.IssueEventAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public abstract class IssueFragmentBase extends ListDataBaseFragment<IssueEventHolder> implements
        View.OnClickListener, IssueEventAdapter.OnCommentAction<IssueEventHolder>,
        CommentBoxFragment.Callback {
    protected static final int REQUEST_EDIT = 1000;

    protected View mListHeaderView;
    protected Issue mIssue;
    protected String mRepoOwner;
    protected String mRepoName;
    private long mInitialCommentId;
    private Date mLastReadAt;
    private boolean mIsCollaborator;
    private boolean mListShown;
    private CommentBoxFragment mCommentFragment;
    private IssueEventAdapter mAdapter;
    private HttpImageGetter mImageGetter;

    protected static Bundle buildArgs(String repoOwner, String repoName,
            Issue issue, boolean isCollaborator, long initialCommentId, Date lastReadAt) {
        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putSerializable("issue", issue);
        args.putSerializable("collaborator", isCollaborator);
        args.putLong("initial_comment", initialCommentId);
        args.putSerializable("last_read_at", lastReadAt);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mIssue = (Issue) args.getSerializable("issue");
        mIsCollaborator = args.getBoolean("collaborator");
        mInitialCommentId = args.getLong("initial_comment", -1);
        mLastReadAt = (Date) args.getSerializable("last_read_at");
        args.remove("initial_comment");
        args.remove("last_read_at");

        updateCommentLockState();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.issue, container, false);

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
    }

    @Override
    public void onRefresh() {
        if (mListHeaderView != null) {
            getActivity().supportInvalidateOptionsMenu();
            fillLabels(null);
        }
        if (mImageGetter != null) {
            mImageGetter.clearHtmlCache();
        }
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

    public void reloadEvents() {
        super.onRefresh();
    }

    @Override
    protected RootAdapter<IssueEventHolder, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new IssueEventAdapter(getActivity(), mRepoOwner, mRepoName,
                mIssue.getNumber(), this);
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
                    scrollToAndHighlightPosition(i + 1 /* adjust for header view */);
                    break;
                }
            }
            mInitialCommentId = -1;
        } else if (mLastReadAt != null) {
            for (int i = 0; i < data.size(); i++) {
                IssueEventHolder event = data.get(i);
                if (event.comment != null && event.getCreatedAt().after(mLastReadAt)) {
                    scrollToAndHighlightPosition(i + 1 /* adjust for header view */);
                    break;
                }
            }
        }

        mLastReadAt = null;

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

    private boolean isLocked() {
        return mIssue.isLocked() && !mIsCollaborator;
    }

    private void updateMentionUsers() {
        Set<User> users = mAdapter.getUsers();
        if (mIssue.getUser() != null) {
            users.add(mIssue.getUser());
        }
        mCommentFragment.setMentionUsers(users);
    }

    private void updateCommentLockState() {
        if (mCommentFragment != null) {
            mCommentFragment.setLocked(isLocked());
        }
    }

    private void fillData() {
        ImageView ivGravatar = (ImageView) mListHeaderView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(ivGravatar, mIssue.getUser());
        ivGravatar.setTag(mIssue.getUser());
        ivGravatar.setOnClickListener(this);

        TextView tvExtra = (TextView) mListHeaderView.findViewById(R.id.tv_extra);
        tvExtra.setText(ApiHelpers.getUserLogin(getActivity(), mIssue.getUser()));

        TextView tvTimestamp = (TextView) mListHeaderView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(getActivity(),
                mIssue.getCreatedAt(), true));

        String body = mIssue.getBodyHtml();
        TextView descriptionView = (TextView) mListHeaderView.findViewById(R.id.tv_desc);
        descriptionView.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        if (!StringUtils.isBlank(body)) {
            mImageGetter.bind(descriptionView, body, mIssue.getId());

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

        View milestoneGroup = mListHeaderView.findViewById(R.id.milestone_container);
        if (mIssue.getMilestone() != null) {
            TextView tvMilestone = (TextView) mListHeaderView.findViewById(R.id.tv_milestone);
            tvMilestone.setText(mIssue.getMilestone().getTitle());
            milestoneGroup.setVisibility(View.VISIBLE);
        } else {
            milestoneGroup.setVisibility(View.GONE);
        }

        View assigneeGroup = mListHeaderView.findViewById(R.id.assignee_container);
        List<User> assignees = mIssue.getAssignees();
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

        assignHighlightColor();
        bindSpecialViews(mListHeaderView);
    }

    private void fillLabels(List<Label> labels) {
        View labelGroup = mListHeaderView.findViewById(R.id.label_container);
        if (labels != null && !labels.isEmpty()) {
            TextView labelView = (TextView) mListHeaderView.findViewById(R.id.labels);
            labelView.setText(UiUtils.formatLabelList(getActivity(), labels));
            labelGroup.setVisibility(View.VISIBLE);
        } else {
            labelGroup.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = UserActivity.makeIntent(getActivity(), (User) v.getTag());
        if (intent != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                reloadEvents();
                getActivity().setResult(Activity.RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void quoteText(CharSequence text) {
        mCommentFragment.addQuote(text);
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        issueService.createComment(mRepoOwner, mRepoName, mIssue.getNumber(), comment);
    }

    @Override
    public void onCommentSent() {
        // reload comments
        if (isAdded()) {
            reloadEvents();
        }
    }

    @Override
    public void deleteComment(final IssueEventHolder comment) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_comment_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteCommentTask(getBaseActivity(), comment.comment).schedule();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    protected abstract void bindSpecialViews(View headerView);
    protected abstract void assignHighlightColor();
    protected abstract void deleteCommentInBackground(RepositoryId repoId, Comment comment)
            throws Exception;

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private final Comment mComment;

        public DeleteCommentTask(BaseActivity activity, Comment comment) {
            super(activity, R.string.deleting_msg);
            mComment = comment;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(getBaseActivity(), mComment);
        }

        @Override
        protected Void run() throws Exception {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            deleteCommentInBackground(repoId, mComment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            reloadEvents();
            getActivity().setResult(Activity.RESULT_OK);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_comment);
        }
    }
}
