package com.gh4a.adapter.timeline;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.HttpImageGetter;

import java.util.Collection;

public class TimelineItemAdapter extends
        RootAdapter<TimelineItem, TimelineItemAdapter.TimelineItemViewHolder> {

    private static final int VIEW_TYPE_COMMENT = CUSTOM_VIEW_TYPE_START + 1;
    private static final int VIEW_TYPE_EVENT = CUSTOM_VIEW_TYPE_START + 2;
    private static final int VIEW_TYPE_COMMIT_COMMENT = CUSTOM_VIEW_TYPE_START + 3;
    private static final int VIEW_TYPE_REVIEW = CUSTOM_VIEW_TYPE_START + 4;
    private static final int VIEW_TYPE_DIFF = CUSTOM_VIEW_TYPE_START + 5;
    private static final int VIEW_TYPE_REPLY = CUSTOM_VIEW_TYPE_START + 6;

    private final HttpImageGetter mImageGetter;
    private boolean mDontClearCacheOnClear;

    public TimelineItemAdapter(Context context) {
        super(context);
        mImageGetter = new HttpImageGetter(context);
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
                holder = new CommentViewHolder(view, mImageGetter);
                break;
            case VIEW_TYPE_EVENT:
                view = inflater.inflate(R.layout.row_timeline_event, parent, false);
                holder = new EventViewHolder(view);
                break;
//            case VIEW_TYPE_COMMIT_COMMENT:
//                view = inflater.inflate(R.layout.row_timeline_commit_comment, parent, false);
//                holder = new CommitCommentViewHolder(view, mImageGetter);
//                break;
            case VIEW_TYPE_REVIEW:
                view = inflater.inflate(R.layout.row_timeline_review, parent, false);
                holder = new ReviewViewHolder(view);
                break;
            case VIEW_TYPE_DIFF:
                view = inflater.inflate(R.layout.row_timeline_diff, parent, false);
                holder = new DiffViewHolder(view);
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
//        if (item instanceof TimelineItem.TimelineCommitComment) {
//            return VIEW_TYPE_COMMIT_COMMENT;
//        }
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
//            case VIEW_TYPE_COMMIT_COMMENT:
//                ((CommitCommentViewHolder) holder).bind((TimelineItem.TimelineCommitComment) item);
//                break;
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
