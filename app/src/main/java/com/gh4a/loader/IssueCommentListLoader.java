package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IssueCommentListLoader extends BaseLoader<List<TimelineItem>> {
    private final boolean mIsPullRequest;

    protected final String mRepoOwner;
    protected final String mRepoName;
    protected final int mIssueNumber;

    private static final List<String> INTERESTING_EVENTS = Arrays.asList(
        IssueEvent.TYPE_CLOSED, IssueEvent.TYPE_REOPENED, IssueEvent.TYPE_MERGED,
        IssueEvent.TYPE_REFERENCED, IssueEvent.TYPE_ASSIGNED, IssueEvent.TYPE_UNASSIGNED,
        IssueEvent.TYPE_LABELED, IssueEvent.TYPE_UNLABELED, IssueEvent.TYPE_LOCKED,
        IssueEvent.TYPE_UNLOCKED, IssueEvent.TYPE_MILESTONED, IssueEvent.TYPE_DEMILESTONED,
        IssueEvent.TYPE_RENAMED
    );

    // TODO
//    protected static final Comparator<IssueEventHolder> SORTER = new Comparator<IssueEventHolder>() {
//        @Override
//        public int compare(IssueEventHolder lhs, IssueEventHolder rhs) {
//            return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
//        }
//    };

    public IssueCommentListLoader(Context context, String repoOwner, String repoName,
            int issueNumber) {
        this(context, repoOwner, repoName, issueNumber, false);
    }

    protected IssueCommentListLoader(Context context, String repoOwner, String repoName,
            int issueNumber, boolean isPullRequest) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
        mIsPullRequest = isPullRequest;
    }

    @Override
    protected List<TimelineItem> doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        List<Comment> comments = issueService.getComments(
                new RepositoryId(mRepoOwner, mRepoName), mIssueNumber);
        List<IssueEvent> events = issueService.getIssueEvents(mRepoOwner, mRepoName, mIssueNumber);
        List<TimelineItem> result = new ArrayList<>();

        for (Comment comment : comments) {
            result.add(new TimelineItem.TimelineComment(comment));
        }
        for (IssueEvent event : events) {
            if (INTERESTING_EVENTS.contains(event.getEvent())) {
                result.add(new TimelineItem.TimelineEvent(event));
            }
        }

//        Collections.sort(result, SORTER);

        return result;
    }
}
