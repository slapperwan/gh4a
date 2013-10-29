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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.WatcherService;

import android.content.Intent;
import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;

public class WatchedRepositoryListFragment extends PagedDataBaseFragment<Repository> {
    private String mLogin;
    
    public static WatchedRepositoryListFragment newInstance(String login) {
        WatchedRepositoryListFragment f = new WatchedRepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString(Constants.User.USER_LOGIN);
    }
    
    @Override
    protected RootAdapter<Repository> onCreateAdapter() {
        return new RepositoryAdapter(getSherlockActivity());
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
        WatcherService watcherService = (WatcherService)
                Gh4Application.get(getActivity()).getService(Gh4Application.WATCHER_SERVICE);
        return watcherService.pageWatched(mLogin);
    }
}