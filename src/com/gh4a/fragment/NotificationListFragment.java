package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.gh4a.R;
import com.gh4a.adapter.NotificationAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.NotificationHolder;
import com.gh4a.loader.NotificationListLoader;

import java.util.List;

public class NotificationListFragment extends LoadingListFragmentBase implements
        RootAdapter.OnItemClickListener<NotificationHolder> {
    private RootAdapter<NotificationHolder, ? extends RecyclerView.ViewHolder> mAdapter;

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    private final LoaderCallbacks<List<NotificationHolder>> mNotificationsCallback =
            new LoaderCallbacks<List<NotificationHolder>>(this) {
        @Override
        protected Loader<LoaderResult<List<NotificationHolder>>> onCreateLoader() {
            return new NotificationListLoader(getContext());
        }

        @Override
        protected void onResultReady(List<NotificationHolder> result) {
            mAdapter.clear();
            mAdapter.addAll(result);
            setContentShown(true);
            mAdapter.notifyDataSetChanged();
            updateEmptyState();
        }
    };

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
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = new NotificationAdapter(getActivity());
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
        Toast.makeText(getActivity(), "Item clicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notification_list_menu, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mark_all_as_read) {
            Toast.makeText(getActivity(), R.string.mark_all_as_read, Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
