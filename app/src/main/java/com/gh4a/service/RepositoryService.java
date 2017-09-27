package com.gh4a.service;

import com.gh4a.BaseActivity;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.rx.RxTools;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;
import io.reactivex.Observable;

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
        return RxTools.runCallable(
                () -> {
                    Gh4Application app = Gh4Application.get();
                    GitHubClient client = new DefaultClient("application/vnd.github.v3.html");
                    client.setOAuth2Token(app.getAuthToken());

                    ContentsService contentService = new ContentsService(client);
                    String html = contentService.getReadmeHtml(new RepositoryId(repoOwner, repoName), ref);
                    if (html != null)
                        return HtmlUtils.rewriteRelativeUrls(html, repoOwner, repoName, ref);
                    else return null;
                },
                activity,
                LOAD_README,
                refresh
        );
    }

    public static Observable<Integer> loadPullRequestCount(BaseActivity activity, Repository repository,
            String state, boolean refresh) {
        return RxTools.runCallable(
            () -> {
                final String QUERY_FORMAT = "type:pr repo:%s/%s state:%s";
                IssueService issueService = (IssueService)
                        Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
                HashMap<String, String> filterData = new HashMap<>();
                filterData.put("q", String.format(Locale.US, QUERY_FORMAT,
                        repository.getOwner().getLogin(), repository.getName(), state));
                return issueService.getSearchIssueResultCount(filterData);
            },
            activity,
            LOAD_PULL_REQUESTS_COUNT,
            refresh
        );
    }

    public static Observable setStarringStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean isStarring) {
        return RxTools.runCallable(
            () -> {
                StarService starService = (StarService)
                        Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
                RepositoryId repoId = new RepositoryId(repoOwner, repoName);
                if (isStarring) {
                    starService.unstar(repoId);
                } else {
                    starService.star(repoId);
                }

                return !isStarring;
            },
            activity,
            UPDATE_STAR
        );
    }

    public static Observable<Boolean> setWatchingStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean isWatching) {
        return RxTools.runCallable(
            () -> {
                WatcherService watcherService = (WatcherService)
                        Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);
                RepositoryId repoId = new RepositoryId(repoOwner, repoName);
                if (isWatching) {
                    watcherService.unwatch(repoId);
                } else {
                    watcherService.watch(repoId);
                }

                return !isWatching;
            },
            activity,
            UPDATE_WATCH
        );
    }

    public static Observable<Boolean> loadStarringStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean refresh) {
        return RxTools.runCallable(
            () -> ((StarService)
                    Gh4Application.get()
                    .getService(Gh4Application.STAR_SERVICE))
                    .isStarring(new RepositoryId(repoOwner, repoName)),
            activity,
            IS_STARRING,
            refresh
        );
    }

    public static Observable<Boolean> loadWatchingStatus(BaseActivity activity, String repoOwner, String repoName,
            boolean refresh) {
        return RxTools.runCallable(
            () -> ((WatcherService) Gh4Application.get()
                    .getService(Gh4Application.WATCHER_SERVICE))
                    .isWatching(new RepositoryId(repoOwner, repoName)),
            activity,
            IS_WATCHING,
            refresh
        );
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
