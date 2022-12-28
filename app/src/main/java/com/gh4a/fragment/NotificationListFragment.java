package com.gh4a.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.model.NotificationHolder;
import com.gh4a.resolver.BrowseFilter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.SingleFactory;
import com.gh4a.worker.NotificationsWorker;
import com.meisolsson.githubsdk.model.NotificationSubject;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.request.NotificationReadRequest;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Single;
import retrofit2.Response;

public class NotificationListFragment extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<NotificationHolder>,
        ConfirmationDialogFragment.Callback,
        NotificationAdapter.OnNotificationActionCallback {
    public static final String EXTRA_INITIAL_REPO_OWNER = "initial_notification_repo_owner";
    public static final String EXTRA_INITIAL_REPO_NAME = "initial_notification_repo_name";

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    private static final int ID_LOADER_NOTIFICATIONS = 0;

    private NotificationAdapter mAdapter;
    private Date mNotificationsLoadTime;
    private MenuItem mMarkAllAsReadMenuItem;
    private ParentCallback mCallback;
    private boolean mAll;
    private boolean mParticipating;

    public interface ParentCallback {
        void setNotificationsIndicatorVisible(boolean visible);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof ParentCallback)) {
            throw new IllegalStateException("context must implement ParentCallback");
        }

        mCallback = (ParentCallback) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        long lastCheck = NotificationsWorker.getLastCheckTimestamp(getActivity());
        long lastFetch = mNotificationsLoadTime != null ? mNotificationsLoadTime.getTime() : 0;
        if (lastFetch == 0 || (lastCheck != 0 && lastCheck > lastFetch)) {
            setContentShown(false);
            // If we know our last fetch is stale, force the reload to make to to not get
            // outdated notifications
            loadNotifications(lastFetch != 0);
            NotificationsWorker.markNotificationsAsSeen(getActivity());
        }
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_notifications_found;
    }

    @Override
    public void onRefresh() {
        if (mAdapter != null) {
            mAdapter.clear();
        }
        setContentShown(false);
        loadNotifications(true);
        updateMenuItemVisibility();
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = new NotificationAdapter(getActivity(), this);
        mAdapter.setOnItemClickListener(this);
        view.setAdapter(mAdapter);
        updateEmptyState();
    }

    @Override
    protected boolean hasDividers() {
        return false;
    }

    @Override
    protected boolean hasCards() {
        return true;
    }

    @Override
    public void onItemClick(NotificationHolder item) {
        if (item.notification == null) {
            var intent = RepositoryActivity.makeIntent(getActivity(), item.repository);
            startActivity(intent);
            return;
        }

        NotificationSubject subject = item.notification.subject();
        String url = subject.url();
        final Intent intent;
        if (url != null) {
            Uri uri = ApiHelpers.normalizeUri(Uri.parse(url));
            intent = BrowseFilter.makeRedirectionIntent(getActivity(), uri,
                    new IntentUtils.InitialCommentMarker(item.notification.updatedAt()));
        } else {
            intent = null;
        }

        if (intent != null) {
            markAsRead(null, item.notification);
            startActivity(intent);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notification_list_menu, menu);
        mMarkAllAsReadMenuItem = menu.findItem(R.id.mark_all_as_read);
        updateMenuItemVisibility();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.mark_all_as_read:
                ConfirmationDialogFragment.show(this, R.string.mark_all_as_read_question,
                        R.string.mark_all_as_read, null, "markallreadconfirm");
                return true;
            case R.id.notification_filter_unread:
            case R.id.notification_filter_all:
            case R.id.notification_filter_participating:
                mAll = itemId == R.id.notification_filter_all;
                mParticipating = itemId == R.id.notification_filter_participating;
                item.setChecked(true);
                onRefresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void markAsRead(NotificationHolder notificationHolder) {
        if (notificationHolder.notification == null) {
            final Repository repository = notificationHolder.repository;

            String title = getString(R.string.mark_repository_as_read_question,
                    ApiHelpers.formatRepoName(getActivity(), repository));

            ConfirmationDialogFragment.show(this, title,
                    R.string.mark_as_read, repository, "markreadconfirm");
        } else {
            markAsRead(null, notificationHolder.notification);
        }
    }

    @Override
    public void unsubscribe(NotificationHolder notificationHolder) {
        NotificationThread notification = notificationHolder.notification;
        NotificationService service = ServiceFactory.get(NotificationService.class, false);
        SubscriptionRequest request = SubscriptionRequest.builder()
                .ignored(true)
                .build();
        service.setNotificationThreadSubscription(notification.id(), request)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> Toast.makeText(getContext(), R.string.unsubscribe_success, Toast.LENGTH_SHORT).show(),
                        error -> handleActionFailure("Unsubscribing notification failed", error));
    }

    @Override
    public void onConfirmed(String tag, Parcelable data) {
        Repository repository = (Repository) data;
        markAsRead(repository, null);
    }

    private void updateMenuItemVisibility() {
        if (mMarkAllAsReadMenuItem == null) {
            return;
        }

        mMarkAllAsReadMenuItem.setVisible(isContentShown() && mAdapter.hasUnreadNotifications());
    }

    private void scrollToInitialNotification(List<NotificationHolder> notifications) {
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras == null) {
            return;
        }

        String repoOwner = extras.getString(EXTRA_INITIAL_REPO_OWNER);
        String repoName = extras.getString(EXTRA_INITIAL_REPO_NAME);
        extras.remove(EXTRA_INITIAL_REPO_OWNER);
        extras.remove(EXTRA_INITIAL_REPO_NAME);

        if (repoOwner == null || repoName == null) {
            return;
        }

        for (int i = 0; i < notifications.size(); i++) {
            NotificationHolder holder = notifications.get(i);
            if (holder.notification == null) {
                Repository repo = holder.repository;
                if (repoOwner.equals(repo.owner().login())
                        && repoName.equals(repo.name())) {
                    scrollToAndHighlightPosition(i);
                    break;
                }
            }
        }
    }

    private void markAsRead(Repository repository, NotificationThread notification) {
        NotificationService service = ServiceFactory.get(NotificationService.class, false);
        final Single<Response<Void>> responseSingle;
        if (notification != null) {
            if (!notification.unread()) {
                return;
            }
            responseSingle = service.markNotificationRead(notification.id());
        } else {
            NotificationReadRequest request = NotificationReadRequest.builder()
                    .lastReadAt(mNotificationsLoadTime)
                    .build();
            if (repository != null) {
                responseSingle = service.markAllRepositoryNotificationsRead(
                        repository.owner().login(), repository.name(), request);
            } else {
                responseSingle = service.markAllNotificationsRead(request);
            }
        }

        responseSingle
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> handleMarkAsRead(repository, notification),
                        error -> handleActionFailure("Mark notifications as read failed", error));
    }

    private void handleMarkAsRead(Repository repository, NotificationThread notification) {
        if (mAdapter.markAsRead(repository, notification)) {
            if (!mAll && !mParticipating) {
                mCallback.setNotificationsIndicatorVisible(false);
            }
        }
        updateMenuItemVisibility();
    }

    private void loadNotifications(boolean force) {
        SingleFactory.getNotifications(mAll, mParticipating, force)
                .compose(makeLoaderSingle(ID_LOADER_NOTIFICATIONS, force))
                .subscribe(result -> {
                    mNotificationsLoadTime = result.loadTime;
                    mAdapter.clear();
                    mAdapter.addAll(result.notifications);
                    mAdapter.notifyDataSetChanged();
                    setContentShown(true);
                    updateEmptyState();
                    updateMenuItemVisibility();
                    if (!mAll && !mParticipating) {
                        mCallback.setNotificationsIndicatorVisible(!result.notifications.isEmpty());
                    }
                    scrollToInitialNotification(result.notifications);
                }, this::handleLoadFailure);
    }
}