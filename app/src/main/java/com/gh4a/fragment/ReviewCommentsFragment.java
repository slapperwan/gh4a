package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import com.gh4a.adapter.timeline.TimelineItemAdapter;
import com.gh4a.loader.TimelineItem;

import org.eclipse.egit.github.core.Comment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ReviewCommentsFragment extends LoadingListFragmentBase {
    private TimelineItemAdapter.OnCommentAction mCallback = new TimelineItemAdapter.OnCommentAction() {
        @Override
        public void editComment(Comment comment) {

        }

        @Override
        public void deleteComment(Comment comment) {

        }

        @Override
        public void quoteText(CharSequence text) {

        }

        @Override
        public String getShareSubject(Comment comment) {
            return null;
        }
    };
    private TimelineItemAdapter mAdapter;

    public static ReviewCommentsFragment newInstance(String repoOwner, String repoName,
            int issueNumber, boolean isPullRequest,
            ArrayList<TimelineItem.Diff> chunks) {
        ReviewCommentsFragment f = new ReviewCommentsFragment();
        Bundle args = new Bundle();
        args.putString("repo_owner", repoOwner);
        args.putString("repo_name", repoName);
        args.putInt("issue_number", issueNumber);
        args.putBoolean("is_pr", isPullRequest);
        args.putSerializable("chunks", chunks);
        f.setArguments(args);
        return f;
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    private boolean mIsPullRequest;
    private Collection<TimelineItem.Diff> mChunks;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("repo_owner");
        mRepoName = args.getString("repo_name");
        mIssueNumber = args.getInt("issue_number");
        mIsPullRequest = args.getBoolean("is_pr");
        mChunks = (List<TimelineItem.Diff>) args.getSerializable("chunks");
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mAdapter = new TimelineItemAdapter(getActivity(), mRepoOwner, mRepoName, mIssueNumber,
                mIsPullRequest, mCallback);
        for (TimelineItem.Diff chunk : mChunks) {
            mAdapter.add(chunk);
            for (TimelineItem.TimelineComment comment : chunk.comments) {
                mAdapter.add(comment);
            }
            mAdapter.add(new TimelineItem.Reply());
        }
        view.setAdapter(mAdapter);
    }

    @Override
    public void onRefresh() {
    }

    @Override
    protected int getEmptyTextResId() {
        return 0;
    }
}
