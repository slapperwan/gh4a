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

import org.eclipse.egit.github.core.RepositoryContents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.FileUtils;

public class FileAdapter extends RootAdapter<RepositoryContents> {

    public FileAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_file_manager, null);
        ViewHolder holder = new ViewHolder();
        
        holder.icon = (ImageView) v.findViewById(R.id.iv_icon);
        holder.fileName = (TextView) v.findViewById(R.id.tv_text);
        
        v.setTag(holder);
        return v;
    }
    
    @Override
    protected void bindView(View v, RepositoryContents content) {
        ViewHolder holder = (ViewHolder) v.getTag();

        holder.icon.setBackgroundResource(getIconId(content.getType(),
                FileUtils.getFileExtension(content.getName())));
        holder.fileName.setText(content.getName());
    }

    private int getIconId(String type, String ext) {
        if (RepositoryContents.TYPE_DIR.equals(type)) {
            return R.drawable.folder;
        } else if (RepositoryContents.TYPE_FILE.equals(type)) {
            if ("png".equalsIgnoreCase(ext) || "ico".equalsIgnoreCase(ext)
                    || "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)
                    || "gif".equalsIgnoreCase(ext)) {
                return R.drawable.picture;
            }
            return R.drawable.page_white_text;
        }
        return R.drawable.page_white_text;
    }
    
    private static class ViewHolder {
        ImageView icon;
        TextView fileName;
    }
}