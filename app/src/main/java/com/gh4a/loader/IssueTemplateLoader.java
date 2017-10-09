package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;

public class IssueTemplateLoader extends BaseLoader<String> {
    private final String mRepoOwner;
    private final String mRepoName;

    private static final String FILE_NAME_PREFIX = "ISSUE_TEMPLATE";

    public IssueTemplateLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public String doLoadInBackground() throws ApiRequestException {
        RepositoryContentService service =
                Gh4Application.get().getGitHubService(RepositoryContentService.class);

        Content template = fetchIssueTemplateContent(service, null);
        if (template == null) {
            template = fetchIssueTemplateContent(service, "/.github");
        }
        if (template != null) {
            // fetch again to get the actual contents; we're at this point sure the file exists
            template = ApiHelpers.throwOnFailure(
                    service.getContents(mRepoOwner, mRepoName, template.path(), null).blockingGet());
        }

        return template == null ? null : StringUtils.fromBase64(template.content());
    }

    private Content fetchIssueTemplateContent(final RepositoryContentService service,
            final String path) throws ApiRequestException {
        List<Content> contents;
        try {
            contents = ApiHelpers.PageIterator
                    .toSingle(page -> service.getDirectoryContents(mRepoOwner, mRepoName, path, null, page))
                    .blockingGet();
        } catch (ApiRequestException e) {
            if (e.getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        }

        if (contents != null) {
            for (Content c : contents) {
                if (c.type() == ContentType.File && c.name().startsWith(FILE_NAME_PREFIX)) {
                    return c;
                }
            }
        }

        return null;
    }
}
