package com.gh4a.fragment;

import java.util.Map;

import android.os.Bundle;

import com.gh4a.Constants;

public class IssueListByCommentsFragment extends IssueListFragment {

    public static IssueListByCommentsFragment newInstance(String repoOwner, String repoName,
            Map<String, String> filterData) {

        IssueListByCommentsFragment f = new IssueListByCommentsFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString("sort", "comments");

        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }
}
