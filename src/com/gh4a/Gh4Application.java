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

package com.gh4a;

import java.util.ArrayList;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Repository;
import org.ocpsoft.pretty.time.PrettyTime;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

/**
 * The Class Gh4Application.
 */
public class Gh4Application extends Application {

    public Typeface boldCondensed;
    public Typeface condensed;
    public Typeface regular;
    public Typeface italic;
    public static int THEME;
    
    /*
     * (non-Javadoc)
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        
        boldCondensed = Typeface.createFromAsset(getAssets(), "fonts/Roboto-BoldCondensed.ttf");
        condensed = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Condensed.ttf");
        regular = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
        italic = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Italic.ttf");
        
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        THEME = sharedPreferences.getInt("THEME", R.style.DefaultTheme);
    }

    /** The Constant pt. */
    public static final PrettyTime pt = new PrettyTime();

    /**
     * Populate Repository into Bundle.
     * 
     * @param repository the repository
     * @return the bundle
     */
    public Bundle populateRepository(Repository repository) {
        Bundle data = new Bundle();
        
        data.putString(Constants.Repository.REPO_OWNER, repository.getOwner().getLogin());
        data.putString(Constants.Repository.REPO_NAME, repository.getName());
        data.putString(Constants.Repository.REPO_DESC, repository.getDescription());
        data.putString(Constants.Repository.REPO_URL, repository.getHtmlUrl());
        data.putInt(Constants.Repository.REPO_WATCHERS, repository.getWatchers());
        data.putInt(Constants.Repository.REPO_FORKS, repository.getForks());
        data.putString(Constants.Repository.REPO_HOMEPAGE, repository.getHomepage());
        data.putString(Constants.Repository.REPO_CREATED, pt.format(repository.getCreatedAt()));
        data.putString(Constants.Repository.REPO_LANGUANGE,
                repository.getLanguage() != null ? repository.getLanguage() : null);
        data.putInt(Constants.Repository.REPO_OPEN_ISSUES, repository.getOpenIssues());
        data.putLong(Constants.Repository.REPO_SIZE, repository.getSize());
        data.putString(Constants.Repository.REPO_PUSHED, pt.format(repository.getPushedAt()));
        data.putBoolean(Constants.Repository.REPO_IS_FORKED, repository.isFork());
        data.putString(Constants.Repository.REPO_PARENT, repository.getParent() != null ?
                repository.getParent().getOwner().getLogin() + "/" + repository.getParent().getName() : "");
        data.putString(Constants.Repository.REPO_SOURCE, repository.getSource() != null ?
                repository.getSource().getOwner().getLogin() + "/" + repository.getSource().getName() : "");
        data.putBoolean(Constants.Repository.REPO_HAS_ISSUES, repository.isHasIssues());
        data.putBoolean(Constants.Repository.REPO_HAS_WIKI, repository.isHasWiki());

        return data;
    }

    /**
     * Populate Issue into Bunlde.
     * 
     * @param issue the issue
     * @return the bundle
     */
    public Bundle populateIssue(Issue issue) {
        PrettyTime pt = new PrettyTime();
        Bundle data = new Bundle();

        data.putInt(Constants.Issue.ISSUE_NUMBER, issue.getNumber());
        data.putString(Constants.Issue.ISSUE_TITLE, issue.getTitle());
        data.putString(Constants.Issue.ISSUE_CREATED_AT, pt.format(issue.getCreatedAt()));
        data.putString(Constants.Issue.ISSUE_CREATED_BY, issue.getUser().getLogin());
        data.putString(Constants.Issue.ISSUE_STATE, issue.getState());
        data.putString(Constants.Issue.ISSUE_BODY, issue.getBody());
        data.putInt(Constants.Issue.ISSUE_COMMENTS, issue.getComments());
        data.putString(Constants.GRAVATAR_ID, issue.getUser().getGravatarId());
        data.putString(Constants.Issue.PULL_REQUEST_URL, issue.getPullRequest().getUrl());
        data.putString(Constants.Issue.PULL_REQUEST_DIFF_URL, issue.getPullRequest().getDiffUrl());
        if (issue.getAssignee() != null) {
            data.putString(Constants.Issue.ISSUE_ASSIGNEE, issue.getAssignee().getLogin());
        }
        
        //TODO: use Label object
        ArrayList<String> labels = new ArrayList<String>();
        for (Label label: issue.getLabels()) {
            labels.add(label.getName());
        }
        data.putStringArrayList(Constants.Issue.ISSUE_LABELS, labels);
        return data;
    }

