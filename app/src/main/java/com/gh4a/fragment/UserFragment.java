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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.FollowerFollowingListActivity;
import com.gh4a.activities.GistListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.RepositoryListActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;
import com.meisolsson.githubsdk.service.organizations.OrganizationService;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;
import com.meisolsson.githubsdk.service.users.UserFollowerService;
import com.meisolsson.githubsdk.service.users.UserService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import retrofit2.Response;

public class UserFragment extends LoadingFragmentBase implements View.OnClickListener {
    public static final String EXTRA_LOGIN = "login";

    public static UserFragment newInstance(String login) {
        UserFragment f = new UserFragment();

        Bundle args = new Bundle();
        args.putString(EXTRA_LOGIN, login);
        f.setArguments(args);

        return f;
    }

    private static final int ID_LOADER_USER = 0;
    private static final int ID_LOADER_REPO_LIST = 1;
    private static final int ID_LOADER_ORG_LIST = 2;
    private static final int ID_LOADER_IS_FOLLOWING = 3;

    private String mUserLogin;
    private User mUser;
    private View mContentView;
    private Boolean mIsFollowing;
    private boolean mIsSelf;

    private Disposable mOrgListSubscription;
    private Disposable mIsFollowingSubscription;
    private Disposable mTopRepoSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString(EXTRA_LOGIN);
        mIsSelf = ApiHelpers.loginEquals(mUserLogin, Gh4Application.get().getAuthLogin());
        setHasOptionsMenu(true);
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.user, parent, false);
        return mContentView;
    }

    @Override
    public void onRefresh() {
        mUser = null;
        mIsFollowing = false;
        if (mContentView != null) {
            fillOrganizations(null);
            fillTopRepos(null);
        }
        setContentShown(false);
        loadUser(true);
        if (mTopRepoSubscription != null) {
            mTopRepoSubscription.dispose();
            mTopRepoSubscription = null;
        }
        if (mIsFollowingSubscription != null) {
            loadIsFollowingState(true);
        }
        if (mOrgListSubscription != null) {
            mOrgListSubscription.dispose();
            mOrgListSubscription = null;
        }
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentShown(false);

        loadUser(false);
        if (!mIsSelf && Gh4Application.get().isAuthorized()) {
            loadIsFollowingState(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.user_follow_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem followAction = menu.findItem(R.id.follow);
        if (followAction != null) {
            if (!mIsSelf && Gh4Application.get().isAuthorized()
                    && mUser != null && mUser.type() == UserType.Organization) {
                followAction.setVisible(true);
                if (mIsFollowing == null) {
                    followAction.setActionView(R.layout.ab_loading);
                    followAction.expandActionView();
                } else if (mIsFollowing) {
                    followAction.setTitle(R.string.user_unfollow_action);
                } else {
                    followAction.setTitle(R.string.user_follow_action);
                }
            } else {
                followAction.setVisible(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.follow) {
            item.setActionView(R.layout.ab_loading);
            item.expandActionView();
            toggleFollowingState();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void fillData(boolean forceLoad) {
        ImageView gravatar = mContentView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mUser);

        TextView tvFollowersCount = mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.followers()));

        View llOrgMembers = mContentView.findViewById(R.id.cell_org_members);
        View llFollowers = mContentView.findViewById(R.id.cell_followers);

        if (mUser.type() == UserType.User) {
            llFollowers.setOnClickListener(this);
            llOrgMembers.setVisibility(View.GONE);
        } else {
            llOrgMembers.setOnClickListener(this);
            llFollowers.setVisibility(View.GONE);
        }

        mContentView.findViewById(R.id.cell_repos).setOnClickListener(this);

        TextView tvReposCount = mContentView.findViewById(R.id.tv_repos_count);
        final int gistCount;
        if (mIsSelf) {
            tvReposCount.setText(String.valueOf(mUser.totalPrivateRepos() + mUser.publicRepos()));
            gistCount = mUser.publicGists() + mUser.privateGists();
        } else {
            tvReposCount.setText(String.valueOf(mUser.publicRepos()));
            gistCount = mUser.publicGists();
        }

        //hide gists repos if organization

        fillCountIfUser(R.id.cell_gists, R.id.tv_gists_count, gistCount);
        //hide following if organization
        fillCountIfUser(R.id.cell_following, R.id.tv_following_count, mUser.following());

        TextView tvName = mContentView.findViewById(R.id.tv_name);
        String name = StringUtils.isBlank(mUser.name()) ? mUser.login() : mUser.name();
        if (mUser.type() == UserType.Organization) {
            tvName.setText(getString(R.string.org_user_template, name));
        } else {
            tvName.setText(name);
        }

        TextView tvCreated = mContentView.findViewById(R.id.tv_created_at);
        if (mUser.createdAt() != null) {
            tvCreated.setText(getString(R.string.user_created_at,
                    DateFormat.getMediumDateFormat(getActivity()).format(mUser.createdAt())));
            tvCreated.setVisibility(View.VISIBLE);
        } else {
            tvCreated.setVisibility(View.GONE);
        }

        fillTextView(R.id.tv_email, mUser.email());
        fillTextView(R.id.tv_website, mUser.blog());
        fillTextView(R.id.tv_company, mUser.company());
        fillTextView(R.id.tv_location, mUser.location());

        loadTopRepositories(forceLoad);
        if (mUser.type() == UserType.User) {
            loadOrganizations(forceLoad);
        } else {
            fillOrganizations(null);
        }
    }

    private void fillCountIfUser(int layoutId, int countId, int count) {
        View layout = mContentView.findViewById(layoutId);
        if (mUser.type() == UserType.User) {
            TextView countView = mContentView.findViewById(countId);
            countView.setText(String.valueOf(count));
            layout.setOnClickListener(this);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fillTextView(int id, String text) {
        TextView view = mContentView.findViewById(id);
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
            if (mUser.type() == UserType.Organization) {
                intent = OrganizationMemberListActivity.makeIntent(getActivity(), mUserLogin);
            } else {
                intent = FollowerFollowingListActivity.makeIntent(getActivity(), mUserLogin, true);
            }
        } else if (id == R.id.cell_following) {
            intent = FollowerFollowingListActivity.makeIntent(getActivity(), mUserLogin, false);
        } else if (id == R.id.cell_repos || id == R.id.btn_repos) {
            intent = RepositoryListActivity.makeIntent(getActivity(), mUserLogin,
                    mUser.type() == UserType.Organization);
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
        LinearLayout ll = mContentView.findViewById(R.id.ll_top_repos);
        ll.removeAllViews();

        LayoutInflater inflater = getLayoutInflater();

        if (topRepos != null) {
            for (Repository repo : topRepos) {
                View rowView = inflater.inflate(R.layout.top_repo, null);
                rowView.setOnClickListener(this);
                rowView.setTag(repo);

                TextView tvTitle = rowView.findViewById(R.id.tv_title);
                tvTitle.setText(repo.owner().login() + "/" + repo.name());

                TextView tvDesc = rowView.findViewById(R.id.tv_desc);
                if (!StringUtils.isBlank(repo.description())) {
                    tvDesc.setVisibility(View.VISIBLE);
                    tvDesc.setText(repo.description());
                } else {
                    tvDesc.setVisibility(View.GONE);
                }

                TextView tvForks = rowView.findViewById(R.id.tv_forks);
                tvForks.setText(String.valueOf(repo.forksCount()));

                TextView tvStars = rowView.findViewById(R.id.tv_stars);
                tvStars.setText(String.valueOf(repo.watchersCount()));

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
        ViewGroup llOrgs = mContentView.findViewById(R.id.ll_orgs);
        LinearLayout llOrg = mContentView.findViewById(R.id.ll_org);
        int count = organizations != null ? organizations.size() : 0;
        LayoutInflater inflater = getLayoutInflater();

        llOrg.removeAllViews();
        llOrgs.setVisibility(count > 0 ? View.VISIBLE : View.GONE);

        for (int i = 0; i < count; i++) {
            User org = organizations.get(i);
            View rowView = inflater.inflate(R.layout.selectable_label_with_avatar, llOrg, false);

            rowView.setOnClickListener(this);
            rowView.setTag(org);

            ImageView avatar = rowView.findViewById(R.id.iv_gravatar);
            AvatarHandler.assignAvatar(avatar, org);

            TextView nameView = rowView.findViewById(R.id.tv_title);
            nameView.setText(org.login());

            llOrg.addView(rowView);
        }
    }

    public void updateFollowingAction() {
        if (mUser == null) {
            return;
        }

        mUser = mUser.toBuilder()
                .followers(mUser.followers() + (mIsFollowing ? 1 : -1))
                .build();
        TextView tvFollowersCount = mContentView.findViewById(R.id.tv_followers_count);
        tvFollowersCount.setText(String.valueOf(mUser.followers()));
    }

    private void toggleFollowingState() {
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, false);
        Single<Response<Void>> responseSingle = mIsFollowing
                ? service.unfollowUser(mUserLogin)
                : service.followUser(mUserLogin);
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    mIsFollowing = !mIsFollowing;
                    updateFollowingAction();
                    getActivity().invalidateOptionsMenu();
                }, error -> {
                    handleActionFailure("Toggling following state failed", error);
                    getActivity().invalidateOptionsMenu();
                });
    }

    private void loadUser(boolean force) {
        UserService service = ServiceFactory.get(UserService.class, force);
        service.getUser(mUserLogin)
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_USER, force))
                .subscribe(result -> {
                    mUser = result;
                    fillData(force);
                    setContentShown(true);
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);

    }

    private void loadTopRepositories(boolean force) {
        RepositoryService service = ServiceFactory.get(RepositoryService.class, force, null, null, 5);
        final Single<Response<Page<Repository>>> observable;

        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", "pushed");
        filterData.put("affiliation", "owner,collaborator");

        if (mIsSelf) {
            observable = service.getUserRepositories(filterData, 1);
        } else if (mUser.type() == UserType.Organization) {
            observable = service.getOrganizationRepositories(mUserLogin, filterData, 1);
        } else {
            observable = service.getUserRepositories(mUserLogin, filterData, 1);
        }

        mTopRepoSubscription = observable
                .map(ApiHelpers::throwOnFailure)
                .map(Page::items)
                .compose(makeLoaderSingle(ID_LOADER_REPO_LIST, force))
                .subscribe(this::fillTopRepos, this::handleLoadFailure);
    }

    private void loadOrganizations(boolean force) {
        final OrganizationService service = ServiceFactory.get(OrganizationService.class, force);
        mOrgListSubscription = ApiHelpers.PageIterator
                .toSingle(page -> mIsSelf
                        ? service.getMyOrganizations(page)
                        : service.getUserPublicOrganizations(mUserLogin, page)
                )
                .compose(makeLoaderSingle(ID_LOADER_ORG_LIST, force))
                .subscribe(this::fillOrganizations, this::handleLoadFailure);
    }

    private void loadIsFollowingState(boolean force) {
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, force);
        mIsFollowingSubscription = service.isFollowing(mUserLogin)
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_IS_FOLLOWING, force))
                .subscribe(result -> {
                    mIsFollowing = result;
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);
    }
}
