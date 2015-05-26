package com.gh4a.utils;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.R;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;

public class CommitUtils {

    //RepositoryCommit
    public static String getAuthorName(Context context, RepositoryCommit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getLogin();
        }
        if (commit.getCommit().getAuthor() != null) {
            return commit.getCommit().getAuthor().getName();
        }
        return context.getString(R.string.unknown);
    }

    public static String getAuthorLogin(RepositoryCommit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getLogin();
        }
        return null;
    }

    public static String getCommitterName(Context context, RepositoryCommit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getLogin();
        }
        if (commit.getCommit().getCommitter() != null) {
            return commit.getCommit().getCommitter().getName();
        }
        return context.getString(R.string.unknown);
    }

    public static boolean authorEqualsCommitter(RepositoryCommit commit) {
        if (commit.getCommitter() != null && commit.getAuthor() != null) {
            return TextUtils.equals(commit.getCommitter().getLogin(), commit.getAuthor().getLogin());
        }

        CommitUser author = commit.getCommit().getAuthor();
        CommitUser committer = commit.getCommit().getCommitter();
        if (author.getEmail() != null && committer.getEmail() != null) {
            return TextUtils.equals(author.getEmail(), committer.getEmail());
        }
        return TextUtils.equals(author.getName(), committer.getName());
    }

    public static String getUserLogin(Context context, User user) {
        if (user != null && user.getLogin() != null) {
            return user.getLogin();
        }
        return context.getString(R.string.unknown);
    }
}