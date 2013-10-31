package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryBranch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
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
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
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
        AlertDialog.Builder builder = UiUtils.createDialogBuilder(getActivity());
        builder.setTitle(R.string.download_file_title);
        builder.setMessage(getString(R.string.download_file_message, branch.getName()));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String url = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/zipball/" + branch.getName();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
}