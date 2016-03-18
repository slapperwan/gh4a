/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.adapter;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Rename;
import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.utils.CommitUtils;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.StyleableTextView;
import com.github.mobile.util.HtmlUtils;
import com.github.mobile.util.HttpImageGetter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueEventAdapter extends RootAdapter<IssueEventHolder, IssueEventAdapter.ViewHolder>
        implements View.OnClickListener {
    public interface OnEditComment {
        void editComment(Comment comment);
    }

    private static final Pattern COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN =
            Pattern.compile(".*github.com\\/repos\\/([^\\/]+)\\/([^\\/]+)\\/commits");

    private HttpImageGetter mImageGetter;
    private OnEditComment mEditCallback;
    private String mRepoOwner;
    private String mRepoName;

    public IssueEventAdapter(Context context, String repoOwner, String repoName,
            OnEditComment editCallback) {
        super(context);
        mImageGetter = new HttpImageGetter(mContext);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mEditCallback = editCallback;
    }

    public void resume() {
        mImageGetter.resume();
    }

    public void pause() {
        mImageGetter.pause();
    }

    public void destroy() {
        mImageGetter.destroy();
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        ViewHolder holder = new ViewHolder(v);
        holder.ivGravatar.setOnClickListener(this);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, IssueEventHolder event) {
        String ourLogin = Gh4Application.get().getAuthLogin();
        String login = event.getUser() != null ? event.getUser().getLogin() : null;

        AvatarHandler.assignAvatar(holder.ivGravatar, event.getUser());
        holder.ivGravatar.setTag(event);

        StringUtils.applyBoldTagsAndSetText(holder.tvExtra,
                mContext.getString(R.string.issue_comment_header,
                        CommitUtils.getUserLogin(mContext, event.getUser())));
        holder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                event.getCreatedAt(), true));

        if (event.comment instanceof CommitComment) {
            CommitComment commitComment = (CommitComment) event.comment;
            String text = mContext.getString(R.string.issue_commit_comment_file,
                    FileUtils.getFileName(commitComment.getPath()));

            holder.tvFile.setVisibility(View.VISIBLE);
            StringUtils.applyBoldTagsAndSetText(holder.tvFile, text);
        } else {
            holder.tvFile.setVisibility(View.GONE);
        }

        if (event.comment != null) {
            String body = HtmlUtils.format(event.comment.getBodyHtml()).toString();
            mImageGetter.bind(holder.tvDesc, body, event.comment.getId());
        } else {
            holder.tvDesc.setTag(null);
            holder.tvDesc.setText(formatEvent(event.event, event.getUser(),
                    holder.tvExtra.getTypefaceValue()));
        }

        boolean canEdit = (login != null && login.equals(ourLogin)) || mRepoOwner.equals(ourLogin);
        if (event.comment != null && canEdit && mEditCallback != null) {
            holder.ivEdit.setVisibility(View.VISIBLE);
            holder.ivEdit.setTag(event.comment);
            holder.ivEdit.setOnClickListener(this);
        } else {
            holder.ivEdit.setVisibility(View.GONE);
        }
    }

    private CharSequence formatEvent(final IssueEvent event, final User user, int typefaceValue) {
        String textBase = null;
        int textResId = 0;

        switch (event.getEvent()) {
            case IssueEvent.TYPE_CLOSED:
                textResId = event.getCommitId() != null
                        ? R.string.issue_event_closed_with_commit
                        : R.string.issue_event_closed;
                break;
            case IssueEvent.TYPE_REOPENED:
                textResId = R.string.issue_event_reopened;
                break;
            case IssueEvent.TYPE_MERGED:
                textResId = event.getCommitId() != null
                        ? R.string.issue_event_merged_with_commit
                        : R.string.issue_event_merged;
                break;
            case IssueEvent.TYPE_REFERENCED:
                textResId = event.getCommitId() != null
                        ? R.string.issue_event_referenced_with_commit
                        : R.string.issue_event_referenced;
                break;
            case IssueEvent.TYPE_ASSIGNED:
            case IssueEvent.TYPE_UNASSIGNED: {
                boolean isAssign = TextUtils.equals(event.getEvent(), IssueEvent.TYPE_ASSIGNED);
                String actorLogin = user != null ? user.getLogin() : null;
                String assigneeLogin = event.getAssignee() != null
                        ? event.getAssignee().getLogin() : null;
                if (assigneeLogin != null && assigneeLogin.equals(actorLogin)) {
                    textResId = isAssign
                            ? R.string.issue_event_assigned_self
                            : R.string.issue_event_unassigned_self;
                } else {
                    textResId = isAssign
                            ? R.string.issue_event_assigned
                            : R.string.issue_event_unassigned;
                    textBase = mContext.getString(textResId,
                            CommitUtils.getUserLogin(mContext, user),
                            CommitUtils.getUserLogin(mContext, event.getAssignee()));
                }
                break;
            }
            case IssueEvent.TYPE_LABELED:
                textResId = R.string.issue_event_labeled;
                break;
            case IssueEvent.TYPE_UNLABELED:
                textResId = R.string.issue_event_unlabeled;
                break;
            case IssueEvent.TYPE_LOCKED:
                textResId = R.string.issue_event_locked;
                break;
            case IssueEvent.TYPE_UNLOCKED:
                textResId = R.string.issue_event_unlocked;
                break;
            case IssueEvent.TYPE_MILESTONED:
            case IssueEvent.TYPE_DEMILESTONED:
                textResId = TextUtils.equals(event.getEvent(), IssueEvent.TYPE_MILESTONED)
                        ? R.string.issue_event_milestoned : R.string.issue_event_demilestoned;
                textBase = mContext.getString(textResId, event.getMilestone().getTitle(),
                        CommitUtils.getUserLogin(mContext, user));
                break;
            case IssueEvent.TYPE_RENAMED: {
                Rename rename = event.getRename();
                textBase = mContext.getString(R.string.issue_event_renamed,
                        rename.getFrom(), rename.getTo(), CommitUtils.getUserLogin(mContext, user));
                break;
            }
            default:
                return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId, CommitUtils.getUserLogin(mContext, user));
        }
        SpannableStringBuilder text = StringUtils.applyBoldTags(mContext, textBase, typefaceValue);

        int pos = text.toString().indexOf("[commit]");
        if (event.getCommitId() != null && pos >= 0) {
            text.replace(pos, pos + 8, event.getCommitId().substring(0, 7));
            text.setSpan(new TypefaceSpan("monospace"), pos, pos + 7, 0);
            text.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    // The commit might be in a different repo. The API doesn't provide
                    // that information directly, so get it indirectly by parsing the URL
                    String repoOwner = mRepoOwner, repoName = mRepoName;
                    String url = event.getCommitUrl();
                    if (url != null) {
                        Matcher matcher = COMMIT_URL_REPO_NAME_AND_OWNER_PATTERN.matcher(url);
                        if (matcher.find()) {
                            repoOwner = matcher.group(1);
                            repoName = matcher.group(2);
                        }
                    }

                    mContext.startActivity(IntentUtils.getCommitInfoActivityIntent(mContext,
                            repoOwner, repoName, event.getCommitId()));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(UiUtils.resolveColor(mContext, android.R.attr.textColorLink));
                }
            }, pos, pos + 7, 0);
        }

        pos = text.toString().indexOf("[label]");
        Label label = event.getLabel();
        if (label != null && pos >= 0) {
            int bgColor = UiUtils.colorForLabel(label);
            int fgColor = UiUtils.textColorForBackground(mContext, bgColor);
            int length = label.getName().length();
            text.replace(pos, pos + 7, label.getName());
            text.setSpan(new BackgroundColorSpan(bgColor), pos, pos + length, 0);
            text.setSpan(new ForegroundColorSpan(fgColor), pos, pos + length, 0);
        }

        return text;
    }

    @Override
    public void clear() {
        super.clear();
        boolean resumed = mImageGetter.isResumed();
        mImageGetter.destroy();
        mImageGetter = new HttpImageGetter(mContext);
        if (resumed) {
            mImageGetter.resume();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            IssueEventHolder event = (IssueEventHolder) v.getTag();
            Intent intent = IntentUtils.getUserActivityIntent(mContext, event.getUser());
            if (intent != null) {
                mContext.startActivity(intent);
            }
        } else if (v.getId() == R.id.iv_edit) {
            Comment comment = (Comment) v.getTag();
            mEditCallback.editComment(comment);
        } else {
            super.onClick(v);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            ivGravatar = (ImageView) view.findViewById(R.id.iv_gravatar);
            tvDesc = (TextView) view.findViewById(R.id.tv_desc);
            tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
            tvExtra = (StyleableTextView) view.findViewById(R.id.tv_extra);
            tvFile = (StyleableTextView) view.findViewById(R.id.tv_file);
            tvTimestamp = (TextView) view.findViewById(R.id.tv_timestamp);
            ivEdit = (ImageView) view.findViewById(R.id.iv_edit);
        }

        private ImageView ivGravatar;
        private TextView tvDesc;
        private StyleableTextView tvExtra;
        private StyleableTextView tvFile;
        private TextView tvTimestamp;
        private ImageView ivEdit;
    }
}
