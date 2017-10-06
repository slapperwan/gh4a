package com.gh4a.fragment;

import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.ContributorAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.ContributorListLoader;
import com.gh4a.loader.LoaderResult;
import com.meisolsson.githubsdk.model.User;

public class ContributorListFragment extends ListDataBaseFragment<User> implements
        RootAdapter.OnItemClickListener<User> {
    public static ContributorListFragment newInstance(String repoOwner, String repoName) {
        ContributorListFragment f = new ContributorListFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        f.setArguments(args);

        return f;
    }

    @Override
    public Loader<LoaderResult<List<User>>> onCreateLoader() {
        String repoOwner = getArguments().getString("owner");
        String repoName = getArguments().getString("repo");
        return new ContributorListLoader(getActivity(), repoOwner, repoName);
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_contributors_found;
    }

    @Override
    protected RootAdapter<User, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        ContributorAdapter adapter = new ContributorAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    public void onItemClick(User item) {
        Intent intent = UserActivity.makeIntent(getActivity(), item);
        if (intent != null) {
            startActivity(intent);
        }
    }
}
