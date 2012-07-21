package com.gh4a.fragment;

import android.view.View;

import com.actionbarsherlock.app.SherlockFragment;
import com.gh4a.R;

public class BaseFragment extends SherlockFragment {

    public void showLoading() {
        if (getView() != null) {
            if (getView().findViewById(R.id.main_content) != null) {
                getView().findViewById(R.id.main_content).setVisibility(View.INVISIBLE);
            }
            else if (getView().findViewById(R.id.list_view) != null) {
                getView().findViewById(R.id.list_view).setVisibility(View.INVISIBLE);
            }
            getView().findViewById(R.id.pb).setVisibility(View.VISIBLE);
        }
    }
    
    public void hideLoading() {
        if (getView() != null) {
            if (getView().findViewById(R.id.main_content) != null) {
                getView().findViewById(R.id.main_content).setVisibility(View.VISIBLE);
            }
            else if (getView().findViewById(R.id.list_view) != null) {
                getView().findViewById(R.id.list_view).setVisibility(View.VISIBLE);
            }
            getView().findViewById(R.id.pb).setVisibility(View.GONE);
        }
    }
    
    public void showLoading(int progressBar, int viewToShow) {
        if (getView() != null) {
            if (viewToShow != 0 && getView().findViewById(viewToShow) != null) {
                getView().findViewById(viewToShow).setVisibility(View.INVISIBLE);
            }
            getView().findViewById(progressBar).setVisibility(View.VISIBLE);
        }
    }
    
    public void hideLoading(int progressBar, int viewToShow) {
        if (getView() != null) {
            if (viewToShow != 0 && getView().findViewById(viewToShow) != null) {
                getView().findViewById(viewToShow).setVisibility(View.VISIBLE);
            }
            getView().findViewById(progressBar).setVisibility(View.GONE);
        }
    }
}
