package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.adapter.ReleaseAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.ReleaseListLoader;
import com.meisolsson.githubsdk.model.Release;

import java.util.List;

public class ReleaseListFragment extends ListDataBaseFragment<Release> implements
        RootAdapter.OnItemClickListener<Release> {
    private String mUserLogin;
    private String mRepoName;

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
    protected Loader<LoaderResult<List<Release>>> onCreateLoader() {
        return new ReleaseListLoader(getActivity(), mUserLogin, mRepoName);
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
