package com.gh4a.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gh4a.R;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;

import org.eclipse.egit.github.core.NotificationSubject;
import org.eclipse.egit.github.core.Repository;

public class NotificationAdapter extends
        RootAdapter<NotificationHolder, NotificationAdapter.ViewHolder> {
    private static final int VIEW_TYPE_NOTIFICATION_HEADER = 3;
    private static final String SUBJECT_ISSUE = "Issue";
    private static final String SUBJECT_PULL_REQUEST = "PullRequest";

    private final int mTopBottomMargin;
    private final int mBottomPadding;
    private final Context mContext;

    public NotificationAdapter(Context context) {
        super(context);
        mContext = context;
        Resources resources = context.getResources();
        mTopBottomMargin = resources.getDimensionPixelSize(R.dimen.card_top_bottom_margin);
        mBottomPadding = resources.getDimensionPixelSize(R.dimen.notification_card_padding_bottom);
    }

    @Override
    protected ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
            int viewType) {
        int layoutResId = viewType == VIEW_TYPE_NOTIFICATION_HEADER
                ? R.layout.row_notification_header
                : R.layout.row_notification;
        View v = inflater.inflate(layoutResId, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivDone.setOnClickListener(this);
        return holder;
    }

    @Override
    protected int getItemViewType(NotificationHolder item) {
        if (item.notification == null) {
            return VIEW_TYPE_NOTIFICATION_HEADER;
        }
        return super.getItemViewType(item);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, NotificationHolder item) {
        if (item.notification == null) {
            Repository repository = item.repository;
            holder.tvTitle.setText(repository.getOwner().getLogin() + "/" + repository.getName());
            return;
        }

        NotificationSubject subject = item.notification.getSubject();

        if (SUBJECT_ISSUE.equals(subject.getType())) {
            holder.ivIcon.setImageResource(UiUtils.resolveDrawable(mContext, R.attr.issueIcon));
            holder.ivIcon.setVisibility(View.VISIBLE);
        } else if (SUBJECT_PULL_REQUEST.equals(subject.getType())) {
            holder.ivIcon.setImageResource(UiUtils.resolveDrawable(mContext, R.attr.pullRequestIcon));
            holder.ivIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivIcon.setVisibility(View.GONE);
        }

        holder.tvTitle.setText(subject.getTitle());

        int bottomPadding = item.isLastRepositoryNotification() ? mBottomPadding : 0;
        holder.cvCard.setContentPadding(0, 0, 0, bottomPadding);

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) holder.cvCard.getLayoutParams();
        int bottomMargin = item.isLastRepositoryNotification() ? mTopBottomMargin : 0;
        layoutParams.setMargins(0, 0, 0, bottomMargin);
        holder.cvCard.setLayoutParams(layoutParams);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.iv_done) {
            return;
        }

        super.onClick(view);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
            ivDone = (ImageView) view.findViewById(R.id.iv_done);
            ivIcon = (ImageView) view.findViewById(R.id.iv_icon);
            tvTitle = (StyleableTextView) view.findViewById(R.id.tv_title);
            cvCard = (CardView) view.findViewById(R.id.cv_card);
        }

        final ImageView ivIcon;
        final ImageView ivDone;
        final StyleableTextView tvTitle;
        final CardView cvCard;
    }
}
