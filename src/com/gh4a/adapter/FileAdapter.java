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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.Tree;

/**
 * The FollowerFollowing adapter.
 */
public class FileAdapter extends RootAdapter<Tree> {

    /**
     * Instantiates a new follower following adapter.
     * 
     * @param context the context
     * @param objects the objects
     */
    public FileAdapter(Context context, List<Tree> objects) {
        super(context, objects);
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View,
     * android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_file_manager, null);
        }
        Tree tree = mObjects.get(position);
        if (tree != null) {
            ImageView ivIcon = (ImageView) v.findViewById(R.id.iv_icon);
            ivIcon.setBackgroundResource(getIconId(tree.getType(), StringUtils
                    .getFileExtension(tree.getName())));

            TextView tvFilename = (TextView) v.findViewById(R.id.tv_text);
            tvFilename.setText(tree.getName());
        }
        return v;
    }

    /**
     * Gets the icon id.
     *
     * @param type the type
     * @param ext the ext
     * @return the icon id
     */
    private int getIconId(Tree.Type type, String ext) {
        if (Tree.Type.TREE.equals(type)) {
            return R.drawable.folder;
        }
        else if (Tree.Type.BLOB.equals(type)) {
            if ("png".equalsIgnoreCase(ext) || "ico".equalsIgnoreCase(ext)
                    || "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)
                    || "gif".equalsIgnoreCase(ext)) {
                return R.drawable.picture;
            }
            return R.drawable.page_white_text;
        }
        return R.drawable.page_white_text;
    }
}