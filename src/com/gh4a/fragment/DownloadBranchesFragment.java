package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryBranch;

import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public class DownloadBranchesFragment extends ListDataBaseFragment<RepositoryBranch> {
    private String mRepoOwner;
    private String mRepoName;

    public static DownloadBranchesFragment newInstance(String repoOwner, String repoName) {
        DownloadBranchesFragment f = new DownloadBranchesFragment();
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
    protected RootAdapter<RepositoryBranch> onCreateAdapter() {
        return new SimpleStringAdapter<RepositoryBranch>(getActivity()) {
            @Override
            protected String objectToString(RepositoryBranch branch) {
                return branch.getName();
            }
        };
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_branches_found;
    }

    @Override
    public Loader<LoaderResult<List<RepositoryBranch>>> onCreateLoader(int id, Bundle args) {
        return new BranchListLoader(getActivity(), mRepoOwner, mRepoName);
    }

    @Override
    public void onItemClick(final RepositoryBranch branch) {
        String url = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/zipball/" + branch.getName();
        UiUtils.enqueueDownload(getActivity(), url, "application/zip",
                mRepoName + "-" + branch.getName() + ".zip", null, null);
    }
}