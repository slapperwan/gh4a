package com.gh4a.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
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
import com.gh4a.ServiceFactory;
import com.gh4a.activities.UserActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Reaction;
import com.meisolsson.githubsdk.model.Reactions;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.reactions.ReactionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.Single;

public class ReactionBar extends LinearLayout implements View.OnClickListener {
    public interface Item {
        Object getCacheKey();
    }
    public interface Callback {
        Single<List<Reaction>> loadReactionDetails(Item item, boolean bypassCache);
        Single<Reaction> addReaction(Item item, String content);
    }

    private static final @IdRes int[] VIEW_IDS = {
        R.id.plus_one, R.id.minus_one, R.id.laugh,
        R.id.hooray, R.id.heart, R.id.confused,
        R.id.rocket, R.id.eyes
    };
    private static final String[] CONTENTS = {
        Reaction.CONTENT_PLUS_ONE, Reaction.CONTENT_MINUS_ONE,
        Reaction.CONTENT_LAUGH, Reaction.CONTENT_HOORAY,
        Reaction.CONTENT_HEART, Reaction.CONTENT_CONFUSED,
        Reaction.CONTENT_ROCKET, Reaction.CONTENT_EYES
    };

    private final TextView mPlusOneView;
    private final TextView mMinusOneView;
    private final TextView mLaughView;
    private final TextView mHoorayView;
    private final TextView mConfusedView;
    private final TextView mHeartView;
    private final TextView mRocketView;
    private final TextView mEyesView;
    private final View mReactButton;

    private Callback mCallback;
    private Item mReferenceItem;
    private ReactionUserPopup mPopup;

    private ReactionDetailsCache mDetailsCache;
    private MenuPopupHelper mAddReactionPopup;
    private AddReactionMenuHelper mAddHelper;
    private final PopupMenu.OnMenuItemClickListener mAddReactionClickListener =
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

        mPlusOneView = findViewById(R.id.plus_one);
        mMinusOneView = findViewById(R.id.minus_one);
        mLaughView = findViewById(R.id.laugh);
        mHoorayView = findViewById(R.id.hooray);
        mConfusedView = findViewById(R.id.confused);
        mHeartView = findViewById(R.id.heart);
        mRocketView = findViewById(R.id.rocket);
        mEyesView = findViewById(R.id.eyes);
        mReactButton = findViewById(R.id.react);

