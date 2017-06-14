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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.timeline.TimelineItemAdapter;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ReactionBar;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.List;

// TODO: Re-enable commented out code
public abstract class IssueFragmentBase extends ListDataBaseFragment<TimelineItem> implements
        View.OnClickListener, CommentBoxFragment.Callback, TimelineItemAdapter.OnCommentAction,
        ReactionBar.Callback, ReactionBar.Item, ReactionBar.ReactionDetailsCache.Listener {
    protected static final int REQUEST_EDIT = 1000;

    protected View mListHeaderView;
    protected Issue mIssue;
    protected String mRepoOwner;
    protected String mRepoName;
    private IntentUtils.InitialCommentMarker mInitialComment;
    private boolean mIsCollaborator;
    private boolean mListShown;
    private CommentBoxFragment mCommentFragment;
    private ReactionBar.AddReactionMenuHelper mReactionMenuHelper;
    private ReactionBar.ReactionDetailsCache mReactionDetailsCache =
            new ReactionBar.ReactionDetailsCache(this);
    private TimelineItemAdapter mAdapter;
    private HttpImageGetter mImageGetter;

    protected static Bundle buildArgs(String repoOwner, String repoName,
            Issue issue, boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putSerializable("issue", issue);
        args.putSerializable("collaborator", isCollaborator);
        args.putParcelable("initial_comment", initialComment);
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
        mInitialComment = args.getParcelable("initial_comment");
        args.remove("initial_comment");

        setHasOptionsMenu(true);
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
        mReactionDetailsCache.clear();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.issue_fragment_menu, menu);

        MenuItem reactItem = menu.findItem(R.id.react);
        inflater.inflate(R.menu.reaction_menu, reactItem.getSubMenu());
        if (mReactionMenuHelper == null) {
            mReactionMenuHelper = new ReactionBar.AddReactionMenuHelper(getActivity(),
                    reactItem.getSubMenu(), this, this, mReactionDetailsCache);
        } else {
            mReactionMenuHelper.updateFromMenu(reactItem.getSubMenu());
        }
        mReactionMenuHelper.startLoadingIfNeeded();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mReactionMenuHelper != null && mReactionMenuHelper.onItemClick(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void reloadEvents(boolean alsoClearCaches) {
        if (mAdapter != null && !alsoClearCaches) {
            // Don't clear adapter's cache, we're only interested in the new event
            mAdapter.suppressCacheClearOnNextClear();
        }
        super.onRefresh();
    }

    @Override
    protected RootAdapter<TimelineItem, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName, mIssue.getNumber(),
                mIssue.getPullRequest() != null, true, this);
        mAdapter.setLocked(isLocked());
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<TimelineItem, ?> adapter, List<TimelineItem> data) {
        super.onAddData(adapter, data);
        if (mInitialComment != null) {
            for (int i = 0; i < data.size(); i++) {
                TimelineItem item = data.get(i);

                if (item instanceof TimelineItem.TimelineComment) {
                    TimelineItem.TimelineComment comment = (TimelineItem.TimelineComment) item;
                    if (mInitialComment.matches(comment.comment.getId(), comment.getCreatedAt())) {
                        scrollToAndHighlightPosition(i + 1 /* adjust for header view */);
                        break;
                    }
                } else if (item instanceof TimelineItem.TimelineReview) {
                    TimelineItem.TimelineReview review = (TimelineItem.TimelineReview) item;
                    if (mInitialComment.matches(review.review.getId(), review.getCreatedAt())) {
                        scrollToAndHighlightPosition(i + 1 /* adjust for header view */);
                        break;
                    }
                }
            }
            mInitialComment = null;
        }

        // TODO
//        updateMentionUsers();
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

//    private void updateMentionUsers() {
//        Set<User> users = mAdapter.getUsers();
//        if (mIssue.getUser() != null) {
//            users.add(mIssue.getUser());
//        }
//        mCommentFragment.setMentionUsers(users);
//    }

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

        ReactionBar reactions = (ReactionBar) mListHeaderView.findViewById(R.id.reactions);
        reactions.setCallback(this, this);
        reactions.setDetailsCache(mReactionDetailsCache);
        reactions.setReactions(mIssue.getReactions());

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
    public Object getCacheKey() {
        return mIssue;
    }

    @Override
    public List<Reaction> loadReactionDetailsInBackground(ReactionBar.Item item) throws IOException {
        IssueService service = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return service.getIssueReactions(new RepositoryId(mRepoOwner, mRepoName),
                mIssue.getNumber());
    }

    @Override
    public Reaction addReactionInBackground(ReactionBar.Item item, String content) throws IOException {
        IssueService service = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return service.addIssueReaction(new RepositoryId(mRepoOwner, mRepoName),
                mIssue.getNumber(), content);
    }

    @Override
    public void onReactionsUpdated(ReactionBar.Item item, Reactions reactions) {
        mIssue.setReactions(reactions);
        if (mListHeaderView != null) {
            ReactionBar bar = (ReactionBar) mListHeaderView.findViewById(R.id.reactions);
            bar.setReactions(reactions);
        }
        if (mReactionMenuHelper != null) {
            mReactionMenuHelper.update();
            getActivity().supportInvalidateOptionsMenu();
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
                reloadEvents(true);
                getActivity().setResult(Activity.RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
            reloadEvents(false);
        }
    }

    protected abstract void bindSpecialViews(View headerView);
    protected abstract void assignHighlightColor();
    protected abstract void deleteCommentInBackground(RepositoryId repoId, Comment comment)
            throws Exception;

    @Override
    public void deleteComment(final Comment comment) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_comment_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteCommentTask(getBaseActivity(), comment).schedule();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void quoteText(CharSequence text) {
        mCommentFragment.addQuote(text);
    }

    @Override
    public String getShareSubject(Comment comment) {
        return getString(R.string.share_comment_subject, comment.getId(), mIssue.getNumber(),
                mRepoOwner + "/" + mRepoName);
    }

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
            reloadEvents(false);
            getActivity().setResult(Activity.RESULT_OK);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_comment);
        }
    }
}
