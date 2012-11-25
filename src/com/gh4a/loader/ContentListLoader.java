package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class ContentListLoader extends BaseLoader {

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
    public void doLoadInBackground(HashMap<Integer, Object> result)
            throws IOException {
        Gh4Application app = (Gh4Application) getContext()
                .getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());

        RepositoryService repoService = new RepositoryService(client);
        ContentService contentService = new ContentService(client);
        if (mRef == null) {
            Repository repo = repoService.getRepository(mRepoOwner, mRepoName);
            mRef = repo.getMasterBranch();
        }

        List<Content> contents = contentService.getContents(new RepositoryId(
                mRepoOwner, mRepoName), mPath, mRef);

        if (contents != null && !contents.isEmpty()) {
            Comparator<Content> comp = new Comparator<Content>() {
                public int compare(Content c1, Content c2) {
                    if ("dir".equals(c1.getType()) && !"dir".equals(c2.getType())) {
                        // Directory before non-directory
                        return -1;
                    } else if (!"dir".equals(c1.getType()) && "dir".equals(c2.getType())) {
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
        
        result.put(LoaderResult.DATA, contents);
    }

}
