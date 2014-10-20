package com.gh4a.fragment;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.activities.EditCommitCommentActivity;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

public class CommitNoteFragment extends ListDataBaseFragment<CommitComment> implements
        View.OnClickListener, CommitNoteAdapter.OnEditComment {
    private static final int REQUEST_EDIT = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

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

        if (!Gh4Application.get(getActivity()).isAuthorized()) {
            v.findViewById(R.id.comment).setVisibility(View.GONE);
            v.findViewById(R.id.divider).setVisibility(View.GONE);
        }

        v.findViewById(R.id.iv_comment).setOnClickListener(this);

        return v;
    }

    @Override
    protected RootAdapter<CommitComment> onCreateAdapter() {
        getListView().setDivider(null);
        getListView().setDividerHeight(0);
        return new CommitNoteAdapter(getActivity(), mRepoOwner, this);
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
    public void onClick(View v) {
        EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
        String text = etComment.getText() == null ? null : etComment.getText().toString();

        if (!StringUtils.isBlank(text)) {
            new CommentCommitTask(text).execute();
        }
        UiUtils.hideImeForView(getActivity().getCurrentFocus());
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

    private class CommentCommitTask extends ProgressDialogTask<Void> {
        private String mText;

        public CommentCommitTask(String text) {
            super(getActivity(), 0, R.string.loading_msg);
            mText = text;
        }

        @Override
        protected Void run() throws IOException {
            CommitService commitService = (CommitService) Gh4Application.get(mContext).getService(
                    Gh4Application.COMMIT_SERVICE);
            CommitComment commitComment = new CommitComment();
            commitComment.setBody(mText);
            commitService.addComment(new RepositoryId(mRepoOwner, mRepoName), mObjectSha, commitComment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            refresh();
            EditText etComment = (EditText) getView().findViewById(R.id.et_comment);
            etComment.setText(null);
            etComment.clearFocus();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_comment);
        }
    }
}
