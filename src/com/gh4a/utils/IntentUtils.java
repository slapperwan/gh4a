package com.gh4a.utils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        openRepositoryInfoActivity(context, repoOwner, repoName, ref,
                RepositoryActivity.PAGE_REPO_OVERVIEW, flags);
    }

    public static void openRepositoryInfoActivity(Context context, String repoOwner, String repoName,
            String ref, int initialPage, int flags) {
        Intent intent = new Intent(context, RepositoryActivity.class);
        if (TextUtils.isEmpty(ref)) {
            ref = null;
        }
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Repository.SELECTED_REF, ref);
        intent.putExtra(Constants.Repository.SELECTED_BRANCHTAG_NAME, ref);
        intent.putExtra(RepositoryActivity.EXTRA_INITIAL_PAGE, initialPage);
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

    public static void launchBrowser(Context context, Uri uri) {
        Intent intent = createBrowserIntent(context, uri);
        if (intent != null) {
            context.startActivity(intent);
        } else {
            ToastUtils.showMessage(context, R.string.no_browser_found);
        }
    }

    // We want to forward the URI to a browser, but our own intent filter matches
    // the browser's intent filters. We therefore resolve the intent by ourselves,
    // strip our own entry from the list and pass the result to the system's
    // activity chooser.
    private static Intent createBrowserIntent(Context context, Uri uri) {
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri)
                .addCategory(Intent.CATEGORY_BROWSABLE);
        final PackageManager pm = context.getPackageManager();
        final List<ResolveInfo> activities = pm.queryIntentActivities(browserIntent,
                PackageManager.MATCH_DEFAULT_ONLY);
        final ArrayList<Intent> chooserIntents = new ArrayList<Intent>();
        final String ourPackageName = context.getPackageName();

        Collections.sort(activities, new ResolveInfo.DisplayNameComparator(pm));

        for (ResolveInfo resInfo : activities) {
            ActivityInfo info = resInfo.activityInfo;
            if (!info.enabled || !info.exported) {
                continue;
            }
            if (info.packageName.equals(ourPackageName)) {
                continue;
            }

            Intent targetIntent = new Intent(browserIntent);
            targetIntent.setPackage(info.packageName);
            chooserIntents.add(targetIntent);
        }

        if (chooserIntents.isEmpty()) {
            return null;
        }

        final Intent lastIntent = chooserIntents.remove(chooserIntents.size() - 1);
        if (chooserIntents.isEmpty()) {
            // there was only one, no need to show the chooser
            return lastIntent;
        }

        Intent chooserIntent = Intent.createChooser(lastIntent, null);
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                chooserIntents.toArray(new Intent[chooserIntents.size()]));
        return chooserIntent;
    }
}
