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

import org.eclipse.egit.github.core.Gist;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

/**
 * The Gist adapter.
 */
public class GistAdapter  extends RootAdapter<Gist> {

    /**
     * Instantiates a new gist adapter.
     *
     * @param context the context
     * @param objects the objects
     */
    public GistAdapter(Context context, List<Gist> objects) {
        super(context, objects);
    }

    /* (non-Javadoc)
     * @see com.gh4a.adapter.RootAdapter#doGetView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder viewHolder = null;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_gist, null);

            Gh4Application app = (Gh4Application) mContext.getApplicationContext();
            Typeface boldCondensed = app.boldCondensed;
            
            viewHolder = new ViewHolder();
            viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
            viewHolder.tvDesc = (TextView) v.findViewById(R.id.tv_desc);
            viewHolder.tvDesc.setTypeface(boldCondensed);
            viewHolder.tvExtra = (TextView) v.findViewById(R.id.tv_extra);
            v.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) v.getTag();
        }

        Gist gist = mObjects.get(position);

        if (gist != null) {
            viewHolder.tvTitle.setText(gist.getId());
            if (StringUtils.isBlank(gist.getDescription())) {
                viewHolder.tvDesc.setVisibility(View.GONE);
            }
            else {
                viewHolder.tvDesc.setText(gist.getDescription());
                viewHolder.tvDesc.setVisibility(View.VISIBLE);
            }
            viewHolder.tvExtra.setText(pt.format(gist.getCreatedAt())
                    + "  "
                    + gist.getFiles().size()
                    + " " + v.getResources().getQuantityString(R.plurals.file, gist.getFiles().size()));
        }
        return v;
    }

    /**
     * The Class ViewHolder.
     */
    private static class ViewHolder {

        /** The tv title. */
        public TextView tvTitle;
        
        /** The tv desc. */
        public TextView tvDesc;
        
        /** The tv extra. */
        public TextView tvExtra;

    }
}
