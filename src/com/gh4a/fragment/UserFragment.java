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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.FollowerFollowingListActivity;
import com.gh4a.activities.GistListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.RepositoryListActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.OrganizationListLoader;
import com.gh4a.loader.RepositoryListLoader;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserFragment extends LoadingFragmentBase implements View.OnClickListener {
    public static UserFragment newInstance(String login) {
        UserFragment f = new UserFragment();

        Bundle args = new Bundle();
        args.putString("login", login);
        f.setArguments(args);

        return f;
    }

    private String mUserLogin;
    private User mUser;
    private View mContentView;

    private final LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>(this) {
        @Override
        protected Loader<LoaderResult<User>> onCreateLoader() {
            return new UserLoader(getActivity(), mUserLogin);
        }
        @Override
        protected void onResultReady(User result) {
            mUser = result;
            fillData();
            setContentShown(true);
            getActivity().supportInvalidateOptionsMenu();
        }
    };

    private final LoaderCallbacks<Collection<Repository>> mRepoListCallback =
            new LoaderCallbacks<Collection<Repository>>(this) {
        @Override
        protected Loader<LoaderResult<Collection<Repository>>> onCreateLoader() {
            Map<String, String> filterData = new HashMap<>();
            filterData.put("sort", "pushed");
            filterData.put("affiliation", "owner,collaborator");
            return new RepositoryListLoader(getActivity(), mUserLogin,
                    mUser.getType(), filterData, 5);
        }
        @Override
        protected void onResultReady(Collection<Repository> result) {
            fillTopRepos(result);
        }
    };

    private final LoaderCallbacks<List<User>> mOrganizationCallback = new LoaderCallbacks<List<User>>(this) {
        @Override
        protected Loader<LoaderResult<List<User>>> onCreateLoader() {
            return new OrganizationListLoader(getActivity(), mUserLogin);
        }
        @Override
        protected void onResultReady(List<User> result) {
            fillOrganizations(result);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString("login");
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.user, parent, false);
        return mContentView;
    }

    @Override
    public void onRefresh() {
        mUser = null;
        if (mContentView != null) {
            fillOrganizations(null);
            fillTopRepos(null);
        }
        hideContentAndRestartLoaders(0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);

        getLoaderManager().initLoader(0, null, mUserCallback);
    }

    private void fillData() {
        ImageView gravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mUser);

        TextView tvFollowersCount = (TextView) mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));

        View llOrgMembers = mContentView.findViewById(R.id.cell_org_members);
        View llFollowers = mContentView.findViewById(R.id.cell_followers);

        if (ApiHelpers.UserType.USER.equals(mUser.getType())) {
            llFollowers.setOnClickListener(this);
            llOrgMembers.setVisibility(View.GONE);
        } else {
            llOrgMembers.setOnClickListener(this);
            llFollowers.setVisibility(View.GONE);
        }

        mContentView.findViewById(R.id.cell_repos).setOnClickListener(this);

        TextView tvReposCount = (TextView) mContentView.findViewById(R.id.tv_repos_count);
        if (ApiHelpers.loginEquals(mUserLogin, Gh4Application.get().getAuthLogin())) {
            tvReposCount.setText(String.valueOf(mUser.getTotalPrivateRepos() + mUser.getPublicRepos()));
        } else {
            tvReposCount.setText(String.valueOf(mUser.getPublicRepos()));
        }

        //hide gists repos if organization
        fillCountIfUser(R.id.cell_gists, R.id.tv_gists_count,
                mUser.getPublicGists() + mUser.getPrivateGists());
        //hide following if organization
        fillCountIfUser(R.id.cell_following, R.id.tv_following_count, mUser.getFollowing());

        TextView tvName = (TextView) mContentView.findViewById(R.id.tv_name);
        String name = StringUtils.isBlank(mUser.getName()) ? mUser.getLogin() : mUser.getName();
        if (ApiHelpers.UserType.ORG.equals(mUser.getType())) {
            tvName.setText(getString(R.string.org_user_template, name));
        } else {
            tvName.setText(name);
        }

        TextView tvCreated = (TextView) mContentView.findViewById(R.id.tv_created_at);
        if (mUser.getCreatedAt() != null) {
            tvCreated.setText(getString(R.string.user_created_at,
                    DateFormat.getMediumDateFormat(getActivity()).format(mUser.getCreatedAt())));
            tvCreated.setVisibility(View.VISIBLE);
        } else {
            tvCreated.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_email, mUser.getEmail());
        fillTextView(R.id.tv_website, mUser.getBlog());
        fillTextView(R.id.tv_company, mUser.getCompany());
        fillTextView(R.id.tv_location, mUser.getLocation());

        getLoaderManager().initLoader(1, null, mRepoListCallback);
        if (User.TYPE_USER.equals(mUser.getType())) {
            getLoaderManager().initLoader(2, null, mOrganizationCallback);
        } else {
            fillOrganizations(null);
        }
    }

    private void fillCountIfUser(int layoutId, int countId, int count) {
        View layout = mContentView.findViewById(layoutId);
        if (ApiHelpers.UserType.USER.equals(mUser.getType())) {
            TextView countView = (TextView) mContentView.findViewById(countId);
            countView.setText(String.valueOf(count));
            layout.setOnClickListener(this);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fillTextView(int id, String text) {
        TextView view = (TextView) mContentView.findViewById(id);
        if (!StringUtils.isBlank(text)) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Intent intent = null;

        if (id == R.id.cell_followers) {
            if (ApiHelpers.UserType.ORG.equals(mUser.getType())) {
                intent = OrganizationMemberListActivity.makeIntent(getActivity(), mUserLogin);
            } else {
                intent = FollowerFollowingListActivity.makeIntent(getActivity(), mUserLogin, true);
            }
        } else if (id == R.id.cell_following) {
            intent = FollowerFollowingListActivity.makeIntent(getActivity(), mUserLogin, false);
        } else if (id == R.id.cell_repos || id == R.id.btn_repos) {
            intent = RepositoryListActivity.makeIntent(getActivity(), mUserLogin,
                    ApiHelpers.UserType.ORG.equals(mUser.getType()));
        } else if (id == R.id.cell_gists) {
            intent = GistListActivity.makeIntent(getActivity(), mUserLogin);
        } else if (id == R.id.cell_org_members) {
            intent = OrganizationMemberListActivity.makeIntent(getActivity(), mUserLogin);
        } else if (view.getTag() instanceof Repository) {
            intent = RepositoryActivity.makeIntent(getActivity(), (Repository) view.getTag());
        } else if (view.getTag() instanceof User) {
            intent = UserActivity.makeIntent(getActivity(), (User) view.getTag());
        }
        if (intent != null) {
            startActivity(intent);
        }
    }

    private void fillTopRepos(Collection<Repository> topRepos) {
        LinearLayout ll = (LinearLayout) mContentView.findViewById(R.id.ll_top_repos);
        ll.removeAllViews();

        LayoutInflater inflater = getLayoutInflater(null);

        if (topRepos != null) {
            for (Repository repo : topRepos) {
                View rowView = inflater.inflate(R.layout.top_repo, null);
                rowView.setOnClickListener(this);
                rowView.setTag(repo);

                TextView tvTitle = (TextView) rowView.findViewById(R.id.tv_title);
                tvTitle.setText(repo.getOwner().getLogin() + "/" + repo.getName());

                TextView tvDesc = (TextView) rowView.findViewById(R.id.tv_desc);
                if (!StringUtils.isBlank(repo.getDescription())) {
                    tvDesc.setVisibility(View.VISIBLE);
                    tvDesc.setText(repo.getDescription());
                } else {
                    tvDesc.setVisibility(View.GONE);
                }

                TextView tvForks = (TextView) rowView.findViewById(R.id.tv_forks);
                tvForks.setText(String.valueOf(repo.getForks()));

                TextView tvStars = (TextView) rowView.findViewById(R.id.tv_stars);
                tvStars.setText(String.valueOf(repo.getWatchers()));

                ll.addView(rowView);
            }
        }

        View btnMore = getView().findViewById(R.id.btn_repos);
        if (topRepos != null && !topRepos.isEmpty()) {
            btnMore.setOnClickListener(this);
            btnMore.setVisibility(View.VISIBLE);
        } else {
            TextView hintView = (TextView) inflater.inflate(R.layout.hint_view, ll, false);
            hintView.setText(R.string.user_no_repos);
            ll.addView(hintView);
        }

        getView().findViewById(R.id.pb_top_repos).setVisibility(View.GONE);
        getView().findViewById(R.id.ll_top_repos).setVisibility(View.VISIBLE);
    }

    private void fillOrganizations(List<User> organizations) {
        ViewGroup llOrgs = (ViewGroup) mContentView.findViewById(R.id.ll_orgs);
        LinearLayout llOrg = (LinearLayout) mContentView.findViewById(R.id.ll_org);
        int count = organizations != null ? organizations.size() : 0;
        LayoutInflater inflater = getLayoutInflater(null);

        llOrg.removeAllViews();
        llOrgs.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < count; i++) {
            User org = organizations.get(i);
            View rowView = inflater.inflate(R.layout.selectable_label_with_avatar, llOrg, false);

            rowView.setOnClickListener(this);
            rowView.setTag(org);

            ImageView avatar = (ImageView) rowView.findViewById(R.id.iv_gravatar);
            AvatarHandler.assignAvatar(avatar, org);

            TextView nameView = (TextView) rowView.findViewById(R.id.tv_title);
            nameView.setText(org.getLogin());

            llOrg.addView(rowView);
        }
    }

    public void updateFollowingAction(boolean following) {
        if (mUser == null) {
            return;
        }

        if (following) {
            mUser.setFollowers(mUser.getFollowers() + 1);
        } else {
            mUser.setFollowers(mUser.getFollowers() - 1);
        }
        TextView tvFollowersCount = (TextView) mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.getFollowers()));
    }
}
