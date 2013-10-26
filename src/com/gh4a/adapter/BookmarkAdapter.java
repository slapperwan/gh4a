package com.gh4a.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider.Columns;
import com.gh4a.utils.StringUtils;

public class BookmarkAdapter extends CursorAdapter {
    public BookmarkAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Columns.TYPE));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(Columns.NAME));
        String extraData = cursor.getString(cursor.getColumnIndexOrThrow(Columns.EXTRA));
        boolean darkTheme = Gh4Application.THEME == R.style.DefaultTheme;

        switch (type) {
            case Columns.TYPE_REPO:
                holder.icon.setImageResource(
                        darkTheme ? R.drawable.search_repos_dark : R.drawable.search_repos);
                break;
            case Columns.TYPE_USER:
                holder.icon.setImageResource(
                        darkTheme ? R.drawable.search_users_dark : R.drawable.search_users);
                break;
            default:
                holder.icon.setImageDrawable(null);
                break;
        }

        holder.title.setText(name);
        if (StringUtils.isBlank(extraData)) {
            holder.extra.setVisibility(View.GONE);
        } else {
            holder.extra.setText(extraData);
            holder.extra.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_bookmark, parent, false);
        ViewHolder holder = new ViewHolder();

        holder.icon = (ImageView) view.findViewById(R.id.iv_icon);
        holder.title = (TextView) view.findViewById(R.id.tv_title);
        holder.extra = (TextView) view.findViewById(R.id.tv_extra);
        view.setTag(holder);

        return view;
    }

    private static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView extra;
    }
}
