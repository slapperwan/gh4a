package com.gh4a.service;

import android.content.Context;
import android.util.Log;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.HtmlUtils;
import com.gh4a.utils.RxTools;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import io.reactivex.Observable;

public class RepositoryService {
    public static Observable<String> loadReadme(Context context, String repoOwner, String repoName, String ref) {
        Observable<String> observable =  Observable.create(emitter -> {
            Log.d("TEST", "loading Readme file from wweb NETWORK CALL");
            Gh4Application app = (Gh4Application) context.getApplicationContext();
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
        });

        return observable;
    }

    public static Observable<Integer> loadPullRequestCount(Repository repository, String state) {
        return Observable.create(emitter -> {
            final String QUERY_FORMAT = "type:pr repo:%s/%s state:%s";
            IssueService issueService = (IssueService)
                    Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
            HashMap<String, String> filterData = new HashMap<>();
            filterData.put("q", String.format(Locale.US, QUERY_FORMAT,
                    repository.getOwner().getLogin(), repository.getName(), state));
            try {
                Log.d("TEST", "getPullRequestCount called --> repository service");
                emitter.onNext(issueService.getSearchIssueResultCount(filterData));
                Log.d("TEST", "getPullRequestCount called --> repository service AFTER ON NEXT");

                emitter.onComplete();
                Log.d("TEST", "getPullRequestCount called --> repository service AFTER ON COMPLETE");

            } catch(IOException ioe) {
                Log.d("TEST", "getPullRequestCount called --> ERROR: " + ioe.toString());
                emitter.onError(ioe);
            }
        });
    }

    public static Observable updateStar(String repoOwner, String repoName, boolean isStarring) {
        return Observable.create(e -> {
            Log.d("TEST", "updateStar creating the observable");
            StarService starService = (StarService)
                    Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
            RepositoryId repoId = new RepositoryId(repoOwner, repoName);
            if (isStarring) starService.unstar(repoId);
            else starService.star(repoId);

            Log.d("TEST", "updateStar calling onNext method");
            e.onNext(!isStarring);
            e.onComplete();
        });
    }

    public static Observable updateWatch(String repoOwner, String repoName, boolean isWatching) {
        return Observable.create(e -> {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);
            RepositoryId repoId = new RepositoryId(repoOwner, repoName);
            if (isWatching) {
                watcherService.unwatch(repoId);
            } else {
                watcherService.watch(repoId);
            }
            e.onNext(!isWatching);
            e.onComplete();
        });
    }

    public static Observable<Boolean> isStarring(String repoOwner, String repoName) {
        return Observable.create(e -> {
            StarService starService = (StarService)
                    Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
            e.onNext(starService.isStarring(new RepositoryId(repoOwner, repoName)));
            e.onComplete();
        });
    }


    public static Observable<Boolean> isWatching(String repoOwner, String repoName) {
        return Observable.create(e -> {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);
            e.onNext(watcherService.isWatching(new RepositoryId(repoOwner, repoName)));
            e.onComplete();
        });
    }

    public static Observable<Repository> loadRepository(String repoOwner, String repoName) {
        return Observable.create(e -> {
            org.eclipse.egit.github.core.service.RepositoryService repoService = (org.eclipse.egit.github.core.service.RepositoryService)
            Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
            e.onNext(repoService.getRepository(repoOwner, repoName));
            e.onComplete();
        });
    }
}
