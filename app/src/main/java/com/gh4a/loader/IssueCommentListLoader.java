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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class IssueCommentListLoader extends BaseLoader<List<TimelineItem>> {

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

    public static final Comparator<TimelineItem> TIMELINE_ITEM_COMPARATOR = new Comparator<TimelineItem>() {
        @Override
        public int compare(TimelineItem lhs, TimelineItem rhs) {
            if (lhs.getCreatedAt() == null) {
                return 1;
            }
            if (rhs.getCreatedAt() == null) {
                return -1;
            }
            return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
        }
    };

    public IssueCommentListLoader(Context context, String repoOwner, String repoName,
            int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
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

        Collections.sort(result, TIMELINE_ITEM_COMPARATOR);

        return result;
    }
}
