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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.gh4a.BaseSherlockFragmentActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.GravatarUtils;
import com.gh4a.utils.StringUtils;

public class CompareAdapter extends RootAdapter<String[]> {

    private AQuery aq;
    
    public CompareAdapter(Context context, List<String[]> objects) {
        super(context, objects);
        aq = new AQuery((BaseSherlockFragmentActivity) context);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_commit, null);
            
            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            viewHolder.ivGravatar = (ImageView) v.findViewById(R.id.iv_gravatar);

            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final String[] commitInfo = mObjects.get(position);
        if (commitInfo != null) {
            aq.recycle(convertView);
            aq.id(viewHolder.ivGravatar).image(GravatarUtils.getGravatarUrl(StringUtils.md5Hex(commitInfo[1])), 
                    true, false, 0, 0, aq.getCachedImage(R.drawable.default_avatar), 0);
            
//            if (!StringUtils.isBlank(commitInfo[3])) {
//                viewHolder.ivGravatar.setOnClickListener(new OnClickListener() {
//
//                    @Override
//                    public void onClick(View v) {
//                        /** Open user activity */
//                        Gh4Application context = (Gh4Application) v.getContext()
//                                .getApplicationContext();
//                        context.openUserInfoActivity(v.getContext(), commitInfo[3],
//                                null);
//                    }
//                });
//            }

            //viewHolder.tvSha.setText(commitInfo[0].substring(0, 7));
            viewHolder.tvDesc.setText(commitInfo[2]);

            Resources res = v.getResources();
            String extraData = String.format(res.getString(R.string.more_data_1), 
                    !StringUtils.isBlank(commitInfo[3]) ? commitInfo[3] : "");

            viewHolder.tvExtra.setText(extraData);
        }
        return v;
    }
    
    private static class ViewHolder {
        
        /** The iv gravatar. */
        public ImageView ivGravatar;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;
        
        /** The sha. */
        public TextView tvSha;
    }

}
