package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.loader.CommitCommentListLoader;

public class CommitNoteFragment extends BaseFragment
    implements LoaderManager.LoaderCallbacks<List<CommitComment>>{

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private ListView mListView;
    private CommitNoteAdapter mAdapter;
    
    public static CommitNoteFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitNoteFragment f = new CommitNoteFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString(Constants.Object.OBJECT_SHA, objectSha);
        f.setArguments(args);
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        mObjectSha = getArguments().getString(Constants.Object.OBJECT_SHA);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setClickable(false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mAdapter = new CommitNoteAdapter(getSherlockActivity(), new ArrayList<CommitComment>());
        mListView.setAdapter(mAdapter);
        
        showLoading();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<CommitComment> comments) {
        if (comments != null && !comments.isEmpty()) {
            mAdapter.clear();
            mAdapter.addAll(comments);
            mAdapter.notifyDataSetChanged();
        }
        else {
            Toast.makeText(getSherlockActivity(), "Notes not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<List<CommitComment>> onCreateLoader(int id, Bundle args) {
        return new CommitCommentListLoader(getSherlockActivity(), mRepoOwner, mRepoName, mObjectSha);
    }

    @Override
    public void onLoadFinished(Loader<List<CommitComment>> loader,
            List<CommitComment> comments) {
        hideLoading();
        fillData(comments);
    }

    @Override
    public void onLoaderReset(Loader<List<CommitComment>> loader) {
        // TODO Auto-generated method stub
        
    }
}
