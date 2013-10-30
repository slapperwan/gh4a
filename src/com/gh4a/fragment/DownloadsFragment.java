package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.Download;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.DownloadAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.DownloadsLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.UiUtils;

public class DownloadsFragment extends ListDataBaseFragment<Download> {
    private String mRepoOwner;
    private String mRepoName;

    public static DownloadsFragment newInstance(String repoOwner, String repoName) {
        DownloadsFragment f = new DownloadsFragment();
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
    protected RootAdapter<Download> onCreateAdapter() {
        return new DownloadAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_downloads_found;
    }

    @Override
    public void onItemClick(final Download download) {
        AlertDialog.Builder builder = UiUtils.createDialogBuilder(getActivity());
        builder.setTitle(R.string.download_file_title);
        builder.setMessage(getString(R.string.download_file_message, download.getName()));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(download.getHtmlUrl()));
                startActivity(browserIntent);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    public Loader<LoaderResult<List<Download>>> onCreateLoader(int id, Bundle args) {
        return new DownloadsLoader(getActivity(), mRepoOwner, mRepoName);
    }
}
