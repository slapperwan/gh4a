package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.loader.GitModuleParserLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.widget.PathBreadcrumbs;
import com.gh4a.widget.SwipeRefreshLayout;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.model.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ContentListContainerFragment extends Fragment implements
        ContentListFragment.ParentCallback, PathBreadcrumbs.SelectionCallback,
        LoaderCallbacks.ParentCallback, SwipeRefreshLayout.ChildScrollDelegate {
    private static final int LOADER_MODULEMAP = 100;

    private static final String STATE_KEY_DIR_STACK = "dir_stack";
    private static final String STATE_KEY_CONTENT_CACHE_PREFIX = "content_cache_";
    private static final String STATE_KEY_INITIAL_PATH = "initial_path";

    private PathBreadcrumbs mBreadcrumbs;
    private ContentListFragment mContentListFragment;
    private Repository mRepository;
    private String mSelectedRef;
    private Map<String, String> mGitModuleMap;
    private final Stack<String> mDirStack = new Stack<>();
    private ArrayList<String> mInitialPathToLoad;
    private boolean mStateSaved;
    private final Map<String, ArrayList<Content>> mContentCache =
            new LinkedHashMap<String, ArrayList<Content>>() {
        private static final long serialVersionUID = -2379579224736389357L;
        private static final int MAX_CACHE_ENTRIES = 100;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ArrayList<Content>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private final LoaderCallbacks<Map<String, String>> mGitModuleCallback =
            new LoaderCallbacks<Map<String, String>>(this) {
        @Override
        protected Loader<LoaderResult<Map<String, String>>> onCreateLoader() {
            return new GitModuleParserLoader(getActivity(), mRepository.owner().login(),
                    mRepository.name(), mSelectedRef);
        }
        @Override
        protected void onResultReady(Map<String, String> result) {
            mGitModuleMap = result;
            if (mContentListFragment != null) {
                mContentListFragment.onSubModuleNamesChanged(getSubModuleNames(mContentListFragment));
            }
        }
    };

    public static ContentListContainerFragment newInstance(Repository repository,
            String ref, String initialPath) {
        ContentListContainerFragment f = new ContentListContainerFragment();

        Bundle args = new Bundle();
        args.putParcelable("repository", repository);
        args.putString("ref", ref);
        args.putString("initialpath", initialPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepository = getArguments().getParcelable("repository");
        mSelectedRef = getArguments().getString("ref");
        mStateSaved = false;

        if (savedInstanceState != null) {
            for (String entry : savedInstanceState.getStringArrayList(STATE_KEY_DIR_STACK)) {
                mDirStack.add(entry);
            }

            int prefixLen = STATE_KEY_CONTENT_CACHE_PREFIX.length();
            for (String key : savedInstanceState.keySet()) {
                if (key.startsWith(STATE_KEY_CONTENT_CACHE_PREFIX)) {
                    String cacheKey = key.substring(prefixLen);
                    if (cacheKey.equals("/")) {
                        cacheKey = null;
                    }
                    ArrayList<Content> content = savedInstanceState.getParcelableArrayList(key);
                    mContentCache.put(cacheKey, content);
                }
            }
            mInitialPathToLoad = savedInstanceState.getStringArrayList(STATE_KEY_INITIAL_PATH);
        } else {
            mDirStack.push("");

            String initialPath = getArguments().getString("initialpath");
            if (initialPath != null) {
                mInitialPathToLoad = new ArrayList<>();
                int pos = initialPath.indexOf("/");
                while (pos > 0) {
                    mInitialPathToLoad.add(initialPath.substring(0, pos));
                    pos = initialPath.indexOf("/", pos + 1);
                }
                mInitialPathToLoad.add(initialPath);
            }
        }
    }

    @Override
    public boolean canChildScrollUp() {
        if (mContentListFragment != null) {
            return mContentListFragment.canChildScrollUp();
        }
        return false;
    }

    @Override
    public void onRefresh() {
        setRef(mSelectedRef);
    }

    @Override
    public BaseActivity getBaseActivity() {
        return (BaseActivity) getActivity();
    }

    public void setRef(String ref) {
        getArguments().putString("ref", ref);
        mSelectedRef = ref;
        mGitModuleMap = null;

        mInitialPathToLoad = new ArrayList<>();
        for (int i = 1; i < mDirStack.size(); i++) {
            mInitialPathToLoad.add(mDirStack.get(i));
        }

        mDirStack.clear();
        mDirStack.push("");
        mContentCache.clear();
        mContentListFragment = null;
        getChildFragmentManager().popBackStackImmediate(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        addFragmentForTopOfStack();
        updateBreadcrumbs();
    }

    public boolean handleBackPress() {
        if (mDirStack.size() > 1) {
            mDirStack.pop();
            getChildFragmentManager().popBackStackImmediate();
            mContentListFragment = (ContentListFragment)
                    getChildFragmentManager().findFragmentById(R.id.content_list_container);
            updateBreadcrumbs();
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.content_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mBreadcrumbs = view.findViewById(R.id.breadcrumbs);
        mBreadcrumbs.setCallback(this);
        mStateSaved = false;
        updateBreadcrumbs();
        addFragmentForTopOfStack();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_KEY_DIR_STACK, new ArrayList<>(mDirStack));
        outState.putStringArrayList(STATE_KEY_INITIAL_PATH, mInitialPathToLoad);
        for (Map.Entry<String, ArrayList<Content>> entry : mContentCache.entrySet()) {
            String key = entry.getKey();
            outState.putParcelableArrayList(STATE_KEY_CONTENT_CACHE_PREFIX + key, entry.getValue());
        }
        mStateSaved = true;
    }

    @Override
    public void onContentsLoaded(ContentListFragment fragment, List<Content> contents) {
        if (contents == null) {
            return;
        }
        mContentCache.put(fragment.getPath(), new ArrayList<>(contents));
        if (fragment.getPath() == null) {
            for (Content content : contents) {
                if (content.type() == ContentType.File && content.name().equals(".gitmodules")) {
                    LoaderManager lm = getActivity().getSupportLoaderManager();
                    lm.restartLoader(LOADER_MODULEMAP, null, mGitModuleCallback);
                    break;
                }
            }
        }
        if (mInitialPathToLoad != null && !mInitialPathToLoad.isEmpty() && !mStateSaved) {
            String itemToLoad = mInitialPathToLoad.get(0);
            boolean found = false;
            for (Content content : contents) {
                if (content.type() == ContentType.Directory) {
                    if (content.path().equals(itemToLoad)) {
                        onTreeSelected(content);
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                mInitialPathToLoad.remove(0);
            } else {
                mInitialPathToLoad = null;
            }
        }
    }

    @Override
    public void onTreeSelected(Content content) {
        String path = content.path();
        if (content.type() == ContentType.Directory) {
            mDirStack.push(path);
            updateBreadcrumbs();
            addFragmentForTopOfStack();
        } else if (mGitModuleMap != null && mGitModuleMap.get(path) != null) {
            String[] userRepo = mGitModuleMap.get(path).split("/");
            startActivity(RepositoryActivity.makeIntent(getActivity(), userRepo[0], userRepo[1]));
        } else {
            startActivity(FileViewerActivity.makeIntent(getActivity(),
                    mRepository.owner().login(), mRepository.name(),
                    getCurrentRef(), content.path()));
        }
    }

    @Override
    public Set<String> getSubModuleNames(ContentListFragment fragment) {
        if (mGitModuleMap == null) {
            return null;
        }

        String prefix = fragment.getPath() == null ? null : (fragment.getPath() + "/");
        Set<String> names = new HashSet<>();
        for (String name : mGitModuleMap.keySet()) {
            if (prefix == null && !name.contains("/")) {
                names.add(name);
            } else if (prefix != null && name.startsWith(prefix)) {
                names.add(name.substring(prefix.length()));
            }
        }
        return names;
    }

    @Override
    public void onCrumbSelection(String absolutePath, int index, int count) {
        FragmentManager fm = getChildFragmentManager();
        boolean poppedAny = false;
        while (mDirStack.size() > 1 && !TextUtils.equals(absolutePath, mDirStack.peek())) {
            mDirStack.pop();
            fm.popBackStack();
            poppedAny = true;
        }
        if (poppedAny) {
            fm.executePendingTransactions();
            updateBreadcrumbs();
        }
    }

    private void addFragmentForTopOfStack() {
        String path = mDirStack.peek();
        mContentListFragment = ContentListFragment.newInstance(mRepository,
                TextUtils.isEmpty(path) ? null : path,
                mContentCache.get(path), mSelectedRef);

        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        if (path != null) {
            ft.addToBackStack(null);
        }
        ft.replace(R.id.content_list_container, mContentListFragment);
        ft.commit();
    }

    private void updateBreadcrumbs() {
        String path = mDirStack.peek();
        mBreadcrumbs.setPath(path);
    }

    private String getCurrentRef() {
        if (!TextUtils.isEmpty(mSelectedRef)) {
            return mSelectedRef;
        }
        return mRepository.defaultBranch();
    }
}
