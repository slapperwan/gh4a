package com.gh4a.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditCommitCommentActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommitNoteFragment extends ListDataBaseFragment<CommitComment> implements
        CommitNoteAdapter.OnCommentAction<CommitComment>, CommentBoxFragment.Callback {
    public static CommitNoteFragment newInstance(String repoOwner, String repoName,
            String commitSha, RepositoryCommit commit,
            List<CommitComment> allComments, IntentUtils.InitialCommentMarker initialComment) {
        CommitNoteFragment f = new CommitNoteFragment();

        ArrayList<CommitComment> comments = new ArrayList<>();
        // we're only interested in unpositional comments
        for (CommitComment comment : allComments) {
            if (comment.getPosition() < 0) {
                comments.add(comment);
            }
        }

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("sha", commitSha);
        args.putSerializable("commit", commit);
        args.putSerializable("comments", comments);
        args.putParcelable("initial_comment", initialComment);

        f.setArguments(args);
        return f;
    }

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private static final int REQUEST_EDIT = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private RepositoryCommit mCommit;
    private IntentUtils.InitialCommentMarker mInitialComment;

    private CommitNoteAdapter mAdapter;
    private CommentBoxFragment mCommentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mObjectSha = args.getString("sha");
        mCommit = (RepositoryCommit) args.getSerializable("commit");
        mInitialComment = args.getParcelable("initial_comment");
        args.remove("initial_comment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.commit_comment_list, container, false);

        FrameLayout listContainer = (FrameLayout) v.findViewById(R.id.list_container);
        listContainer.addView(listContent);

        if (!Gh4Application.get().isAuthorized()) {
            v.findViewById(R.id.comment_box).setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        mCommentFragment = (CommentBoxFragment) fm.findFragmentById(R.id.comment_box);
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.pause();
    }

    @Override
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return super.canChildScrollUp();
    }

    @Override
    protected RootAdapter<CommitComment, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new CommitNoteAdapter(getActivity(), mRepoOwner, mRepoName, this);
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<CommitComment, ?> adapter, List<CommitComment> data) {
        super.onAddData(adapter, data);
        Set<User> users = mAdapter.getUsers();
        if (mCommit.getAuthor() != null) {
            users.add(mCommit.getAuthor());
        }
        if (mCommit.getCommitter() != null) {
            users.add(mCommit.getCommitter());
        }
        mCommentFragment.setMentionUsers(users);

        if (mInitialComment != null) {
            for (int i = 0; i < data.size(); i++) {
                if (mInitialComment.matches(data.get(i).getId(), data.get(i).getCreatedAt())) {
                    scrollToAndHighlightPosition(i);
                    break;
                }
            }
            mInitialComment = null;
        }
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_comments_found;
    }

    @Override
    public Loader<LoaderResult<List<CommitComment>>> onCreateLoader() {
        CommitCommentListLoader loader = new CommitCommentListLoader(getActivity(),
                mRepoOwner, mRepoName, mObjectSha, true, false);
        loader.prefillData((List<CommitComment>) getArguments().getSerializable("comments"));
        return loader;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                refreshComments();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void editComment(CommitComment comment) {
        Intent intent = EditCommitCommentActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, mObjectSha, comment);
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void deleteComment(final CommitComment comment) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_comment_message)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteCommentTask(getBaseActivity(), comment.getId()).schedule();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void quoteText(CharSequence text) {
        mCommentFragment.addQuote(text);
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.commit_comment_hint;
    }

    @Override
    public void onSendCommentInBackground(String comment) throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        CommitComment commitComment = new CommitComment();
        commitComment.setBody(comment);
        commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mObjectSha, commitComment);
    }

    @Override
    public void onCommentSent() {
        refreshComments();
    }

    private void refreshComments() {
        if (getActivity() instanceof CommentUpdateListener) {
            ((CommentUpdateListener) getActivity()).onCommentsUpdated();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private final long mId;

        public DeleteCommentTask(BaseActivity activity, long id) {
            super(activity, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(getBaseActivity(), mId);
        }

        @Override
        protected Void run() throws Exception {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            Gh4Application app = Gh4Application.get();
            CommitService commitService =
                    (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);

            commitService.deleteComment(repoId, mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refreshComments();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_comment);
        }
    }
}
