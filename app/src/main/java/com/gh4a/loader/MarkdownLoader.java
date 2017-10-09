package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.request.RequestMarkdown;
import com.meisolsson.githubsdk.service.misc.MarkdownService;

public class MarkdownLoader extends BaseLoader<String> {
    private final String mText;
    private final String mRepoOwner;
    private final String mRepoName;

    public MarkdownLoader(Context context, String repoOwner, String repoName, String text) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mText = text;
    }

    @Override
    public String doLoadInBackground() throws ApiRequestException {
        MarkdownService service = Gh4Application.get().getGitHubService(MarkdownService.class);
        RequestMarkdown request = RequestMarkdown.builder()
                .context(mRepoName != null && mRepoOwner != null ? mRepoOwner + "/" + mRepoName : null)
                .mode("gfm")
                .text(mText)
                .build();
        return ApiHelpers.throwOnFailure(service.renderMarkdown(request).blockingGet());
    }
}
