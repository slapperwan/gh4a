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
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.HttpImageGetter;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.Comment;

import java.util.Collection;

import static com.gh4a.R.id.delete;
import static com.gh4a.R.id.edit;
import static com.gh4a.R.id.share;
import static com.gh4a.R.id.view_in_file;

public class TimelineItemAdapter extends
        RootAdapter<TimelineItem, TimelineItemAdapter.TimelineItemViewHolder> {

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
    private final OnCommentAction mActionCallback;

    private boolean mDontClearCacheOnClear;
    private boolean mLocked;

    public interface OnCommentAction {
        void editComment(Comment comment);
        void deleteComment(Comment comment);
        void quoteText(CharSequence text);
        String getShareSubject(Comment comment);
    }

    private CommentViewHolder.Callback mCallback = new CommentViewHolder.Callback() {
        @Override
        public boolean canQuote(Comment comment) {
            return !mLocked;
        }

        @Override
        public void quoteText(CharSequence text) {
            mActionCallback.quoteText(text);
        }

        @Override
        public boolean onMenItemClick(TimelineItem.TimelineComment comment, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case edit:
                    mActionCallback.editComment(comment.comment);
                    return true;

                case delete:
                    mActionCallback.deleteComment(comment.comment);
                    return true;

                case share:
                    IntentUtils.share(mContext, mActionCallback.getShareSubject(comment.comment),
                            comment.comment.getHtmlUrl());
                    return true;

                case view_in_file:
                    Intent intent = comment.makeDiffIntent(mContext);
                    if (intent != null) {
                        mContext.startActivity(intent);
                    }
                    return true;
            }

            return false;
        }
    };

    public TimelineItemAdapter(Context context, String repoOwner, String repoName, int issueNumber,
            boolean isPullRequest, OnCommentAction callback) {
        super(context);
        mImageGetter = new HttpImageGetter(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mIsPullRequest = isPullRequest;
        mActionCallback = callback;
    }

    public void setLocked(boolean locked) {
        mLocked = locked;
        notifyDataSetChanged();
    }

    public void destroy() {
        mImageGetter.destroy();
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
                holder = new CommentViewHolder(view, mImageGetter, mRepoOwner, mCallback);
                break;
            case VIEW_TYPE_EVENT:
                view = inflater.inflate(R.layout.row_timeline_event, parent, false);
                holder = new EventViewHolder(view, mIsPullRequest);
                break;
            case VIEW_TYPE_REVIEW:
                view = inflater.inflate(R.layout.row_timeline_review, parent, false);
                holder = new ReviewViewHolder(view, mRepoOwner, mRepoName, mIssueNumber,
                        mIsPullRequest);
                break;
            case VIEW_TYPE_DIFF:
                view = inflater.inflate(R.layout.row_timeline_diff, parent, false);
                holder = new DiffViewHolder(view, mRepoOwner, mRepoName, mIssueNumber);
                break;
            case VIEW_TYPE_REPLY:
                view = inflater.inflate(R.layout.row_timeline_reply, parent, false);
                holder = new ReplyViewHolder(view);
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
                ((CommentViewHolder) holder).bind((TimelineItem.TimelineComment) item);
                break;
            case VIEW_TYPE_EVENT:
                ((EventViewHolder) holder).bind((TimelineItem.TimelineEvent) item);
                break;
            case VIEW_TYPE_REVIEW:
                ((ReviewViewHolder) holder).bind((TimelineItem.TimelineReview) item);
                break;
            case VIEW_TYPE_DIFF:
                ((DiffViewHolder) holder).bind((TimelineItem.Diff) item);
                break;
            case VIEW_TYPE_REPLY:
                ((ReplyViewHolder) holder).bind((TimelineItem.Reply) item);
                break;
        }
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
