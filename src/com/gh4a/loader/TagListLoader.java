package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class TagListLoader extends BaseLoader<List<RepositoryTag>> {
    private String mRepoOwner;
    private String mRepoName;

    public TagListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<RepositoryTag> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        List<RepositoryTag> tags = repoService.getTags(new RepositoryId(mRepoOwner, mRepoName));
        ArrayList<RepositoryTag> result = new ArrayList<>();

        if (tags != null) {
            for (RepositoryTag tag : tags) {
                if (tag != null) {
                    result.add(tag);
                }
            }
        }
        return result;
    }
}
