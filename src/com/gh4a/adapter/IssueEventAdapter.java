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
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
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
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        int layoutResId = viewType == 1 ? R.layout.row_issue_event : R.layout.row_gravatar_comment;
        View v = inflater.inflate(layoutResId, parent, false);
        ViewHolder viewHolder = new ViewHolder();

        viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
        viewHolder.ivGravatar.setOnClickListener(this);

        viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
        viewHolder.tvDesc.setMovementMethod(UiUtils.CHECKING_LINK_METHOD);
        viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
        viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);

        v.setTag(viewHolder);
        return v;
    }

    @Override
    protected void bindView(View view, IssueEventHolder event) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String ourLogin = Gh4Application.get(mContext).getAuthLogin();
        String login = event.getUser().getLogin();

        AvatarHandler.assignAvatar(viewHolder.ivGravatar, event.getUser());
        viewHolder.ivGravatar.setTag(event);

        if (event.comment != null) {
            String body = HtmlUtils.format(event.comment.getBodyHtml()).toString();
            mImageGetter.bind(viewHolder.tvDesc, body, event.comment.getId());

            String extra = formatCommentExtra(event.comment);
            viewHolder.tvExtra.setText(StringUtils.applyBoldTags(extra, null));
        } else {
            viewHolder.tvDesc.setText(formatEvent(event.event));
            viewHolder.tvExtra.setText(StringUtils.formatRelativeTime(mContext,
                    event.event.getCreatedAt(), true));
        }

        if (viewHolder.ivEdit != null) {
            if ((login.equals(ourLogin) || mRepoOwner.equals(ourLogin)) && mEditCallback != null) {
                viewHolder.ivEdit.setVisibility(View.VISIBLE);
                viewHolder.ivEdit.setTag(event.comment);
                viewHolder.ivEdit.setOnClickListener(this);
            } else {
                viewHolder.ivEdit.setVisibility(View.GONE);
            }
        }
    }

    private String formatCommentExtra(Comment comment) {
        String login = comment.getUser().getLogin();
        CharSequence timestamp = StringUtils.formatRelativeTime(mContext,
                comment.getCreatedAt(), true);

        if (comment instanceof CommitComment) {
            CommitComment commitComment = (CommitComment) comment;
            return mContext.getString(R.string.issue_commit_comment_header, login,
                    timestamp, FileUtils.getFileName(commitComment.getPath()));
        } else {
            return mContext.getString(R.string.issue_comment_header, login, timestamp);
        }
    }

    private CharSequence formatEvent(final IssueEvent event) {
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
            if (TextUtils.equals(event.getActor().getLogin(), event.getAssignee().getLogin())) {
                textResId = R.string.issue_event_assigned_self;
            } else {
                textBase = mContext.getString(R.string.issue_event_assigned,
                        event.getActor().getLogin(), event.getAssignee().getLogin());
            }
        } else if (TextUtils.equals(type, "unassigned")) {
            textResId = R.string.issue_event_unassigned;
        } else {
            return null;
        }

        if (textBase == null) {
            textBase = mContext.getString(textResId, event.getActor().getLogin());
        }
        SpannableStringBuilder text = StringUtils.applyBoldTags(textBase, null);
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
            public void updateDrawState(TextPaint ds) {
                ds.setColor(mContext.getResources().getColor(R.color.highlight));
            }
        }, pos, pos + 7, 0);

        return text;
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
        public TextView tvExtra;
        public ImageView ivEdit;
    }
}
