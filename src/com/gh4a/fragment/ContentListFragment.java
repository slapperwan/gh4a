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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;

import android.app.Activity;
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
import com.gh4a.adapter.FileAdapter;
import com.gh4a.loader.ContentListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

public class ContentListFragment extends BaseFragment implements OnItemClickListener {

    private Repository mRepository;
    public String mPath;
    public String mRef;
    private ListView mListView;
    public FileAdapter mAdapter;
    private ParentCallback mCallback;
    private boolean mDataLoaded;

    public interface ParentCallback {
        public void onModuleMapFound(ContentListFragment fragment);
        public void onTreeSelected(ContentListFragment fragment, RepositoryContents content, String ref);
    }

    private LoaderCallbacks<List<RepositoryContents>> mContentsListCallback =
            new LoaderCallbacks<List<RepositoryContents>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader(int id, Bundle args) {
            return new ContentListLoader(getSherlockActivity(), mRepository.getOwner().getLogin(),
                    mRepository.getName(), mPath, mRef);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryContents>> result) {
            hideLoading();
            mDataLoaded = true;
            if (!result.handleError(getActivity())) {
                fillData(result.getData());
                for (RepositoryContents content : result.getData()) {
                    if (RepositoryContents.TYPE_FILE.equals(content.getType())) {
                        if (content.getName().equals(".gitmodules")) {
                            mCallback.onModuleMapFound(ContentListFragment.this);
                            break;
                        }
                    }
                }
            }
        }
    };

    public static ContentListFragment newInstance(Repository repository,
            String path, ArrayList<RepositoryContents> contents, String ref) {
        ContentListFragment f = new ContentListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Object.PATH, path);
        args.putString(Constants.Object.REF, ref);
        args.putSerializable("REPOSITORY", repository);
        args.putSerializable("CONTENTS", contents);
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
            mCallback = (ParentCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTreeSelectedListener");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mAdapter == null) {
            mAdapter = new FileAdapter(getSherlockActivity());
            @SuppressWarnings("unchecked")
            ArrayList<RepositoryContents> contents =
                    (ArrayList<RepositoryContents>) getArguments().getSerializable("CONTENTS");
            if (contents != null) {
                mAdapter.addAll(contents);
                mDataLoaded = true;
            }
        }
        mListView.setAdapter(mAdapter);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!mDataLoaded) {
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, mContentsListCallback);
            }
            else {
                getLoaderManager().restartLoader(0, null, mContentsListCallback);
            }
            getLoaderManager().getLoader(0).forceLoad();
        }
        else {
            hideLoading();
        }
    }
    
    private void fillData(List<RepositoryContents> entries) {
        if (entries != null && entries.size() > 0) {
            mAdapter.clear();
            mAdapter.addAll(entries);
        }
        mAdapter.notifyDataSetChanged();
    }

    public String getPath() {
        return mPath;
    }

    public List<RepositoryContents> getContents() {
        if (mAdapter == null) {
            return new ArrayList<RepositoryContents>();
        }
        return mAdapter.getObjects();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        RepositoryContents content = (RepositoryContents) adapterView.getAdapter().getItem(position);
        mCallback.onTreeSelected(this, content, mRef);
    }
}