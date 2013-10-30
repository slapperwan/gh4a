package com.gh4a.fragment;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.CommitNoteAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

public class CommitNoteFragment extends ListDataBaseFragment<CommitComment> implements OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    public static CommitNoteFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitNoteFragment f = new CommitNoteFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.REPO_OWNER, repoOwner);
        args.putString(Constants.Repository.REPO_NAME, repoName);
        args.putString(Constants.Object.OBJECT_SHA, objectSha);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getArguments().getString(Constants.Repository.REPO_NAME);
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
        return new CommitNoteAdapter(getActivity());
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
        return new CommitCommentListLoader(getActivity(), mRepoOwner, mRepoName, mObjectSha);
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
            getLoaderManager().restartLoader(0, null, CommitNoteFragment.this);

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
