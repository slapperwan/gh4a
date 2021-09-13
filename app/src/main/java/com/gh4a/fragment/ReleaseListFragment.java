package com.gh4a.fragment;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.adapter.ReleaseAdapter;
import com.gh4a.adapter.RootAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

import java.util.Collections;
import java.util.Comparator;

import io.reactivex.Single;
import retrofit2.Response;

import static java.util.Comparator.reverseOrder;
import static java.util.Comparator.nullsFirst;

public class ReleaseListFragment extends PagedDataBaseFragment<Release> implements
        RootAdapter.OnItemClickListener<Release> {
    private String mUserLogin;
    private String mRepoName;

    private static final Comparator<Release> MOST_RECENT_RELEASES_AND_DRAFTS_FIRST =
            Comparator.comparing(Release::publishedAt, nullsFirst(reverseOrder()));

    public static ReleaseListFragment newInstance(String owner, String repo) {
        ReleaseListFragment f = new ReleaseListFragment();
        Bundle args = new Bundle();
        args.putString("owner", owner);
        args.putString("repo", repo);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
    }

    @Override
    protected Single<Response<Page<Release>>> loadPage(int page, boolean bypassCache) {
        final RepositoryReleaseService service = ServiceFactory.get(RepositoryReleaseService.class, bypassCache);
        return service.getReleases(mUserLogin, mRepoName, page)
                // Sometimes the API returns releases in a slightly wrong order (see TeamNewPipe/NewPipe repo
                // for an example), so we need to fix the sorting locally
                .map(response -> {
                    if (response.body() != null) {
                        Collections.sort(response.body().items(), MOST_RECENT_RELEASES_AND_DRAFTS_FIRST);
                    }
                    return response;
                });
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
