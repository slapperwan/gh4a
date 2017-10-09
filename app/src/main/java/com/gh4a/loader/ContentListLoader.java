package com.gh4a.loader;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

public class ContentListLoader extends BaseLoader<List<Content>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mPath;
    private String mRef;

    public ContentListLoader(Context context, String repoOwner,
            String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path != null ? path : "";
        mRef = ref;
    }

    @Override
    public List<Content> doLoadInBackground() throws ApiRequestException {
        Gh4Application app = Gh4Application.get();
        RepositoryService repoService = app.getGitHubService(RepositoryService.class);
        final RepositoryContentService contentService = app.getGitHubService(RepositoryContentService.class);

        if (mRef == null) {
            Repository repo = ApiHelpers.throwOnFailure(
                    repoService.getRepository(mRepoOwner, mRepoName).blockingGet());
            mRef = repo.defaultBranch();
        }

        List<Content> contents;
        try {
            contents = ApiHelpers.PageIterator
                    .toSingle(page -> contentService.getDirectoryContents(
                            mRepoOwner, mRepoName, mPath, mRef, page))
                    .blockingGet();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof ApiRequestException && ((ApiRequestException) e.getCause()).getStatus() == 404) {
                contents = null;
            } else {
                throw e;
            }
        }

        if (contents != null && !contents.isEmpty()) {
            Comparator<Content> comp = new Comparator<Content>() {
                @Override
                public int compare(Content c1, Content c2) {
                    boolean c1IsDir = c1.type() == ContentType.Directory;
                    boolean c2IsDir = c2.type() == ContentType.Directory;
                    if (c1IsDir && !c2IsDir) {
                        // Directory before non-directory
                        return -1;
                    } else if (!c1IsDir && c2IsDir) {
                        // Non-directory after directory
                        return 1;
                    } else {
                        // Alphabetic order otherwise
                        // return o1.compareTo(o2);
                        return c1.name().compareTo(c2.name());
                    }
                }
            };
            Collections.sort(contents, comp);
        }

        return contents;
    }
}
