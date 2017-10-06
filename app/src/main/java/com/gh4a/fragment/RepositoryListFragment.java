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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.PageIteratorLoader;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

import retrofit2.Response;

public class RepositoryListFragment extends PagedDataBaseFragment<Repository> {
    private String mLogin;
    private String mRepoType;
    private boolean mIsOrg;
    private String mSortOrder;
    private String mSortDirection;

    public static RepositoryListFragment newInstance(String login, boolean isOrg,
            String repoType, String sortOrder, String sortDirection) {
        RepositoryListFragment f = new RepositoryListFragment();

        Bundle args = new Bundle();
        args.putString("user", login);
        args.putBoolean("is_org", isOrg);
        args.putString("repo_type", repoType);
        args.putString("sort_order", sortOrder);
        args.putString("sort_direction", sortDirection);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("user");
        mRepoType = getArguments().getString("repo_type");
        mIsOrg = getArguments().getBoolean("is_org");
        mSortOrder = getArguments().getString("sort_order");
        mSortDirection = getArguments().getString("sort_direction");
    }

    @Override
    protected RootAdapter<Repository, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new RepositoryAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_repos_found;
    }

    @Override
    protected void onAddData(RootAdapter<Repository, ? extends RecyclerView.ViewHolder> adapter,
            Collection<Repository> repositories) {
        if ("sources".equals(mRepoType) || "forks".equals(mRepoType)) {
            for (Repository repository : repositories) {
                if ("sources".equals(mRepoType) && !repository.isFork()) {
                    adapter.add(repository);
                } else if ("forks".equals(mRepoType) && repository.isFork()) {
                    adapter.add(repository);
                }
            }
            adapter.notifyDataSetChanged();
        } else {
            adapter.addAll(repositories);
        }
    }

    @Override
    public void onItemClick(Repository repository) {
        startActivity(RepositoryActivity.makeIntent(getActivity(), repository));
    }

    @Override
    protected PageIteratorLoader<Repository> onCreateLoader() {
        final boolean isSelf = ApiHelpers.loginEquals(mLogin, Gh4Application.get().getAuthLogin());
        final Map<String, String> filterData = new HashMap<>();

        // We're operating on the limit of what Github's repo API supports. Specifically,
        // it doesn't support sorting for the organization repo list endpoint, so we're using
        // the user repo list endpoint for organizations as well. Doing so has a few quirks though:
        // - the 'all' filter returns an empty list when querying organization repos, so we
        //   need to omit the filter in that case
        // - 'sources' and 'forks' filter types are only supported for the org repo list endpoint,
        //   but not for the user repo list endpoint, hence we emulate it by querying for 'all'
        //   and filtering the result
        // Additionally, using affiliation together with type is not supported, so omit
        // type when adding affiliation.

        String actualFilterType = "sources".equals(mRepoType) || "forks".equals(mRepoType)
                ? "all" : mRepoType;

        if (isSelf && TextUtils.equals(actualFilterType, "all")) {
            filterData.put("affiliation", "owner,collaborator");
        } else if (!TextUtils.equals(actualFilterType, "all") || !mIsOrg) {
            filterData.put("type", actualFilterType);
        }

        if (mSortOrder != null) {
            filterData.put("sort", mSortOrder);
            filterData.put("direction", mSortDirection);
        }

        return new PageIteratorLoader<Repository>(getActivity()) {
            final RepositoryService service =
                    Gh4Application.get().getGitHubService(RepositoryService.class);
            @Override
            protected Page<Repository> loadPage(int page) throws IOException {
                Response<Page<Repository>> response = isSelf
                        ? service.getUserRepositories(filterData, page).blockingGet()
                        : service.getUserRepositories(mLogin, filterData, page).blockingGet();
                return ApiHelpers.throwOnFailure(response);
            }
        };
    }
}