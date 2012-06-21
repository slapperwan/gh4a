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

import org.eclipse.egit.github.core.User;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.FollowerFollowingListActivity;
import com.gh4a.GistListActivity;
import com.gh4a.OrganizationListActivity;
import com.gh4a.OrganizationMemberListActivity;
import com.gh4a.PublicRepoListActivity;
import com.gh4a.PushableRepoListActivity;
import com.gh4a.R;
import com.gh4a.UserNewActivity;
import com.gh4a.UserPublicActivity;
import com.gh4a.UserYourActionsActivity;
import com.gh4a.WatchedRepoListActivity;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;

public class UserFragment extends SherlockFragment implements 
    OnClickListener, OnItemClickListener, LoaderManager.LoaderCallbacks<User> {

    private String mUserLogin;
    private String mUserName;
    private User mUser;

    public static UserFragment newInstance(String login, String name) {
        UserFragment f = new UserFragment();

        Bundle args = new Bundle();
        args.putString(Constants.User.USER_LOGIN, login);
        args.putString(Constants.User.USER_NAME, name);
        f.setArguments(args);
        
        return f;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserLogin = getArguments().getString(Constants.User.USER_LOGIN);
        mUserName = getArguments().getString(Constants.User.USER_NAME);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.user, container, false);
        return v;
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        getLoaderManager().initLoader(0, null, this);
        getLoaderManager().getLoader(0).forceLoad();
    }
    
    private void fillData() {
        View v = getView();
        ImageView ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(mUser.getGravatarId(), ivGravatar, 80);

        TextView tvName = (TextView) v.findViewById(R.id.tv_name);
        TextView tvCreated = (TextView) v.findViewById(R.id.tv_created_at);

        RelativeLayout rlNewsFeed = (RelativeLayout) v.findViewById(R.id.rl_news_feed);
        if (mUserLogin.equals(((UserNewActivity)getActivity()).getAuthLogin())) {
            ImageButton btnNews = (ImageButton) v.findViewById(R.id.btn_news);
            btnNews.setOnClickListener(this);
            rlNewsFeed.setVisibility(View.VISIBLE);
            
            TextView tvPublicActivity = (TextView) v.findViewById(R.id.tv_pub_activity_label);
            tvPublicActivity.setText(R.string.user_your_actions);
        }
        else {
            rlNewsFeed.setVisibility(View.GONE);
        }
        
        ImageButton btnPublicActivity = (ImageButton) v.findViewById(R.id.btn_public_activity);
        btnPublicActivity.setOnClickListener(this);
        
//        Button btnYourActions = (Button) findViewById(R.id.btn_your_actions);
//        
//        if (mUserLogin.equals(getAuthUsername())) {
//            btnYourActions.setOnClickListener(this);
//            btnYourActions.setVisibility(View.VISIBLE);
//        }
//        else {
//            btnYourActions.setVisibility(View.GONE);
//        }
        
        Button btnPublicRepos = (Button) v.findViewById(R.id.btn_pub_repos);
        btnPublicRepos.setOnClickListener(this);

        RelativeLayout rlPushableRepos = (RelativeLayout) v.findViewById(R.id.rl_pushable_repos);
        if (mUserLogin.equals(((UserNewActivity) getActivity()).getAuthLogin())) {
            Button btnPushableRepos = (Button) v.findViewById(R.id.btn_pushable_repos);
            btnPushableRepos.setOnClickListener(this);
            rlPushableRepos.setVisibility(View.VISIBLE);
        }
        else {
            rlPushableRepos.setVisibility(View.GONE);
        }
        
        Button btnWatchedRepos = (Button) v.findViewById(R.id.btn_watched_repos);
        btnWatchedRepos.setOnClickListener(this);

        Button btnFollowers = (Button) v.findViewById(R.id.btn_followers);
        btnFollowers.setOnClickListener(this);
        TextView tvFollowers = (TextView) v.findViewById(R.id.tv_followers_label);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            tvFollowers.setText(R.string.user_followers);
        }
        else {
            tvFollowers.setText(R.string.user_members);
        }
        
        //hide following if organization
        RelativeLayout rlFollowing = (RelativeLayout) v.findViewById(R.id.rl_following);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            Button btnFollowing = (Button) v.findViewById(R.id.btn_following);
            btnFollowing.setText(String.valueOf(mUser.getFollowing()));
            btnFollowing.setOnClickListener(this);
            rlFollowing.setVisibility(View.VISIBLE);
        }
        else {
            rlFollowing.setVisibility(View.GONE);
        }
        
        //hide organizations if organization
        RelativeLayout rlOrganizations = (RelativeLayout) v.findViewById(R.id.rl_organizations);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            ImageButton btnOrganizations = (ImageButton) v.findViewById(R.id.btn_organizations);
            btnOrganizations.setOnClickListener(this);
            //registerForContextMenu(btnOrganizations);
            rlOrganizations.setVisibility(View.VISIBLE);
        }
        else {
            rlOrganizations.setVisibility(View.GONE);
        }
        
        RelativeLayout rlGists = (RelativeLayout) v.findViewById(R.id.rl_gists);
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            ImageButton btnGists = (ImageButton) v.findViewById(R.id.btn_gists);
            btnGists.setOnClickListener(this);
            btnGists.setVisibility(View.VISIBLE);
        }
        else {
            rlGists.setVisibility(View.GONE);
        }

        tvName.setText(StringUtils.formatName(mUser.getLogin(), mUser.getName()));
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            tvName.append(" (");
            tvName.append(Constants.User.USER_TYPE_ORG);
            tvName.append(")");
        }
        tvCreated.setText(mUser.getCreatedAt() != null ? 
                getResources().getString(R.string.user_created_at,
                        StringUtils.formatDate(mUser.getCreatedAt())) : "");

        //show email row if not blank
        TextView tvEmail = (TextView) v.findViewById(R.id.tv_email);
        if (!StringUtils.isBlank(mUser.getEmail())) {
            tvEmail.setText(mUser.getEmail());
            tvEmail.setVisibility(View.VISIBLE);
        }
        else {
            tvEmail.setVisibility(View.GONE);
        }
        
        //show website if not blank
        TextView tvWebsite = (TextView) v.findViewById(R.id.tv_website);
        if (!StringUtils.isBlank(mUser.getBlog())) {
            tvWebsite.setText(mUser.getBlog());
            tvWebsite.setVisibility(View.VISIBLE);
        }
        else {
            tvWebsite.setVisibility(View.GONE);
        }
        
        //show company if not blank
        TextView tvCompany = (TextView) v.findViewById(R.id.tv_company);
        if (!StringUtils.isBlank(mUser.getCompany())) {
            tvCompany.setText(mUser.getCompany());
            tvCompany.setVisibility(View.VISIBLE);
        }
        else {
            tvCompany.setVisibility(View.GONE);
        }
        
        //Show location if not blank
        TextView tvLocation = (TextView) v.findViewById(R.id.tv_location);
        if (!StringUtils.isBlank(mUser.getLocation())) {
            tvLocation.setText(mUser.getLocation());
            tvLocation.setVisibility(View.VISIBLE);
        }
        else {
            tvLocation.setVisibility(View.GONE);
        }
        
        btnPublicRepos.setText(String.valueOf(mUser.getPublicRepos() + mUser.getTotalPrivateRepos()));
        
        if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
            btnFollowers.setText(String.valueOf(mUser.getFollowers()));
            ProgressBar progressBar = (ProgressBar) v.findViewById(R.id.pb_followers);
            progressBar.setVisibility(View.GONE);
        }
        
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
        int id = view.getId();

        switch (id) {
        case R.id.btn_news:
            getFeeds(view);
            break;
        case R.id.btn_public_activity:
            getPublicActivities(view);
            break;
//        case R.id.btn_private_activity:
//            getPrivateActivities(view);
//            break;
        case R.id.btn_pub_repos:
            getPublicRepos(view);
            break;
        case R.id.btn_followers:
            getFollowers(view);
            break;
        case R.id.btn_following:
            getFollowing(view);
            break;
        case R.id.btn_watched_repos:
            getWatchedRepos(view);
            break;
        case R.id.btn_organizations:
            getOrganizations(view);
            break;
        case R.id.btn_gists:
            getGists(view);
            break;
        case R.id.btn_pushable_repos:
            getPushableRepos(view);
            break;
          
        default:
            break;
        }
    }

    /**
     * Gets the feeds when Activity button clicked.
     *
     * @param view the view
     * @return the feeds
     */
    public void getFeeds(View view) {
//        Intent intent = new Intent().setClass(this, UserPrivateActivity.class);
//        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
//        intent.putExtra(Constants.User.USER_TYPE, mUser.getType());
//        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin
//                + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : ""));
//        intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_news_feed));
//        startActivity(intent);
        
        Intent intent = new Intent().setClass(this.getActivity(), UserNewActivity.class);
        startActivity(intent);
    }

    /**
     * Gets the your actions.
     *
     * @param view the view
     * @return the your actions
     */
    public void getPublicActivities(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), UserPublicActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin);
        if (mUserLogin.equals(((UserNewActivity) getActivity()).getAuthLogin())) {
            intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_your_actions));
        }
        else {
            intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_public_activity));
        }
        startActivity(intent);
    }
    
    public void getPrivateActivities(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), UserYourActionsActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
    }
    
    /**
     * Gets the public repos when Public Repository button clicked.
     *
     * @param view the view
     * @return the public repos
     */
    public void getPublicRepos(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), PublicRepoListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        intent.putExtra(Constants.User.USER_TYPE, mUser.getType());
        startActivity(intent);
    }

    /**
     * Gets the followers when Followers button clicked.
     *
     * @param view the view
     * @return the followers
     */
    public void getFollowers(View view) {
        if (Constants.User.USER_TYPE_ORG.equals(mUser.getType())) {
            Intent intent = new Intent().setClass(this.getActivity(), OrganizationMemberListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            startActivity(intent);
        }
        else {
            Intent intent = new Intent().setClass(this.getActivity(), FollowerFollowingListActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            if (Constants.User.USER_TYPE_USER.equals(mUser.getType())) {
                intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_followers));
            }
            else {
                intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_members));
            }
            intent.putExtra(Constants.FIND_FOLLOWER, true);
            startActivity(intent);
        }
    }

    /**
     * Gets the following when Following button clicked.
     *
     * @param view the view
     * @return the following
     */
    public void getFollowing(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), FollowerFollowingListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin
                + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : ""));
        intent.putExtra(Constants.SUBTITLE, getResources().getString(R.string.user_following));
        intent.putExtra(Constants.FIND_FOLLOWER, false);
        startActivity(intent);
    }

    /**
     * Gets the watched repos.
     * 
     * @param view the view
     * @return the watched repos
     */
    public void getWatchedRepos(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), WatchedRepoListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        startActivity(intent);
    }
    
    /**
     * Gets the pushable repos.
     *
     * @param view the view
     * @return the pushable repos
     */
    public void getPushableRepos(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), PushableRepoListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        startActivity(intent);
    }
    
    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        
    }
    
    public void getOrganizations(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), OrganizationListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        startActivity(intent);
    }
    
    public void getGists(View view) {
        Intent intent = new Intent().setClass(this.getActivity(), GistListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        startActivity(intent);
    }
    
    public boolean onContextItemSelected(MenuItem item) {
        String orgLogin = item.getTitle().toString();
        return true;
    }
    
    @Override
    public Loader<User> onCreateLoader(int id, Bundle args) {
        return new UserLoader(getSherlockActivity(), mUserLogin);
    }
    
    @Override
    public void onLoadFinished(Loader<User> loader, User user) {
        if (user != null) {
            this.mUser = user;
            fillData();
        }
    }
    
    @Override
    public void onLoaderReset(Loader<User> arg0) {
        // TODO Auto-generated method stub
        
    }
}