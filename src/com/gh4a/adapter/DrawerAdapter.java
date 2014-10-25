package com.gh4a.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.gh4a.R;

import java.util.List;

public class DrawerAdapter extends BaseAdapter {
    private static final int ITEM_TYPE_SECTION = 0;
    private static final int ITEM_TYPE_SECTION_ENTRY = 1;
    private static final int ITEM_TYPE_MISC = 2;
    private static final int ITEM_TYPE_RADIO = 3;

    public static abstract class Item {
        private final int mTitleResId;
        private final int mId;
        protected Item(int titleResId, int id) {
            mTitleResId = titleResId;
            mId = id;
        }
        public int getId() {
            return mId;
        }
        protected abstract int getItemType();
    }

    public static class SectionItem extends Item {
        public SectionItem(int titleResId) {
            super(titleResId, 0);
        }

        @Override
        protected int getItemType() {
            return ITEM_TYPE_SECTION;
        }
    }

    public static class SectionEntryItem extends Item {
        private final int mIconResId;

        public SectionEntryItem(int titleResId, int iconResId, int id) {
            super(titleResId, id);
            mIconResId = iconResId;
        }

        @Override
        protected int getItemType() {
            return ITEM_TYPE_SECTION_ENTRY;
        }
    }

    public static class MiscItem extends Item {
        public MiscItem(int titleResId, int iconResId, int id) {
            super(titleResId, id);
        }

        @Override
        protected int getItemType() {
            return ITEM_TYPE_MISC;
        }
    }

    public static class RadioItem extends Item {
        private boolean mChecked = false;

        public void setChecked(boolean checked) {
            mChecked = checked;
        }

        public RadioItem(int titleResId, int id) {
            super(titleResId, id);
        }

        @Override
        protected int getItemType() {
            return ITEM_TYPE_RADIO;
        }
    }

    private Context mContext;
    private LayoutInflater mInflater;
    private List<Item> mItems;

    public DrawerAdapter(Context context, List<Item> items) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).getItemType();
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean isEnabled(int position) {
        return mItems.get(position).getId() != 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        Item item = mItems.get(position);
        int type = item.getItemType();

        if (convertView == null) {
            final int layoutResId =
                    type == ITEM_TYPE_MISC ? R.layout.drawer_misc_entry :
                    type == ITEM_TYPE_RADIO ? R.layout.drawer_radio :
                    type == ITEM_TYPE_SECTION_ENTRY ? R.layout.drawer_section_entry :
                    R.layout.drawer_section;
            convertView = mInflater.inflate(layoutResId, container, false);
        }

        // TOOD: ViewHolder, icon?
        TextView title = (TextView) convertView.findViewById(R.id.title);
        title.setText(item.mTitleResId);

        if (item instanceof RadioItem) {
            boolean checked = ((RadioItem) item).mChecked;
            RadioButton radio = (RadioButton) convertView.findViewById(R.id.radio);
            radio.setChecked(checked);
        }

        return convertView;
    }
}
