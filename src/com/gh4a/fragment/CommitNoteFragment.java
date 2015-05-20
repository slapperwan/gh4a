package com.gh4a.fragment;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.EditCommitCommentActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;

public class CommitNoteFragment extends ListDataBaseFragment<CommitComment> implements
        CommitNoteAdapter.OnEditComment, CommentBoxFragment.Callback {
    private static final int REQUEST_EDIT = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    private CommitNoteAdapter mAdapter;
    private CommentBoxFragment mCommentFragment;

    public static CommitNoteFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitNoteFragment f = new CommitNoteFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putString(Constants.Object.OBJECT_SHA, objectSha);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mObjectSha = getArguments().getString(Constants.Object.OBJECT_SHA);
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
    public boolean canChildScrollUp() {
        if (mCommentFragment != null && mCommentFragment.canChildScrollUp()) {
            return true;
        }
        return super.canChildScrollUp();
    }

    @Override
    protected RootAdapter<CommitComment> onCreateAdapter() {
        mAdapter = new CommitNoteAdapter(getActivity(), mRepoOwner, this);
        return mAdapter;
    }

    @Override
    protected int getEmptyTextResId() {
        return R.string.no_comments_found;
    }

    @Override
    protected void onItemClick(CommitComment comment) {
    }

    @Override
    public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
        return new CommitCommentListLoader(getActivity(), mRepoOwner, mRepoName,
                mObjectSha, true, false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_EDIT) {
            if (resultCode == Activity.RESULT_OK) {
                refresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void editComment(CommitComment comment) {
        Intent intent = new Intent(getActivity(), EditCommitCommentActivity.class);

        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.Comment.ID, comment.getId());
        intent.putExtra(Constants.Comment.BODY, comment.getBody());
        startActivityForResult(intent, REQUEST_EDIT);
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
        refresh();
    }
}
