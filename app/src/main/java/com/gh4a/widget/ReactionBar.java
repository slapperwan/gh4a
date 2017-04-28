package com.gh4a.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Reaction;
import org.eclipse.egit.github.core.Reactions;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.ReactionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReactionBar extends LinearLayout implements View.OnClickListener {
    public interface Callback {
        List<Reaction> loadReactionDetailsInBackground(Object item) throws IOException;
        Reaction addReactionInBackground(Object item, String content) throws IOException;
        void onReactionsUpdated(Object item, Reactions reactions, List<Reaction> details);
    }

    private static final @IdRes int[] VIEW_IDS = {
        R.id.plus_one, R.id.minus_one, R.id.laugh,
        R.id.hooray, R.id.heart, R.id.confused
    };
    private static final String[] CONTENTS = {
        Reaction.CONTENT_PLUS_ONE, Reaction.CONTENT_MINUS_ONE,
        Reaction.CONTENT_LAUGH, Reaction.CONTENT_HOORAY,
        Reaction.CONTENT_HEART, Reaction.CONTENT_CONFUSED
    };

    private TextView mPlusOneView;
    private TextView mMinusOneView;
    private TextView mLaughView;
    private TextView mHoorayView;
    private TextView mConfusedView;
    private TextView mHeartView;
    private View mReactButton;

    private Callback mCallback;
    private Object mReferenceItem;
    private ReactionUserPopup mPopup;

    private MenuPopupHelper mAddReactionPopup;
    private AddReactionMenuHelper mAddHelper;
    private PopupMenu.OnMenuItemClickListener mAddReactionClickListener =
            new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            return mAddHelper.onItemClick(item);
        }
    };

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
        mReactButton = findViewById(R.id.react);

        setReactions(null);
    }

    public void setReactions(Reactions reactions) {
        if (mPopup != null) {
            mPopup.clearCache();
        }
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

    public void updateReactionDetails(List<Reaction> details) {
        if (mPopup != null) {
            mPopup.updateCache(details);
        }
        if (mAddHelper != null) {
            mAddHelper.updateDetails(details);
        }
    }

    public void setCallback(Callback callback, Object item) {
        mCallback = callback;
        mReferenceItem = item;

        for (int i = 0; i < VIEW_IDS.length; i++) {
            findViewById(VIEW_IDS[i]).setOnClickListener(callback != null ? this : null);
        }
        mReactButton.setVisibility(callback != null ? View.VISIBLE : View.GONE);
        mReactButton.setOnClickListener(callback != null ? this : null);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mPopup != null) {
            mPopup.dismiss();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        if (mPopup != null) {
            mPopup.dismiss();
        }
        return super.onSaveInstanceState();
    }

    @Override
    public void onClick(View view) {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
            return;
        }
        if (view == mReactButton) {
            if (mAddReactionPopup == null) {
                PopupMenu popup = new PopupMenu(getContext(), mReactButton);
                popup.inflate(R.menu.reaction_menu);
                popup.setOnMenuItemClickListener(mAddReactionClickListener);
                mAddHelper = new AddReactionMenuHelper(getContext(), popup.getMenu(),
                        mCallback, mReferenceItem);

                mAddReactionPopup = new MenuPopupHelper(getContext(), (MenuBuilder) popup.getMenu(),
                        mReactButton);
                mAddReactionPopup.setForceShowIcon(true);
            }
            mAddHelper.startLoadingIfNeeded();
            mAddReactionPopup.show();
            return;
        }
        for (int i = 0; i < VIEW_IDS.length; i++) {
            if (view.getId() == VIEW_IDS[i]) {
                if (mPopup == null) {
                    mPopup = new ReactionUserPopup(getContext(), mCallback, mReferenceItem);
                }
                mPopup.setAnchorView(view);
                mPopup.show(CONTENTS[i]);
            }
        }
    }

    private void updateView(TextView view, int count) {
        if (count > 0) {
            view.setText(String.valueOf(count));
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private static class ReactionUserPopup extends ListPopupWindow {
        private Callback mCallback;
        private Object mItem;
        private List<Reaction> mCachedReactions;
        private ReactionUserAdapter mAdapter;
        private String mContent;

        public ReactionUserPopup(@NonNull Context context, Callback callback, Object item) {
            super(context);

            mCallback = callback;
            mItem = item;
            mAdapter = new ReactionUserAdapter(context, this);
            setContentWidth(
                    context.getResources()
                            .getDimensionPixelSize(R.dimen.reaction_details_popup_width));
            setAdapter(mAdapter);
        }

        public void updateCache(List<Reaction> reactions) {
            mCachedReactions = reactions;
            populateAdapter();
        }

        public void clearCache() {
            mCachedReactions = null;
            dismiss();
        }

        public void show(String content) {
            if (!TextUtils.equals(content, mContent)) {
                mAdapter.setUsers(null);
                mContent = content;
            }
            show();

            if (mCachedReactions != null) {
                populateAdapter();
            } else {
                new FetchReactionTask(mCallback, mItem) {
                    @Override
                    protected void onPostExecute(List<Reaction> reactions) {
                        mCachedReactions = reactions;
                        populateAdapter();
                    }
                }.execute();
            }
        }

        private void populateAdapter() {
            if (mCachedReactions != null) {
                List<User> users = new ArrayList<>();
                for (Reaction reaction : mCachedReactions) {
                    if (TextUtils.equals(mContent, reaction.getContent())) {
                        users.add(reaction.getUser());
                    }
                }
                mAdapter.setUsers(users);
            } else {
                dismiss();
            }
        }
    }

    private static class ReactionUserAdapter extends BaseAdapter implements View.OnClickListener {
        private Context mContext;
        private ListPopupWindow mParent;
        private LayoutInflater mInflater;
        private List<User> mUsers;

        public ReactionUserAdapter(Context context, ListPopupWindow popup) {
            mContext = context;
            mParent = popup;
            mInflater = LayoutInflater.from(context);
        }

        public void setUsers(List<User> users) {
            mUsers = users;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mUsers != null ? mUsers.size() : 1;
        }

        @Override
        public int getItemViewType(int position) {
            return mUsers != null ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public Object getItem(int position) {
            return mUsers != null ? mUsers.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (mUsers == null) {
                return convertView != null
                        ? convertView
                        : mInflater.inflate(R.layout.reaction_details_progress, parent, false);
            }

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_reaction_details, parent, false);
            }

            User user = mUsers.get(position);
            ImageView avatar = (ImageView) convertView.findViewById(R.id.avatar);
            TextView name = (TextView) convertView.findViewById(R.id.name);

            AvatarHandler.assignAvatar(avatar, user);
            name.setText(ApiHelpers.getUserLogin(mContext, user));
            convertView.setTag(user);
            convertView.setOnClickListener(this);

            return convertView;
        }

        @Override
        public void onClick(View view) {
            User user = (User) view.getTag();
            mParent.dismiss();
            mContext.startActivity(UserActivity.makeIntent(mContext, user));
        }
    }

    private static class FetchReactionTask extends AsyncTask<Void, Void, List<Reaction>> {
        private Callback mCallback;
        private Object mItem;

        public FetchReactionTask(Callback callback, Object item) {
            mCallback = callback;
            mItem = item;
        }

        @Override
        protected List<Reaction> doInBackground(Void... voids) {
            try {
                List<Reaction> reactions =
                        mCallback.loadReactionDetailsInBackground(mItem);
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
    }

    public static class AddReactionMenuHelper {
        private Context mContext;
        private MenuItem mLoadingItem;
        private MenuItem[] mItems = new MenuItem[CONTENTS.length];
        private List<Reaction> mCachedReactions;
        private int[] mOldReactionIds;
        private Callback mCallback;
        private Object mItem;
        private Reactions mReactions = new Reactions();

        public AddReactionMenuHelper(@NonNull Context context, Menu menu,
                Callback callback, Object item) {
            mContext = context;
            mCallback = callback;
            mItem = item;

            updateFromMenu(menu);
        }

        public void updateFromMenu(Menu menu) {
            mLoadingItem = menu.findItem(R.id.loading);

            for (int i = 0; i < VIEW_IDS.length; i++) {
                mItems[i] = menu.findItem(VIEW_IDS[i]);
                Drawable icon = DrawableCompat.wrap(mItems[i].getIcon().mutate());
                DrawableCompat.setTintMode(icon, PorterDuff.Mode.SRC_ATOP);
                mItems[i].setIcon(icon);
            }
            if (mOldReactionIds != null) {
                showDataItems();
            }
        }

        public boolean onItemClick(MenuItem item) {
            for (int i = 0; i < mItems.length; i++) {
                if (item == mItems[i]) {
                    item.setChecked(mOldReactionIds[i] == 0);
                    addOrRemoveReaction(CONTENTS[i], mOldReactionIds[i]);
                    updateDrawableState();
                    return true;
                }
            }
            return false;
        }

        public void updateDetails(List<Reaction> reactions) {
            mCachedReactions = reactions != null ? reactions : new ArrayList<Reaction>();
            mOldReactionIds = new int[mItems.length];
            mReactions = new Reactions();
            if (reactions != null) {
                String ownLogin = Gh4Application.get().getAuthLogin();
                for (Reaction reaction : reactions) {
                    updateReactionsCache(reaction.getContent(), 1);
                    if (!ApiHelpers.loginEquals(reaction.getUser(), ownLogin)) {
                        continue;
                    }
                    for (int i = 0; i < CONTENTS.length; i++) {
                        if (TextUtils.equals(CONTENTS[i], reaction.getContent())) {
                            mOldReactionIds[i] = reaction.getId();
                            break;
                        }
                    }
                }
            }
        }

        public void startLoadingIfNeeded() {
            if (mOldReactionIds != null) {
                syncCheckStates();
                return;
            }
            new FetchReactionTask(mCallback, mItem) {
                @Override
                protected void onPostExecute(List<Reaction> reactions) {
                    updateDetails(reactions);
                    showDataItems();
                }
            }.execute();

        }

        private void showDataItems() {
            mLoadingItem.setVisible(false);
            for (int i = 0; i < mItems.length; i++) {
                mItems[i].setVisible(true);
            }
            syncCheckStates();
        }

        private void syncCheckStates() {
            for (int i = 0; i < mItems.length; i++) {
                mItems[i].setChecked(mOldReactionIds[i] != 0);
            }
            updateDrawableState();
        }

        private void updateDrawableState() {
            @ColorInt int accentColor = UiUtils.resolveColor(mContext, R.attr.colorAccent);
            @ColorInt int secondaryColor = UiUtils.resolveColor(mContext,
                    android.R.attr.textColorSecondary);
            for (int i = 0; i < mItems.length; i++) {
                DrawableCompat.setTint(mItems[i].getIcon(), mItems[i].isChecked()
                        ? accentColor : secondaryColor);
            }
        }

        private void addOrRemoveReaction(final String content, final int id) {
            updateReactionsCache(content, id != 0 ? -1 : 1);

            new AsyncTask<Void, Void, Pair<Boolean, Reaction>>() {
                @Override
                protected Pair<Boolean, Reaction> doInBackground(Void... voids) {
                    try {
                        if (id == 0) {
                            Reaction result = mCallback.addReactionInBackground(mItem, content);
                            return Pair.create(true, result);
                        } else {
                            ReactionService service = (ReactionService)
                                    Gh4Application.get().getService(Gh4Application.REACTION_SERVICE);
                            service.deleteReaction(id);
                            return Pair.create(true, null);
                        }
                    } catch (IOException e) {
                        android.util.Log.d("foo", "save fail", e);
                        return Pair.create(false, null);
                    }
                }

                @Override
                protected void onPostExecute(Pair<Boolean, Reaction> result) {
                    if (!result.first) {
                        // revert the change we did before
                        updateReactionsCache(content, id != 0 ? 1 : -1);
                    } else {
                        for (int i = 0; i < CONTENTS.length; i++) {
                            if (TextUtils.equals(CONTENTS[i], content)) {
                                mOldReactionIds[i] = result.second != null ? result.second.getId() : 0;
                                break;
                            }
                        }
                        if (result.second != null) {
                            mCachedReactions.add(result.second);
                        } else {
                            for (int i = 0; i < mCachedReactions.size(); i++) {
                                Reaction reaction = mCachedReactions.get(i);
                                if (reaction.getId() == id) {
                                    mCachedReactions.remove(i);
                                    break;
                                }
                            }
                        }
                    }
                    syncCheckStates();
                    mCallback.onReactionsUpdated(mItem, mReactions, mCachedReactions);
                }
            }.execute();
        }

        private void updateReactionsCache(String content, int delta) {
            switch (content) {
                case Reaction.CONTENT_PLUS_ONE: mReactions.setPlusOne(mReactions.getPlusOne() + delta); break;
                case Reaction.CONTENT_MINUS_ONE: mReactions.setMinusOne(mReactions.getMinusOne() + delta); break;
                case Reaction.CONTENT_CONFUSED: mReactions.setConfused(mReactions.getConfused() + delta); break;
                case Reaction.CONTENT_HEART: mReactions.setHeart(mReactions.getHeart() + delta); break;
                case Reaction.CONTENT_HOORAY: mReactions.setHooray(mReactions.getHooray() + delta); break;
                case Reaction.CONTENT_LAUGH: mReactions.setLaugh(mReactions.getLaugh() + delta); break;
            }
        }
    }
}
