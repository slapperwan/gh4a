package com.gh4a.fragment;

import android.os.Bundle;

import com.gh4a.Constants;

public class PublicEventListFragment extends EventListFragment {
    public static PublicEventListFragment newInstance(String login) {
        PublicEventListFragment f = new PublicEventListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.LOGIN, login);
        f.setArguments(args);

        return f;
    }
}
