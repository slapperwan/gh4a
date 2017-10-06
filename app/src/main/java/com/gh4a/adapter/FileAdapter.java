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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;

import java.util.Set;

public class FileAdapter extends RootAdapter<Content, FileAdapter.ViewHolder> {
    private Set<String> mSubModuleNames;

    public FileAdapter(Context context) {
        super(context);
    }

    public void setSubModuleNames(Set<String> subModules) {
        mSubModuleNames = subModules;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_file_manager, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, Content content) {
        String name = content.name();
        boolean isSubModule = mSubModuleNames != null && mSubModuleNames.contains(name);

        holder.icon.setBackgroundResource(getIconId(content.type(), name));
        holder.fileName.setText(name);

        if (!isSubModule && content.type() == ContentType.File) {
            holder.fileSize.setText(Formatter.formatShortFileSize(mContext, content.size()));
            holder.fileSize.setVisibility(View.VISIBLE);
        } else {
            holder.fileSize.setVisibility(View.GONE);
        }
    }

    private int getIconId(ContentType type, String fileName) {
        int iconId;
        if (mSubModuleNames != null && mSubModuleNames.contains(fileName)) {
            iconId = R.attr.submoduleIcon;
        } else if (type == ContentType.Directory) {
            iconId = R.attr.dirIcon;
        } else if (type == ContentType.File && FileUtils.isImage(fileName)) {
            iconId = R.attr.contentPictureIcon;
        } else {
            iconId = R.attr.fileIcon;
        }

        return UiUtils.resolveDrawable(mContext, iconId);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.iv_icon);
            fileName = view.findViewById(R.id.tv_text);
            fileSize = view.findViewById(R.id.tv_size);
        }

        private final ImageView icon;
        private final TextView fileName;
        private final TextView fileSize;
    }
}