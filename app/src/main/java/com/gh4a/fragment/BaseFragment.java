package com.gh4a.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import io.reactivex.disposables.CompositeDisposable;

public class BaseFragment extends Fragment {
    protected CompositeDisposable mCompositeDisposable;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCompositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(mCompositeDisposable.isDisposed())
            mCompositeDisposable.dispose();
    }
}