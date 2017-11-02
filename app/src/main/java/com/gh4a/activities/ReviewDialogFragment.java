package com.gh4a.activities;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

public class ReviewDialogFragment extends DialogFragment {
    private static final String EXTRA_PR_NUMBER = "pr_number";
    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";

    public static ReviewDialogFragment newInstance(String repoOwner, String repoName,
            int pullRequestNumber) {
        ReviewDialogFragment fragment = new ReviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        args.putInt(EXTRA_PR_NUMBER, pullRequestNumber);
        fragment.setArguments(args);
        return fragment;
    }

    private Callback mCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (!(context instanceof Callback)) {
            throw new IllegalStateException("Parent activity must implement Callback interface");
        }
        mCallback = (Callback) context;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        final int pullRequestNumber = args.getInt(EXTRA_PR_NUMBER);
        final String repoOwner = args.getString(EXTRA_OWNER);
        final String repoName = args.getString(EXTRA_REPO);

        String title = getString(R.string.pull_request_review_dialog_title, pullRequestNumber);
        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater()
                .inflate(R.layout.pull_review_message_dialog, null);

        final EditText editor = view.findViewById(R.id.et_commit_message);

        final ArrayAdapter<ReviewEventDesc> adapter = new ArrayAdapter<>(getContext(),
                R.layout.spinner_item);
        adapter.add(new ReviewEventDesc(R.string.pull_request_review_event_comment,
                PullRequestService.REVIEW_EVENT_COMMENT));
        adapter.add(new ReviewEventDesc(R.string.pull_request_review_event_approve,
                PullRequestService.REVIEW_EVENT_APPROVE));
        adapter.add(new ReviewEventDesc(R.string.pull_request_review_event_request_changes,
                PullRequestService.REVIEW_EVENT_REQUEST_CHANGES));

        final Spinner reviewEvent = view.findViewById(R.id.pull_request_review_event);
        reviewEvent.setAdapter(adapter);

        return new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = editor.getText() == null ? null : editor.getText().toString();
                        int position = reviewEvent.getSelectedItemPosition();
                        @SuppressWarnings("ConstantConditions")
                        String eventType = adapter.getItem(position).mEventType;
                        new PullRequestReviewTask(mCallback, repoOwner, repoName, pullRequestNumber,
                                text, eventType).schedule();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .create();
    }

    interface Callback {
        BaseActivity getBaseActivity();
        void onReviewCreated(Review result);
    }

    private class ReviewEventDesc {
        @StringRes
        public final int mTextResId;
        public final String mEventType;

        public ReviewEventDesc(@StringRes int textResId, String eventType) {
            mTextResId = textResId;
            mEventType = eventType;
        }

        @Override
        public String toString() {
            return getContext().getString(mTextResId);
        }
    }

    private static class PullRequestReviewTask extends ProgressDialogTask<Review> {
        private final Callback mCallback;
        private final String mReviewMessage;
        private final String mReviewEvent;
        private final String mRepoOwner;
        private final String mRepoName;
        private final int mPullRequestNumber;

        public PullRequestReviewTask(Callback callback, String repoOwner, String repoName,
                int pullRequestNumber, String reviewMessage, String reviewEvent) {
            super(callback.getBaseActivity(), R.string.reviewing_msg);
            mCallback = callback;
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mPullRequestNumber = pullRequestNumber;
            mReviewMessage = reviewMessage;
            mReviewEvent = reviewEvent;
        }

        @Override
        protected ProgressDialogTask<Review> clone() {
            return new PullRequestReviewTask(mCallback, mRepoOwner, mRepoName, mPullRequestNumber,
                    mReviewMessage, mReviewEvent);
        }

        @Override
        protected Review run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            return pullService.createReview(repoId, mPullRequestNumber, mReviewEvent,
                    mReviewMessage);
        }

        @Override
        protected void onSuccess(Review result) {
            mCallback.onReviewCreated(result);
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.pull_request_review_error, mPullRequestNumber);
        }
    }
}
