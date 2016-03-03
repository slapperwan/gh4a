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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.FileAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.ContentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ContentListFragment extends ListDataBaseFragment<RepositoryContents> {
    private Repository mRepository;
    private String mPath;
    private String mRef;

    private ParentCallback mCallback;
    private FileAdapter mAdapter;

    public interface ParentCallback {
        void onContentsLoaded(ContentListFragment fragment, List<RepositoryContents> contents);
        void onTreeSelected(RepositoryContents content);
        Set<String> getSubModuleNames(ContentListFragment fragment);
    }

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
            mRef = mRepository.getDefaultBranch();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof ParentCallback) {
            mCallback = (ParentCallback) getParentFragment();
        } else if (context instanceof ParentCallback) {
            mCallback = (ParentCallback) context;
        } else {
            throw new ClassCastException("No callback provided");
        }
    }

    @Override
    protected RootAdapter<RepositoryContents, ?> onCreateAdapter() {
        mAdapter = new FileAdapter(getActivity(), mRepository, mRef);
        mAdapter.setSubModuleNames(mCallback.getSubModuleNames(this));
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_files_found;
    }

    public String getPath() {
        return mPath;
    }

    public void onSubModuleNamesChanged(Set<String> subModules) {
        if (mAdapter != null) {
            mAdapter.setSubModuleNames(subModules);
        }
    }

    @Override
    protected void onAddData(RootAdapter<RepositoryContents, ?> adapter, List<RepositoryContents> data) {
        super.onAddData(adapter, data);
        mCallback.onContentsLoaded(this, data);
    }

    @Override
    public void onItemClick(RepositoryContents content) {
        mCallback.onTreeSelected(content);
    }

    @Override
    public Loader<LoaderResult<List<RepositoryContents>>> onCreateLoader() {
        ContentListLoader loader = new ContentListLoader(getActivity(),
                mRepository.getOwner().getLogin(), mRepository.getName(), mPath, mRef);
        @SuppressWarnings("unchecked")
        ArrayList<RepositoryContents> contents =
                (ArrayList<RepositoryContents>) getArguments().getSerializable("CONTENTS");
        if (contents != null) {
            loader.prefillData(contents);
        }
        return loader;
    }
}