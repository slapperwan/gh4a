package com.gh4a.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
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
            if (mUserLogin.equals(Gh4Application.get(this).getAuthLogin())) {
                for (String item : getResources().getStringArray(R.array.repo_login_item)) {
                    list.add(item);    
                }
            }
            else {
                for (String item : getResources().getStringArray(R.array.repo_user_item)) {
                    list.add(item);    
                }
            }
        }
        else {
            for (String item : getResources().getStringArray(R.array.repo_org_item)) {
                list.add(item);    
            }
        }
        
        list.setDropDownViewResource(R.layout.row_simple);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(list, this);
    }
    
    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        ListFragment fragment = null;
        if (mUserLogin.equals(Gh4Application.get(this).getAuthLogin())) {
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
    protected void navigateUp() {
        finish();
    }
}
