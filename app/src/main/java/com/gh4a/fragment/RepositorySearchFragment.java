package com.gh4a.fragment;

import java.util.List;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositorySearchLoader;
import com.meisolsson.githubsdk.model.Repository;

public class RepositorySearchFragment extends ListDataBaseFragment<Repository> implements
        RootAdapter.OnItemClickListener<Repository> {
    private RepositorySearchLoader mLoader;

    public static RepositorySearchFragment newInstance(String userLogin) {
        RepositorySearchFragment f = new RepositorySearchFragment();

        Bundle args = new Bundle();
        args.putString("user", userLogin);
        f.setArguments(args);

        return f;
    }

    public void setQuery(String query) {
        if (mLoader != null) {
            mLoader.setQuery(query);
        }
        getArguments().putString("query", query);
        onRefresh();
    }

    @Override
    public Loader<LoaderResult<List<Repository>>> onCreateLoader() {
        String login = getArguments().getString("user");
        mLoader = new RepositorySearchLoader(getActivity(), login);
        mLoader.setQuery(getArguments().getString("query"));
        return mLoader;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_search_repos_found;
    }

    @Override
    protected RootAdapter<Repository, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        RepositoryAdapter adapter = new RepositoryAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    public void onItemClick(Repository item) {
        startActivity(RepositoryActivity.makeIntent(getActivity(), item));
    }
}
