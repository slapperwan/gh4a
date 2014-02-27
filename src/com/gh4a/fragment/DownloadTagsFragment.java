package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryTag;

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.TagListLoader;
import com.gh4a.utils.UiUtils;

public class DownloadTagsFragment extends ListDataBaseFragment<RepositoryTag> {
    private String mRepoOwner;
    private String mRepoName;

    public static DownloadTagsFragment newInstance(String repoOwner, String repoName) {
        DownloadTagsFragment f = new DownloadTagsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
    }

    @Override
    protected RootAdapter<RepositoryTag> onCreateAdapter() {
        return new SimpleStringAdapter<RepositoryTag>(getActivity()) {
            @Override
            protected String objectToString(RepositoryTag tag) {
                return tag.getName();
            }
        };
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_tags_found;
    }

    @Override
    public Loader<LoaderResult<List<RepositoryTag>>> onCreateLoader(int id, Bundle args) {
        return new TagListLoader(getActivity(), mRepoOwner, mRepoName);
    }

    @Override
    public void onItemClick(final RepositoryTag tag) {
        UiUtils.enqueueDownload(getActivity(), tag.getZipballUrl(), "application/zip",
                mRepoName + "-" + tag.getName() + ".zip", null, null);
    }
}