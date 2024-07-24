package com.gh4a.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gh4a.R;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ItemsWithDescriptionAdapter extends ArrayAdapter<ItemsWithDescriptionAdapter.Item> {
    public record Item(String title, String description) {}

    private final LayoutInflater mInflater;

    public ItemsWithDescriptionAdapter(Context context, List<Item> items) {
        super(context, R.layout.row_item_with_description, items);
        mInflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.row_item_with_description, parent, false);
        } else {
            view = convertView;
        }

        TextView titleView = view.findViewById(R.id.title);
        TextView descriptionView = view.findViewById(R.id.description);
        var item = getItem(position);

        titleView.setText(item.title());
        descriptionView.setText(item.description());
        descriptionView.setVisibility(TextUtils.isEmpty(item.description()) ? View.GONE : View.VISIBLE);

        return view;
    }
}
