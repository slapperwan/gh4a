package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IssueCommentListLoader extends BaseLoader<List<IssueEventHolder>> {
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;

    private static final List<String> INTERESTING_EVENTS = Arrays.asList(
        "closed", "reopened", "merged", "referenced", "assigned", "unassigned"
    );

    protected static Comparator<IssueEventHolder> SORTER = new Comparator<IssueEventHolder>() {
        @Override
        public int compare(IssueEventHolder lhs, IssueEventHolder rhs) {
            return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
        }
    };

    public IssueCommentListLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    protected List<IssueEventHolder> doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        List<Comment> comments = issueService.getComments(
                new RepositoryId(mRepoOwner, mRepoName), mIssueNumber);
        List<IssueEvent> events = issueService.getIssueEvents(mRepoOwner, mRepoName, mIssueNumber);
        List<IssueEventHolder> result = new ArrayList<>();

        for (Comment comment : comments) {
            result.add(new IssueEventHolder(comment));
        }
        for (IssueEvent event : events) {
            if (INTERESTING_EVENTS.contains(event.getEvent())) {
                result.add(new IssueEventHolder(event));
            }
        }

        Collections.sort(result, SORTER);

        return result;
    }
}
