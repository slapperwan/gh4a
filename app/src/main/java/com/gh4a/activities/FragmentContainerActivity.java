package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.fragment.LoadingFragmentBase;
import com.gh4a.widget.SwipeRefreshLayout;

public abstract class FragmentContainerActivity extends BaseActivity {
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            mFragment = onCreateFragment();
            fm.beginTransaction().add(R.id.content_container, mFragment).commit();
        } else {
            mFragment = fm.findFragmentById(R.id.content_container);
        }

        if (mFragment instanceof SwipeRefreshLayout.ChildScrollDelegate) {
            setChildScrollDelegate((SwipeRefreshLayout.ChildScrollDelegate) mFragment);
        }
    }

    protected abstract Fragment onCreateFragment();

    protected Fragment getFragment() {
        return mFragment;
    }

    @Override
    public void onRefresh() {
        super.onRefresh();
        if (mFragment instanceof RefreshableChild) {
            ((RefreshableChild) mFragment).onRefresh();
        }
    }

    @Override
    public void onBackPressed() {
        if (mFragment instanceof LoadingFragmentBase
                && ((LoadingFragmentBase) mFragment).onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}