package com.gh4a.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.UserType;
import com.meisolsson.githubsdk.model.git.GitUser;

import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.subjects.BehaviorSubject;
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
        User author = commit.author();
        if (author != null && !TextUtils.isEmpty(author.login())) {
            return author.login();
        }
        GitUser commitAuthor = commit.commit().author();
        if (commitAuthor != null && !TextUtils.isEmpty(commitAuthor.name())) {
            return commitAuthor.name();
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
        User actualUser = adjustUserForBotSuffix(user);
        if (actualUser != null && actualUser.login() != null) {
            return actualUser.login();
        }
        return context.getString(R.string.deleted);
    }

    public static SpannableStringBuilder getUserLoginWithType(Context context, User user) {
        return getUserLoginWithType(context, user, false);
    }

    public static SpannableStringBuilder getUserLoginWithType(Context context, User user, boolean boldifyLogin) {
        final SpannableStringBuilder builder = new SpannableStringBuilder(getUserLogin(context, user));
        if (boldifyLogin) {
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, builder.length(), 0);
        }
        final User actualUser = adjustUserForBotSuffix(user);
        if (actualUser != null && actualUser.type() == UserType.Bot) {
            StringUtils.addUserTypeSpan(context, builder, builder.length(),
                    context.getString(R.string.user_type_bot));
        }
        return builder;
    }

    private static User adjustUserForBotSuffix(User user) {
        final String login = user != null ? user.login() : null;
        if (login != null && login.endsWith("[bot]")) {
            UserType type = user.type();
            if (type == null || type == UserType.Bot) {
                return user.toBuilder()
                        .login(login.substring(0, login.length() - 5))
                        .type(UserType.Bot)
                        .build();
            }
        }
        return user;
    }

    public static String formatRepoName(Context context, Repository repository) {
        if (repository == null || TextUtils.isEmpty(repository.name())) {
            return context.getString(R.string.deleted);
        }
        return getUserLogin(context, repository.owner()) + "/" + repository.name();
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

    private final static char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(input.getBytes());
            byte[] messageDigest = digest.digest();
            char[] hexChars = new char[messageDigest.length * 2];
            for ( int i = 0; i < messageDigest.length; i++ ) {
                int b = messageDigest[i] & 0xFF;
                hexChars[i * 2] = HEX_CHARS[b >>> 4];
                hexChars[i * 2 + 1] = HEX_CHARS[b & 0x0F];
            }
            return new String(hexChars);
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

    public static boolean mapToTrueOnSuccess(Response<Void> response) throws ApiRequestException {
        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            Gh4Application.get().logout();
        }
        if (!response.isSuccessful()) {
            throw new ApiRequestException(response);
        }
        return true;
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

    public static class SearchPageAdapter<U, D> extends Page<D> {
        private final SearchPage<U> mPage;
        private final Optional.Mapper<U, D> mMapper;

        public SearchPageAdapter(SearchPage<U> page, Optional.Mapper<U, D> mapper) {
            mPage = page;
            mMapper = mapper;
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
        public List<D> items() {
            List<U> items = mPage.items();
            if (items == null) {
                return null;
            }
            ArrayList<D> result = new ArrayList<>();
            for (U item : items) {
                result.add(mMapper.map(item));
            }
            return result;
        }
    }

    public static class PageIterator<T> {
        public interface PageProducer<T> {
            Single<Response<Page<T>>> getPage(long page);
        }

        public static <T> Observable<List<T>> toObservable(PageProducer<T> producer) {
            BehaviorSubject<Optional<Integer>> pageControl =
                    BehaviorSubject.createDefault(Optional.of(1));
            return pageControl.concatMap(page -> {
                if (page.isPresent()) {
                    return producer.getPage(page.get())
                            .toObservable()
                            .compose(PageIterator::evaluateError)
                            .doOnNext(resultPage -> pageControl.onNext(Optional.ofWithNull(resultPage.next())))
                            .map(responsePage -> responsePage.items());
                } else {
                    return Observable.<List<T>>empty().doOnComplete(() -> pageControl.onComplete());
                }
            });
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

        private static <T> Observable<Page<T>> evaluateError(Observable<Response<Page<T>>> upstream) {
            return upstream.map(response -> {
                throwOnFailure(response);
                return response.body();
            });
        }
    }
}