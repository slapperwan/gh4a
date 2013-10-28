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

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.TrendAdapter;
import com.gh4a.holder.Trend;
import com.gh4a.loader.TrendLoader;

public class TrendingFragment extends BaseFragment 
    implements LoaderManager.LoaderCallbacks<List<Trend>>, OnItemClickListener {

    public String mUrl;
    private ListView mListView;
    public TrendAdapter mAdapter;

    public static TrendingFragment newInstance(String url) {
        TrendingFragment f = new TrendingFragment();

        Bundle args = new Bundle();
        args.putString("url", url);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUrl = getArguments().getString("url");
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new TrendAdapter(getSherlockActivity());
        mListView.setAdapter(mAdapter);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData(List<Trend> trends) {
        getActivity().invalidateOptionsMenu();
        mAdapter.clear();
        mAdapter.addAll(trends);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Gh4Application app = Gh4Application.get(getActivity());
        Trend trend = (Trend) adapterView.getAdapter().getItem(position);
        String[] repos = trend.getTitle().split("/");
        app.openRepositoryInfoActivity(getSherlockActivity(), repos[0].trim(), repos[1].trim(), 0);
    }

    @Override
    public Loader<List<Trend>> onCreateLoader(int id, Bundle args) {
        return new TrendLoader(getActivity(), mUrl);
    }

    @Override
    public void onLoadFinished(Loader<List<Trend>> loader, List<Trend> trends) {
        hideLoading();
        fillData(trends);
    }

    @Override
    public void onLoaderReset(Loader<List<Trend>> arg0) {
        // TODO Auto-generated method stub
        
    }

}