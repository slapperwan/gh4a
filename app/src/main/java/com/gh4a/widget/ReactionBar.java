package com.gh4a.widget;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ReactionBar extends LinearLayout {
    public interface ReactionDetailsProvider {
        List<Reaction> loadReactionDetailsInBackground(ReactionBar view) throws IOException;
    }

    private TextView mPlusOneView;
    private TextView mMinusOneView;
    private TextView mLaughView;
    private TextView mHoorayView;
    private TextView mConfusedView;
    private TextView mHeartView;

    private ReactionDetailsProvider mProvider;

    public ReactionBar(Context context) {
        this(context, null);
    }

    public ReactionBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReactionBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
        inflate(context, R.layout.reaction_bar, this);

        mPlusOneView = (TextView) findViewById(R.id.plus_one);
        mMinusOneView = (TextView) findViewById(R.id.minus_one);
        mLaughView = (TextView) findViewById(R.id.laugh);
        mHoorayView = (TextView) findViewById(R.id.hooray);
        mConfusedView = (TextView) findViewById(R.id.confused);
        mHeartView = (TextView) findViewById(R.id.heart);

        setReactions(null);
    }

    public void setReactions(Reactions reactions) {
        if (reactions != null && reactions.getTotalCount() > 0) {
            updateView(mPlusOneView, reactions.getPlusOne());
            updateView(mMinusOneView, reactions.getMinusOne());
            updateView(mLaughView, reactions.getLaugh());
            updateView(mHoorayView, reactions.getHooray());
            updateView(mConfusedView, reactions.getConfused());
            updateView(mHeartView, reactions.getHeart());
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    public void setReactionDetailsProvider(ReactionDetailsProvider provider) {
        mProvider = provider;
        setClickable(provider != null);
    }

    @Override
    public boolean performClick() {
        if (mProvider == null) {
            return super.performClick();
        }

        final ListPopupWindow popup = new ListPopupWindow(getContext());
        final ReactionDetailsAdapter adapter = new ReactionDetailsAdapter(getContext());

        popup.setContentWidth(
                getResources().getDimensionPixelSize(R.dimen.reaction_details_popup_width));
        popup.setAnchorView(this);
        popup.setAdapter(adapter);
        popup.show();

        new AsyncTask<Void, Void, List<Reaction>>() {
            @Override
            protected List<Reaction> doInBackground(Void... voids) {
                try {
                    List<Reaction> reactions =
                            mProvider.loadReactionDetailsInBackground(ReactionBar.this);
                    Collections.sort(reactions, new Comparator<Reaction>() {
                        @Override
                        public int compare(Reaction lhs, Reaction rhs) {
                            int result = lhs.getContent().compareTo(rhs.getContent());
                            if (result == 0) {
                                result = rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
                            }
                            return result;
                        }
                    });
                    return reactions;
                } catch (IOException e) {
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Reaction> reactions) {
                if (reactions != null) {
                    adapter.setReactions(reactions);
                } else {
                    popup.dismiss();
                }
            }
        }.execute();
        return true;
    }

    private void updateView(TextView view, int count) {
        if (count > 0) {
            view.setText(String.valueOf(count));
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private static class ReactionDetailsAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private List<Reaction> mReactions;
        private HashMap<String, Integer> mIconLookup = new HashMap<>();

        public ReactionDetailsAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(context);

            mIconLookup.put(Reaction.CONTENT_PLUS_ONE,
                    UiUtils.resolveDrawable(context, R.attr.reactionPlusOneIcon));
            mIconLookup.put(Reaction.CONTENT_MINUS_ONE,
                    UiUtils.resolveDrawable(context, R.attr.reactionMinusOneIcon));
            mIconLookup.put(Reaction.CONTENT_CONFUSED,
                    UiUtils.resolveDrawable(context, R.attr.reactionConfusedIcon));
            mIconLookup.put(Reaction.CONTENT_HEART,
                    UiUtils.resolveDrawable(context, R.attr.reactionHeartIcon));
            mIconLookup.put(Reaction.CONTENT_HOORAY,
                    UiUtils.resolveDrawable(context, R.attr.reactionHoorayIcon));
            mIconLookup.put(Reaction.CONTENT_LAUGH,
                    UiUtils.resolveDrawable(context, R.attr.reactionLaughIcon));
        }

        public void setReactions(List<Reaction> reactions) {
            mReactions = reactions;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mReactions != null ? mReactions.size() : 1;
        }

        @Override
        public int getItemViewType(int position) {
            return mReactions != null ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public Object getItem(int position) {
            return mReactions != null ? mReactions.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mReactions == null) {
                return convertView != null
                        ? convertView
                        : mInflater.inflate(R.layout.reaction_details_progress, parent, false);
            }

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_reaction_details, parent, false);
            }

            Reaction reaction = mReactions.get(position);
            Reaction prevReaction = position == 0 ? null : mReactions.get(position - 1);

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar);
            TextView name = (TextView) convertView.findViewById(R.id.name);

            if (prevReaction == null
                    || !TextUtils.equals(reaction.getContent(), prevReaction.getContent())) {
                icon.setImageResource(mIconLookup.get(reaction.getContent()));
            } else {
                icon.setImageDrawable(null);
            }
            AvatarHandler.assignAvatar(avatar, reaction.getUser());
            name.setText(ApiHelpers.getUserLogin(mContext, reaction.getUser()));

            return convertView;
        }
    }
}
