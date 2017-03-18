package com.gh4a.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gh4a.R;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.TextMatch;

import java.util.List;

public class CodeSearchAdapter extends RootAdapter<CodeSearchResult, CodeSearchAdapter.ViewHolder> {
    public CodeSearchAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_code_search, parent, false);
        return new ViewHolder(v);
    }

    @Override
    protected void onBindViewHolder(ViewHolder holder, CodeSearchResult result) {
        Repository repo = result.getRepository();
        holder.tvTitle.setText(result.getName());
        holder.tvRepo.setText(repo.getOwner().getLogin() + "/" + repo.getName());

        List<TextMatch> matches = result.getTextMatches();
        if (matches != null && !matches.isEmpty()) {
            SpannableStringBuilder builder = new SpannableStringBuilder();

            for (TextMatch match : matches) {
                int pos = builder.length();
                if (pos > 0) {
                    builder.append("\n\n");
                    builder.setSpan(new RelativeSizeSpan(0.5f), pos, pos + 2, 0);
                    pos += 2;
                }
                builder.append(match.getFragment());

                List<TextMatch.MatchItem> items = match.getMatches();
                if (items != null) {
                    for (TextMatch.MatchItem item : items) {
                        int start = item.getStartPos();
                        int end = item.getEdndPos();
                        if (start >= 0 && end > start) {
                            builder.setSpan(new StyleSpan(Typeface.BOLD), pos + start, pos + end, 0);
                        }
                    }
                }
            }
            holder.tvMatches.setText(builder);
            holder.tvMatches.setVisibility(View.VISIBLE);
        } else {
            holder.tvMatches.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.tv_title);
            tvRepo = (TextView) view.findViewById(R.id.tv_repo);
            tvMatches = (TextView) view.findViewById(R.id.tv_matches);
        }

        private final TextView tvTitle;
        private final TextView tvRepo;
        private final TextView tvMatches;
    }
}
