package com.gh4a.service;

import android.content.Context;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.HtmlUtils;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import java.io.IOException;
import io.reactivex.Observable;

public class RepositoryService {
    public static Observable<String> loadReadme(Context context, String repoOwner, String repoName, String ref) {
        return Observable.create(emitter -> {
            Gh4Application app = (Gh4Application) context.getApplicationContext();
            GitHubClient client = new DefaultClient("application/vnd.github.v3.html");
            client.setOAuth2Token(app.getAuthToken());

            ContentsService contentService = new ContentsService(client);
            try {
                String html = contentService.getReadmeHtml(new RepositoryId(repoOwner, repoName), ref);
                if (html != null)
                    emitter.onNext(HtmlUtils.rewriteRelativeUrls(html, repoOwner, repoName, ref));
            } catch (RequestException e) {
                /* don't spam logcat with 404 errors, those are normal */
                if (e.getStatus() != 404) {
                    emitter.onError(e);
                }
            } catch (IOException ioe) {
                emitter.onError(ioe);
            }
        });
    }
}
