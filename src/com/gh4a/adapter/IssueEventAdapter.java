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
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
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

    public void destroy() {
        mImageGetter.destroy();
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_gravatar_comment, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        viewHolder.tvExtra = (StyleableTextView) v.findViewById(R.id.tv_extra);
        viewHolder.tvFile = (StyleableTextView) v.findViewById(R.id.tv_file);
        viewHolder.tvTimestamp = (TextView) v.findViewById(R.id.tv_timestamp);
        viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View view, IssueEventHolder event) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String ourLogin = Gh4Application.get().getAuthLogin();
        String login = event.getUser() != null ? event.getUser().getLogin() : null;

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, event.getUser());
        viewHolder.ivGravatar.setTag(event);

        StringUtils.applyBoldTagsAndSetText(viewHolder.tvExtra,
                mContext.getString(R.string.issue_comment_header,
                        CommitUtils.getUserLogin(mContext, event.getUser())));
        viewHolder.tvTimestamp.setText(StringUtils.formatRelativeTime(mContext,
                event.getCreatedAt(), true));

        if (event.comment instanceof CommitComment) {
            CommitComment commitComment = (CommitComment) event.comment;
            String text = mContext.getString(R.string.issue_commit_comment_file,
                    FileUtils.getFileName(commitComment.getPath()));

            viewHolder.tvFile.setVisibility(View.VISIBLE);
            StringUtils.applyBoldTagsAndSetText(viewHolder.tvFile, text);
        } else {
            viewHolder.tvFile.setVisibility(View.GONE);
        }

        if (event.comment != null) {
            String body = HtmlUtils.format(event.comment.getBodyHtml()).toString();
            mImageGetter.bind(viewHolder.tvDesc, body, event.comment.getId());
        } else {
            viewHolder.tvDesc.setText(formatEvent(event.event,
                    viewHolder.tvExtra.getTypefaceValue()));
        }

        boolean canEdit = (login != null && login.equals(ourLogin)) || mRepoOwner.equals(ourLogin);
        if (event.comment != null && canEdit && mEditCallback != null) {
            viewHolder.ivEdit.setVisibility(View.VISIBLE);
            viewHolder.ivEdit.setTag(event.comment);
            viewHolder.ivEdit.setOnClickListener(this);
        } else {
            viewHolder.ivEdit.setVisibility(View.GONE);
        }
    }

    private CharSequence formatEvent(final IssueEvent event, int typefaceValue) {
        String type = event.getEvent();
        String textBase = null;
        int textResId = 0;

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
            String actorLogin = event.getActor() != null ? event.getActor().getLogin() : null;
            String assigneeLogin = event.getAssignee() != null ? event.getAssignee().getLogin() : null;
            if (assigneeLogin != null && assigneeLogin.equals(actorLogin)) {
                textResId = R.string.issue_event_assigned_self;
            } else {
                textBase = mContext.getString(R.string.issue_event_assigned, actorLogin,
                        assigneeLogin != null ? assigneeLogin : mContext.getString(R.string.unknown));
            }
        } else if (TextUtils.equals(type, "unassigned")) {
            textBase = mContext.getString(R.string.issue_event_unassigned,
                    event.getActor().getLogin(), event.getAssignee().getLogin());
        } else {
            return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId,
                    CommitUtils.getUserLogin(mContext, event.getActor()));
        }
        SpannableStringBuilder text = StringUtils.applyBoldTags(mContext, textBase, typefaceValue);
        if (event.getCommitId() == null) {
            return text;
        }

        int pos = text.toString().indexOf("[commit]");
        if (pos < 0) {
            return text;
        }

        text.replace(pos, pos + 8, event.getCommitId().substring(0, 7));
        text.setSpan(new TypefaceSpan("monospace"), pos, pos + 7, 0);
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View view) {
                mContext.startActivity(IntentUtils.getCommitInfoActivityIntent(mContext,
                        mRepoOwner, mRepoName, event.getCommitId()));
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(UiUtils.resolveColor(mContext, android.R.attr.textColorLink));
            }
        }, pos, pos + 7, 0);

        return text;
    }

    @Override
    public void clear() {
        super.clear();
        mImageGetter.destroy();
        mImageGetter = new HttpImageGetter(mContext);
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
        }
    }

    private static class ViewHolder {
        public ImageView ivGravatar;
        public TextView tvDesc;
        public StyleableTextView tvExtra;
        public StyleableTextView tvFile;
        public TextView tvTimestamp;
        public ImageView ivEdit;
    }
}
