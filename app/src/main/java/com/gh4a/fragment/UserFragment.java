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
import com.gh4a.widget.OverviewRow;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;
import com.meisolsson.githubsdk.service.organizations.OrganizationMemberService;
import com.meisolsson.githubsdk.service.organizations.OrganizationService;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;
import com.meisolsson.githubsdk.service.users.UserFollowerService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Single;
import retrofit2.Response;

public class UserFragment extends LoadingFragmentBase implements View.OnClickListener {
    public static UserFragment newInstance(User user) {
        UserFragment f = new UserFragment();

        Bundle args = new Bundle();
        args.putParcelable("user", user);
        f.setArguments(args);

        return f;
    }

    private static final int ID_LOADER_REPO_LIST = 0;
    private static final int ID_LOADER_ORG_LIST = 1;
    private static final int ID_LOADER_IS_FOLLOWING = 2;
    private static final int ID_LOADER_ORG_MEMBER_COUNT = 3;

    private User mUser;
    private View mContentView;
    private Boolean mIsFollowing;
    private boolean mIsSelf;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUser = getArguments().getParcelable("user");
        mIsSelf = ApiHelpers.loginEquals(mUser, Gh4Application.get().getAuthLogin());
        setHasOptionsMenu(true);
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.user, parent, false);
        return mContentView;
    }

    @Override
    public void onRefresh() {
        mIsFollowing = false;
        loadTopRepositories(true);
        loadOrganizationsIfUser(true);
        loadOrganizationMemberCountIfOrg(true);
        loadIsFollowingStateIfNeeded(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        fillData();
        loadTopRepositories(false);
        loadOrganizationsIfUser(false);
        loadOrganizationMemberCountIfOrg(false);
        loadIsFollowingStateIfNeeded(false);

        setContentShown(true);
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

    private void fillData() {
        ImageView gravatar = mContentView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(gravatar, mUser);

        OverviewRow joinDateRow = mContentView.findViewById(R.id.join_date_row);
        if (mUser.createdAt() != null) {
            joinDateRow.setText(getString(R.string.user_created_at,
                    DateFormat.getMediumDateFormat(getActivity()).format(mUser.createdAt())));
            joinDateRow.setVisibility(View.VISIBLE);
        } else {
            joinDateRow.setVisibility(View.GONE);
        }

        boolean isUser = mUser.type() == UserType.User;
        OverviewRow membersRow = mContentView.findViewById(R.id.members_row);
        OverviewRow followersRow = mContentView.findViewById(R.id.followers_row);
        OverviewRow followingRow = mContentView.findViewById(R.id.following_row);

        followersRow.setVisibility(isUser ? View.VISIBLE : View.GONE);
        followingRow.setVisibility(isUser ? View.VISIBLE : View.GONE);
        membersRow.setVisibility(isUser ? View.GONE : View.VISIBLE);

        if (isUser) {
            followersRow.setText(getResources().getQuantityString(R.plurals.follower,
                    mUser.followers(), mUser.followers()));
            followersRow.setClickIntent(FollowerFollowingListActivity.makeIntent(
                    getActivity(), mUser.login(), true));
            followingRow.setText(getResources().getQuantityString(R.plurals.following,
                    mUser.following(), mUser.following()));
            followingRow.setClickIntent(FollowerFollowingListActivity.makeIntent(
                    getActivity(), mUser.login(), false));
        } else {
            membersRow.setClickIntent(OrganizationMemberListActivity.makeIntent(
                    getActivity(), mUser.login()));
        }

        OverviewRow gistsRow = mContentView.findViewById(R.id.gists_row);
        gistsRow.setVisibility(isUser ? View.VISIBLE : View.GONE);
        if (isUser) {
            int totalCount = orZero(mUser.publicGists()) + orZero(mUser.privateGists());
            gistsRow.setText(getResources().getQuantityString(R.plurals.gist, totalCount, totalCount));
            gistsRow.setClickIntent(GistListActivity.makeIntent(getActivity(), mUser.login()));
        }

        OverviewRow reposRow = mContentView.findViewById(R.id.repos_row);
        int repoCount = orZero(mUser.totalPrivateRepos()) + orZero(mUser.publicRepos());
        reposRow.setText(getResources().getQuantityString(R.plurals.repository, repoCount, repoCount));
        reposRow.setClickIntent(RepositoryListActivity.makeIntent(getActivity(), mUser.login(), !isUser));

        TextView tvName = mContentView.findViewById(R.id.tv_name);
        String name = StringUtils.isBlank(mUser.name()) ? mUser.login() : mUser.name();
        if (mUser.type() == UserType.Organization) {
            tvName.setText(getString(R.string.org_user_template, name));
        } else {
            tvName.setText(name);
        }

        fillTextView(R.id.tv_email, mUser.email());
        fillTextView(R.id.tv_website, mUser.blog());
        fillTextView(R.id.tv_company, mUser.company());
        fillTextView(R.id.tv_location, mUser.location());
    }

    private static int orZero(Integer count) {
        return count != null ? count : 0;
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

        if (id == R.id.btn_repos) {
            intent = RepositoryListActivity.makeIntent(getActivity(), mUser.login(),
                    mUser.type() == UserType.Organization);
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
        View progress = mContentView.findViewById(R.id.pb_top_repos);
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

        ll.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
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
        OverviewRow followersRow = mContentView.findViewById(R.id.followers_row);
        followersRow.setText(getResources().getQuantityString(R.plurals.follower,
                mUser.followers(), mUser.followers()));
    }

    private void toggleFollowingState() {
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, false);
        Single<Response<Void>> responseSingle = mIsFollowing
                ? service.unfollowUser(mUser.login())
                : service.followUser(mUser.login());
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

    private void loadTopRepositories(boolean force) {
        RepositoryService service = ServiceFactory.get(RepositoryService.class, force, null, null, 5);
        final Single<Response<Page<Repository>>> observable;

        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", "pushed");
        filterData.put("affiliation", "owner,collaborator");

        if (mIsSelf) {
            observable = service.getUserRepositories(filterData, 1);
        } else if (mUser.type() == UserType.Organization) {
            observable = service.getOrganizationRepositories(mUser.login(), filterData, 1);
        } else {
            observable = service.getUserRepositories(mUser.login(), filterData, 1);
        }

        observable.map(ApiHelpers::throwOnFailure)
                .map(Page::items)
                .compose(makeLoaderSingle(ID_LOADER_REPO_LIST, force))
                .doOnSubscribe(disposable -> {
                    mContentView.findViewById(R.id.pb_top_repos).setVisibility(View.VISIBLE);
                    mContentView.findViewById(R.id.ll_top_repos).setVisibility(View.GONE);
                })
                .subscribe(this::fillTopRepos, this::handleLoadFailure);
    }

    private void loadOrganizationsIfUser(boolean force) {
        if (mUser.type() != UserType.User) {
            fillOrganizations(null);
            return;
        }

        final OrganizationService service = ServiceFactory.get(OrganizationService.class, force);
        ApiHelpers.PageIterator
                .toSingle(page -> mIsSelf
                        ? service.getMyOrganizations(page)
                        : service.getUserPublicOrganizations(mUser.login(), page)
                )
                .compose(makeLoaderSingle(ID_LOADER_ORG_LIST, force))
                .subscribe(this::fillOrganizations, this::handleLoadFailure);
    }

    private void loadOrganizationMemberCountIfOrg(boolean force) {
        if (mUser.type() != UserType.Organization) {
            return;
        }
        final OrganizationMemberService service =
                ServiceFactory.get(OrganizationMemberService.class, force);
        ApiHelpers.PageIterator
                .toSingle(page -> service.getMembers(mUser.login(), page))
                .map(memberList -> memberList.size())
                .compose(makeLoaderSingle(ID_LOADER_ORG_MEMBER_COUNT, force))
                .subscribe(count -> {
                    OverviewRow membersRow = mContentView.findViewById(R.id.members_row);
                    membersRow.setText(getResources().getQuantityString(R.plurals.member, count, count));
                }, this::handleLoadFailure);
    }

    private void loadIsFollowingStateIfNeeded(boolean force) {
        if (mIsSelf || !Gh4Application.get().isAuthorized()) {
            return;
        }
        UserFollowerService service = ServiceFactory.get(UserFollowerService.class, force);
        service.isFollowing(mUser.login())
                .map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_IS_FOLLOWING, force))
                .subscribe(result -> {
                    mIsFollowing = result;
                    getActivity().invalidateOptionsMenu();
                }, this::handleLoadFailure);
    }
}
