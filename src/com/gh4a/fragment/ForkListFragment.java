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
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.os.Bundle;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.IntentUtils;

public class ForkListFragment extends PagedDataBaseFragment<Repository> {
    private String mRepoOwner;
    private String mRepoName;

    public static ForkListFragment newInstance(String repoOwner, String repoName) {
        ForkListFragment f = new ForkListFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
    }

    @Override
    protected RootAdapter<Repository> onCreateAdapter() {
        return new RepositoryAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_forks_found;
    }

    @Override
    protected void onItemClick(Repository repo) {
        startActivity(IntentUtils.getRepoActivityIntent(getActivity(),
                repo.getOwner().getLogin(), repo.getName(), null));
    }

    @Override
    protected PageIterator<Repository> onCreateIterator() {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.pageForks(new RepositoryId(mRepoOwner, mRepoName));
    }
}