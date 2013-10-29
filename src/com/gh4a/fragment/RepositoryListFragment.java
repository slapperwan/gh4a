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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Intent;
import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;

public class RepositoryListFragment extends PagedDataBaseFragment<Repository> {
    private String mLogin;
    private String mRepoType;
    
    public static RepositoryListFragment newInstance(String login, String userType, String repoType) {
        RepositoryListFragment f = new RepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putString(Constants.User.USER_TYPE, userType);
        args.putString(Constants.Repository.REPO_TYPE, repoType);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mRepoType = getArguments().getString(Constants.Repository.REPO_TYPE);
    }
    
    @Override
    protected RootAdapter<Repository> onCreateAdapter() {
        return new RepositoryAdapter(getSherlockActivity());
    }

    @Override
    protected void onAddData(RootAdapter<Repository> adapter, Collection<Repository> repositories) {
        if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
            for (Repository repository : repositories) {
                if ("sources".equals(mRepoType) && !repository.isFork()) {
                    adapter.add(repository);
                } else if ("forks".equals(mRepoType) && repository.isFork()) {
                    adapter.add(repository);
                }
            }
        } else {
            adapter.addAll(repositories);  
        }
    }

    @Override
    protected void onItemClick(Repository repository) {
        Gh4Application app = Gh4Application.get(getActivity());
        
        Intent intent = new Intent(getActivity(), RepositoryActivity.class);
        Bundle data = app.populateRepository(repository);
        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }

    @Override
    protected PageIterator<Repository> onCreateIterator() {
        Gh4Application app = Gh4Application.get(getActivity());
        RepositoryService repoService = (RepositoryService) app.getService(Gh4Application.REPO_SERVICE);
            
        Map<String, String> filterData = new HashMap<String, String>();
        if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
            filterData.put("type", "all");
        } else {
            filterData.put("type", mRepoType);
        }

        return mLogin.equals(app.getAuthLogin())
                ? repoService.pageRepositories(filterData, 100)
                : repoService.pageRepositories(mLogin, filterData, 100);
    }
}