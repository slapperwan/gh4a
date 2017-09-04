package com.gh4a.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
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
import com.gh4a.widget.EditorBottomSheet;

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
        CommitNoteAdapter.OnCommentAction<CommitComment>,
        EditorBottomSheet.Callback, EditorBottomSheet.Listener {

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
        args.putSerializable("commit_author", commit.getAuthor());
        args.putSerializable("committer", commit.getCommitter());
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
    private User mCommitAuthor;
    private User mCommitter;
    private IntentUtils.InitialCommentMarker mInitialComment;

    private CommitNoteAdapter mAdapter;
    private EditorBottomSheet mBottomSheet;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mObjectSha = args.getString("sha");
        mCommitAuthor = (User) args.getSerializable("commit_author");
        mCommitter = (User) args.getSerializable("committer");
        mInitialComment = args.getParcelable("initial_comment");
        args.remove("initial_comment");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View listContent = super.onCreateView(inflater, container, savedInstanceState);
        View v = inflater.inflate(R.layout.comment_list, container, false);

        FrameLayout listContainer = v.findViewById(R.id.list_container);
        listContainer.addView(listContent);

        mBottomSheet = v.findViewById(R.id.bottom_sheet);
        mBottomSheet.setCallback(this);
        mBottomSheet.setResizingView(listContainer);
        mBottomSheet.setListener(this);

        if (!Gh4Application.get().isAuthorized()) {
            mBottomSheet.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getBaseActivity().addAppBarOffsetListener(mBottomSheet);
        mBottomSheet.post(new Runnable() {
            @Override
            public void run() {
                // Fix an issue where the bottom sheet is initially located outside of the visible
                // screen area
                mBottomSheet.resetPeekHeight(getBaseActivity().getAppBarTotalScrollRange());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mAdapter != null) {
            mAdapter.destroy();
            mAdapter = null;
        }

        getBaseActivity().removeAppBarOffsetListener(mBottomSheet);
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
        return (mBottomSheet != null && mBottomSheet.isExpanded()) || super.canChildScrollUp();
    }

    @Override
    public CoordinatorLayout getRootLayout() {
        return getBaseActivity().getRootLayout();
    }

    @Override
    public boolean onBackPressed() {
        if (mBottomSheet != null && mBottomSheet.isInAdvancedMode()) {
            mBottomSheet.setAdvancedMode(false);
            return true;
        }
        return false;
    }

    @Override
    public void onToggleAdvancedMode(boolean advancedMode) {
        getBaseActivity().collapseAppBar();
        getBaseActivity().setAppBarLocked(advancedMode);
        mBottomSheet.resetPeekHeight(0);
    }

    @Override
    public void onScrollingInBasicEditor(boolean scrolling) {
        getBaseActivity().setAppBarLocked(scrolling);
    }

    @Override
    protected void setHighlightColors(int colorAttrId, int statusBarColorAttrId) {
        super.setHighlightColors(colorAttrId, statusBarColorAttrId);
        mBottomSheet.setHighlightColor(colorAttrId);
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
        if (mCommitAuthor != null) {
            users.add(mCommitAuthor);
        }
        if (mCommitter != null) {
            users.add(mCommitter);
        }
        mBottomSheet.setMentionUsers(users);

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
        mBottomSheet.addQuote(text);
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
