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
import com.gh4a.fragment.SearchFragment;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchCode;
import com.meisolsson.githubsdk.model.TextMatch;
import com.meisolsson.githubsdk.model.User;

import java.util.List;

public class SearchAdapter extends RootAdapter<Object, RecyclerView.ViewHolder> {
    public interface Callback {
        void onSearchFragmentClick(SearchCode result, int matchIndex);
    }

    private UserAdapter mUserAdapter;
    private RepositoryAdapter mRepoAdapter;
    private CodeSearchAdapter mCodeAdapter;
    private int mMode;

    public SearchAdapter(Context context, Callback callback) {
        super(context);
        mUserAdapter = new UserAdapter(context);
        mRepoAdapter = new RepositoryAdapter(context);
        mCodeAdapter = new CodeSearchAdapter(context, callback);
        mMode = SearchFragment.SEARCH_TYPE_REPO;
    }

    public void setMode(int mode) {
        mMode = mode;
        clear();
    }

    @Override
    public int getItemViewType(Object item) {
        if (item instanceof User) {
            return mUserAdapter.getItemViewType((User) item) + 10000;
        } else if (item instanceof Repository) {
            return mRepoAdapter.getItemViewType((Repository) item) + 20000;
        } else {
            return mCodeAdapter.getItemViewType((SearchCode) item) + 30000;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(LayoutInflater inflater,
            ViewGroup parent, int viewType) {
        RootAdapter<?, ? extends RecyclerView.ViewHolder> adapter =
                mMode == SearchFragment.SEARCH_TYPE_REPO ? mRepoAdapter :
                mMode == SearchFragment.SEARCH_TYPE_USER ? mUserAdapter :
                mCodeAdapter;
        return adapter.onCreateViewHolder(inflater, parent, viewType % 10000);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, Object item) {
        if (item instanceof User) {
            mUserAdapter.onBindViewHolder((UserAdapter.ViewHolder) holder, (User) item);
        } else if (item instanceof Repository) {
            mRepoAdapter.onBindViewHolder((RepositoryAdapter.ViewHolder) holder, (Repository) item);
        } else {
            mCodeAdapter.onBindViewHolder((CodeSearchAdapter.ViewHolder) holder, (SearchCode) item);
        }
    }

    public static class CodeSearchAdapter extends RootAdapter<SearchCode, CodeSearchAdapter.ViewHolder> {
        private final Callback mCallback;

        public CodeSearchAdapter(Context context, Callback callback) {
            super(context);
            mCallback = callback;
        }

        @Override
        public ViewHolder onCreateViewHolder(LayoutInflater inflater, ViewGroup parent,
                int viewType) {
            View v = inflater.inflate(R.layout.row_code_search, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, SearchCode result) {
            Repository repo = result.repository();
            holder.tvTitle.setText(result.name());
            holder.tvRepo.setText(repo.owner().login() + "/" + repo.name());

            List<TextMatch> matches = result.textMatches();
            if (matches != null && !matches.isEmpty()) {
                LayoutInflater inflater = LayoutInflater.from(mContext);

                for (int i = 0; i < matches.size(); i++) {
                    TextMatch match = matches.get(i);
                    SpannableStringBuilder builder = new SpannableStringBuilder(match.fragment());

                    List<TextMatch.MatchItem> items = match.matches();
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

                    TextView tvMatch = row.findViewById(R.id.tv_match);
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
                SearchCode searchResult = (SearchCode) view.getTag();
                mCallback.onSearchFragmentClick(searchResult,
                        (int) view.getTag(R.id.search_match_index));
                return;
            }

            super.onClick(view);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private ViewHolder(View view) {
                super(view);
                tvTitle = view.findViewById(R.id.tv_title);
                tvRepo = view.findViewById(R.id.tv_repo);
                matchesContainer = view.findViewById(R.id.matches_container);
            }

            private final TextView tvTitle;
            private final TextView tvRepo;
            private final ViewGroup matchesContainer;
        }
    }
}
