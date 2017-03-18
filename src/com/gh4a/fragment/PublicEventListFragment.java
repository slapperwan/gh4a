package com.gh4a.fragment;

public class PublicEventListFragment extends EventListFragment {
    public static PublicEventListFragment newInstance(String login) {
        PublicEventListFragment f = new PublicEventListFragment();
        f.setArguments(makeArguments(login));
        return f;
    }
}
