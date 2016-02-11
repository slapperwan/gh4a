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

import com.gh4a.R;
import com.gh4a.loader.GitModuleParserLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.IntentUtils;
import com.gh4a.widget.PathBreadcrumbs;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class ContentListContainerFragment extends Fragment implements
        ContentListFragment.ParentCallback, PathBreadcrumbs.SelectionCallback {
    private static final int LOADER_MODULEMAP = 100;

    private static final String STATE_KEY_DIR_STACK = "dir_stack";
    private static final String STATE_KEY_CONTENT_CACHE_PREFIX = "content_cache_";

    private PathBreadcrumbs mBreadcrumbs;
    private ContentListFragment mContentListFragment;
    private Repository mRepository;
    private String mSelectedRef;
    private Map<String, String> mGitModuleMap;
    private Stack<String> mDirStack = new Stack<>();
    private Map<String, ArrayList<RepositoryContents>> mContentCache =
            new LinkedHashMap<String, ArrayList<RepositoryContents>>() {
        private static final long serialVersionUID = -2379579224736389357L;
        private static final int MAX_CACHE_ENTRIES = 100;

        @Override
        protected boolean removeEldestEntry(Entry<String, ArrayList<RepositoryContents>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private LoaderCallbacks<Map<String, String>> mGitModuleCallback =
            new LoaderCallbacks<Map<String, String>>() {
        @Override
        public Loader<LoaderResult<Map<String, String>>> onCreateLoader(int id, Bundle args) {
            return new GitModuleParserLoader(getActivity(), mRepository.getOwner().getLogin(),
                    mRepository.getName(), ".gitmodules", mSelectedRef);
        }
        @Override
        public void onResultReady(LoaderResult<Map<String, String>> result) {
            mGitModuleMap = result.getData();
            if (mContentListFragment != null) {
                mContentListFragment.onSubModuleNamesChanged(getSubModuleNames(mContentListFragment));
            }
        }
    };

    public static ContentListContainerFragment newInstance(Repository repository, String ref) {
        ContentListContainerFragment f = new ContentListContainerFragment();

        Bundle args = new Bundle();
        args.putSerializable("repository", repository);
        args.putString("ref", ref);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRepository = (Repository) getArguments().getSerializable("repository");
        mSelectedRef = getArguments().getString("ref");

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
                    mContentCache.put(cacheKey,
                            (ArrayList<RepositoryContents>) savedInstanceState.getSerializable(key));
                }
            }
        } else {
            mDirStack.push(null);
        }
    }

    public void setRef(String ref) {
        mSelectedRef = ref;
        mGitModuleMap = null;
        mDirStack.clear();
        mDirStack.push(null);
        mContentCache.clear();
        mContentListFragment = null;
        getChildFragmentManager().popBackStackImmediate(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        addFragmentForTopOfStack();
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
        mBreadcrumbs = (PathBreadcrumbs) view.findViewById(R.id.breadcrumbs);
        mBreadcrumbs.setCallback(this);
        updateBreadcrumbs();
        addFragmentForTopOfStack();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_KEY_DIR_STACK, new ArrayList<>(mDirStack));
        for (Map.Entry<String, ArrayList<RepositoryContents>> entry : mContentCache.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                key = "/";
            }
            outState.putSerializable(STATE_KEY_CONTENT_CACHE_PREFIX + key, entry.getValue());
        }
    }

    @Override
    public void onContentsLoaded(ContentListFragment fragment, List<RepositoryContents> contents) {
        if (contents == null) {
            return;
        }
        mContentCache.put(fragment.getPath(), new ArrayList<>(contents));
        if (fragment.getPath() == null) {
            for (RepositoryContents content : contents) {
                if (RepositoryContents.TYPE_FILE.equals(content.getType())) {
                    if (content.getName().equals(".gitmodules")) {
                        LoaderManager lm = getActivity().getSupportLoaderManager();
                        lm.restartLoader(LOADER_MODULEMAP, null, mGitModuleCallback);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onTreeSelected(RepositoryContents content) {
        String path = content.getPath();
        if (RepositoryContents.TYPE_DIR.equals(content.getType())) {
            mDirStack.push(path);
            updateBreadcrumbs();
            addFragmentForTopOfStack();
        } else if (mGitModuleMap != null && mGitModuleMap.get(path) != null) {
            String[] userRepo = mGitModuleMap.get(path).split("/");
            startActivity(IntentUtils.getRepoActivityIntent(getActivity(),
                    userRepo[0], userRepo[1], null));
        } else {
            startActivity(IntentUtils.getFileViewerActivityIntent(getActivity(),
                    mRepository.getOwner().getLogin(), mRepository.getName(),
                    getCurrentRef(), content.getPath()));
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
        if (TextUtils.isEmpty(absolutePath)) {
            absolutePath = null;
        }
        while (!TextUtils.equals(absolutePath, mDirStack.peek())) {
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
        mContentListFragment = ContentListFragment.newInstance(mRepository, path,
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
        mBreadcrumbs.setPath(path == null ? "" : path);
    }

    private String getCurrentRef() {
        if (!TextUtils.isEmpty(mSelectedRef)) {
            return mSelectedRef;
        }
        return mRepository.getDefaultBranch();
    }
}
