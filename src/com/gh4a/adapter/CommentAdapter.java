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

import java.util.List;

import org.eclipse.egit.github.core.Comment;

import android.content.Context;
import android.content.Intent;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Constants;
import com.gh4a.EditCommentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;
import com.github.mobile.util.HttpImageGetter;

public class CommentAdapter extends RootAdapter<Comment> {

    private HttpImageGetter imageGetter;
    private AQuery aq;
    private int issueNumber;
    private String issueState;
    private String repoOwner;
    private String repoName;
    
    public CommentAdapter(Context context, List<Comment> objects) {
        super(context, objects);
        imageGetter = new HttpImageGetter(mContext);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }
    
    public CommentAdapter(Context context, List<Comment> objects,
            int issueNumber, String issueState, String repoOwner, String repoName) {
        super(context, objects);
        imageGetter = new HttpImageGetter(mContext);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
        this.issueNumber = issueNumber;
        this.issueState = issueState;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gravatar_comment, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setMovementMethod(LinkMovementMethod.getInstance());
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            viewHolder.ivEdit = (ImageView) v.findViewById(R.id.iv_edit);
            viewHolder.ivEdit.setBackgroundResource(R.drawable.abs__list_selector_holo_dark);
            viewHolder.ivEdit.setPadding(10, 5, 10, 5);
            if (Gh4Application.THEME != R.style.DefaultTheme) {
                viewHolder.ivEdit.setImageResource(R.drawable.content_edit);  
            }
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }
        
        final Comment comment = mObjects.get(position);
        if (comment != null) {
            
            final Gh4Application appContext = (Gh4Application) v.getContext().getApplicationContext();
            
            aq.recycle(convertView);
            aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(comment.getUser().getGravatarId()), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);

            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    appContext.openUserInfoActivity(v.getContext(), comment.getUser().getLogin(), null);
                }
            });

            viewHolder.tvExtra.setText(comment.getUser().getLogin() + "\n" + pt.format(comment.getCreatedAt()));
            
            String body = comment.getBodyHtml();
            imageGetter.bind(viewHolder.tvDesc, body, comment.getId());
            
            if (comment.getUser().getLogin().equals(appContext.getAuthLogin())
                    || repoOwner.equals(appContext.getAuthLogin())) {
                
                viewHolder.ivEdit.setVisibility(View.VISIBLE);
                
                viewHolder.ivEdit.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        Intent intent = new Intent().setClass(mContext, EditCommentActivity.class);
                        intent.putExtra(Constants.Repository.REPO_OWNER, repoOwner);
                        intent.putExtra(Constants.Repository.REPO_NAME, repoName);
                        intent.putExtra(Constants.Issue.ISSUE_NUMBER, issueNumber);
                        intent.putExtra(Constants.Issue.ISSUE_STATE, issueState);
                        intent.putExtra(Constants.Comment.ID, comment.getId());
                        intent.putExtra(Constants.Comment.BODY, comment.getBody());
                        ((BaseSherlockFragmentActivity)mContext).startActivityForResult(intent, 0);
                    }
                });
            }
            else {
                viewHolder.ivEdit.setVisibility(View.GONE);
            }
        }
        return v;
    }
    
    private static class ViewHolder {
        
        public ImageView ivGravatar;
        public TextView tvDesc;
        public TextView tvExtra;
        public ImageView ivEdit;
        
    }
}