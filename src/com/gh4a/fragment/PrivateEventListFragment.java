package com.gh4a.fragment;

import android.os.Bundle;
import android.view.MenuItem;

import com.gh4a.Constants;

public class PrivateEventListFragment extends EventListFragment {
    public static PrivateEventListFragment newInstance(String login, boolean isPrivate) {
        PrivateEventListFragment f = new PrivateEventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, login);
        args.putBoolean("private", isPrivate);
        f.setArguments(args);

        return f;
    }

    @Override
    public int getMenuGroupId() {
        return 1;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getGroupId() == 1) {
            open(item);
            return true;
        }
        return false;
    }
}
