package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.HtmlUtils;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;

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
    public String doLoadInBackground() throws ApiRequestException {
        RepositoryContentService service = ServiceFactory.createService(
                RepositoryContentService.class, "application/vnd.github.v3.html", null, null);

        try {
            String html = ApiHelpers.throwOnFailure(
                    service.getReadmeHtml(mRepoOwner, mRepoName, mRef).blockingGet());
            if (html != null) {
                return HtmlUtils.rewriteRelativeUrls(html, mRepoOwner, mRepoName, mRef);
            }
        } catch (ApiRequestException e) {
            /* don't spam logcat with 404 errors, those are normal */
            if (e.getStatus() != 404) {
                throw e;
            }
        }
        return null;
    }
}
