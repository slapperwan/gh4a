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

import org.eclipse.egit.github.core.PullRequest;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ImageDownloader;

/**
 * The PullRequest adapter.
 */
public class PullRequestAdapter extends RootAdapter<PullRequest> {

    /**
     * Instantiates a new pull request adapter.
     *
     * @param context the context
     * @param objects the objects
     */
    public PullRequestAdapter(Context context, List<PullRequest> objects) {
        super(context, objects);
    }

    /* (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_issue, null);
            viewHolder = new ViewHolder();
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);
            //viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            viewHolder.llLabels = (LinearLayout) v.findViewById(R.id.ll_labels);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final PullRequest pullRequest = mObjects.get(position);
        if (pullRequest != null) {
            ImageDownloader.getInstance().download(pullRequest.getUser().getGravatarId(),
                    viewHolder.ivGravatar);
            viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    /** Open user activity */
                    Gh4Application context = (Gh4Application) v.getContext()
                            .getApplicationContext();
                    context.openUserInfoActivity(v.getContext(), pullRequest.getUser()
                            .getLogin(), null);
                }
            });

            //show labels
            viewHolder.llLabels.removeAllViews();
//            List<String> labels = pullRequest.getLabels();
//            if (labels != null && !labels.isEmpty()) {
//                for (String label : labels) {
//                    TextView tvLabel = new TextView(v.getContext());
//                    tvLabel.setSingleLine(true);
//                    tvLabel.setText(label);
//                    tvLabel.setTextAppearance(v.getContext(), R.style.default_text_small);
//                    tvLabel.setBackgroundResource(R.drawable.default_grey_box);
//                    
//                    viewHolder.llLabels.addView(tvLabel);
//                }
//                viewHolder.llLabels.setVisibility(View.VISIBLE);
//            }
//            else {
//                viewHolder.llLabels.setVisibility(View.GONE);
//            }
            
            viewHolder.tvDesc.setText("#" + pullRequest.getNumber() + " - " + pullRequest.getTitle());
            //viewHolder.tvDesc.setText(StringUtils.doTeaser(pullRequest.getBody()));
            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data_3), pullRequest
                    .getUser().getLogin(), pt.format(pullRequest.getCreatedAt()),
                    pullRequest.getComments()
                            + " "
                            + res.getQuantityString(R.plurals.issue_comment, pullRequest
                                    .getComments()));
            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv title. */
        //public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
        
        /** The ll labels. */
        public LinearLayout llLabels;

    }
}
