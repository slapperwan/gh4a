package com.gh4a.fragment;

import java.util.List;

import org.eclipse.egit.github.core.RepositoryBranch;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.adapter.SimpleStringAdapter;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;

public class DownloadBranchesFragment extends BaseFragment implements OnItemClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private ListView mListView;
    private List<RepositoryBranch> mBranches;
    private SimpleStringAdapter mAdapter;
    
    private LoaderCallbacks<List<RepositoryBranch>> mBranchCallback =
            new LoaderCallbacks<List<RepositoryBranch>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryBranch>>> onCreateLoader(int id, Bundle args) {
            return new BranchListLoader(getSherlockActivity(), mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryBranch>> result) {
            hideLoading();
            if (!((BaseSherlockFragmentActivity) getSherlockActivity()).isLoaderError(result)) {
                fillData(result.getData());
            }
        }
    };

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new SimpleStringAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        showLoading();
        
        getLoaderManager().initLoader(0, null, mBranchCallback);
        getLoaderManager().getLoader(0).forceLoad();
    }

    public void fillData(List<RepositoryBranch> branches) {
        mBranches = branches;
        for (RepositoryBranch branch : branches) {
            mAdapter.add(branch.getName());
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
        final RepositoryBranch branch = mBranches.get(position);
        BaseSherlockFragmentActivity activity = (BaseSherlockFragmentActivity) getSherlockActivity();
        AlertDialog.Builder builder = activity.createDialogBuilder();
        builder.setTitle(R.string.download_file_title);
        builder.setMessage(getString(R.string.download_file_message, branch.getName()));
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
