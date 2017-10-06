package com.gh4a.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitUser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import retrofit2.Response;

public class ApiHelpers {
    public interface IssueState {
        String OPEN = "open";
        String CLOSED = "closed";
        String MERGED = "merged";
        String UNMERGED = "unmerged";
    }

    public static final Comparator<GitHubCommentBase> COMMENT_COMPARATOR = new Comparator<GitHubCommentBase>() {
        @Override
        public int compare(GitHubCommentBase lhs, GitHubCommentBase rhs) {
            if (lhs.createdAt() == null) {
                return 1;
            }
            if (rhs.createdAt() == null) {
                return -1;
            }
            return lhs.createdAt().compareTo(rhs.createdAt());
        }
    };

    //RepositoryCommit
    public static String getAuthorName(Context context, Commit commit) {
        if (commit.author() != null) {
            return commit.author().login();
        }
        if (commit.commit().author() != null) {
            return commit.commit().author().name();
        }
        return context.getString(R.string.unknown);
    }

    public static String getAuthorLogin(Commit commit) {
        if (commit.author() != null) {
            return commit.author().login();
        }
        return null;
    }

    public static String getCommitterName(Context context, Commit commit) {
        if (commit.committer() != null) {
            return commit.committer().login();
        }
        if (commit.commit().committer() != null) {
            return commit.commit().committer().name();
        }
        return context.getString(R.string.unknown);
    }

    public static boolean authorEqualsCommitter(Commit commit) {
        if (commit.committer() != null && commit.author() != null) {
            return TextUtils.equals(commit.committer().login(), commit.author().login());
        }

        GitUser author = commit.commit().author();
        GitUser committer = commit.commit().committer();
        if (author.email() != null && committer.email() != null) {
            return TextUtils.equals(author.email(), committer.email());
        }
        return TextUtils.equals(author.name(), committer.name());
    }

    public static String getUserLogin(Context context, User user) {
        if (user != null && user.login() != null) {
            return user.login();
        }
        return context.getString(R.string.unknown);
    }

    public static int colorForLabel(Label label) {
        return Color.parseColor("#" + label.color());
    }

    public static boolean userEquals(User lhs, User rhs) {
        if (lhs == null || rhs == null) {
            return false;
        }
        return loginEquals(lhs.login(), rhs.login());
    }

    public static boolean loginEquals(User user, String login) {
        if (user == null) {
            return false;
        }
        return loginEquals(user.login(), login);
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

    public static <T> T throwOnFailure(Response<T> response) throws IOException {
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Gh4Application.get().logout();
        }
        if (!response.isSuccessful()) {
            throw new ApiRequestException(response);
        }
        return response.body();
    }

    public static class Pager<T> {
        public interface PageProvider<T> {
            Page<T> providePage(long page) throws IOException;
        }

        public static <T> List<T> fetchAllPages(PageProvider<T> provider) throws IOException {
            List<T> result = new ArrayList<>();
            int nextPage = 1;
            do {
                Page<T> page = provider.providePage(nextPage);
                result.addAll(page.items());
                nextPage = page.next() != null ? page.next() : 0;
            } while (nextPage > 0);
            return result;
        }
    }
}