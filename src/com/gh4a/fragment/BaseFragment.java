package com.gh4a.fragment;

import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.R;

public class BaseFragment extends SherlockFragment {

    public void showLoading() {
        if (getView().findViewById(R.id.main_content) != null) {
            getView().findViewById(R.id.main_content).setVisibility(View.INVISIBLE);
        }
        else if (getView().findViewById(R.id.list_view) != null) {
            getView().findViewById(R.id.list_view).setVisibility(View.INVISIBLE);
        }
        getView().findViewById(R.id.pb).setVisibility(View.VISIBLE);
    }
    
    public void hideLoading() {
        if (getView().findViewById(R.id.main_content) != null) {
            getView().findViewById(R.id.main_content).setVisibility(View.VISIBLE);
        }
        else if (getView().findViewById(R.id.list_view) != null) {
            getView().findViewById(R.id.list_view).setVisibility(View.VISIBLE);
        }
        getView().findViewById(R.id.pb).setVisibility(View.GONE);
    }
}
