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

public class BookmarkAdapter extends CursorAdapter {
    public BookmarkAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ImageView icon = (ImageView) view.findViewById(R.id.iv_icon);
        TextView title = (TextView) view.findViewById(R.id.tv_title);
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(Columns.TYPE));
        String name = cursor.getString(cursor.getColumnIndexOrThrow(Columns.NAME));
        boolean darkTheme = Gh4Application.THEME == R.style.DefaultTheme;

        switch (type) {
            case Columns.TYPE_REPO:
                icon.setImageResource(darkTheme ? R.drawable.search_repos_dark : R.drawable.search_repos);
                break;
            case Columns.TYPE_USER:
                icon.setImageResource(darkTheme ? R.drawable.search_users_dark : R.drawable.search_users);
                break;
            default:
                icon.setImageDrawable(null);
                break;
        }

        title.setText(name);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(R.layout.row_bookmark, null);
    }
}
