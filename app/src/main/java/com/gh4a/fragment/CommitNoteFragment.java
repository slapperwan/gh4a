package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.EditCommitCommentActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.widget.EditorBottomSheet;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.request.repository.CreateCommitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.reactivex.Single;

public class CommitNoteFragment extends ListDataBaseFragment<GitComment> implements
        CommitNoteAdapter.OnCommentAction<GitComment>,
        EditorBottomSheet.Callback, EditorBottomSheet.Listener {

    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_SHA = "sha";
    private static final String EXTRA_COMMIT_AUTHOR = "commit_author";
    private static final String EXTRA_COMMITTER = "committer";
    private static final String EXTRA_COMMENTS = "comments";
    private static final String EXTRA_INITIAL_COMMENT = "initial_comment";

    public static CommitNoteFragment newInstance(String repoOwner, String repoName,
            String commitSha, Commit commit,
            List<GitComment> allComments, IntentUtils.InitialCommentMarker initialComment) {
        CommitNoteFragment f = new CommitNoteFragment();

        ArrayList<GitComment> comments = new ArrayList<>();
        // we're only interested in unpositional comments
        for (GitComment comment : allComments) {
            if (comment.position() == null) {
                comments.add(comment);
            }
        }

        Bundle args = new Bundle();
        args.putString(EXTRA_OWNER, repoOwner);
        args.putString(EXTRA_REPO, repoName);
        args.putString(EXTRA_SHA, commitSha);
        args.putParcelable(EXTRA_COMMIT_AUTHOR, commit.author());
        args.putParcelable(EXTRA_COMMITTER, commit.committer());
        args.putParcelableArrayList(EXTRA_COMMENTS, comments);
        args.putParcelable(EXTRA_INITIAL_COMMENT, initialComment);

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
        mRepoOwner = args.getString(EXTRA_OWNER);
        mRepoName = args.getString(EXTRA_REPO);
        mObjectSha = args.getString(EXTRA_SHA);
        mCommitAuthor = args.getParcelable(EXTRA_COMMIT_AUTHOR);
        mCommitter = args.getParcelable(EXTRA_COMMITTER);
        mInitialComment = args.getParcelable(EXTRA_INITIAL_COMMENT);
        args.remove(EXTRA_INITIAL_COMMENT);
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
        mBottomSheet.post(() -> {
            // Fix an issue where the bottom sheet is initially located outside of the visible
            // screen area
            final BaseActivity activity = getBaseActivity();
            if (activity != null) {
                mBottomSheet.resetPeekHeight(activity.getAppBarTotalScrollRange());
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
        BaseActivity activity = getBaseActivity();
        if (activity != null) {
            activity.collapseAppBar();
            activity.setAppBarLocked(advancedMode);
        }
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
    protected RootAdapter<GitComment, ? extends RecyclerView.ViewHolder> onCreateAdapter() {
        mAdapter = new CommitNoteAdapter(getActivity(), mRepoOwner, mRepoName, this);
        return mAdapter;
    }

    @Override
    protected void onAddData(RootAdapter<GitComment, ?> adapter, List<GitComment> data) {
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
                if (mInitialComment.matches(data.get(i).id(), data.get(i).createdAt())) {
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
    protected Single<List<GitComment>> onCreateDataSingle(boolean bypassCache) {
        List<GitComment> comments = getArguments().getParcelableArrayList(EXTRA_COMMENTS);
        if (comments != null && !comments.isEmpty()) {
            return Single.just(comments);
        }

        final RepositoryCommentService service =
                ServiceFactory.get(RepositoryCommentService.class, bypassCache);

        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitComments(mRepoOwner, mRepoName, mObjectSha, page))
                .compose(RxUtils.filter(comment -> comment.position() == null));
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
    public void editComment(GitComment comment) {
        Intent intent = EditCommitCommentActivity.makeIntent(getActivity(),
                mRepoOwner, mRepoName, mObjectSha, comment.id(), comment.body());
        startActivityForResult(intent, REQUEST_EDIT);
    }

    @Override
    public void deleteComment(final GitComment comment) {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.delete_comment_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteComment(comment.id()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void quoteText(CharSequence text) {
        mBottomSheet.addQuote(text);
    }

    @Override
    public void addText(CharSequence text) {
        mBottomSheet.addText(text);
    }

    @Override
    public int getCommentEditorHintResId() {
        return R.string.commit_comment_hint;
    }

    @Override
    public Single<?> onEditorDoSend(String comment) {
        RepositoryCommentService service = ServiceFactory.get(RepositoryCommentService.class, false);
        CreateCommitComment request = CreateCommitComment.builder().body(comment).build();
        return service.createCommitComment(mRepoOwner, mRepoName, mObjectSha, request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    public void onEditorTextSent() {
        refreshComments();
    }

    @Override
    public int getEditorErrorMessageResId() {
        return R.string.issue_error_comment;
    }

    private void refreshComments() {
        if (getActivity() instanceof CommentUpdateListener) {
            ((CommentUpdateListener) getActivity()).onCommentsUpdated();
        }
    }

    private void deleteComment(long id) {
        RepositoryCommentService service = ServiceFactory.get(RepositoryCommentService.class, false);
        service.deleteCommitComment(mRepoOwner, mRepoName, id)
                .map(ApiHelpers::throwOnFailure)
                .compose(RxUtils.wrapForBackgroundTask(getBaseActivity(),
                        R.string.deleting_msg, R.string.error_delete_comment))
                .subscribe(result -> refreshComments(),
                        error -> handleActionFailure("Deleting comment failed", error));
    }
}
