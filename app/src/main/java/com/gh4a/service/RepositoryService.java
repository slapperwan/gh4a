package com.gh4a.service;

import com.gh4a.BaseActivity;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.rx.RxTools;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class RepositoryService {
    public static final int LOAD_README = 0;
    public static final int LOAD_PULL_REQUESTS_COUNT = 1;
    public static final int UPDATE_STAR = 2;
    public static final int UPDATE_WATCH = 3;
    public static final int IS_STARRING = 4;
    public static final int IS_WATCHING = 5;
    public static final int LOAD_REPOSITORY = 6;

    public static Observable<String> loadReadme(BaseActivity activity, String repoOwner, String repoName,
            String ref, boolean refresh) {
        Gh4Application app = Gh4Application.get();

        return Observable.<String>create(emitter -> {
            GitHubClient client = new DefaultClient("application/vnd.github.v3.html");
            client.setOAuth2Token(app.getAuthToken());

            ContentsService contentService = new ContentsService(client);
            try {
                String html = contentService.getReadmeHtml(new RepositoryId(repoOwner, repoName), ref);
                if (html != null) {
                    emitter.onNext(HtmlUtils.rewriteRelativeUrls(html, repoOwner, repoName, ref));
                    emitter.onComplete();
                }
            } catch (RequestException e) {
                /* don't spam logcat with 404 errors, those are normal */
                if (e.getStatus() != 404) {
                    emitter.onError(e);
                }
            } catch (IOException ioe) {
                emitter.onError(ioe);
            }
        }).compose(RxTools.handle(activity, LOAD_README, refresh));
    }

    public static Observable<Integer> loadPullRequestCount(BaseActivity activity, Repository repository,
            String state, boolean refresh) {
        return Observable.<Integer>create(emitter -> {
            final String QUERY_FORMAT = "type:pr repo:%s/%s state:%s";
            IssueService issueService = (IssueService)
                    Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
            HashMap<String, String> filterData = new HashMap<>();
            filterData.put("q", String.format(Locale.US, QUERY_FORMAT,
                    repository.getOwner().getLogin(), repository.getName(), state));
            try {
                int count = issueService.getSearchIssueResultCount(filterData);
                emitter.onNext(count);
                emitter.onComplete();
            } catch(IOException ioe) {
                emitter.onError(ioe);
            }
        }).compose(RxTools.handle(activity, LOAD_PULL_REQUESTS_COUNT, refresh));
    }

    public static Observable setStarringStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean isStarring) {
        return Observable.create(emitter -> {
            StarService starService = (StarService)
                    Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
            RepositoryId repoId = new RepositoryId(repoOwner, repoName);
            if (isStarring) {
                starService.unstar(repoId);
            } else {
                starService.star(repoId);
            }

            emitter.onNext(!isStarring);
            emitter.onComplete();
        })
        .compose(RxTools.handle(activity, UPDATE_STAR));
    }

    public static Observable<Boolean> setWatchingStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean isWatching) {
        return Observable.<Boolean>create(emitter -> {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);
            RepositoryId repoId = new RepositoryId(repoOwner, repoName);
            if (isWatching) {
                watcherService.unwatch(repoId);
            } else {
                watcherService.watch(repoId);
            }

            emitter.onNext(!isWatching);
            emitter.onComplete();
        })
        .compose(RxTools.handle(activity, UPDATE_WATCH));
    }

    public static Observable<Boolean> loadStarringStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean refresh) {
        return Observable.<Boolean>create(emitter -> {
            StarService starService = (StarService)
                    Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
            try {
                boolean isStarring = starService.isStarring(new RepositoryId(repoOwner, repoName));
                emitter.onNext(isStarring);
                emitter.onComplete();
            } catch(Exception ex) {
                emitter.onError(ex);
            }
        })
        .compose(RxTools.handle(activity, IS_STARRING, refresh));
    }

    public static Observable<Boolean> loadWatchingStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean refresh) {
        return Observable.<Boolean>create(emitter -> {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);

            try {
                boolean isWatching =
                        watcherService.isWatching(new RepositoryId(repoOwner, repoName));
                emitter.onNext(isWatching);
                emitter.onComplete();
            } catch (Exception e) {
                emitter.onError(e);
            }
        })
        .compose(RxTools.handle(activity, IS_WATCHING, refresh));
    }

    public static Observable<Repository> loadRepository(BaseActivity activity, String repoOwner, String repoName,
            boolean refresh) {
        return RxTools.runCallable(
            () -> ((org.eclipse.egit.github.core.service.RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE))
                .getRepository(repoOwner, repoName),
            activity,
            LOAD_REPOSITORY,
            refresh
        );
    }
}