    /**
     * Open issue activity.
     * 
     * @param context the context
     * @param login the login
     * @param repoName the repo name
     * @param issueNumber the issue number
     */
    public void openIssueActivity(Context context, String login, String repoName, int issueNumber) {
        Intent intent = new Intent().setClass(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, login);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issueNumber);
        context.startActivity(intent);
    }
    
    /**
     * Open issue activity.
     * 
     * @param context
     * @param login
     * @param repoName
     * @param issueNumber
     * @param state
     */
    public void openIssueActivity(Context context, String login, String repoName, int issueNumber,
            String state) {
        Intent intent = new Intent().setClass(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, login);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issueNumber);
        intent.putExtra(Constants.Issue.ISSUE_STATE, state);
        context.startActivity(intent);
    }
    
    /**
     * Open issue activity with intent  flags
     * @param context
     * @param login
     * @param repoName
     * @param issueNumber
     */
    public void openIssueActivity(Context context, String login, String repoName, 
            int issueNumber, int flags) {
        Intent intent = new Intent().setClass(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, login);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issueNumber);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    /**
     * Open issue list activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     * @param state the state
     */
    public void openIssueListActivity(Context context, String repoOwner, String repoName,
            String state) {
        Intent intent = new Intent().setClass(context, IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, state);
        context.startActivity(intent);
    }
    
    public void openIssueListActivity(Context context, String repoOwner, String repoName,
            String state, int flags) {
        Intent intent = new Intent().setClass(context, IssueListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, state);
        intent.setFlags(flags);
        context.startActivity(intent);
    }
    
    /**
     * Open repository activity.
     * 
     * @param context the context
     * @param repository the repository
     */
    public void openRepositoryInfoActivity(Context context, Repository repository) {
        if (repository != null) {
            Intent intent = new Intent().setClass(context, RepositoryActivity.class);
            Bundle data = populateRepository(repository);
            intent.putExtra(Constants.DATA_BUNDLE, data);
            context.startActivity(intent);
        }
        else {
            notFoundMessage(getApplicationContext(), R.plurals.repository);
        }
    }

    /**
     * Open repository activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     */
    public void openRepositoryInfoActivity(Context context, String repoOwner, String repoName, int flags) {
        Intent intent = new Intent().setClass(context, RepositoryActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    /**
     * Open user activity.
     * 
     * @param context the context
     * @param login the login
     * @param name the name
     */
    public void openUserInfoActivity(Context context, String login, String name) {
        Intent intent = new Intent().setClass(context, UserActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, login);
        intent.putExtra(Constants.User.USER_NAME, name);
        context.startActivity(intent);
    }
    
    /**
     * Open user info activity.
     *
     * @param context the context
     * @param login the login
     * @param name the name
     * @param flags the flags
     */
    public void openUserInfoActivity(Context context, String login, String name, int flags) {
        Intent intent = new Intent().setClass(context, UserActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, login);
        intent.putExtra(Constants.User.USER_NAME, name);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    /**
     * Open commit activity.
     * 
     * @param context the context
     * @param login the login
     * @param repoName the repo name
     * @param sha the sha
     */
    public void openCommitInfoActivity(Context context, String login, String repoName, String sha, int flags) {
        Intent intent = new Intent().setClass(context, CommitActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, login);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, sha);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    /**
     * Open commit list activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     * @param branchName the branch name
     * @param sha the sha
     * @param viewId the view id
     */
    public void openCommitListActivity(Context context, String repoOwner, String repoName,
            String branchName, String sha, int viewId) {
        Intent intent = new Intent().setClass(context, CommitListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.Repository.REPO_BRANCH, branchName);
        intent.putExtra(Constants.Object.TREE_SHA, sha);
        intent.putExtra(Constants.VIEW_ID, viewId);
        context.startActivity(intent);
    }

    /**
     * Open pull request activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     * @param pullRequestNumber the pull request number
     */
    public void openPullRequestActivity(Context context, String repoOwner, String repoName,
            int pullRequestNumber) {
        Intent intent = new Intent().setClass(context, PullRequestActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.PullRequest.NUMBER, pullRequestNumber);
        context.startActivity(intent);
    }

    /**
     * Open pull request list activity.
     * 
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     */
    public void openPullRequestListActivity(Context context, String repoOwner, String repoName, String state) {
        Intent intent = new Intent().setClass(context, PullRequestListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.PullRequest.STATE, state);
        context.startActivity(intent);
    }
    
    public void openPullRequestListActivity(Context context, String repoOwner, String repoName, String state,
            int flags) {
        Intent intent = new Intent().setClass(context, PullRequestListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.PullRequest.STATE, state);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    /**
     * Open branch list activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     * @param buttonId the button id
     */
    public void openBranchListActivity(Context context, String repoOwner, String repoName,
            int buttonId) {
        Intent intent = new Intent().setClass(context, BranchListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.VIEW_ID, buttonId);

        context.startActivity(intent);
    }

    /**
     * Open tag list activity.
     *
     * @param context the context
     * @param repoOwner the repo owner
     * @param repoName the repo name
     * @param buttonId the button id
     */
    public void openTagListActivity(Context context, String repoOwner, String repoName, int buttonId) {
        Intent intent = new Intent().setClass(context, TagListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
        intent.putExtra(Constants.VIEW_ID, buttonId);

        context.startActivity(intent);
    }

    /**
     * Open browser.
     * 
     * @param context the context
     * @param url the url
     */
    public void openBrowser(Context context, String url) {
        Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(url));
        context.startActivity(browserIntent);
    }

    public void openGistActivity(Context context, String userLogin, String gistId, int flags) {
        Intent intent = new Intent().setClass(context, GistActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, userLogin);
        intent.putExtra(Constants.Gist.ID, gistId);
        if (flags != 0) {
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        context.startActivity(intent);
    }
    
    /**
     * Not found message.
     * 
     * @param context the context
     * @param pluralsId the plurals id
     */
    public void notFoundMessage(Context context, int pluralsId) {
        Resources res = context.getResources();
        Toast.makeText(
                context,
                String.format(res.getString(R.string.record_not_found), res.getQuantityString(
                        pluralsId, 1)), Toast.LENGTH_SHORT).show();
    }

    /**
     * Not found message.
     * 
     * @param context the context
     * @param object the object
     */
    public void notFoundMessage(Context context, String object) {
        Resources res = context.getResources();
        Toast.makeText(context, String.format(res.getString(R.string.record_not_found), object),
                Toast.LENGTH_SHORT).show();
    }
    
    public String getAuthLogin() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences != null) {
            if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null){
                return sharedPreferences.getString(Constants.User.USER_LOGIN, null);
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
    }
    
    public String getAuthToken() {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, Context.MODE_PRIVATE);
        String token = sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null);
        return token;
    }
    
}
