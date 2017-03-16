package com.gh4a.widget;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.gh4a.R;
import com.gh4a.fragment.CommentBoxFragment;
import com.gh4a.fragment.CommentPreviewFragment;

public class CommentBoxFragmentAdapter extends FragmentStatePagerAdapter {

    private static final int[] TITLES = new int[]{
        R.string.edit, R.string.preview
    };

    private final Context mContext;
    private final CommentBoxFragment mCommentFragment;

    public CommentBoxFragmentAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
        mCommentFragment = new CommentBoxFragment();
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 1) {
            return new CommentPreviewFragment();
        }

        return mCommentFragment;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mContext.getString(TITLES[position]);
    }

    @Override
    public int getCount() {
        return TITLES.length;
    }

    public CommentBoxFragment getCommentFragment() {
        return mCommentFragment;
    }
}
