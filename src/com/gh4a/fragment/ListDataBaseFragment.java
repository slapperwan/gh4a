package com.gh4a.fragment;

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.R;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.LoaderResult;

public abstract class ListDataBaseFragment<T> extends BaseFragment implements
        OnItemClickListener, LoaderCallbacks<LoaderResult<List<T>>> {
    protected ListView mListView;
    private RootAdapter<T> mAdapter;

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
        
        mAdapter = onCreateAdapter();
        mListView.setAdapter(mAdapter);
        
        showLoading();
        
        getLoaderManager().initLoader(0, null, this);
    }

    protected void onAddData(RootAdapter<T> adapter, List<T> data) {
        adapter.addAll(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
        onItemClick(mAdapter.getItem(position));
    }

    @Override
    public void onLoadFinished(Loader<LoaderResult<List<T>>> loader, LoaderResult<List<T>> result) {
        hideLoading();
        if (!result.handleError(getActivity())) {
            onAddData(mAdapter, result.getData());
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<LoaderResult<List<T>>> loader) {
    }

    protected abstract RootAdapter<T> onCreateAdapter();
    protected abstract void onItemClick(T item);
}
