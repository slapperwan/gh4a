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

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.activity.WatchingService;

import io.reactivex.Single;
import retrofit2.Response;

public class WatchedRepositoryListFragment extends PagedDataBaseFragment<Repository> {
    public static WatchedRepositoryListFragment newInstance(String login) {
        WatchedRepositoryListFragment f = new WatchedRepositoryListFragment();

        Bundle args = new Bundle();
        args.putString("user", login);
        f.setArguments(args);

        return f;
    }

    private String mLogin;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLogin = getArguments().getString("user");
    }

    @Override
    protected RootAdapter<Repository, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        return new RepositoryAdapter(getActivity());
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_watched_repos_found;
    }

    @Override
    public void onItemClick(Repository repository) {
        startActivity(RepositoryActivity.makeIntent(getActivity(), repository));
    }

    @Override
    protected Single<Response<Page<Repository>>> loadPage(int page, boolean bypassCache) {
        final WatchingService service = ServiceFactory.get(WatchingService.class, bypassCache);
        return service.getWatchedRepositories(mLogin, page);
    }
}