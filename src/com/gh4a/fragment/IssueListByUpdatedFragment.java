package com.gh4a.fragment;

import java.util.Map;

import android.os.Bundle;

import com.gh4a.Constants;

public class IssueListByUpdatedFragment extends IssueListFragment {
    public static IssueListByUpdatedFragment newInstance(String repoOwner, String repoName, 
            Map<String, String> filterData) {
        
        IssueListByUpdatedFragment f = new IssueListByUpdatedFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString("sort", "updated");
        
        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }
}
