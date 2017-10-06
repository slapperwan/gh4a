package com.gh4a.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.BackgroundTask;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.job.NotificationsJob;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoadResult;
import com.gh4a.loader.NotificationListLoader;
import com.gh4a.resolver.BrowseFilter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.NotificationSubject;
import com.meisolsson.githubsdk.model.NotificationThread;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.request.NotificationReadRequest;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.NotificationService;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class NotificationListFragment extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<NotificationHolder>,NotificationAdapter.OnNotificationActionCallback {
    public static final String EXTRA_INITIAL_REPO_OWNER = "initial_notification_repo_owner";
    public static final String EXTRA_INITIAL_REPO_NAME = "initial_notification_repo_name";

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    private final LoaderCallbacks<NotificationListLoadResult> mNotificationsCallback =
            new LoaderCallbacks<NotificationListLoadResult>(this) {
        @Override
        protected Loader<LoaderResult<NotificationListLoadResult>> onCreateLoader() {
            return new NotificationListLoader(getContext(), mAll, mParticipating);
        }

        @Override
        protected void onResultReady(NotificationListLoadResult result) {
            mNotificationsLoadTime = result.loadTime;
            mAdapter.clear();
            mAdapter.addAll(result.notifications);
            setContentShown(true);
            mAdapter.notifyDataSetChanged();
            updateEmptyState();
            updateMenuItemVisibility();
            if (!mAll && !mParticipating) {
                mCallback.setNotificationsIndicatorVisible(!result.notifications.isEmpty());
            }

            scrollToInitialNotification(result.notifications);
        }
    };

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentShown(false);
        getLoaderManager().initLoader(0, null, mNotificationsCallback);
        NotificationsJob.markNotificationsAsSeen(getActivity());
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
        hideContentAndRestartLoaders(0);
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
        final Intent intent;

        if (item.notification == null) {
            intent = RepositoryActivity.makeIntent(getActivity(), item.repository);
        } else {
            new MarkReadTask(null, item.notification).schedule();

            NotificationSubject subject = item.notification.subject();
            String url = subject.url();
            if (url != null) {
                Uri uri = ApiHelpers.normalizeUri(Uri.parse(url));
                intent = BrowseFilter.makeRedirectionIntent(getActivity(), uri,
                        new IntentUtils.InitialCommentMarker(item.notification.lastReadAt()));
            } else {
                intent = null;
            }
        }

        if (intent != null) {
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
                new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.mark_all_as_read_question)
                        .setPositiveButton(R.string.mark_all_as_read,
                                new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new MarkReadTask(null, null).schedule();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case R.id.notification_filter_unread:
            case R.id.notification_filter_all:
            case R.id.notification_filter_participating:
                mAll = itemId == R.id.notification_filter_all;
                mParticipating = itemId == R.id.notification_filter_participating;
                item.setChecked(true);
                reloadNotification();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadNotification() {
        if (mAdapter != null) {
            mAdapter.clear();
        }
        setContentShown(false);
        updateMenuItemVisibility();

        getLoaderManager().destroyLoader(0);
        getLoaderManager().initLoader(0, null, mNotificationsCallback);
    }

    @Override
    public void markAsRead(NotificationHolder notificationHolder) {
        if (notificationHolder.notification == null) {
            final Repository repository = notificationHolder.repository;

            String login = ApiHelpers.getUserLogin(getActivity(), repository.owner());
            String title = getString(R.string.mark_repository_as_read_question,
                    login + "/" + repository.name());

            new AlertDialog.Builder(getActivity())
                    .setMessage(title)
                    .setPositiveButton(R.string.mark_as_read,
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new MarkReadTask(repository, null).schedule();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            new MarkReadTask(null, notificationHolder.notification).schedule();
        }
    }

    @Override
    public void unsubscribe(NotificationHolder notificationHolder) {
        new UnsubscribeTask(notificationHolder.notification).schedule();
    }

    private void updateMenuItemVisibility() {
        if (mMarkAllAsReadMenuItem == null) {
            return;
        }

        mMarkAllAsReadMenuItem.setVisible(isContentShown() && mAdapter.hasUnreadNotifications());
    }

    private void markAsRead(Repository repository, NotificationThread notification) {
        if (mAdapter.markAsRead(repository, notification)) {
            if (!mAll && !mParticipating) {
                mCallback.setNotificationsIndicatorVisible(false);
            }
        }
        updateMenuItemVisibility();
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

    private class MarkReadTask extends BackgroundTask<Void> {
        @Nullable
        private final Repository mRepository;
        @Nullable
        private final NotificationThread mNotification;

        public MarkReadTask(@Nullable Repository repository, @Nullable NotificationThread notification) {
            super(getActivity());
            mRepository = repository;
            mNotification = notification;
        }

        @Override
        protected Void run() throws IOException {
            NotificationService service =
                    Gh4Application.get().getGitHubService(NotificationService.class);

            if (mNotification != null) {
                ApiHelpers.throwOnFailure(
                        service.markNotificationRead(mNotification.id()).blockingGet());
            } else if (mRepository != null) {
                ApiHelpers.throwOnFailure(service.markAllRepositoryNotificationsRead(
                        mRepository.owner().login(), mRepository.name(),
                        NotificationReadRequest.builder().lastReadAt(mNotificationsLoadTime).build()).blockingGet());
            } else {
                ApiHelpers.throwOnFailure(service.markAllNotificationsRead(
                        NotificationReadRequest.builder().lastReadAt(mNotificationsLoadTime).build()).blockingGet());
            }

            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            markAsRead(mRepository, mNotification);
        }
    }

    private class UnsubscribeTask extends BackgroundTask<Void> {
        private final NotificationThread mNotification;

        public UnsubscribeTask(NotificationThread notification) {
            super(getActivity());
            mNotification = notification;
        }

        @Override
        protected Void run() throws IOException {
            NotificationService service =
                    Gh4Application.get().getGitHubService(NotificationService.class);
            SubscriptionRequest request = SubscriptionRequest.builder()
                    .subscribed(false)
                    .ignored(true)
                    .build();

            ApiHelpers.throwOnFailure(
                    service.setNotificationThreadSubscription(mNotification.id(), request).blockingGet());
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            markAsRead(null, mNotification);
        }
    }
}
