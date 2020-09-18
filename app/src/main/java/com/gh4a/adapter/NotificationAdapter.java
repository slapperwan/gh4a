package com.gh4a.adapter;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.model.NotificationHolder;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.NotificationSubject;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;

public class NotificationAdapter extends
        RootAdapter<NotificationHolder, NotificationAdapter.ViewHolder> {
    private static final int VIEW_TYPE_NOTIFICATION_HEADER = RootAdapter.CUSTOM_VIEW_TYPE_START + 1;
    public static final String SUBJECT_ISSUE = "Issue";
    public static final String SUBJECT_PULL_REQUEST = "PullRequest";
    public static final String SUBJECT_COMMIT = "Commit";
    public static final String SUBJECT_RELEASE = "Release";

    public interface OnNotificationActionCallback {
        void markAsRead(NotificationHolder notificationHolder);

        void unsubscribe(NotificationHolder notificationHolder);
    }

    private final int mBottomMargin;
    private final Context mContext;
    private final OnNotificationActionCallback mActionCallback;

    public NotificationAdapter(Context context, OnNotificationActionCallback actionCallback) {
        super(context);
        mContext = context;
        mActionCallback = actionCallback;

        mBottomMargin = context.getResources().getDimensionPixelSize(R.dimen.card_margin);
    }

    public boolean hasUnreadNotifications() {
        for (int i = 0; i < getCount(); i++) {
            NotificationHolder item = getItem(i);

            if (item.notification != null && !item.isRead()) {
                return true;
            }
        }

        return false;
    }

    public boolean markAsRead(@Nullable Repository repository,
            @Nullable NotificationThread notification) {
        NotificationHolder previousRepoItem = null;
        int unreadNotificationsInSameRepoCount = 0;
        boolean hasReadEverything = true;

        boolean isMarkingSingleNotification = repository == null && notification != null;

        for (int i = 0; i < getCount(); i++) {
            NotificationHolder item = getItem(i);

            // Passing both repository and notification as null will mark everything as read
            if ((repository == null && notification == null)
                    || (repository != null && item.repository.equals(repository))
                    || (item.notification != null && item.notification.equals(notification))) {
                item.setIsRead(true);
            }

            // When marking single notification as read also mark the repository if it contained
            // only 1 unread notification
            if (isMarkingSingleNotification) {
                if (item.notification == null) {
                    if (previousRepoItem != null && unreadNotificationsInSameRepoCount == 0
                            && previousRepoItem.repository.equals(notification.repository())) {
                        previousRepoItem.setIsRead(true);
                    }
                    previousRepoItem = item;
                    unreadNotificationsInSameRepoCount = 0;
                } else if (!item.isRead()) {
                    unreadNotificationsInSameRepoCount += 1;
                }
            }

            if (item.notification != null && !item.isRead()) {
                hasReadEverything = false;
            }
        }

        // Additional check for the very last notification
        if (isMarkingSingleNotification && previousRepoItem != null
                && unreadNotificationsInSameRepoCount == 0
                && previousRepoItem.repository.equals(notification.repository())) {
            previousRepoItem.setIsRead(true);
        }

        notifyDataSetChanged();
        return hasReadEverything;
    }

    @Override
    protected ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
            int viewType) {
        int layoutResId = viewType == VIEW_TYPE_NOTIFICATION_HEADER
                ? R.layout.row_notification_header
                : R.layout.row_notification;
        View v = inflater.inflate(layoutResId, parent, false);
        return new ViewHolder(v, mActionCallback);
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
        holder.ivAction.setTag(item);

        float alpha = item.isRead() ? 0.5f : 1f;
        holder.tvTitle.setAlpha(alpha);

        if (item.notification == null) {
            holder.ivAction.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
            holder.tvTitle.setText(ApiHelpers.formatRepoName(mContext, item.repository));

            User owner = item.repository.owner();
            AvatarHandler.assignAvatar(holder.ivAvatar, owner);
            holder.ivAvatar.setTag(owner);
            holder.ivAvatar.setAlpha(alpha);
            return;
        }

        holder.ivIcon.setAlpha(alpha);
        holder.tvTimestamp.setAlpha(alpha);
        holder.mPopupMenu.getMenu().findItem(R.id.mark_as_read).setVisible(!item.isRead());

        NotificationSubject subject = item.notification.subject();
        int iconResId = getIconResId(subject.type());
        if (iconResId > 0) {
            holder.ivIcon.setImageResource(iconResId);
            holder.ivIcon.setVisibility(View.VISIBLE);
        } else {
            holder.ivIcon.setVisibility(View.INVISIBLE);
        }

        holder.tvTitle.setText(subject.title());
        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                item.notification.updatedAt(), true));

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) holder.vNotificationContent.getLayoutParams();
        int bottomMargin = item.isLastRepositoryNotification() ? mBottomMargin : 0;
        layoutParams.setMargins(0, 0, 0, bottomMargin);
        holder.vNotificationContent.setLayoutParams(layoutParams);

        holder.vBottomShadow.setVisibility(
                item.isLastRepositoryNotification() ? View.VISIBLE : View.GONE);
    }

    private int getIconResId(String subjectType) {
        if (SUBJECT_ISSUE.equals(subjectType)) {
            return R.drawable.issue;
        } else if (SUBJECT_PULL_REQUEST.equals(subjectType)) {
            return R.drawable.pull_request;
        } else if (SUBJECT_COMMIT.equals(subjectType)) {
            return R.drawable.commit;
        } else if (SUBJECT_RELEASE.equals(subjectType)) {
            return R.drawable.release;
        } else {
            return 0;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
            PopupMenu.OnMenuItemClickListener {
        public ViewHolder(View view, OnNotificationActionCallback actionCallback) {
            super(view);
            mActionCallback = actionCallback;

            ivAction = view.findViewById(R.id.iv_action);
            ivAction.setOnClickListener(this);
            ivIcon = view.findViewById(R.id.iv_icon);
            tvTitle = view.findViewById(R.id.tv_title);
            tvTimestamp = view.findViewById(R.id.tv_timestamp);
            vNotificationContent = view.findViewById(R.id.v_notification_content);
            vBottomShadow = view.findViewById(R.id.v_bottom_shadow);
            ivAvatar = view.findViewById(R.id.iv_avatar);
            if (ivAvatar != null) {
                ivAvatar.setOnClickListener(this);
            }

            mPopupMenu = new PopupMenu(view.getContext(), ivAction);
            mPopupMenu.getMenuInflater().inflate(R.menu.notification_menu, mPopupMenu.getMenu());
            mPopupMenu.setOnMenuItemClickListener(this);
        }

        private final ImageView ivIcon;
        private final ImageView ivAction;
        private final ImageView ivAvatar;
        private final TextView tvTitle;
        private final TextView tvTimestamp;
        private final View vNotificationContent;
        private final View vBottomShadow;
        private final PopupMenu mPopupMenu;
        private final OnNotificationActionCallback mActionCallback;

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_action: {
                    NotificationHolder notificationHolder = (NotificationHolder) v.getTag();

                    if (notificationHolder.notification == null) {
                        mActionCallback.markAsRead(notificationHolder);
                    } else {
                        mPopupMenu.show();
                    }
                    break;
                }
                case R.id.iv_avatar: {
                    User user = (User) v.getTag();
                    Intent intent = UserActivity.makeIntent(v.getContext(), user);
                    if (intent != null) {
                        v.getContext().startActivity(intent);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            NotificationHolder notificationHolder = (NotificationHolder) ivAction.getTag();

            switch (item.getItemId()) {
                case R.id.mark_as_read:
                    mActionCallback.markAsRead(notificationHolder);
                    return true;
                case R.id.unsubscribe:
                    mActionCallback.unsubscribe(notificationHolder);
                    return true;
            }

            return false;
        }
    }
}
