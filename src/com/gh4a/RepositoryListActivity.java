package com.gh4a;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.BaseFragment;
import com.gh4a.fragment.RepositoryListFragment;
import com.gh4a.fragment.StarredRepositoryListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;

public class RepositoryListActivity extends BaseSherlockFragmentActivity implements ActionBar.OnNavigationListener {

    public String mUserLogin;
    public String mUserType;
    private ActionBar mActionBar;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserType = data.getString(Constants.User.USER_TYPE);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.frame_layout);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mUserLogin);
        mActionBar.setSubtitle(R.string.user_pub_repos);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Context context = mActionBar.getThemedContext();
        ArrayAdapter<CharSequence> list = new ArrayAdapter<CharSequence>(context, R.layout.sherlock_spinner_item);
        if (!Constants.User.USER_TYPE_ORG.equals(mUserType)) {
            if (mUserLogin.equals(getAuthLogin())) {
                list.addAll(getResources().getStringArray(R.array.repo_login_item));
            }
            else {
                list.addAll(getResources().getStringArray(R.array.repo_user_item));
            }
            list.setDropDownViewResource(R.layout.row_simple);

            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mActionBar.setListNavigationCallbacks(list, this);
        }
        else {
            
        }
    }
    
    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        BaseFragment fragment = null;
        if (mUserLogin.equals(getAuthLogin())) {
            switch (position) {
            case 0:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "all");
                break;
            case 1:
                fragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                break;
            case 2:
                fragment = StarredRepositoryListFragment.newInstance(mUserLogin);
                break;
            case 3:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "public");
                break;
            case 4:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "private");
                break;
            case 5:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "sources");
                break;
            case 6:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "forks");
                break;
            case 7:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "member");
                break;
            default:
                break;
            }
        }
        else {
            switch (position) {
            case 0:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "all");
                break;
            case 1:
                fragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                break;
            case 2:
                fragment = StarredRepositoryListFragment.newInstance(mUserLogin);
                break;
            case 3:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "sources");
                break;
            case 4:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "forks");
                break;
            case 5:
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "member");
                break;
            default:
                break;
            }
        }
        
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            
            fragmentTransaction.replace(R.id.details, fragment);
            fragmentTransaction.commit();
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;     
            default:
                return true;
        }
    }
}
