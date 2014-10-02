package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.Repository;

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositorySearchLoader;
import com.gh4a.utils.IntentUtils;

public class RepositorySearchFragment extends ListDataBaseFragment<Repository> {
    private RepositorySearchLoader mLoader;

    public static RepositorySearchFragment newInstance(String userLogin) {
        RepositorySearchFragment f = new RepositorySearchFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, userLogin);
        f.setArguments(args);

        return f;
    }

    public void setQuery(String query) {
        mLoader.setQuery(query);
        getArguments().putString("query", query);
        refresh();
    }

    @Override
    public Loader<LoaderResult<List<Repository>>> onCreateLoader(int id, Bundle args) {
        String login = getArguments().getString(Constants.User.LOGIN);
        mLoader = new RepositorySearchLoader(getActivity(), login);
        mLoader.setQuery(getArguments().getString("query"));
        return mLoader;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_search_repos_found;
    }

    @Override
    protected RootAdapter<Repository> onCreateAdapter() {
        return new RepositoryAdapter(getActivity());
    }

    @Override
    protected void onItemClick(Repository item) {
        startActivity(IntentUtils.getRepoActivityIntent(getActivity(),
                item.getOwner().getLogin(), item.getName(), null));
    }
}
