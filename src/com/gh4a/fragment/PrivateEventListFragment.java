package com.gh4a.fragment;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.gh4a.Constants;

public class PrivateEventListFragment extends EventListFragment {

    public static EventListFragment newInstance(String login, boolean isPrivate) {
        EventListFragment f = new PrivateEventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putBoolean(Constants.Event.IS_PRIVATE, isPrivate);
        f.setArguments(args);
        
        return f;
    }

    @Override
    public int getMenuGroupId() {
        return 1;
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getGroupId() == 1) { 
            open(item);
            return true;
        }
        else {
            return false;
        }
    }
}
