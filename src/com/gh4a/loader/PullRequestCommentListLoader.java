package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class PullRequestCommentListLoader extends IssueCommentListLoader {

    public PullRequestCommentListLoader(Context context, String repoOwner,
            String repoName, int issueNumber) {
        super(context, repoOwner, repoName, issueNumber, true);
    }

    @Override
    public List<IssueEventHolder> doLoadInBackground() throws IOException {
        // combine issue comments and pull request comments (to get comments on diff)
        List<IssueEventHolder> events = super.doLoadInBackground();

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        List<CommitComment> commitComments = pullRequestService.getComments(repoId, mIssueNumber);
        HashMap<String, CommitFile> filesByName = new HashMap<>();

        for (CommitFile file : pullRequestService.getFiles(repoId, mIssueNumber)) {
            filesByName.put(file.getFilename(), file);
        }

        // only add comment that is not outdated
        for (CommitComment commitComment: commitComments) {
            if (commitComment.getPosition() != -1) {
                CommitFile file = filesByName.get(commitComment.getPath());
                events.add(new IssueEventHolder(commitComment, file));
            }
        }

        Collections.sort(events, SORTER);

        return events;
    }
}
