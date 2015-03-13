package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ContentListLoader extends BaseLoader<List<RepositoryContents>> {

    private String mRepoOwner;
    private String mRepoName;
    private String mPath;
    private String mRef;

    public ContentListLoader(Context context, String repoOwner,
            String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }

    @Override
    public List<RepositoryContents> doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get();
        ContentsService contentService = (ContentsService) app.getService(Gh4Application.CONTENTS_SERVICE);
        RepositoryService repoService = (RepositoryService) app.getService(Gh4Application.REPO_SERVICE);

        if (mRef == null) {
            Repository repo = repoService.getRepository(mRepoOwner, mRepoName);
            mRef = repo.getMasterBranch();
        }

        List<RepositoryContents> contents;
        try {
            contents = contentService.getContents(new RepositoryId(mRepoOwner, mRepoName), mPath, mRef);
        } catch (RequestException e) {
            if (e.getStatus() == 404) {
                contents = null;
            } else {
                throw e;
            }
        }

        if (contents != null && !contents.isEmpty()) {
            Comparator<RepositoryContents> comp = new Comparator<RepositoryContents>() {
                public int compare(RepositoryContents c1, RepositoryContents c2) {
                    boolean c1IsDir = RepositoryContents.TYPE_DIR.equals(c1.getType());
                    boolean c2IsDir = RepositoryContents.TYPE_DIR.equals(c2.getType());
                    if (c1IsDir && !c2IsDir) {
                        // Directory before non-directory
                        return -1;
                    } else if (!c1IsDir && c2IsDir) {
                        // Non-directory after directory
                        return 1;
                    } else {
                        // Alphabetic order otherwise
                        // return o1.compareTo(o2);
                        return c1.getName().compareTo(c2.getName());
                    }
                }
            };
            Collections.sort(contents, comp);
        }

        return contents;
    }
}
