package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.content.Context;

import com.gh4a.Gh4Application;

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
    public String doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        ContentsService contentService =
                (ContentsService) app.getService(Gh4Application.CONTENTS_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        RepositoryContents template = fetchIssueTemplateContent(contentService, repoId, null);
        if (template == null) {
            template = fetchIssueTemplateContent(contentService, repoId, "/.github");
        }
        if (template != null) {
            // fetch again to get the actual contents; we're at this point sure the file exists
            template = contentService.getContents(repoId, template.getPath(), null).get(0);
        }

        return template == null
                ? null
                : new String(EncodingUtils.fromBase64(template.getContent()));
    }

    private RepositoryContents fetchIssueTemplateContent(ContentsService service,
            RepositoryId repo, String path) throws IOException {
        List<RepositoryContents> contents;
        try {
            contents = service.getContents(repo, path, null);
        } catch (RequestException e) {
            if (e.getStatus() == 404) {
                return null;
            } else {
                throw e;
            }
        }

        if (contents != null) {
            for (RepositoryContents c : contents) {
                if (RepositoryContents.TYPE_FILE.equals(c.getType())
                        && c.getName().startsWith(FILE_NAME_PREFIX)) {
                    return c;
                }
            }
        }

        return null;
    }
}
