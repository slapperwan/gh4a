/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.fragment;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Intent;
import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.activities.IssueActivity;
import com.gh4a.adapter.IssueAdapter;
import com.gh4a.adapter.RootAdapter;

public class IssueListFragment extends PagedDataBaseFragment<Issue> {
    private String mRepoOwner;
    private String mRepoName;
    private Map<String, String> mFilterData;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
        
        mFilterData = new HashMap<String, String>();
        
        Bundle args = getArguments();
        for (String key : args.keySet()) {
            if (!key.equals(Constants.Repository.REPO_OWNER) 
                    && !key.equals(Constants.Repository.REPO_NAME)) {
                mFilterData.put(key, args.getString(key));
            }
        }
    }

    @Override
    protected RootAdapter<Issue> onCreateAdapter() {
        return new IssueAdapter(getSherlockActivity());
    }
    
    @Override
    protected void onItemClick(Issue issue) {
        Intent intent = new Intent(getSherlockActivity(), IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issue.getNumber());
        intent.putExtra(Constants.Issue.ISSUE_STATE, issue.getState());
        startActivity(intent);
    }

    @Override
    protected PageIterator<Issue> onCreateIterator() {
        IssueService issueService = (IssueService)
                Gh4Application.get(getActivity()).getService(Gh4Application.ISSUE_SERVICE);
        return issueService.pageIssues(new RepositoryId(mRepoOwner, mRepoName), mFilterData);
    }

}