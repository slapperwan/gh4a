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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryIssue;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.IssueService;

import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryIssueAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.IntentUtils;

public class RepositoryIssueListFragment extends PagedDataBaseFragment<RepositoryIssue> {
    private Map<String, String> mFilterData;

    public static RepositoryIssueListFragment newInstance(Map<String, String> filterData) {
        RepositoryIssueListFragment f = new RepositoryIssueListFragment();

        Bundle args = new Bundle();
        if (filterData != null) {
            for (String key : filterData.keySet()) {
                args.putString(key, filterData.get(key));
            }
        }
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFilterData = new HashMap<String, String>();

        Bundle args = getArguments();
        for (String key : args.keySet()) {
            if (!key.equals(Constants.User.LOGIN)
                    && !key.equals(Constants.Repository.NAME)) {
                mFilterData.put(key, args.getString(key));
            }
        }
    }

    @Override
    public void onItemClick(RepositoryIssue issue) {
        Repository repo = issue.getRepository();
        startActivity(IntentUtils.getIssueActivityIntent(getActivity(), repo.getOwner().getLogin(),
                repo.getName(), issue.getNumber()));
    }

    @Override
    protected RootAdapter<RepositoryIssue> onCreateAdapter() {
        getListView().setDivider(null);
        getListView().setDividerHeight(0);
        return new RepositoryIssueAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_issues_found;
    }

    @Override
    protected PageIterator<RepositoryIssue> onCreateIterator() {
        IssueService issueService = (IssueService)
                Gh4Application.get(getActivity()).getService(Gh4Application.ISSUE_SERVICE);
        return issueService.pageIssues(mFilterData);
    }
}