package com.gh4a.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.Repository;

public class CodeSearchAdapter extends RootAdapter<CodeSearchResult, CodeSearchAdapter.ViewHolder> {
    public CodeSearchAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent) {
        View v = inflater.inflate(R.layout.row_two_line, parent, false);
        return new ViewHolder(v);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, CodeSearchResult item) {
        Repository repo = item.getRepository();
        holder.tvTitle.setText(item.getName());
        holder.tvExtra.setText(mContext.getString(R.string.code_search_result_in,
                repo.getOwner().getLogin(), repo.getName()));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.tv_title);
            tvExtra = (TextView) view.findViewById(R.id.tv_extra);
        }

        private TextView tvTitle;
        private TextView tvExtra;
    }
}
