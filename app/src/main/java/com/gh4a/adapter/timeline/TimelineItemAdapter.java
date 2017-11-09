package com.gh4a.adapter.timeline;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.ReactionBar;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.User;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

public class TimelineItemAdapter
        extends RootAdapter<TimelineItem, TimelineItemAdapter.TimelineItemViewHolder>
        implements ReactionBar.ReactionDetailsCache.Listener {

    private static final int VIEW_TYPE_COMMENT = CUSTOM_VIEW_TYPE_START + 1;
    private static final int VIEW_TYPE_EVENT = CUSTOM_VIEW_TYPE_START + 2;
    private static final int VIEW_TYPE_REVIEW = CUSTOM_VIEW_TYPE_START + 3;
    private static final int VIEW_TYPE_DIFF = CUSTOM_VIEW_TYPE_START + 4;
    private static final int VIEW_TYPE_REPLY = CUSTOM_VIEW_TYPE_START + 5;

    private final HttpImageGetter mImageGetter;
    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;
    private final boolean mIsPullRequest;
    private final boolean mDisplayReviewDetails;
    private final ReactionBar.ReactionDetailsCache mReactionDetailsCache =
            new ReactionBar.ReactionDetailsCache(this);
    private final OnCommentAction mActionCallback;

    private boolean mDontClearCacheOnClear;
    private boolean mLocked;

    public interface OnCommentAction {
        void editComment(GitHubCommentBase comment);
        void deleteComment(GitHubCommentBase comment);
        void quoteText(CharSequence text);
        void addText(CharSequence text);
        void onReplyCommentSelected(long replyToId);
        long getSelectedReplyCommentId();
        String getShareSubject(GitHubCommentBase comment);
        Single<List<Reaction>> loadReactionDetails(GitHubCommentBase comment);
        Single<Reaction> addReaction(GitHubCommentBase comment, String content);
    }

    private final ReviewViewHolder.Callback mReviewCallback = new ReviewViewHolder.Callback() {
        @Override
        public boolean canQuote() {
            return !mLocked && mDisplayReviewDetails;
        }

        @Override
        public void quoteText(CharSequence text) {
            mActionCallback.quoteText(text);
        }
    };

    private final CommentViewHolder.Callback mCommentCallback = new CommentViewHolder.Callback() {
        @Override
        public boolean canQuote() {
            return !mLocked;
        }

        @Override
        public void quoteText(CharSequence text) {
            mActionCallback.quoteText(text);
        }

        @Override
        public void addText(CharSequence text) {
            mActionCallback.addText(text);
        }

        @Override
        public boolean onMenItemClick(TimelineItem.TimelineComment comment, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit:
                    mActionCallback.editComment(comment.comment());
                    return true;

                case R.id.delete:
                    mActionCallback.deleteComment(comment.comment());
                    return true;

                case R.id.share:
                    IntentUtils.share(mContext, mActionCallback.getShareSubject(comment.comment()),
                            comment.comment().htmlUrl());
                    return true;

                case R.id.view_in_file:
                    Intent intent = comment.makeDiffIntent(mContext);
                    if (intent != null) {
                        mContext.startActivity(intent);
                    }
                    return true;
            }

            return false;
        }

        @Override
        public Single<List<Reaction>> loadReactionDetails(TimelineItem.TimelineComment item) {
            return mActionCallback.loadReactionDetails(item.comment());
        }

        @Override
        public Single<Reaction> addReaction(TimelineItem.TimelineComment item, String content) {
            return mActionCallback.addReaction(item.comment(), content);
        }
    };

    private final ReplyViewHolder.Callback mReplyCallback = new ReplyViewHolder.Callback() {
        @Override
        public long getSelectedCommentId() {
            return mActionCallback.getSelectedReplyCommentId();
        }

        @Override
        public void reply(long replyToId) {
            mActionCallback.onReplyCommentSelected(replyToId);
            notifyDataSetChanged();
        }
    };

    public TimelineItemAdapter(Context context, String repoOwner, String repoName, int issueNumber,
            boolean isPullRequest, boolean displayReviewDetails, OnCommentAction callback) {
        super(context);
        mImageGetter = new HttpImageGetter(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mIsPullRequest = isPullRequest;
        mDisplayReviewDetails = displayReviewDetails;
        mActionCallback = callback;
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        notifyDataSetChanged();
    }

    public void destroy() {
        mImageGetter.destroy();
        mReactionDetailsCache.destroy();
    }

    public void pause() {
        mImageGetter.pause();
    }

    public void resume() {
        mImageGetter.resume();
    }

    public void suppressCacheClearOnNextClear() {
        mDontClearCacheOnClear = true;
    }

    public Set<User> getUsers() {
        final HashSet<User> users = new HashSet<>();
        for (int i = 0; i < getCount(); i++) {
            User user = getItem(i).getUser();
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    @Override
    public void clear() {
        super.clear();
        if (!mDontClearCacheOnClear) {
            mImageGetter.clearHtmlCache();
        }
    }

    @Override
    public void addAll(Collection<TimelineItem> objects) {
        mDontClearCacheOnClear = false;
        super.addAll(objects);
    }

    @Override
    public TimelineItemViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
            int viewType) {
        View view;
        TimelineItemViewHolder holder;
        switch (viewType) {
            case VIEW_TYPE_COMMENT:
                view = inflater.inflate(R.layout.row_timeline_comment, parent, false);
                holder = new CommentViewHolder(view, mImageGetter, mRepoOwner,
                        mReactionDetailsCache, mCommentCallback);
                break;
            case VIEW_TYPE_EVENT:
                view = inflater.inflate(R.layout.row_timeline_event, parent, false);
                holder = new EventViewHolder(view, mRepoOwner, mRepoName, mIsPullRequest);
                break;
            case VIEW_TYPE_REVIEW:
                view = inflater.inflate(R.layout.row_timeline_review, parent, false);
                holder = new ReviewViewHolder(view, mImageGetter, mRepoOwner, mRepoName,
                        mIssueNumber, mDisplayReviewDetails, mReviewCallback);
                break;
            case VIEW_TYPE_DIFF:
                view = inflater.inflate(R.layout.row_timeline_diff, parent, false);
                holder = new DiffViewHolder(view, mRepoOwner, mRepoName, mIssueNumber);
                break;
            case VIEW_TYPE_REPLY:
                view = inflater.inflate(R.layout.row_timeline_reply, parent, false);
                holder = new ReplyViewHolder(view, mReplyCallback);
                break;
            default:
                throw new IllegalArgumentException("viewType: Unknown timeline item type.");
        }
        return holder;
    }

    @Override
    protected int getItemViewType(TimelineItem item) {
        if (item instanceof TimelineItem.TimelineComment) {
            return VIEW_TYPE_COMMENT;
        }
        if (item instanceof TimelineItem.TimelineEvent) {
            return VIEW_TYPE_EVENT;
        }
        if (item instanceof TimelineItem.TimelineReview) {
            return VIEW_TYPE_REVIEW;
        }
        if (item instanceof TimelineItem.Diff) {
            return VIEW_TYPE_DIFF;
        }
        if (item instanceof TimelineItem.Reply) {
            return VIEW_TYPE_REPLY;
        }
        return super.getItemViewType(item);
    }

    @Override
    public void onBindViewHolder(TimelineItemViewHolder holder, TimelineItem item) {
        switch (getItemViewType(item)) {
            case VIEW_TYPE_COMMENT:
            case VIEW_TYPE_EVENT:
            case VIEW_TYPE_REVIEW:
            case VIEW_TYPE_DIFF:
            case VIEW_TYPE_REPLY:
                //noinspection unchecked
                holder.bind(item);
                holder.itemView.setAlpha(shouldFadeReplyGroup(item) ? 0.5f : 1f);
                break;
        }
    }

    @Override
    public void onReactionsUpdated(ReactionBar.Item item, Reactions reactions) {
        CommentViewHolder holder = (CommentViewHolder) item;
        holder.updateReactions(reactions);
    }

    private boolean shouldFadeReplyGroup(TimelineItem item) {
        long replyCommentId = mActionCallback.getSelectedReplyCommentId();
        if (replyCommentId == 0) {
            return false;
        }
        if (item instanceof TimelineItem.Diff) {
            return ((TimelineItem.Diff) item).getInitialComment().id() != replyCommentId;
        }
        if (item instanceof TimelineItem.TimelineComment) {
            TimelineItem.TimelineComment tc = (TimelineItem.TimelineComment) item;
            if (tc.getParentDiff() != null) {
                return tc.getParentDiff().getInitialComment().id() != replyCommentId;
            }
            return tc.comment().id() != replyCommentId;
        }
        return false;
    }

    public static abstract class TimelineItemViewHolder<TItem extends TimelineItem> extends
            RecyclerView.ViewHolder {

        protected final Context mContext;

        public TimelineItemViewHolder(View itemView) {
            super(itemView);

            mContext = itemView.getContext();
        }

        public abstract void bind(TItem item);
    }
}
