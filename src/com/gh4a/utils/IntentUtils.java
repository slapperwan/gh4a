package com.gh4a.utils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.PullRequestListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.UserActivity;

public class IntentUtils {
    public static void openIssueActivity(Context context, String login, String repoName, int issueNumber) {
        openIssueActivity(context, login, repoName, issueNumber, null);
    }

    public static void openIssueActivity(Context context, String login, String repoName,
            int issueNumber, String state) {
        Intent intent = new Intent(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Issue.NUMBER, issueNumber);
        if (state != null) {
            intent.putExtra(Constants.Issue.STATE, state);
        }
        context.startActivity(intent);
    }

    public static void openIssueActivity(Context context, String login, String repoName,
            int issueNumber, int flags) {
        Intent intent = new Intent(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Issue.NUMBER, issueNumber);
        intent.setFlags(flags);
        context.startActivity(intent);
    }

    public static void openIssueListActivity(Context context, String repoOwner, String repoName,
            String state) {
        openIssueListActivity(context, repoOwner, repoName, state, 0);
    }

    public static void openIssueListActivity(Context context, String repoOwner, String repoName,
            String state, int flags) {
        Intent intent = new Intent(context, IssueListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Issue.STATE, state);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void openRepositoryInfoActivity(Context context, Repository repository) {
        if (repository != null) {
            Intent intent = new Intent(context, RepositoryActivity.class);
            // ideally, we'd be able to just pass the repository object into RepositoryActivity
            // and wouldn't need any additional network activity there. Unfortunately, the
            // repository objects returned from other API calls are incomplete :(
            intent.putExtra(Constants.Repository.OWNER, repository.getOwner().getLogin());
            intent.putExtra(Constants.Repository.NAME, repository.getName());
            context.startActivity(intent);
        } else {
            ToastUtils.notFoundMessage(context, R.plurals.repository);
        }
    }

    public static void openRepositoryInfoActivity(Context context, String repoOwner, String repoName,
            String ref, int flags) {
        Intent intent = new Intent(context, RepositoryActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Repository.SELECTED_REF, ref);
        intent.putExtra(Constants.Repository.SELECTED_BRANCHTAG_NAME, ref);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void openUserInfoActivity(Context context, String login) {
        openUserInfoActivity(context, login, null, 0);
    }

    public static void openUserInfoActivity(Context context, String login, String name) {
        openUserInfoActivity(context, login, name, 0);
    }

    public static void openUserInfoActivity(Context context, String login, String name, int flags) {
        if (login == null) {
            return;
        }
        Intent intent = new Intent(context, UserActivity.class);
        intent.putExtra(Constants.User.LOGIN, login);
        intent.putExtra(Constants.User.NAME, name);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void openUserInfoActivity(Context context, User user) {
        if (user == null) {
            return;
        }
        openUserInfoActivity(context, user.getLogin(), user.getName());
    }

    public static void openCommitInfoActivity(Context context, String login,
            String repoName, String sha, int flags) {
        Intent intent = new Intent(context, CommitActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, sha);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void openFileViewerActivity(Context context, String login, String repoName,
            String sha, String fullPath, String fileName) {
        Intent intent = new Intent(context, FileViewerActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Object.PATH, fullPath);
        intent.putExtra(Constants.Object.REF, sha);
        intent.putExtra(Constants.Object.NAME, fileName);
        intent.putExtra(Constants.Object.OBJECT_SHA, sha);
        context.startActivity(intent);
    }

    public static void openPullRequestActivity(Context context, String repoOwner, String repoName,
            int pullRequestNumber) {
        Intent intent = new Intent(context, PullRequestActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.PullRequest.NUMBER, pullRequestNumber);
        context.startActivity(intent);
    }

    public static void openPullRequestListActivity(Context context, String repoOwner,
                String repoName, String state) {
        openPullRequestListActivity(context, repoOwner, repoName, state, 0);
    }

    public static void openPullRequestListActivity(Context context, String repoOwner,
            String repoName, String state, int flags) {
        Intent intent = new Intent(context, PullRequestListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.PullRequest.STATE, state);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

    public static void openGistActivity(Context context, String userLogin, String gistId, int flags) {
        Intent intent = new Intent(context, GistActivity.class);
        intent.putExtra(Constants.User.LOGIN, userLogin);
        intent.putExtra(Constants.Gist.ID, gistId);
        if (flags != 0) {
            intent.setFlags(flags);
        }
        context.startActivity(intent);
    }

}
