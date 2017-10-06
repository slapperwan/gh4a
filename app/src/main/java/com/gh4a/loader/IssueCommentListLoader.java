package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubComment;
import com.meisolsson.githubsdk.model.IssueEvent;
import com.meisolsson.githubsdk.model.IssueEventType;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.issues.IssueEventService;

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

    private static final List<IssueEventType> INTERESTING_EVENTS = Arrays.asList(
        IssueEventType.Closed, IssueEventType.Reopened, IssueEventType.Merged,
        IssueEventType.Referenced, IssueEventType.Assigned, IssueEventType.Unassigned,
        IssueEventType.Labeled, IssueEventType.Unlocked, IssueEventType.Locked,
        IssueEventType.Unlocked, IssueEventType.Milestoned, IssueEventType.Demilestoned,
        IssueEventType.Renamed, IssueEventType.HeadRefDeleted, IssueEventType.HeadRefRestored
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
        final Gh4Application app = Gh4Application.get();
        final IssueEventService eventService = app.getGitHubService(IssueEventService.class);
        final IssueCommentService commentService = app.getGitHubService(IssueCommentService.class);
        List<GitHubComment> comments = ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<GitHubComment>() {
            @Override
            public Page<GitHubComment> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        commentService.getIssueComments(mRepoOwner, mRepoName, mIssueNumber, page).blockingGet());
            }
        });
        List<IssueEvent> events = ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<IssueEvent>() {
            @Override
            public Page<IssueEvent> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        eventService.getIssueEvents(mRepoOwner, mRepoName, mIssueNumber, page).blockingGet());
            }
        });
        List<TimelineItem> result = new ArrayList<>();

        for (GitHubComment comment : comments) {
            result.add(new TimelineItem.TimelineComment(comment));
        }
        for (IssueEvent event : events) {
            if (INTERESTING_EVENTS.contains(event.event())) {
                result.add(new TimelineItem.TimelineEvent(event));
            }
        }

        Collections.sort(result, TIMELINE_ITEM_COMPARATOR);

        return result;
    }
}
