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
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.RepositoryContents;

import java.util.Set;

public class FileAdapter extends RootAdapter<RepositoryContents> {
    private Set<String> mSubModuleNames;

    public FileAdapter(Context context) {
        super(context);
    }

    public void setSubModuleNames(Set<String> subModules) {
        mSubModuleNames = subModules;
        notifyDataSetChanged();
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_file_manager, parent, false);
        ViewHolder holder = new ViewHolder();

        holder.icon = (ImageView) v.findViewById(R.id.iv_icon);
        holder.fileName = (TextView) v.findViewById(R.id.tv_text);
        holder.fileSize = (TextView) v.findViewById(R.id.tv_size);

        v.setTag(holder);
        return v;
    }

    @Override
    protected void bindView(View v, RepositoryContents content) {
        ViewHolder holder = (ViewHolder) v.getTag();
        String name = content.getName();
        boolean isSubModule = mSubModuleNames != null && mSubModuleNames.contains(name);

        holder.icon.setBackgroundResource(getIconId(content.getType(), name));
        holder.fileName.setText(name);

        if (!isSubModule && RepositoryContents.TYPE_FILE.equals(content.getType())) {
            holder.fileSize.setText(Formatter.formatShortFileSize(mContext, content.getSize()));
            holder.fileSize.setVisibility(View.VISIBLE);
        } else {
            holder.fileSize.setVisibility(View.GONE);
        }
    }

    private int getIconId(String type, String fileName) {
        int iconId;
        if (mSubModuleNames != null && mSubModuleNames.contains(fileName)) {
            iconId = R.attr.submoduleIcon;
        } else if (RepositoryContents.TYPE_DIR.equals(type)) {
            iconId = R.attr.dirIcon;
        } else if (RepositoryContents.TYPE_FILE.equals(type) && FileUtils.isImage(fileName)) {
            iconId = R.attr.contentPictureIcon;
        } else {
            iconId = R.attr.fileIcon;
        }

        return UiUtils.resolveDrawable(mContext, iconId);
    }

    private static class ViewHolder {
        ImageView icon;
        TextView fileName;
        TextView fileSize;
    }
}