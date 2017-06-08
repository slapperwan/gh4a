package com.gh4a.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
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
    public interface Callback {
        void onSearchFragmentClick(CodeSearchResult result, int matchIndex);
    }

    private final Callback mCallback;

    public CodeSearchAdapter(Context context, Callback callback) {
        super(context);
        this.mCallback = callback;
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
            LayoutInflater inflater = LayoutInflater.from(mContext);

            for (int i = 0; i < matches.size(); i++) {
                TextMatch match = matches.get(i);
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(match.getFragment());

                List<TextMatch.MatchItem> items = match.getMatches();
                if (items != null) {
                    for (TextMatch.MatchItem item : items) {
                        int start = item.getStartPos();
                        int end = item.getEndPos();
                        if (start >= 0 && end > start) {
                            builder.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                        }
                    }
                }

                View row = holder.matchesContainer.getChildAt(i);
                if (row == null) {
                    row = inflater.inflate(R.layout.row_search_match,
                            holder.matchesContainer, false);
                    holder.matchesContainer.addView(row);
                }

                TextView tvMatch = (TextView) row.findViewById(R.id.tv_match);
                tvMatch.setOnClickListener(this);
                tvMatch.setText(builder);
                tvMatch.setTag(result);
                tvMatch.setTag(R.id.search_match_index, i);
                row.setVisibility(View.VISIBLE);
            }
            for (int i = matches.size(); i < holder.matchesContainer.getChildCount(); i++) {
                holder.matchesContainer.getChildAt(i).setVisibility(View.GONE);
            }
            holder.matchesContainer.setVisibility(View.VISIBLE);
        } else {
            holder.matchesContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tv_match) {
            CodeSearchResult searchResult = (CodeSearchResult) view.getTag();
            mCallback.onSearchFragmentClick(searchResult, (int) view.getTag(R.id.search_match_index));
            return;
        }

        super.onClick(view);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ViewHolder(View view) {
            super(view);
            tvTitle = (TextView) view.findViewById(R.id.tv_title);
            tvRepo = (TextView) view.findViewById(R.id.tv_repo);
            matchesContainer = (ViewGroup) view.findViewById(R.id.matches_container);
        }

        private final TextView tvTitle;
        private final TextView tvRepo;
        private final ViewGroup matchesContainer;
    }
}
