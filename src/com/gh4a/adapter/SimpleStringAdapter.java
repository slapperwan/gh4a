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
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;

public class SimpleStringAdapter<T> extends RootAdapter<T> {
    public SimpleStringAdapter(Context context) {
        super(context);
    }

    @Override
    protected View createView(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_simple, null);
        ViewHolder viewHolder;
        new ViewHolder();

        Gh4Application app = (Gh4Application) mContext.getApplicationContext();
        Typeface boldCondensed = app.boldCondensed;

        viewHolder = new ViewHolder();
        viewHolder.tvTitle = (TextView) v.findViewById(R.id.tv_title);
        viewHolder.tvTitle.setTypeface(boldCondensed);
        v.setTag(viewHolder);

        return v;
    }

    @Override
    protected void bindView(View view, T object) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();
        viewHolder.tvTitle.setText(objectToString(object));
    }

    protected String objectToString(T object) {
        return object.toString();
    }

    private static class ViewHolder {
        public TextView tvTitle;
    }
}