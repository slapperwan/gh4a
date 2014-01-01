package com.gh4a.utils;

import android.text.TextUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.RepositoryCommit;

import com.gh4a.R;

import android.content.Context;

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
    
    public static String getAuthorLogin(Context context, RepositoryCommit commit) {
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
    
    public static String getCommitterLogin(Context context, RepositoryCommit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getLogin();
        }
        return null;
    }
    
    public static String getAuthorGravatarId(Context context, RepositoryCommit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getGravatarId();
        }
        CommitUser author = commit.getCommit().getAuthor();
        if (author != null && author.getEmail() != null) {
            return StringUtils.md5Hex(author.getEmail());
        }
        return null;
    }
    
    public static String getCommitterGravatarId(Context context, RepositoryCommit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getGravatarId();
        }
        CommitUser committer = commit.getCommit().getCommitter();
        if (committer != null && committer.getEmail() != null) {
            return StringUtils.md5Hex(committer.getEmail());
        }
        return null;
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

    //Commit
    public static String getAuthorName(Context context, Commit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getName();
        }
        return context.getString(R.string.unknown);
    }
    
    public static String getCommitterName(Context context, Commit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getName();
        }
        return context.getString(R.string.unknown);
    }
    
    public static String getAuthorEmail(Context context, Commit commit) {
        if (commit.getAuthor() != null) {
            return commit.getAuthor().getEmail();
        }
        return null;
    }
    
    public static String getCommitterEmail(Context context, Commit commit) {
        if (commit.getCommitter() != null) {
            return commit.getCommitter().getEmail();
        }
        return null;
    }
}