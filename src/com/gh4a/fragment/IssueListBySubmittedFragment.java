package com.gh4a.fragment;

import java.util.Iterator;
import java.util.Map;

import android.os.Bundle;

import com.gh4a.Constants;

public class IssueListBySubmittedFragment extends IssueListFragment {

    public static IssueListBySubmittedFragment newInstance(String repoOwner, String repoName, 
            Map<String, String> filterData) {
        
        IssueListBySubmittedFragment f = new IssueListBySubmittedFragment();
        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString("sort", "created");
        
        if (filterData != null) {
            Iterator<String> i = filterData.keySet().iterator();
            while (i.hasNext()) {
                String key = i.next();
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }
}
