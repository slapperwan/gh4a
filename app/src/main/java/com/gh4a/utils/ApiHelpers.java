package com.gh4a.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;

import com.gh4a.R;

import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ApiHelpers {
    public interface IssueState {
        String OPEN = "open";
        String CLOSED = "closed";
        String MERGED = "merged";
        String UNMERGED = "unmerged";
    }

    public interface UserType {
        String USER = "User";
        String ORG = "Organization";
    }

    public interface MilestoneState {
        String OPEN = "open";
        String CLOSED = "closed";
    }

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

    public static int colorForLabel(Label label) {
        return Color.parseColor("#" + label.getColor());
    }

    public static boolean userEquals(User lhs, User rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        return loginEquals(lhs.getLogin(), rhs.getLogin());
    }

    public static boolean loginEquals(User user, String login) {
        if (user == null) {
            return false;
        }
        return loginEquals(user.getLogin(), login);
    }

    public static boolean loginEquals(String user, String login) {
        return user != null && user.equalsIgnoreCase(login);
    }

    public static Uri normalizeUri(Uri uri) {
        if (uri == null || uri.getAuthority() == null) {
            return uri;
        }

        // Only normalize API links
        if (!uri.getPath().contains("/api/v3/") && !uri.getAuthority().contains("api.")) {
            return uri;
        }

        String path = uri.getPath()
                .replace("/api/v3/", "/")
                .replace("repos/", "")
                .replace("commits/", "commit/")
                .replace("pulls/", "pull/");

        String authority = uri.getAuthority()
                .replace("api.", "");

        return uri.buildUpon()
                .path(path)
                .authority(authority)
                .build();
    }

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte[] messageDigest = digest.digest();

            StringBuilder builder = new StringBuilder();
            for (byte b : messageDigest) {
                String hexString = Integer.toHexString(0xFF & b);
                while (hexString.length() < 2) {
                    hexString = "0" + hexString;
                }
                builder.append(hexString);
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}