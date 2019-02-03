package com.gh4a.dialogs;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.gh4a.BaseActivity;
import com.gh4a.R;

public abstract class BasePagerDialog extends DialogFragment implements View.OnClickListener {
    private LinearLayout mButtonBar;
    private FragmentAdapter mPagerAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_pager, container, false);

        ViewPager pager = view.findViewById(R.id.dialog_pager);
        mPagerAdapter = new FragmentAdapter(getChildFragmentManager());
        pager.setAdapter(mPagerAdapter);

        mButtonBar = view.findViewById(R.id.button_bar);

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.cancel_button) {
            dismiss();
        }
    }

    protected void refreshPages() {
        for (int position = 0; position < mPagerAdapter.getCount(); position++) {
            Fragment fragment = mPagerAdapter.getExistingFragment(position);
            if (fragment instanceof BaseActivity.RefreshableChild) {
                ((BaseActivity.RefreshableChild) fragment).onRefresh();
            }
        }
    }

    protected Button addButton(int textResId) {
        Button button = (Button) getLayoutInflater()
                .inflate(R.layout.dialog_button, mButtonBar, false);
        button.setText(textResId);
        button.setOnClickListener(this);

        mButtonBar.addView(button, mButtonBar.getChildCount() - 1);
        if (mButtonBar.getChildCount() >= 3) {
            mButtonBar.setOrientation(LinearLayout.VERTICAL);
        }

        return button;
    }

    protected abstract int[] getTabTitleResIds();

    protected abstract Fragment makeFragment(int position);

    private class FragmentAdapter extends FragmentPagerAdapter {
        private final SparseArray<Fragment> mFragments = new SparseArray<>();

        FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return makeFragment(position);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment f = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, f);
            return f;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragments.remove(position);
        }

        Fragment getExistingFragment(int position) {
            return mFragments.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(getTabTitleResIds()[position]);
        }

        @Override
        public int getCount() {
            int[] titleResIds = getTabTitleResIds();
            return titleResIds != null ? titleResIds.length : 0;
        }
    }
}
