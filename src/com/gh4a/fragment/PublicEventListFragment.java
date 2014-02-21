package com.gh4a.fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.gh4a.Constants;

public class PublicEventListFragment extends EventListFragment {
    public static PublicEventListFragment newInstance(String login, boolean isPrivate) {
        PublicEventListFragment f = new PublicEventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, login);
        args.putBoolean("private", isPrivate);
        f.setArguments(args);

        return f;
    }

    @Override
    public int getMenuGroupId() {
        return 2;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == 2) {
            open(item);
            return true;
        }
        return false;
    }
}
