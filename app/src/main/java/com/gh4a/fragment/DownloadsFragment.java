package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.Download;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.DownloadAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.DownloadsLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public class DownloadsFragment extends ListDataBaseFragment<Download> implements
        RootAdapter.OnItemClickListener<Download> {
    private String mRepoOwner;
    private String mRepoName;

    public static DownloadsFragment newInstance(String repoOwner, String repoName) {
        DownloadsFragment f = new DownloadsFragment();
        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
    }

    @Override
    protected RootAdapter<Download, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        DownloadAdapter adapter = new DownloadAdapter(getActivity());
        adapter.setOnItemClickListener(this);
        return adapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_downloads_found;
    }

    @Override
    public void onItemClick(final Download download) {
        UiUtils.enqueueDownloadWithPermissionCheck((BaseActivity) getActivity(),
                download.getHtmlUrl(), download.getContentType(),
                download.getName(), download.getDescription(), null);
    }

    @Override
    public Loader<LoaderResult<List<Download>>> onCreateLoader() {
        return new DownloadsLoader(getActivity(), mRepoOwner, mRepoName);
    }
}
