package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ReviewTimelineLoader extends BaseLoader<List<TimelineItem>> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;
    private final long mReviewId;

    public ReviewTimelineLoader(Context context, String repoOwner, String repoName,
            int pullRequestNumber, long reviewId) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mReviewId = reviewId;
    }

    @Override
    protected List<TimelineItem> doLoadInBackground() throws Exception {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        Review review = pullRequestService.getReview(repoId, mPullRequestNumber, mReviewId);
        TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);

        List<CommitComment> reviewComments =
                pullRequestService.getReviewComments(repoId, mPullRequestNumber, mReviewId);

        if (!reviewComments.isEmpty()) {
            HashMap<String, CommitFile> filesByName = new HashMap<>();
            for (CommitFile file : pullRequestService.getFiles(repoId, mPullRequestNumber)) {
                filesByName.put(file.getFilename(), file);
            }

            for (CommitComment reviewComment : reviewComments) {
                CommitFile file = filesByName.get(reviewComment.getPath());
                timelineReview.addComment(reviewComment, file, true);
            }

            List<CommitComment> commitComments =
                    pullRequestService.getComments(repoId, mPullRequestNumber);

            Collections.sort(commitComments, new Comparator<CommitComment>() {
                @Override
                public int compare(CommitComment o1, CommitComment o2) {
                    return o1.getCreatedAt().compareTo(o2.getCreatedAt());
                }
            });

            for (CommitComment commitComment : commitComments) {
                if (reviewComments.contains(commitComment)) {
                    continue;
                }
                CommitFile file = filesByName.get(commitComment.getPath());
                timelineReview.addComment(commitComment, file, false);
            }
        }

        List<TimelineItem.Diff> diffHunks = new ArrayList<>(timelineReview.getDiffHunks());
        Collections.sort(diffHunks);

        List<TimelineItem> items = new ArrayList<>();
        items.add(timelineReview);

        for (TimelineItem.Diff diffHunk : diffHunks) {
            items.add(diffHunk);
            for (TimelineItem.TimelineComment comment : diffHunk.comments) {
                items.add(comment);
            }

            items.add(new TimelineItem.Reply(diffHunk.getInitialTimelineComment()));
        }

        return items;
    }
}
