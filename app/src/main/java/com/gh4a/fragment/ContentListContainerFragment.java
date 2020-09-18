package com.gh4a.fragment;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.widget.PathBreadcrumbs;
import com.gh4a.widget.SwipeRefreshLayout;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.Content;
import com.meisolsson.githubsdk.model.ContentType;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryContentService;
import com.philosophicalhacker.lib.RxLoader;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class ContentListContainerFragment extends Fragment implements
        ContentListFragment.ParentCallback, PathBreadcrumbs.SelectionCallback,
        BaseActivity.RefreshableChild, SwipeRefreshLayout.ChildScrollDelegate {
    public interface CommitSelectionCallback {
        void onCommitSelectedAsBase(Commit commit);
    }
    private static final int ID_LOADER_MODULEMAP = 100;

    private static final String STATE_KEY_DIR_STACK = "dir_stack";
    private static final String STATE_KEY_INITIAL_PATH = "initial_path";

    private RxLoader mRxLoader;
    private PathBreadcrumbs mBreadcrumbs;
    private ContentListFragment mContentListFragment;
    private Repository mRepository;
    private String mSelectedRef;
    private Map<String, String> mGitModuleMap;
    private final Stack<String> mDirStack = new Stack<>();
    private ArrayList<String> mInitialPathToLoad;
    private boolean mStateSaved;
    private CommitSelectionCallback mCommitCallback;
    private ContentListCacheFragment mCacheFragment;

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

        mRxLoader = new RxLoader(getActivity(), LoaderManager.getInstance(this));
        mRepository = getArguments().getParcelable("repository");
        mSelectedRef = getArguments().getString("ref");
        mStateSaved = false;

        mCacheFragment = (ContentListCacheFragment)
                getParentFragmentManager().findFragmentByTag("content_list_cache");
        if (mCacheFragment == null) {
            mCacheFragment = new ContentListCacheFragment();
            getParentFragmentManager().beginTransaction()
                    .add(mCacheFragment, "content_list_cache")
                    .commitAllowingStateLoss();
        }

        if (savedInstanceState != null) {
            mDirStack.addAll(savedInstanceState.getStringArrayList(STATE_KEY_DIR_STACK));
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CommitSelectionCallback) {
            mCommitCallback = (CommitSelectionCallback) context;
        } else {
            throw new ClassCastException("No callback provided");
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
        mCacheFragment.clear();
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
        mStateSaved = true;
    }

    @Override
    public void onContentsLoaded(ContentListFragment fragment, List<Content> contents) {
        if (contents == null) {
            return;
        }
        mCacheFragment.addToCache(fragment.getPath(), contents);
        if (TextUtils.isEmpty(fragment.getPath())) {
            for (Content content : contents) {
                if (content.type() == ContentType.File && content.name().equals(".gitmodules")) {
                    loadModuleMap();
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
    public void onCommitSelected(Commit commit) {
        mCommitCallback.onCommitSelectedAsBase(commit);
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

        String prefix = TextUtils.isEmpty(fragment.getPath()) ? null : fragment.getPath() + "/";
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
                mCacheFragment.getFromCache(path), mSelectedRef);

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

    private void loadModuleMap() {
        RepositoryContentService service = ServiceFactory.get(RepositoryContentService.class, false);
        String repoOwner = mRepository.owner().login();
        String repoName = mRepository.name();

        service.getContents(repoOwner, repoName, ".gitmodules", mSelectedRef)
                .map(ApiHelpers::throwOnFailure)
                .map(Optional::of)
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, Optional.<Content>absent()))
                .map(contentOpt -> contentOpt.map(content -> StringUtils.fromBase64(content.content())))
                .map(this::parseModuleMap)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(mRxLoader.makeSingleTransformer(ID_LOADER_MODULEMAP, true))
                .subscribe(resultOpt -> {
                    mGitModuleMap = resultOpt.orNull();
                    if (mContentListFragment != null) {
                        mContentListFragment.onSubModuleNamesChanged(getSubModuleNames(mContentListFragment));
                    }
                }, ((BaseActivity) getActivity())::handleLoadFailure);
    }

    private Optional<Map<String, String>> parseModuleMap(Optional<String> inputOpt) {
        String input = inputOpt.orNull();
        if (StringUtils.isBlank(input)) {
            return Optional.absent();
        }
        Map<String, String> result = new HashMap<>();
        String pendingPath = null;
        String pendingTarget = null;

        for (String line : input.split("\n")) {
            line = line.trim();
            if (line.startsWith("[submodule")) {
                if (pendingPath != null && pendingTarget != null) {
                    result.put(pendingPath, pendingTarget);
                }
                pendingPath = null;
                pendingTarget = null;
            } else if (line.startsWith("path = ")) {
                // chop off "path ="
                pendingPath = line.substring(6).trim();
            } else if (line.startsWith("url = ")) {
                String url = line.substring(5).trim().replace("github.com:", "github.com/");
                int pos = url.indexOf("git@");
                if (pos == 0) {
                    url = "ssh://" + url.substring(4);
                }

                Uri uri = Uri.parse(url);
                if (!TextUtils.equals(uri.getHost(), "github.com")) {
                    continue;
                }
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments == null || pathSegments.size() < 2) {
                    continue;
                }
                String user = pathSegments.get(pathSegments.size() - 2);
                String repo = pathSegments.get(pathSegments.size() - 1);

                pos = repo.lastIndexOf(".");
                if (pos != -1) {
                    repo = repo.substring(0, pos);
                }
                pendingTarget = user + "/" + repo;
            }
        }

        if (pendingPath != null && pendingTarget != null) {
            result.put(pendingPath, pendingTarget);
        }

        return Optional.of(result);

    }
}
