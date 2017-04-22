package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;

import android.content.Context;

import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;
import com.gh4a.utils.HtmlUtils;

public class ReadmeLoader extends BaseLoader<String> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final String mRef;

    public ReadmeLoader(Context context, String repoOwner, String repoName, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mRef = ref;
    }

    @Override
    public String doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient("application/vnd.github.v3.html");
        client.setOAuth2Token(app.getAuthToken());

        ContentsService contentService = new ContentsService(client);
        try {
            String html = contentService.getReadmeHtml(new RepositoryId(mRepoOwner, mRepoName), mRef);
            if (html != null) {
                return HtmlUtils.rewriteRelativeUrls(html, mRepoOwner, mRepoName, mRef);
            }
        } catch (RequestException e) {
            /* don't spam logcat with 404 errors, those are normal */
            if (e.getStatus() != 404) {
                throw e;
            }
        }
        return null;
    }
}
