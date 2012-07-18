/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.Repository;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.FileAdapter;
import com.gh4a.loader.ContentListLoader;
import com.gh4a.utils.StringUtils;

public class ContentListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Content>>, OnItemClickListener {

    private Repository mRepository;
    public String mPath;
    public String mRef;
    private ListView mListView;
    public FileAdapter mAdapter;
    private OnTreeSelectedListener mCallback;
    private List<Content> mContents;

    public static ContentListFragment newInstance(Repository repository,
            String path, String ref) {
        ContentListFragment f = new ContentListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Object.PATH, path);
        args.putString(Constants.Object.REF, ref);
        args.putSerializable("REPOSITORY", repository);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mRepository = (Repository) getArguments().getSerializable("REPOSITORY");
        mPath = getArguments().getString(Constants.Object.PATH);
        mRef = getArguments().getString(Constants.Object.REF);
        if (StringUtils.isBlank(mRef)) {
            mRef = mRepository.getMasterBranch();
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.generic_list, container, false);
        mListView = (ListView) v.findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);
        return v;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (OnTreeSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTreeSelectedListener");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (mContents == null) {
            mContents = new ArrayList<Content>();
            mAdapter = new FileAdapter(getSherlockActivity(), mContents);
            mListView.setAdapter(mAdapter);
            
            getLoaderManager().initLoader(0, null, this);
            getLoaderManager().getLoader(0).forceLoad();
        }
        else {
            mAdapter = new FileAdapter(getSherlockActivity(), mContents);
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
        }
    }
    
    public void setTreeEntryList(List<Content> contents) {
        mContents = contents;
    }
    
    private void fillData(List<Content> entries) {
        if (entries != null && entries.size() > 0) {
            mAdapter.addAll(entries);
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader<List<Content>> onCreateLoader(int id, Bundle args) {
        return new ContentListLoader(getSherlockActivity(), mRepository.getOwner().getLogin(),
                mRepository.getName(), mPath, mRef);
    }

    @Override
    public void onLoadFinished(Loader<List<Content>> loader, List<Content> contents) {
        hideLoading();
        fillData(contents);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        // TODO Auto-generated method stub
    }
    
    public interface OnTreeSelectedListener {
        public void onTreeSelected(int position, 
                AdapterView<?> adapterView,
                Content content,
                List<Content> contents,
                String ref);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Content content = (Content) adapterView.getAdapter().getItem(position);
        mCallback.onTreeSelected(position, adapterView, content, mContents, mRef);
    }

}