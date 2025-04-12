package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
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
        // Commits can be huge, depending on the number of patches attached to it,
        // and can potentially have a very high number of comments.
        // In order to avoid TransactionTooLargeExceptions being thrown when the activity we're
        // attached to is stopped, store the data in compressed form.
        IntentUtils.putCompressedValueToBundle(args, "commit", commit);
        IntentUtils.putCompressedValueToBundle(args, "comments", comments);
        f.setArguments(args);
        return f;
    }

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private Commit mCommit;
    private List<GitComment> mComments;
    protected View mContentView;

    private final ActivityResultLauncher<Intent> mDiffViewerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                // reload comments
                if (getActivity() instanceof CommentUpdateListener) {
                    ((CommentUpdateListener) getActivity()).onCommentsUpdated();
                }
            }));

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mObjectSha = args.getString("sha");
        mCommit = IntentUtils.readCompressedValueFromBundle(args, "commit");
        mComments = IntentUtils.readCompressedValueFromBundle(args, "comments");
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

    private void fillHeader() {
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

        GitCommit commit = mCommit.commit();

        TextView tvAuthor = mContentView.findViewById(R.id.tv_author);
        tvAuthor.setText(ApiHelpers.getAuthorName(app, mCommit));

        TextView tvTimestamp = mContentView.findViewById(R.id.tv_timestamp);
        tvTimestamp.setText(StringUtils.formatRelativeTime(
                activity, commit.author().date(), true));

        View committerContainer = mContentView.findViewById(R.id.committer);

        if (!ApiHelpers.authorEqualsCommitter(mCommit)) {
            ImageView commitGravatar = mContentView.findViewById(R.id.iv_commit_gravatar);
            TextView commitExtra = mContentView.findViewById(R.id.tv_commit_extra);

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
        llChanged.removeAllViews();
        llAdded.removeAllViews();
        llRenamed.removeAllViews();
        llDeleted.removeAllViews();

        int addedFiles = 0, changedFiles = 0, renamedFiles = 0, deletedFiles = 0;
        int totalAdditions = 0, totalDeletions = 0;
        int filesCount = files != null ? files.size() : 0;
        int highlightColor = UiUtils.resolveColor(getActivity(), android.R.attr.textColorPrimary);
        ForegroundColorSpan additionsSpan = new ForegroundColorSpan(
                UiUtils.resolveColor(getActivity(), R.attr.colorCommitAddition));
        ForegroundColorSpan deletionsSpan = new ForegroundColorSpan(
                UiUtils.resolveColor(getActivity(), R.attr.colorCommitDeletion));

        for (int i = 0; i < filesCount; i++) {
            GitHubFile file = files.get(i);
            final LinearLayout parent;

            switch (file.status()) {
                case "added":
                    parent = llAdded;
                    addedFiles++;
                    break;
                case "modified":
                    parent = llChanged;
                    changedFiles++;
                    break;
                case "renamed":
                    parent = llRenamed;
                    renamedFiles++;
                    break;
                case "removed":
                    parent = llDeleted;
                    deletedFiles++;
                    break;
                default:
                    continue;
            }

            totalAdditions += file.additions();
            totalDeletions += file.deletions();

            View fileView = getLayoutInflater().inflate(R.layout.commit_filename, parent, false);
            TextView fileNameView = fileView.findViewById(R.id.filename);

            fillFileName(fileNameView, file);
            fillFileStats(fileView, file, additionsSpan, deletionsSpan);
            fillFileCommentsCount(fileView, file, comments);

            if (file.patch() != null ||
                    (parent != llDeleted && FileUtils.isImage(file.filename()))) {
                fileNameView.setTextColor(highlightColor);
                fileView.setOnClickListener(this);
                fileView.setTag(file);
            }

            parent.addView(fileView);
        }

        adjustVisibility(R.id.card_added, addedFiles);
        adjustVisibility(R.id.card_changed, changedFiles);
        adjustVisibility(R.id.card_renamed, renamedFiles);
        adjustVisibility(R.id.card_deleted, deletedFiles);

        TextView tvSummary = mContentView.findViewById(R.id.tv_desc);
        tvSummary.setText(getString(R.string.commit_summary,
                addedFiles + changedFiles + renamedFiles + deletedFiles,
                totalAdditions, totalDeletions));
    }

    private void fillFileName(TextView fileNameView, GitHubFile file) {
        if (file.previousFilename() != null) {
            SpannableStringBuilder fileNames = new SpannableStringBuilder();
            fileNames.append(file.previousFilename()).append('\n').append(file.filename());
            fileNames.setSpan(new StrikethroughSpan(), 0, file.previousFilename().length(), 0);
            fileNameView.setText(fileNames);
        } else {
            fileNameView.setText(file.filename());
        }
    }

    private void fillFileStats(View fileView, GitHubFile file, ForegroundColorSpan additionsSpan,
            ForegroundColorSpan deletionsSpan) {
        TextView statsView = fileView.findViewById(R.id.stats);
        if (file.additions() > 0 || file.deletions() > 0) {
            SpannableStringBuilder stats = new SpannableStringBuilder();
            stats.append("+").append(String.valueOf(file.additions()));
            int addLength = stats.length();
            stats.setSpan(additionsSpan, 0, addLength, 0);
            stats.append("\u00a0\u00a0\u00a0-").append(String.valueOf(file.deletions()));
            stats.setSpan(deletionsSpan, addLength, stats.length(), 0);
            statsView.setText(stats);
            statsView.setVisibility(View.VISIBLE);
        } else {
            statsView.setVisibility(View.GONE);
        }
    }

    private void fillFileCommentsCount(View fileView, GitHubFile file,
            List<? extends PositionalCommentBase> comments) {
        int commentCount = 0;
        for (PositionalCommentBase comment : comments) {
            if (TextUtils.equals(file.filename(), comment.path())) {
                commentCount++;
            }
        }
        if (commentCount > 0) {
            TextView commentView = fileView.findViewById(R.id.comments);
            commentView.setText(String.valueOf(commentCount));
            commentView.setVisibility(View.VISIBLE);
        }
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
        mDiffViewerLauncher.launch(intent);
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