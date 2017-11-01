package com.gh4a.utils;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitUser;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.Single;
import retrofit2.Response;

public class ApiHelpers {
    public interface IssueState {
        String OPEN = "open";
        String CLOSED = "closed";
        String MERGED = "merged";
        String UNMERGED = "unmerged";
    }

    public static final Comparator<GitHubCommentBase> COMMENT_COMPARATOR = (lhs, rhs) -> {
        if (lhs.createdAt() == null) {
            return 1;
        }
        if (rhs.createdAt() == null) {
            return -1;
        }
        return lhs.createdAt().compareTo(rhs.createdAt());
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

    public static <T> T throwOnFailure(Response<T> response) throws ApiRequestException {
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Gh4Application.get().logout();
        }
        if (!response.isSuccessful()) {
            throw new ApiRequestException(response);
        }
        return response.body();
    }

    public static Boolean mapToBooleanOrThrowOnFailure(Response<Void> response)
            throws ApiRequestException {
        if (response.isSuccessful()) {
            return true;
        } else if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        }

        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Gh4Application.get().logout();
        }
        throw new ApiRequestException(response);
    }

    public static class DummyPage<T> extends Page<T> {
        @Nullable
        @Override
        public Integer next() {
            return null;
        }

        @Nullable
        @Override
        public Integer last() {
            return null;
        }

        @Nullable
        @Override
        public Integer first() {
            return null;
        }

        @Nullable
        @Override
        public Integer prev() {
            return null;
        }

        @NonNull
        @Override
        public List<T> items() {
            return new ArrayList<>();
        }
    }

    public static class SearchPageAdapter<T> extends Page<T> {
        private final SearchPage<T> mPage;

        public SearchPageAdapter(SearchPage<T> page) {
            mPage = page;
        }

        @Nullable
        @Override
        public Integer next() {
            return mPage.next();
        }

        @Nullable
        @Override
        public Integer last() {
            return mPage.last();
        }

        @Nullable
        @Override
        public Integer first() {
            return mPage.first();
        }

        @Nullable
        @Override
        public Integer prev() {
            return mPage.prev();
        }

        @NonNull
        @Override
        public List<T> items() {
            return mPage.items();
        }
    }

    public static class PageIterator<T> {
        public interface PageProducer<T> {
            Single<Response<Page<T>>> getPage(long page);
        }

        public static <T> Observable<List<T>> toObservable(PageProducer<T> producer) {
            return pageToObservable(producer, 1);
        }

        public static <T> Single<List<T>> toSingle(PageProducer<T> producer) {
            return toObservable(producer)
                    .toList()
                    .map(lists -> {
                        List<T> result = new ArrayList<>();
                        for (List<T> l : lists) {
                            result.addAll(l);
                        }
                        return result;
                    });
        }

        private static <T> Observable<List<T>> pageToObservable(PageProducer<T> producer, int page) {
            return producer.getPage(page)
                    .toObservable()
                    .compose(PageIterator::evaluateError)
                    .compose(chain(producer));
        }

        private static <T> Observable<Page<T>> evaluateError(Observable<Response<Page<T>>> upstream) {
            return upstream.map(response -> {
                throwOnFailure(response);
                return response.body();
            });
        }

        private static <T> ObservableTransformer<Page<T>, List<T>> chain(PageProducer<T> producer) {
            return upstream -> upstream.concatMap(page -> {
                Observable<List<T>> result = Observable.just(page.items());
                return page.next() != null
                        ? result.concatWith(pageToObservable(producer, page.next()))
                        : result;
            });
        }
    }
}