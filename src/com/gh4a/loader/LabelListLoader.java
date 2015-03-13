package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.service.LabelService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class LabelListLoader extends BaseLoader<List<Label>> {

    private String mRepoOwner;
    private String mRepoName;

    public LabelListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Label> doLoadInBackground() throws IOException {
        LabelService labelService = (LabelService)
                Gh4Application.get().getService(Gh4Application.LABEL_SERVICE);
        return labelService.getLabels(mRepoOwner, mRepoName);
    }
}
