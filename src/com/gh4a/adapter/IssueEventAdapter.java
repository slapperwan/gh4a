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

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.IssueEventHolder;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HttpImageGetter;

public class IssueEventAdapter extends RootAdapter<IssueEventHolder> implements
        View.OnClickListener {
    public interface OnEditComment {
        void editComment(Comment comment);
    }

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

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        IssueEventHolder holder = getItem(position);
        return holder.comment != null ? 0 : 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        IssueEventHolder holder = getItem(position);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = holder.comment != null
                    ? createCommentView(inflater, parent) : createEventView(inflater, parent);
        }
        if (holder.comment != null) {
            bindCommentView(convertView, holder, holder.comment);
        } else {
            bindEventView(convertView, holder, holder.event);
        }

        return convertView;
    }

    private View createCommentView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        CommentViewHolder viewHolder = new CommentViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);

        v.setTag(viewHolder);
        return v;
    }

    private View createEventView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_issue_event, parent, false);
        EventViewHolder viewHolder = new EventViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);

        v.setTag(viewHolder);
        return v;
    }

    private void bindCommentView(View v, IssueEventHolder event, Comment comment) {
        CommentViewHolder viewHolder = (CommentViewHolder) v.getTag();
        String ourLogin = Gh4Application.get(mContext).getAuthLogin();
        String login = comment.getUser().getLogin();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, comment.getUser());

        String body = comment.getBodyHtml();
        mImageGetter.bind(viewHolder.tvDesc, body, comment.getId());

        viewHolder.ivGravatar.setTag(event);

        String extra;
        if (comment instanceof CommitComment) {
            CommitComment commitComment = (CommitComment) comment;
            extra = mContext.getString(R.string.issue_commit_comment_header, login,
                    StringUtils.formatRelativeTime(mContext, comment.getCreatedAt(), true),
                    FileUtils.getFileName(commitComment.getPath()));
        } else {
            extra = mContext.getString(R.string.issue_comment_header, login,
                    StringUtils.formatRelativeTime(mContext, comment.getCreatedAt(), true));
        }
        viewHolder.tvExtra.setText(StringUtils.applyBoldTags(extra, null));

        if ((login.equals(ourLogin) || mRepoOwner.equals(ourLogin)) && mEditCallback != null) {
            viewHolder.ivEdit.setVisibility(View.VISIBLE);
            viewHolder.ivEdit.setTag(comment);
            viewHolder.ivEdit.setOnClickListener(this);
        } else {
            viewHolder.ivEdit.setVisibility(View.GONE);
        }
    }

    private void bindEventView(View v, IssueEventHolder eventHolder, IssueEvent event) {
        EventViewHolder viewHolder = (EventViewHolder) v.getTag();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, event.getActor());
        viewHolder.ivGravatar.setTag(eventHolder);

        viewHolder.tvDesc.setText(formatEvent(event));
        viewHolder.tvExtra.setText(StringUtils.formatRelativeTime(mContext, event.getCreatedAt(), true));
    }

    private CharSequence formatEvent(final IssueEvent event) {
        String type = event.getEvent();
        int textResId;

        if (TextUtils.equals(type, "closed")) {
            textResId = event.getCommitId() != null
                    ? R.string.issue_event_closed_with_commit : R.string.issue_event_closed;
        } else if (TextUtils.equals(type, "reopened")) {
            textResId = R.string.issue_event_reopened;
        } else if (TextUtils.equals(type, "merged")) {
            textResId = R.string.issue_event_merged;
        } else if (TextUtils.equals(type, "referenced")) {
            textResId = event.getCommitId() != null
                    ? R.string.issue_event_referenced_with_commit : R.string.issue_event_referenced;
        } else if (TextUtils.equals(type, "assigned")) {
            textResId = R.string.issue_event_assigned;
        } else if (TextUtils.equals(type, "unassigned")) {
            textResId = R.string.issue_event_unassigned;
        } else {
            return null;
        }

        SpannableStringBuilder text = StringUtils.applyBoldTags(mContext.getString(textResId,
                event.getActor().getLogin()), null);
        if (event.getCommitId() == null) {
            return text;
        }

        int pos = text.toString().indexOf("[commit]");
        if (pos < 0) {
            return text;
        }

        text.replace(pos, pos + 8, event.getCommitId().substring(0, 7));
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                IntentUtils.openCommitInfoActivity(mContext, mRepoOwner, mRepoName,
                        event.getCommitId(), 0);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
                ds.setTypeface(Typeface.MONOSPACE);
                ds.setColor(mContext.getResources().getColor(R.color.highlight));
            }
        }, pos, pos + 7, 0);
        return text;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        // dummy only
        return null;
    }

    @Override
    protected void bindView(View view, IssueEventHolder object) {
        // dummy only
    }

    @Override
    public void clear() {
        super.clear();
        mImageGetter = new HttpImageGetter(mContext);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            IssueEventHolder event = (IssueEventHolder) v.getTag();
            IntentUtils.openUserInfoActivity(mContext, event.getUser());
        } else if (v.getId() == R.id.iv_edit) {
            Comment comment = (Comment) v.getTag();
            mEditCallback.editComment(comment);
        }
    }

    private static class CommentViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public ImageView ivEdit;
    }

    private static class EventViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
    }
}