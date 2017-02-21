package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.util.EncodingUtils;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

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
        ContentsService contentService = (ContentsService)
                Gh4Application.get().getService(Gh4Application.CONTENTS_SERVICE);
        RepositoryContents contents = null;
        try {
            contents = contentService.getReadme(new RepositoryId(mRepoOwner, mRepoName), mRef);
        } catch (RequestException e) {
            /* don't spam logcat with 404 errors, those are normal */
            if (e.getStatus() != 404) {
                throw e;
            }
        }
        String encodedContent = contents != null ? contents.getContent() : null;

        if (TextUtils.isEmpty(encodedContent)) {
            return null;
        }
        return new String(EncodingUtils.fromBase64(encodedContent));
    }
}
