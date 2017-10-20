package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.PositionalCommentBase;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.model.git.GitCommit;
import com.meisolsson.githubsdk.model.git.GitUser;
import com.vdurmont.emoji.EmojiParser;

import java.util.ArrayList;
import java.util.List;

public class CommitFragment extends LoadingFragmentBase implements OnClickListener {
    public static CommitFragment newInstance(String repoOwner, String repoName, String commitSha,
            Commit commit, List<GitComment> comments) {
        CommitFragment f = new CommitFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putString("sha", commitSha);
        args.putParcelable("commit", commit);
        args.putParcelableArrayList("comments", new ArrayList<>(comments));
        f.setArguments(args);
        return f;
    }

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private static final int REQUEST_DIFF_VIEWER = 1000;

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private Commit mCommit;
    private List<GitComment> mComments;
    protected View mContentView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString("owner");
        mRepoName = getArguments().getString("repo");
        mObjectSha = getArguments().getString("sha");
        mCommit = getArguments().getParcelable("commit");
        mComments = getArguments().getParcelableArrayList("comments");
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, ViewGroup parent) {
        mContentView = inflater.inflate(R.layout.commit, parent, false);
        return mContentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        populateUiIfReady();
    }

    @Override
    public void onRefresh() {
        // we got all our data through arguments
    }

    protected void populateUiIfReady() {
        fillHeader();
        fillStats(mCommit.files(), mComments);
    }

    protected void fillHeader() {
        final Activity activity = getActivity();
        final Gh4Application app = Gh4Application.get();

        ImageView ivGravatar = mContentView.findViewById(R.id.iv_gravatar);
        User author = mCommit.author();
        if (author != null) {
            AvatarHandler.assignAvatar(ivGravatar, author);
        } else {
            GitUser commitAuthor = mCommit.commit().author();
            String email = commitAuthor != null ? commitAuthor.email() : null;
            ivGravatar.setImageDrawable(new AvatarHandler.DefaultAvatarDrawable(null, email));
        }

        String login = ApiHelpers.getAuthorLogin(mCommit);
        if (login != null) {
            ivGravatar.setOnClickListener(this);
            ivGravatar.setTag(login);
        }

        TextView tvMessage = mContentView.findViewById(R.id.tv_message);
        TextView tvTitle = mContentView.findViewById(R.id.tv_title);

        String message = mCommit.commit().message();
        int pos = message.indexOf('\n');
        String title = pos > 0 ? message.substring(0, pos) : message;
        title = EmojiParser.parseToUnicode(title);
        int length = message.length();
        while (pos > 0 && pos < length && Character.isWhitespace(message.charAt(pos))) {
            pos++;
        }
        message = pos > 0 && pos < length ? message.substring(pos) : null;
        if (message != null) {
            message = EmojiParser.parseToUnicode(message);
        }

        tvTitle.setText(title);
        tvMessage.setText(message);
        tvTitle.setVisibility(StringUtils.isBlank(title) ? View.GONE : View.VISIBLE);
        tvMessage.setVisibility(StringUtils.isBlank(message) ? View.GONE : View.VISIBLE);
        tvMessage.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);

        GitCommit commit = mCommit.commit();

        TextView tvAuthor = mContentView.findViewById(R.id.tv_author);
        tvAuthor.setText(ApiHelpers.getAuthorName(app, mCommit));

        TextView tvTimestamp = mContentView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(
                activity, commit.author().date(), true));

        View committerContainer = mContentView.findViewById(R.id.committer);

        if (!ApiHelpers.authorEqualsCommitter(mCommit)) {
            ImageView commitGravatar = mContentView.findViewById(R.id.iv_commit_gravatar);
            StyleableTextView commitExtra =
                    mContentView.findViewById(R.id.tv_commit_extra);

            AvatarHandler.assignAvatar(commitGravatar, mCommit.committer());
            String committerText = getString(R.string.commit_details,
                    ApiHelpers.getCommitterName(app, mCommit),
                    StringUtils.formatRelativeTime(activity, commit.committer().date(), true));
            StringUtils.applyBoldTagsAndSetText(commitExtra, committerText);

            committerContainer.setVisibility(View.VISIBLE);
        } else {
            committerContainer.setVisibility(View.GONE);
        }
    }

    protected void fillStats(List<GitHubFile> files, List<? extends PositionalCommentBase> comments) {
        LinearLayout llChanged = mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = mContentView.findViewById(R.id.ll_added);
        LinearLayout llRenamed = mContentView.findViewById(R.id.ll_renamed);
        LinearLayout llDeleted = mContentView.findViewById(R.id.ll_deleted);
        final LayoutInflater inflater = getLayoutInflater();
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
            GitHubFile file = files.get(i);
            final LinearLayout parent;

            switch (file.status()) {
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

            additions += file.additions();
            deletions += file.deletions();

            int commentCount = 0;
            for (PositionalCommentBase comment : comments) {
                if (TextUtils.equals(file.filename(), comment.path())) {
                    commentCount++;
                }
            }

            ViewGroup fileView = (ViewGroup) inflater.inflate(R.layout.commit_filename, parent, false);
            TextView fileNameView = fileView.findViewById(R.id.filename);
            fileNameView.setText(file.filename());

            TextView statsView = fileView.findViewById(R.id.stats);
            if (file.patch() != null) {
                SpannableStringBuilder stats = new SpannableStringBuilder();
                stats.append("+").append(String.valueOf(file.additions()));
                int addLength = stats.length();
                stats.setSpan(addSpan, 0, addLength, 0);
                stats.append("\u00a0\u00a0\u00a0-").append(String.valueOf(file.deletions()));
                stats.setSpan(deleteSpan, addLength, stats.length(), 0);
                statsView.setText(stats);
                statsView.setVisibility(View.VISIBLE);
            } else {
                statsView.setVisibility(View.GONE);
            }

            if (file.patch() != null ||
                    (parent != llDeleted && FileUtils.isImage(file.filename()))) {
                fileNameView.setTextColor(highlightColor);
                fileView.setOnClickListener(this);
                fileView.setTag(file);
            }
            if (commentCount > 0) {
                TextView commentView = fileView.findViewById(R.id.comments);
                commentView.setText(String.valueOf(commentCount));
                commentView.setVisibility(View.VISIBLE);
            }

            parent.addView(fileView);
        }

        adjustVisibility(R.id.card_added, added);
        adjustVisibility(R.id.card_changed, changed);
        adjustVisibility(R.id.card_renamed, renamed);
        adjustVisibility(R.id.card_deleted, deleted);

        TextView tvSummary = mContentView.findViewById(R.id.tv_desc);
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
            Intent intent = UserActivity.makeIntent(getActivity(), (String) v.getTag());
            if (intent != null) {
                startActivity(intent);
            }
        } else {
            GitHubFile file = (GitHubFile) v.getTag();
            handleFileClick(file);
        }
    }

    protected void handleFileClick(GitHubFile file) {
        final Intent intent;
        if (FileUtils.isImage(file.filename())) {
            intent = FileViewerActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mObjectSha, file.filename());
        } else {
            intent = CommitDiffViewerActivity.makeIntent(getActivity(), mRepoOwner, mRepoName,
                    mObjectSha, file.filename(), file.patch(),
                    commentsForFile(file), -1, -1, false, null);
        }
        startActivityForResult(intent, REQUEST_DIFF_VIEWER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DIFF_VIEWER) {
            if (resultCode == Activity.RESULT_OK) {
                // reload comments
                if (getActivity() instanceof CommentUpdateListener) {
                    ((CommentUpdateListener) getActivity()).onCommentsUpdated();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private ArrayList<GitComment> commentsForFile(GitHubFile file) {
        if (mComments == null) {
            return null;
        }
        String path = file.filename();
        ArrayList<GitComment> result = null;
        for (GitComment comment : mComments) {
            if (!TextUtils.equals(comment.path(), path)) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>();
            }
            result.add(comment);
        }
        return result;
    }
}