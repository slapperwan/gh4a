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

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

import io.reactivex.Single;
import retrofit2.Response;

public class RepositoryListFragment extends PagedDataBaseFragment<Repository> {
    private static final String EXTRA_USER = "user";
    private static final String EXTRA_IS_ORG = "is_org";
    private static final String EXTRA_REPO_TYPE = "repo_type";
    private static final String EXTRA_SORT_ORDER = "sort_order";
    private static final String EXTRA_SORT_DIRECTION = "sort_direction";
    private static final String REPO_TYPE_SOURCES = "sources";
    private static final String REPO_TYPE_FORKS = "forks";
    private static final String REPO_TYPE_ALL = "all";
    private static final String FILTER_DATA_AFFILIATION = "affiliation";
    private static final String FILTER_DATA_TYPE = "type";
    private static final String FILTER_DATA_SORT = "sort";
    private static final String FILTER_DATA_DIRECTION = "direction";

    public static RepositoryListFragment newInstance(String login, boolean isOrg,
            String repoType, String sortOrder, String sortDirection) {
        RepositoryListFragment f = new RepositoryListFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_USER, login);
        args.putBoolean(EXTRA_IS_ORG, isOrg);
        args.putString(EXTRA_REPO_TYPE, repoType);
        args.putString(EXTRA_SORT_ORDER, sortOrder);
        args.putString(EXTRA_SORT_DIRECTION, sortDirection);
        f.setArguments(args);

        return f;
    }

    private final Map<String, String> mFilterData = new HashMap<>();

    private String mLogin;
    private String mRepoType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();

        mLogin = args.getString(EXTRA_USER);
        mRepoType = args.getString(EXTRA_REPO_TYPE);

        final boolean isSelf = ApiHelpers.loginEquals(mLogin, Gh4Application.get().getAuthLogin());

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

        String actualFilterType = REPO_TYPE_SOURCES.equals(mRepoType) || REPO_TYPE_FORKS.equals(mRepoType)
                ? REPO_TYPE_ALL : mRepoType;

        if (isSelf && TextUtils.equals(actualFilterType, REPO_TYPE_ALL)) {
            mFilterData.put(FILTER_DATA_AFFILIATION, "owner,collaborator");
        } else if (!TextUtils.equals(actualFilterType, REPO_TYPE_ALL) || !args.getBoolean(EXTRA_IS_ORG)) {
            mFilterData.put(FILTER_DATA_TYPE, actualFilterType);
        }

        final String sortOrder = args.getString(EXTRA_SORT_ORDER);
        if (sortOrder != null) {
            mFilterData.put(FILTER_DATA_SORT, sortOrder);
            mFilterData.put(FILTER_DATA_DIRECTION, args.getString(EXTRA_SORT_DIRECTION));
        }
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
        if (REPO_TYPE_SOURCES.equals(mRepoType) || REPO_TYPE_FORKS.equals(mRepoType)) {
            for (Repository repository : repositories) {
                if (REPO_TYPE_SOURCES.equals(mRepoType) && !repository.isFork()) {
                    adapter.add(repository);
                } else if (REPO_TYPE_FORKS.equals(mRepoType) && repository.isFork()) {
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
    protected Single<Response<Page<Repository>>> loadPage(int page, boolean bypassCache) {
        final RepositoryService service = ServiceFactory.get(RepositoryService.class, bypassCache);
        return ApiHelpers.loginEquals(mLogin, Gh4Application.get().getAuthLogin())
                ? service.getUserRepositories(mFilterData, page)
                : service.getUserRepositories(mLogin, mFilterData, page);
    }
}