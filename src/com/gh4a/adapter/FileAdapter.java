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
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.CommitHistoryActivity;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;

import java.util.Set;

public class FileAdapter extends RootAdapter<RepositoryContents, FileAdapter.ViewHolder>
        implements View.OnCreateContextMenuListener {
    private static final int MENU_HISTORY = 1;

    private Repository mRepository;
    private String mRef;
    private Set<String> mSubModuleNames;

    public FileAdapter(Context context, Repository repository, String ref) {
        super(context);
        mRepository = repository;
        mRef = ref;
    }

    public void setSubModuleNames(Set<String> subModules) {
        mSubModuleNames = subModules;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_file_manager, parent, false);
        v.setOnCreateContextMenuListener(this);
        return new ViewHolder(v);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        ViewHolder holder = (ViewHolder) v.getTag();

        if (mSubModuleNames == null || !mSubModuleNames.contains(holder.contents.getName())) {
            Intent historyIntent = new Intent(mContext, CommitHistoryActivity.class);
            historyIntent.putExtra(Constants.Repository.OWNER, mRepository.getOwner().getLogin());
            historyIntent.putExtra(Constants.Repository.NAME, mRepository.getName());
            historyIntent.putExtra(Constants.Object.PATH, holder.contents.getPath());
            historyIntent.putExtra(Constants.Object.REF, mRef);

            menu.add(Menu.NONE, MENU_HISTORY, Menu.NONE, R.string.history).setIntent(historyIntent);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, RepositoryContents content) {
        String name = content.getName();
        boolean isSubModule = mSubModuleNames != null && mSubModuleNames.contains(name);

        holder.contents = content;
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            icon = (ImageView) view.findViewById(R.id.iv_icon);
            fileName = (TextView) view.findViewById(R.id.tv_text);
            fileSize = (TextView) view.findViewById(R.id.tv_size);
        }

        private RepositoryContents contents;

        private ImageView icon;
        private TextView fileName;
        private TextView fileSize;
    }
}