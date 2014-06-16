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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.GravatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.github.mobile.util.HttpImageGetter;

public class CommentAdapter extends RootAdapter<Comment> implements OnClickListener {
    public interface OnEditComment {
        void editComment(Comment comment);
    }

    private HttpImageGetter mImageGetter;
    private OnEditComment mEditCallback;
    private String mRepoOwner;

    public CommentAdapter(Context context, String repoOwner, OnEditComment editCallback) {
        super(context);
        mImageGetter = new HttpImageGetter(mContext);
        mRepoOwner = repoOwner;
        mEditCallback = editCallback;
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.row_gravatar_comment, null);
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
    protected void bindView(View v, Comment comment) {
        boolean isCommitComment = comment instanceof CommitComment;
        if (!isCommitComment ||
                ((CommitComment) comment).getPosition() != -1) {
            ViewHolder viewHolder = (ViewHolder) v.getTag();
            String login = Gh4Application.get(mContext).getAuthLogin();

            GravatarHandler.assignGravatar(viewHolder.ivGravatar, comment.getUser());

            if (isCommitComment) {
                viewHolder.tvExtra.setText(mContext.getString(R.string.issue_commit_comment_header,
                        comment.getUser().getLogin(),
                        StringUtils.formatRelativeTime(mContext, comment.getCreatedAt(), true),
                        FileUtils.getFileName(((CommitComment) comment).getPath())));
            } else {
                viewHolder.tvExtra.setText(mContext.getString(R.string.issue_comment_header,
                        comment.getUser().getLogin(),
                        StringUtils.formatRelativeTime(mContext, comment.getCreatedAt(), true)));
            }

            String body = comment.getBodyHtml();
            mImageGetter.bind(viewHolder.tvDesc, body, comment.getId());

            viewHolder.ivGravatar.setTag(comment);

            if (!isCommitComment &&
                    (comment.getUser().getLogin().equals(login) || mRepoOwner.equals(login))) {
                viewHolder.ivEdit.setVisibility(View.VISIBLE);
                viewHolder.ivEdit.setTag(comment);
                viewHolder.ivEdit.setOnClickListener(this);
            } else {
                viewHolder.ivEdit.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.iv_gravatar) {
            Comment comment = (Comment) v.getTag();
            IntentUtils.openUserInfoActivity(mContext, comment.getUser());
        } else if (v.getId() == R.id.iv_edit && mEditCallback != null) {
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