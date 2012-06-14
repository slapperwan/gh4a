package com.gh4a.adapter;

import java.util.List;

import org.eclipse.egit.github.core.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;

public class OrganizationAdapter extends RootAdapter<User>{

    public OrganizationAdapter(Context context, List<User> objects) {
        super(context, objects);
    }
    
    @Override
    public View doGetView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater) LayoutInflater.from(mContext);
            v = vi.inflate(R.layout.row_simple, null);
        }
        User organization = mObjects.get(position);
        if (organization != null) {
            TextView tvFormattedName = (TextView) v.findViewById(R.id.tv_title);
            tvFormattedName.setText(organization.getLogin());
        }
        return v;
    }

}
