package com.gh4a.fragment;

import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;

import java.util.ArrayList;
import java.util.List;

public class CommitFragment extends LoadingFragmentBase implements OnClickListener {
    private static final int REQUEST_DIFF_VIEWER = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private RepositoryCommit mCommit;
    private List<CommitComment> mComments;
    protected View mContentView;

    private LoaderCallbacks<RepositoryCommit> mCommitCallback = new LoaderCallbacks<RepositoryCommit>() {
        @Override
        public Loader<LoaderResult<RepositoryCommit>> onCreateLoader(int id, Bundle args) {
            return new CommitLoader(getActivity(), mRepoOwner, mRepoName, mObjectSha);
        }

        @Override
        public void onResultReady(LoaderResult<RepositoryCommit> result) {
            if (result.handleError(getActivity())) {
                setContentShown(true);
                setContentEmpty(true);
                return;
            }
            mCommit = result.getData();
            fillDataIfReady();
        }
    };
    private LoaderCallbacks<List<CommitComment>> mCommentCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new CommitCommentListLoader(getActivity(), mRepoOwner, mRepoName,
                    mObjectSha, false, true);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            if (result.handleError(getActivity())) {
                setContentShown(true);
                setContentEmpty(true);
                return;
            }
            mComments = result.getData();
            fillDataIfReady();
        }
    };

    public static CommitFragment newInstance(String repoOwner, String repoName, String objectSha) {
        CommitFragment f = new CommitFragment();

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.commit, null);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        setContentShown(false);

        initLoader();
    }

    protected void initLoader() {
        getLoaderManager().initLoader(0, null, mCommitCallback);
        getLoaderManager().initLoader(1, null, mCommentCallback);
    }

    private void fillDataIfReady() {
        if (mCommit != null && mComments != null) {
            fillHeader();
            fillStats(mCommit.getFiles(), mComments);
            setContentShown(true);
        }
    }

    private void fillHeader() {
        final Activity activity = getActivity();
        final Gh4Application app = Gh4Application.get();

        ImageView ivGravatar = (ImageView) mContentView.findViewById(R.id.iv_gravatar);
        AvatarHandler.assignAvatar(ivGravatar, mCommit.getAuthor());

        String login = CommitUtils.getAuthorLogin(mCommit);
        if (login != null) {
            ivGravatar.setOnClickListener(this);
            ivGravatar.setTag(login);
        }

        TextView tvMessage = (TextView) mContentView.findViewById(R.id.tv_message);
        TextView tvTitle = (TextView) mContentView.findViewById(R.id.tv_title);

        String message = mCommit.getCommit().getMessage();
        int pos = message.indexOf('\n');
        String title = pos > 0 ? message.substring(0, pos) : message;
        int length = message.length();
        while (pos > 0 && pos < length && Character.isWhitespace(message.charAt(pos))) {
            pos++;
        }
        message = pos > 0 && pos < length ? message.substring(pos) : null;

        tvTitle.setText(title);
        tvMessage.setText(message);
        tvTitle.setVisibility(StringUtils.isBlank(title) ? View.GONE : View.VISIBLE);
        tvMessage.setVisibility(StringUtils.isBlank(message) ? View.GONE : View.VISIBLE);

        Commit commit = mCommit.getCommit();

        TextView tvAuthor = (TextView) mContentView.findViewById(R.id.tv_author);
        tvAuthor.setText(CommitUtils.getAuthorName(app, mCommit));

        TextView tvTimestamp = (TextView) mContentView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(
                activity, commit.getAuthor().getDate(), true));

        View committerContainer = mContentView.findViewById(R.id.committer);

        if (!CommitUtils.authorEqualsCommitter(mCommit)) {
            ImageView commitGravatar = (ImageView) mContentView.findViewById(R.id.iv_commit_gravatar);
            StyleableTextView commitExtra =
                    (StyleableTextView) mContentView.findViewById(R.id.tv_commit_extra);

            AvatarHandler.assignAvatar(commitGravatar, mCommit.getCommitter());
            String committerText = getString(R.string.commit_details,
                    CommitUtils.getCommitterName(app, mCommit),
                    StringUtils.formatRelativeTime(activity, commit.getCommitter().getDate(), true));
            StringUtils.applyBoldTagsAndSetText(commitExtra, committerText);

            committerContainer.setVisibility(View.VISIBLE);
        } else {
            committerContainer.setVisibility(View.GONE);
        }
    }

    protected void fillStats(List<CommitFile> files, List<CommitComment> comments) {
        LinearLayout llChanged = (LinearLayout) mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) mContentView.findViewById(R.id.ll_added);
        LinearLayout llRenamed = (LinearLayout) mContentView.findViewById(R.id.ll_renamed);
        LinearLayout llDeleted = (LinearLayout) mContentView.findViewById(R.id.ll_deleted);
        final LayoutInflater inflater = getLayoutInflater(null);
        int added = 0, changed = 0, renamed = 0, deleted = 0;
        int additions = 0, deletions = 0;
        int count = files != null ? files.size() : 0;
        int highlightColor = UiUtils.resolveColor(getActivity(), android.R.attr.textColorPrimary);
        ForegroundColorSpan addSpan = new ForegroundColorSpan(
                UiUtils.resolveColor(getActivity(), R.attr.colorCommitAddition));
        ForegroundColorSpan deleteSpan = new ForegroundColorSpan(
                UiUtils.resolveColor(getActivity(), R.attr.colorCommitDeletion));

        llChanged.removeAllViews();
        llAdded.removeAllViews();
        llRenamed.removeAllViews();
        llDeleted.removeAllViews();

        for (int i = 0; i < count; i++) {
            CommitFile file = files.get(i);
            final LinearLayout parent;

            switch (file.getStatus()) {
                case "added":
                    parent = llAdded;
                    added++;
                    break;
                case "modified":
                    parent = llChanged;
                    changed++;
                    break;
                case "renamed":
                    parent = llRenamed;
                    renamed++;
                    break;
                case "removed":
                    parent = llDeleted;
                    deleted++;
                    break;
                default:
                    continue;
            }

            additions += file.getAdditions();
            deletions += file.getDeletions();

            int commentCount = 0;
            for (CommitComment comment : comments) {
                if (TextUtils.equals(file.getFilename(), comment.getPath())) {
                    commentCount++;
                }
            }

            ViewGroup fileView = (ViewGroup) inflater.inflate(R.layout.commit_filename, parent, false);
            TextView fileNameView = (TextView) fileView.findViewById(R.id.filename);
            fileNameView.setText(file.getFilename());

            TextView statsView = (TextView) fileView.findViewById(R.id.stats);
            if (file.getPatch() != null) {
                SpannableStringBuilder stats = new SpannableStringBuilder();
                stats.append("+").append(String.valueOf(file.getAdditions()));
                int addLength = stats.length();
                stats.setSpan(addSpan, 0, addLength, 0);
                stats.append("\u00a0\u00a0\u00a0-").append(String.valueOf(file.getDeletions()));
                stats.setSpan(deleteSpan, addLength, stats.length(), 0);
                statsView.setText(stats);
                statsView.setVisibility(View.VISIBLE);
            } else {
                statsView.setVisibility(View.GONE);
            }

            if (file.getPatch() != null ||
                    (parent != llDeleted && FileUtils.isImage(file.getFilename()))) {
                fileNameView.setTextColor(highlightColor);
                fileView.setOnClickListener(this);
                fileView.setTag(file);
            }
            if (commentCount > 0) {
                TextView commentView = (TextView) fileView.findViewById(R.id.comments);
                commentView.setText(String.valueOf(commentCount));
                commentView.setVisibility(View.VISIBLE);
            }

            parent.addView(fileView);
        }

        adjustVisibility(R.id.card_added, added);
        adjustVisibility(R.id.card_changed, changed);
        adjustVisibility(R.id.card_renamed, renamed);
        adjustVisibility(R.id.card_deleted, deleted);

        TextView tvSummary = (TextView) mContentView.findViewById(R.id.tv_desc);
        tvSummary.setText(getString(R.string.commit_summary, added + changed + renamed + deleted,
                additions, deletions));
    }

    private void adjustVisibility(int containerResId, int count) {
        int visibility = count > 0 ? View.VISIBLE : View.GONE;
        mContentView.findViewById(containerResId).setVisibility(visibility);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            String login = (String) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(getActivity(), login);
            if (intent != null) {
                startActivity(intent);
            }
        } else {
            CommitFile file = (CommitFile) v.getTag();

            Intent intent = new Intent(getActivity(), FileUtils.isImage(file.getFilename())
                    ? FileViewerActivity.class : CommitDiffViewerActivity.class);
            intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.NAME, mRepoName);
            intent.putExtra(Constants.Object.REF, mObjectSha);
            intent.putExtra(Constants.Object.OBJECT_SHA, mObjectSha);
            intent.putExtra(Constants.Commit.DIFF, file.getPatch());
            intent.putExtra(Constants.Commit.COMMENTS, new ArrayList<>(mComments));
            intent.putExtra(Constants.Object.PATH, file.getFilename());
            startActivityForResult(intent, REQUEST_DIFF_VIEWER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DIFF_VIEWER) {
            if (resultCode == Activity.RESULT_OK) {
                // reload comments
                getLoaderManager().getLoader(1).onContentChanged();
                getActivity().setResult(Activity.RESULT_OK);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}