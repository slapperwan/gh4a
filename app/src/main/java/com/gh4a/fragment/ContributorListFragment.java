package com.gh4a.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.ContributorAdapter;
import com.gh4a.adapter.RootAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

import io.reactivex.Single;
import retrofit2.Response;

public class ContributorListFragment extends PagedDataBaseFragment<User> implements
        RootAdapter.OnItemClickListener<User> {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";

    public static ContributorListFragment newInstance(String repoOwner, String repoName) {
        ContributorListFragment f = new ContributorListFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        f.setArguments(args);

        return f;
    }

    @Override
    protected Single<Response<Page<User>>> loadPage(int page, boolean bypassCache) {
        String repoOwner = getArguments().getString(EXTRA_OWNER);
        String repoName = getArguments().getString(EXTRA_REPO);
        RepositoryService service = ServiceFactory.get(RepositoryService.class, bypassCache);
        return service.getContributors(repoOwner, repoName, page);
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
