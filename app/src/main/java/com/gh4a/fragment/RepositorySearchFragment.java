package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.RepositorySearchLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.search.SearchService;

import io.reactivex.Single;

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
    protected Single<List<Repository>> onCreateDataSingle() {
        String login = getArguments().getString("user");
        String query = getArguments().getString("query");

        if (TextUtils.isEmpty(query)) {
            return Single.just(new ArrayList<Repository>());
        }

        SearchService service = Gh4Application.get().getGitHubService(SearchService.class);
        String params = query + " fork:true user: " + login;

        return ApiHelpers.PageIterator
                .toSingle(page -> service.searchRepositories(params.toString(), null, null, page)
                        .compose(RxUtils::searchPageAdapter))
                // With that status code, Github wants to tell us there are no
                // repositories to search in. Just pretend no error and return
                // an empty list in that case.
                .compose(RxUtils.mapFailureToValue(422, new ArrayList<>()));
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
