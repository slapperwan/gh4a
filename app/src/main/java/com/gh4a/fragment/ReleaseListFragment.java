package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.adapter.ReleaseAdapter;
import com.gh4a.adapter.RootAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import io.reactivex.Single;
import retrofit2.Response;

public class ReleaseListFragment extends PagedDataBaseFragment<Release> implements
        RootAdapter.OnItemClickListener<Release> {
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";

    private String mUserLogin;
    private String mRepoName;

    public static ReleaseListFragment newInstance(String owner, String repo) {
        ReleaseListFragment f = new ReleaseListFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, owner);
        args.putString(EXTRA_REPO, repo);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString(EXTRA_OWNER);
        mRepoName = getArguments().getString(EXTRA_REPO);
    }

    @Override
    protected Single<Response<Page<Release>>> loadPage(int page, boolean bypassCache) {
        final RepositoryReleaseService service =
                ServiceFactory.get(RepositoryReleaseService.class, bypassCache);
        return service.getReleases(mUserLogin, mRepoName, page);
    }

    @Override
    protected RootAdapter<Release, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        ReleaseAdapter adapter = new ReleaseAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_releases_found;
    }

    @Override
    public void onItemClick(Release release) {
        startActivity(ReleaseInfoActivity.makeIntent(getActivity(), mUserLogin, mRepoName, release));
    }
}
