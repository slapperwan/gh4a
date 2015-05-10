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
    public static Intent getIssueActivityIntent(Context context, String login, String repoName,
            int issueNumber) {
        Intent intent = new Intent(context, IssueActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Issue.NUMBER, issueNumber);
        return intent;
    }

    public static Intent getIssueListActivityIntent(Context context, String repoOwner,
            String repoName, String state) {
        Intent intent = new Intent(context, IssueListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Issue.STATE, state);
        return intent;
    }

    public static void openRepositoryInfoActivity(Context context, Repository repository) {
        if (repository != null) {
            // ideally, we'd be able to just pass the repository object into RepositoryActivity
            // and wouldn't need any additional network activity there. Unfortunately, the
            // repository objects returned from other API calls are incomplete :(
            Intent intent = getRepoActivityIntent(context, repository.getOwner().getLogin(),
                    repository.getName(), null);
            context.startActivity(intent);
        } else {
            ToastUtils.notFoundMessage(context, R.string.repository);
        }
    }

    public static Intent getRepoActivityIntent(Context context, Repository repo) {
        return getRepoActivityIntent(context, repo.getOwner().getLogin(), repo.getName(), null);
    }

    public static Intent getRepoActivityIntent(Context context,
            String repoOwner, String repoName, String ref) {
        return getRepoActivityIntent(context, repoOwner, repoName, ref,
                RepositoryActivity.PAGE_REPO_OVERVIEW);
    }

    public static Intent getRepoActivityIntent(Context context,
            String repoOwner, String repoName, String ref, int initialPage) {
        Intent intent = new Intent(context, RepositoryActivity.class);
        if (TextUtils.isEmpty(ref)) {
            ref = null;
        }
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Repository.SELECTED_REF, ref);
        intent.putExtra(RepositoryActivity.EXTRA_INITIAL_PAGE, initialPage);
        return intent;
    }

    public static Intent getUserActivityIntent(Context context, String login) {
        return getUserActivityIntent(context, login, null);
    }

    public static Intent getUserActivityIntent(Context context, String login, String name) {
        if (login == null) {
            return null;
        }
        Intent intent = new Intent(context, UserActivity.class);
        intent.putExtra(Constants.User.LOGIN, login);
        intent.putExtra(Constants.User.NAME, name);
        return intent;
    }

    public static Intent getUserActivityIntent(Context context, User user) {
        if (user == null) {
            return null;
        }
        return getUserActivityIntent(context, user.getLogin(), user.getName());
    }

    public static Intent getCommitInfoActivityIntent(Context context, String login,
            String repoName, String sha) {
        Intent intent = new Intent(context, CommitActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Object.OBJECT_SHA, sha);
        return intent;
    }

    public static Intent getFileViewerActivityIntent(Context context, String login, String repoName,
            String ref, String fullPath) {
        Intent intent = new Intent(context, FileViewerActivity.class);
        intent.putExtra(Constants.Repository.OWNER, login);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.Object.PATH, fullPath);
        intent.putExtra(Constants.Object.REF, ref);
        return intent;
    }

    public static Intent getPullRequestActivityIntent(Context context, String repoOwner,
            String repoName, int pullRequestNumber) {
        Intent intent = new Intent(context, PullRequestActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.PullRequest.NUMBER, pullRequestNumber);
        return intent;
    }

    public static Intent getPullRequestListActivityIntent(Context context, String repoOwner,
                String repoName, String state) {
        Intent intent = new Intent(context, PullRequestListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, repoOwner);
        intent.putExtra(Constants.Repository.NAME, repoName);
        intent.putExtra(Constants.PullRequest.STATE, state);
        return intent;
    }

    public static Intent getGistActivityIntent(Context context, String userLogin, String gistId) {
        Intent intent = new Intent(context, GistActivity.class);
        intent.putExtra(Constants.User.LOGIN, userLogin);
        intent.putExtra(Constants.Gist.ID, gistId);
        return intent;
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
        final ArrayList<Intent> chooserIntents = new ArrayList<>();
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