        setReactions(null);
    }

    public void setReactions(Reactions reactions) {
        if (mPopup != null) {
            mPopup.update();
        }
        if (mAddHelper != null) {
            mAddHelper.update();
        }
        if (reactions != null && reactions.totalCount() > 0) {
            updateView(mPlusOneView, reactions.plusOne());
            updateView(mMinusOneView, reactions.minusOne());
            updateView(mLaughView, reactions.laugh());
            updateView(mHoorayView, reactions.hooray());
            updateView(mConfusedView, reactions.confused());
            updateView(mHeartView, reactions.heart());
            updateView(mRocketView, reactions.rocket());
            updateView(mEyesView, reactions.eyes());
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    public void setDetailsCache(ReactionDetailsCache cache) {
        mDetailsCache = cache;
    }

    public void setCallback(Callback callback, Item item) {
        mCallback = callback;
        mReferenceItem = item;

        for (int id : VIEW_IDS) {
            findViewById(id).setOnClickListener(callback != null ? this : null);
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
                        mCallback, mReferenceItem, mDetailsCache);

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
                    mPopup = new ReactionUserPopup(getContext(),
                            mCallback, mReferenceItem, mDetailsCache);
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
        private final Callback mCallback;
        private final Item mItem;
        private List<Reaction> mLastKnownDetails;
        private final ReactionDetailsCache mDetailsCache;
        private final ReactionUserAdapter mAdapter;
        private String mContent;

        public ReactionUserPopup(@NonNull Context context, Callback callback,
                Item item, ReactionDetailsCache detailsCache) {
            super(context);

            mCallback = callback;
            mItem = item;
            mDetailsCache = detailsCache;
            mAdapter = new ReactionUserAdapter(context, this);
            setContentWidth(
                    context.getResources()
                            .getDimensionPixelSize(R.dimen.reaction_details_popup_width));
            setAdapter(mAdapter);
        }

        public void update() {
            List<Reaction> details = mDetailsCache.getEntry(mItem);
            if (details != null) {
                populateAdapter(details);
            }
        }

        public void show(String content) {
            if (!TextUtils.equals(content, mContent)) {
                mAdapter.setReactions(null);
                mContent = content;
            }
            show();

            List<Reaction> details = mDetailsCache.getEntry(mItem);
            if (details != null) {
                populateAdapter(details);
            } else {
                fetchReactions(mCallback, mItem, mDetailsCache)
                        .subscribe(this::populateAdapter, error -> {
                            Log.d(Gh4Application.LOG_TAG, "Fetching reactions failed", error);
                            dismiss();
                        });
            }
        }

        public void toggleOwnReaction(Reaction currentReaction) {
            final long id = currentReaction != null ? currentReaction.id() : 0;
            toggleReaction(mContent, id, mLastKnownDetails, mCallback, mItem, mDetailsCache)
                    .subscribe(result -> dismiss(), error -> {
                        Log.d(Gh4Application.LOG_TAG, "Toggling reaction failed", error);
                        dismiss();
                    });
        }

        private void populateAdapter(List<Reaction> details) {
            List<Reaction> reactions = new ArrayList<>();
            for (Reaction reaction : details) {
                if (TextUtils.equals(mContent, reaction.content())) {
                    reactions.add(reaction);
                }
            }
            mLastKnownDetails = details;
            mAdapter.setReactions(reactions);
        }
    }

    private static class ReactionUserAdapter extends BaseAdapter implements View.OnClickListener {
        private final Context mContext;
        private final ReactionUserPopup mParent;
        private final LayoutInflater mInflater;
        private List<User> mUsers;
        private Reaction mOwnReaction;

        public ReactionUserAdapter(Context context, ReactionUserPopup popup) {
            mContext = context;
            mParent = popup;
            mInflater = LayoutInflater.from(context);
        }

        public void setReactions(List<Reaction> reactions) {
            mOwnReaction = null;
            if (reactions != null) {
                User ownUser = Gh4Application.get().getCurrentAccountInfoForAvatar();
                String ownLogin = ownUser != null ? ownUser.login() : null;

                mUsers = new ArrayList<>();
                for (Reaction reaction : reactions) {
                    if (ApiHelpers.loginEquals(reaction.user(), ownLogin)) {
                        mOwnReaction = reaction;
                    } else {
                        mUsers.add(reaction.user());
                    }
                }
                if (ownUser != null) {
                    mUsers.add(null);
                    mUsers.add(ownUser);
                }
            } else {
                mUsers = null;
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mUsers != null ? mUsers.size() : 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (mUsers == null) {
                return 1;
            }
            return getItem(position) == null ? 2 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
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
            int viewType = getItemViewType(position);
            @LayoutRes int layoutResId =
                    viewType == 0 ? R.layout.row_reaction_details :
                    viewType == 1 ? R.layout.reaction_details_progress :
                    R.layout.reaction_details_divider;

            if (convertView == null) {
                convertView = mInflater.inflate(layoutResId, parent, false);
            }

            if (viewType == 0) {
                ImageView avatar = convertView.findViewById(R.id.avatar);
                TextView name = convertView.findViewById(R.id.name);
                String ownLogin = Gh4Application.get().getAuthLogin();
                User user = mUsers.get(position);

                AvatarHandler.assignAvatar(avatar, user);
                convertView.setOnClickListener(this);

                if (ApiHelpers.loginEquals(user, ownLogin)) {
                    avatar.setAlpha(mOwnReaction != null ? 1.0f : 0.4f);
                    name.setText(mOwnReaction != null
                            ? R.string.remove_reaction : R.string.add_reaction);
                    convertView.setTag(mOwnReaction);
                } else {
                    avatar.setAlpha(1.0f);
                    name.setText(ApiHelpers.getUserLogin(mContext, user));
                    convertView.setTag(user);
                }
            }

            return convertView;
        }

        @Override
        public void onClick(View view) {
            if (view.getTag() instanceof User) {
                User user = (User) view.getTag();
                mParent.dismiss();
                mContext.startActivity(UserActivity.makeIntent(mContext, user));
            } else {
                // own entry
                mParent.toggleOwnReaction(mOwnReaction);
            }
        }
    }

    private static Single<List<Reaction>> fetchReactions(Callback callback, Item item,
            ReactionDetailsCache cache) {
        return callback.loadReactionDetails(item, false)
                .compose(RxUtils::doInBackground)
                .compose(RxUtils.sortList((lhs, rhs) -> {
                    int result = lhs.content().compareTo(rhs.content());
                    if (result == 0) {
                        result = rhs.createdAt().compareTo(lhs.createdAt());
                    }
                    return result;
                }))
                .doOnSuccess(result -> cache.putEntry(item, result));
    }

    private static Single<Optional<Reaction>> toggleReaction(String content, long id,
            List<Reaction> existingDetails, Callback callback, Item item,
            ReactionDetailsCache cache) {
        final Single<Optional<Reaction>> resultSingle;

        if (id == 0) {
            resultSingle = callback.addReaction(item, content)
                    .map(Optional::of);
        } else {
            ReactionService service = ServiceFactory.get(ReactionService.class, false);
            resultSingle = service.deleteReaction(id)
                    .map(response -> Optional.absent());
        }

        return resultSingle
                .compose(RxUtils::doInBackground)
                .doOnSuccess(reactionOpt -> {
                    if (reactionOpt.isPresent()) {
                        existingDetails.add(reactionOpt.get());
                    } else {
                        for (int i = 0; i < existingDetails.size(); i++) {
                            Reaction reaction = existingDetails.get(i);
                            if (reaction.id() == id) {
                                existingDetails.remove(i);
                                break;
                            }
                        }
                    }
                    cache.putEntry(item, existingDetails);
                });
    }

    public static class AddReactionMenuHelper {
        private final Context mContext;
        private MenuItem mLoadingItem;
        private final MenuItem[] mItems = new MenuItem[CONTENTS.length];
        private final ReactionDetailsCache mDetailsCache;
        private List<Reaction> mLastKnownDetails;
        private long[] mOldReactionIds;
        private final Callback mCallback;
        private final Item mItem;
        private boolean mLoading;

        public AddReactionMenuHelper(@NonNull Context context, Menu menu,
                Callback callback, Item item, ReactionDetailsCache detailsCache) {
            mContext = context;
            mCallback = callback;
            mItem = item;
            mDetailsCache = detailsCache;

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
                setDataItemsVisible(true);
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

        public void update() {
            List<Reaction> reactions = mDetailsCache.getEntry(mItem);
            if (reactions != null) {
                mOldReactionIds = new long[mItems.length];
                mLastKnownDetails = new ArrayList<>(reactions);

                String ownLogin = Gh4Application.get().getAuthLogin();
                for (Reaction reaction : reactions) {
                    if (!ApiHelpers.loginEquals(reaction.user(), ownLogin)) {
                        continue;
                    }
                    for (int i = 0; i < CONTENTS.length; i++) {
                        if (TextUtils.equals(CONTENTS[i], reaction.content())) {
                            mOldReactionIds[i] = reaction.id();
                            break;
                        }
                    }
                }
            } else {
                mOldReactionIds = null;
                mLastKnownDetails = null;
            }
            setDataItemsVisible(mOldReactionIds != null);
        }

        public void startLoadingIfNeeded() {
            if (mOldReactionIds != null) {
                syncCheckStates();
            } else if (mDetailsCache.hasEntryFor(mItem)) {
                update();
            } else if (!mLoading) {
                fetchReactions(mCallback, mItem, mDetailsCache)
                        .doOnSubscribe(disposable -> mLoading = true)
                        .doOnSuccess(result -> mLoading = false)
                        .doOnError(error -> mLoading = false)
                        .subscribe(reactions -> update(), error -> {
                            Log.d(Gh4Application.LOG_TAG, "Fetching reactions failed", error);
                            update();
                        });
            }
        }

        private void setDataItemsVisible(boolean visible) {
            mLoadingItem.setVisible(!visible);
            for (MenuItem item : mItems) {
                item.setVisible(visible);
            }
            syncCheckStates();
        }

        private void syncCheckStates() {
            for (int i = 0; i < mItems.length; i++) {
                mItems[i].setChecked(mOldReactionIds != null && mOldReactionIds[i] != 0);
            }
            updateDrawableState();
        }

        private void updateDrawableState() {
            @ColorInt int accentColor = UiUtils.resolveColor(mContext, R.attr.colorAccent);
            @ColorInt int secondaryColor = UiUtils.resolveColor(mContext,
                    android.R.attr.textColorSecondary);
            for (MenuItem item : mItems) {
                DrawableCompat.setTint(item.getIcon(),
                        item.isChecked() ? accentColor : secondaryColor);
            }
        }

        private void addOrRemoveReaction(final String content, final long id) {
            toggleReaction(content, id, mLastKnownDetails, mCallback, mItem, mDetailsCache)
                    .subscribe(result -> {
                        for (int i = 0; i < CONTENTS.length; i++) {
                            if (TextUtils.equals(CONTENTS[i], content)) {
                                mOldReactionIds[i] = result.isPresent() ? result.get().id() : 0;
                                break;
                            }
                        }
                    }, error -> Log.d(Gh4Application.LOG_TAG, "Changing reaction failed", error));
        }
    }

    public static class ReactionDetailsCache {
        public interface Listener {
            void onReactionsUpdated(Item item, Reactions reactions);
        }

        private final Listener mListener;
        private boolean mDestroyed;
        private final HashMap<Object, List<Reaction>> mMap = new HashMap<>();

        public ReactionDetailsCache(Listener listener) {
            super();
            mListener = listener;
        }

        public void destroy() {
            mDestroyed = true;
        }

        public void clear() {
            mMap.clear();
        }

        public boolean hasEntryFor(Item item) {
            return mMap.containsKey(item.getCacheKey());
        }

        public List<Reaction> getEntry(Item item) {
            return mMap.get(item.getCacheKey());
        }

        public List<Reaction> putEntry(Item item, List<Reaction> value) {
            Object key = item.getCacheKey();
            List<Reaction> result = mMap.put(key, new ArrayList<>(value));
            if (result != null && !mDestroyed) {
                mListener.onReactionsUpdated(item, buildReactions(value));
            }
            return result;
        }

        private Reactions buildReactions(List<Reaction> reactions) {
            int plusOne = 0, minusOne = 0, confused = 0, heart = 0;
            int hooray = 0, laugh = 0, rocket = 0, eyes = 0;
            for (Reaction reaction : reactions) {
                switch (reaction.content()) {
                    case Reaction.CONTENT_PLUS_ONE: ++plusOne; break;
                    case Reaction.CONTENT_MINUS_ONE: ++minusOne; break;
                    case Reaction.CONTENT_CONFUSED: ++confused; break;
                    case Reaction.CONTENT_HEART: ++heart; break;
                    case Reaction.CONTENT_HOORAY: ++hooray; break;
                    case Reaction.CONTENT_LAUGH: ++laugh; break;
                    case Reaction.CONTENT_ROCKET: ++rocket; break;
                    case Reaction.CONTENT_EYES: ++eyes; break;
                }
            }
            return Reactions.builder()
                    .plusOne(plusOne)
                    .minusOne(minusOne)
                    .confused(confused)
                    .heart(heart)
                    .hooray(hooray)
                    .laugh(laugh)
                    .rocket(rocket)
                    .eyes(eyes)
                    .build();
        }
    }
}
