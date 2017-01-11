package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.MarkdownService;

import android.content.Context;

import com.gh4a.Gh4Application;

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
    public String doLoadInBackground() throws IOException {
        MarkdownService markdownService = (MarkdownService)
                Gh4Application.get().getService(Gh4Application.MARKDOWN_SERVICE);
        if (mRepoOwner != null && mRepoName != null) {
            return markdownService.getRepositoryHtml(new RepositoryId(mRepoOwner, mRepoName), mText);
        }
        return markdownService.getHtml(mText, null);
    }
}
