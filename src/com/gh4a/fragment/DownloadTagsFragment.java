package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryTag;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.R;
import com.gh4a.adapter.TagAdapter;
import com.gh4a.loader.TagListLoader;

public class DownloadTagsFragment extends BaseFragment implements OnItemClickListener,
    LoaderManager.LoaderCallbacks<HashMap<Integer, Object>> {

    private String mRepoOwner;
    private String mRepoName;
    private ListView mListView;
    private TagAdapter mAdapter;
    
    public static DownloadTagsFragment newInstance(String repoOwner, String repoName) {
        DownloadTagsFragment f = new DownloadTagsFragment();
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
        
        mAdapter = new TagAdapter(getSherlockActivity(), new ArrayList<RepositoryTag>());
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        
        showLoading();
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }

    public void fillData(List<RepositoryTag> tags) {
        mAdapter.addAll(tags);            
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryTag tag = (RepositoryTag) mAdapter.getItem(position);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(tag.getZipballUrl()));
        startActivity(browserIntent);
    }

    @Override
    public Loader<HashMap<Integer, Object>> onCreateLoader(int id, Bundle args) {
        return new TagListLoader(getSherlockActivity(), mRepoOwner, mRepoName);
    }

    @Override
    public void onLoadFinished(Loader<HashMap<Integer, Object>> loader,
            HashMap<Integer, Object> object) {
        hideLoading();
        HashMap<Integer, Object> result = (HashMap<Integer, Object>) object;
        
        if (!((BaseSherlockFragmentActivity) getSherlockActivity()).isLoaderError(result)) {
            fillData((List<RepositoryTag>) result.get(LoaderResult.DATA));
        }
    }

    @Override
    public void onLoaderReset(Loader<HashMap<Integer, Object>> arg0) {
        // TODO Auto-generated method stub
        
    }
}
