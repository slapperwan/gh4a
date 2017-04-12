package com.gh4a.fragment;

public class StarredRepositoryListContainerFragment extends RepositoryListContainerFragment {
    public static StarredRepositoryListContainerFragment newInstance(String userLogin,
            boolean isOrg) {
        StarredRepositoryListContainerFragment f = new StarredRepositoryListContainerFragment();
        f.setArguments(createArgs(userLogin, isOrg));
        return f;
    }

    @Override
    protected boolean displayOnlyStarredRepos() {
        return true;
    }
}