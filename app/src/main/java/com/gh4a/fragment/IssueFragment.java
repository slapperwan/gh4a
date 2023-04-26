package com.gh4a.fragment;

import android.content.Intent;
import androidx.annotation.AttrRes;
import android.view.View;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.EditIssueCommentActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;
import com.meisolsson.githubsdk.service.issues.IssueTimelineService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class IssueFragment extends IssueFragmentBase {
    public static IssueFragment newInstance(String repoOwner, String repoName, Issue issue,
            boolean isCollaborator, IntentUtils.InitialCommentMarker initialComment) {
        IssueFragment f = new IssueFragment();
        f.setArguments(buildArgs(repoOwner, repoName, issue, isCollaborator, initialComment));
        return f;
    }

    public void updateState(Issue issue) {
        mIssue = mIssue.toBuilder().state(issue.state()).build();
        reloadEvents(false);
    }

    @Override
    protected void bindSpecialViews(View headerView) {
        TextView tvPull = headerView.findViewById(R.id.tv_pull);
        if (mIssue.pullRequest() != null && mIssue.pullRequest().diffUrl() != null) {
            tvPull.setVisibility(View.VISIBLE);
            tvPull.setOnClickListener(this);
        } else {
            tvPull.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_pull) {
            startActivity(PullRequestActivity.makeIntent(getActivity(),
                    mRepoOwner, mRepoName, mIssue.number()));
        } else {
            super.onClick(v);
        }
    }

    @Override
    protected Single<List<TimelineItem>> onCreateDataSingle(boolean bypassCache) {
        final int issueNumber = mIssue.number();
        final IssueTimelineService timelineService = ServiceFactory.get(IssueTimelineService.class, bypassCache);
        final IssueCommentService commentService =
                ServiceFactory.get(IssueCommentService.class, bypassCache);

        Single<List<TimelineItem.TimelineComment>> commentSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getIssueComments(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.mapList(TimelineItem.TimelineComment::new))
                .subscribeOn(Schedulers.io());
        Single<List<TimelineItem.TimelineEvent>> eventSingle = ApiHelpers.PageIterator
                .toSingle(page -> timelineService.getTimeline(mRepoOwner, mRepoName, issueNumber, page))
                .compose(RxUtils.filter(event -> INTERESTING_EVENTS.contains(event.event())))
                .compose((RxUtils.mapList(TimelineItem.TimelineEvent::new)))
                .subscribeOn(Schedulers.io());

        return Single.zip(commentSingle, eventSingle, (comments, events) -> {
            ArrayList<TimelineItem> result = new ArrayList<>();
            result.addAll(comments);
            result.addAll(events);
            Collections.sort(result, TimelineItem.COMPARATOR);
            return result;
        });
    }

    @Override
    public void editComment(GitHubCommentBase comment) {
        @AttrRes int highlightColorAttr = mIssue.state() == IssueState.Closed
                ? R.attr.colorPrimary : R.attr.colorSecondary;
        Intent intent = EditIssueCommentActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                mIssue.number(), comment.id(), comment.body(), highlightColorAttr);
        mEditLauncher.launch(intent);
    }

    @Override
    protected Single<Response<Void>> doDeleteComment(GitHubCommentBase comment) {
        IssueCommentService service = ServiceFactory.get(IssueCommentService.class, false);
        return service.deleteIssueComment(mRepoOwner, mRepoName, comment.id());
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.issue_comment_hint;
    }
}
