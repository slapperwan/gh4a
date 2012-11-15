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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.androidquery.callback.AjaxStatus;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.R;
import com.gh4a.RepositoryActivity;
import com.gh4a.adapter.FileAdapter;
import com.gh4a.loader.ContentListLoader;
import com.gh4a.loader.ContentLoader;
import com.gh4a.loader.GitModuleParserLoader;
import com.gh4a.loader.MarkdownLoader;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

public class ContentListFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<Object>, OnItemClickListener {

    private Repository mRepository;
    public String mPath;
    public String mRef;
    private ListView mListView;
    public FileAdapter mAdapter;
    private OnTreeSelectedListener mCallback;
    private List<Content> mContents;
    private boolean mDataLoaded;
    private String mPathReadme;
    private TextView mFooter;
    private String mMarkdownText; 
    
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
        
        mFooter = (TextView) inflater.inflate(R.layout.row_simple, mListView, false);
        mFooter.setMovementMethod(LinkMovementMethod.getInstance());
        
        mFooter.setTextAppearance(getSherlockActivity(), android.R.style.TextAppearance_Small);
        mListView.addFooterView(mFooter, null, false);
        
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
        }
        else {
            hideLoading();
            mAdapter = new FileAdapter(getSherlockActivity(), mContents);
            mListView.setAdapter(mAdapter);
            mAdapter.notifyDataSetChanged();
            
            readmeExists(mContents);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (!mDataLoaded) {
            if (getLoaderManager().getLoader(0) == null) {
                getLoaderManager().initLoader(0, null, this);
            }
            else {
                getLoaderManager().restartLoader(0, null, this);
            }
            getLoaderManager().getLoader(0).forceLoad();
            
            //get .gitmodules to be parsed
            if (StringUtils.isBlank(mPath)) {
                if (getLoaderManager().getLoader(1) == null) {
                    getLoaderManager().initLoader(1, null, this);
                }
                else {
                    getLoaderManager().restartLoader(1, null, this);
                }
                getLoaderManager().getLoader(1).forceLoad();
            }
           
        }
    }
    
    public void setTreeEntryList(List<Content> contents) {
        mContents = contents;
    }
    
    private void fillData(List<Content> entries) {
        RepositoryActivity activity = (RepositoryActivity) getSherlockActivity();
        activity.hideLoading();
        if (entries != null && entries.size() > 0) {
            mAdapter.clear();
            mAdapter.addAll(entries);
        }
        mAdapter.notifyDataSetChanged();
    }
    
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == 0) {
            return new ContentListLoader(getSherlockActivity(), mRepository.getOwner().getLogin(),
                    mRepository.getName(), mPath, mRef);
        }
        else if (id == 1) {
            return new GitModuleParserLoader(getSherlockActivity(), mRepository.getOwner().getLogin(),
                    mRepository.getName(), ".gitmodules", mRef);            
        }
        else if (id == 2) {
            return new ContentLoader(getSherlockActivity(), mRepository.getOwner().getLogin(), 
                    mRepository.getName(), mPathReadme, mRef);
        }
        else {
            return new MarkdownLoader(getSherlockActivity(), mMarkdownText, "markdown");
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        HashMap<Integer, Object> result = (HashMap<Integer, Object>) object;
        Object data = result.get(LoaderResult.DATA);
        
        hideLoading();
        if (loader.getId() == 1) {
            mCallback.setGitModuleMap((Map<String, String>) data);
        }
        else if (loader.getId() == 0) {
            mDataLoaded = true;
            if (!((BaseSherlockFragmentActivity) getSherlockActivity()).isLoaderError(result)) {
                List<Content> files = (List<Content>) data;
                fillData(files);
                
                readmeExists(files);
            }
        }
        else if (loader.getId() == 2) {
            if (data != null) {
                loadMarkdown((Content) data);
            }
        }
        else if (loader.getId() == 3) {
            if (data != null) {
                String readme = HtmlUtils.format((String) data).toString();
                HttpImageGetter imageGetter = new HttpImageGetter(getSherlockActivity());
                imageGetter.bind(mFooter, readme, mRepository.getId());
                mAdapter.notifyDataSetChanged();    
            }
        }
    }
    
    public void readmeExists(List<Content> files) {
        for (Content content : files) {
            if ("file".equals(content.getType())) {
                String nameWithoutExt = null;
                String filename = content.getName();
                        
                int mid = filename.lastIndexOf(".");
                
                if (mid != -1) {
                    nameWithoutExt = filename.substring(0, mid);
                }
                
                if (nameWithoutExt != null && nameWithoutExt.equalsIgnoreCase("readme")) {
                    if (mPath != null) {
                        mPathReadme = mPath + "/" + filename;
                    }
                    else {
                        mPathReadme = filename;
                    }
                    
                    if (getLoaderManager().getLoader(2) == null) {
                        getLoaderManager().initLoader(2, null, this);
                    }
                    else {
                        getLoaderManager().restartLoader(2, null, this);
                    }
                    getLoaderManager().getLoader(2).forceLoad();
                    
                    break;
                }
            }
        }
    }
    
    public void loadMarkdown(Content content) {
        String ext = FileUtils.getFileExtension(content.getName());
        mMarkdownText = content.getContent();
        if (Arrays.asList(Constants.MARKDOWN_EXT).contains(ext)) {
            mMarkdownText = new String(EncodingUtils.fromBase64(mMarkdownText));
            
            if (getLoaderManager().getLoader(3) == null) {
                getLoaderManager().initLoader(3, null, this);
            }
            else {
                getLoaderManager().restartLoader(3, null, this);
            }
            getLoaderManager().getLoader(3).forceLoad();
        }
        else {
            mFooter.setText(mMarkdownText);
            mAdapter.notifyDataSetChanged();    
        }
    }
    
    public void markdown(String url, String readme, AjaxStatus status) {
        if (readme != null) {
            readme = HtmlUtils.format(readme).toString();
            HttpImageGetter imageGetter = new HttpImageGetter(getSherlockActivity());
            imageGetter.bind(mFooter, readme, mRepository.getId());
        }
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
        
        public void setGitModuleMap(Map<String, String> gitModuleMap);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Content content = (Content) adapterView.getAdapter().getItem(position);
        mCallback.onTreeSelected(position, adapterView, content, mContents, mRef);
    }

}