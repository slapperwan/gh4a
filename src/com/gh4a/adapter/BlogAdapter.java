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

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html.ImageGetter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.holder.Blog;

public class BlogAdapter extends RootAdapter<Blog> {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
    
    public BlogAdapter(Context context, List<Blog> objects) {
        super(context, objects);
    }

    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_simple_3, null);
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        final Blog blog = mObjects.get(position);
        if (blog != null) {
            viewHolder.tvTitle.setText(blog.getTitle());

            viewHolder.tvDesc.setText(blog.getContent().replaceAll("<(.|\n)*?>",""));
            viewHolder.tvDesc.setSingleLine(true);
            
            viewHolder.tvExtra.setText(blog.getAuthor() + " | " + sdf.format(blog.getPublished()));
        }
        return v;
    }

    private static class BlogImageGetter implements ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            try {
                InputStream is = (InputStream)new URL(source).getContent();
                Drawable d = Drawable.createFromStream(is, "img");
                return d;
            } catch (Exception e) {
                System.out.println("Exc=" + e);
                return null;
            }
        }

        
    }
    
    private static class ViewHolder {
        public TextView tvTitle;
        public TextView tvDesc;
        public TextView tvExtra;

    }
}
