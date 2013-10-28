package com.gh4a.activities
;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.TrendingFragment;

public class TrendingActivity extends BaseSherlockFragmentActivity {

    private static final String TODAY = "http://github-trends.oscardelben.com/explore/today.xml";
    private static final String WEEK = "http://github-trends.oscardelben.com/explore/week.xml";
    private static final String MONTH = "http://github-trends.oscardelben.com/explore/month.xml";
    private static final String FOREVER = "http://github-trends.oscardelben.com/explore/forever.xml";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.trend);
        actionBar.setSubtitle(R.string.explore);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setupPager(new ThisPageAdapter(getSupportFragmentManager()), new int[] {
            R.string.trend_today, R.string.trend_month,
            R.string.trend_month, R.string.trend_forever
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.explore_menu, menu);
        menu.removeItem(R.id.refresh);
        menu.findItem(R.id.trend).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        if (!Gh4Application.get(this).isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        else {
            Gh4Application app = Gh4Application.get(this);
            app.openUserInfoActivity(this, app.getAuthLogin(), null,
                    Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.pub_timeline:
                Intent intent = new Intent().setClass(this, TimelineActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.trend:
                intent = new Intent().setClass(this, TrendingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.blog:
                intent = new Intent().setClass(this, BlogListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public class ThisPageAdapter extends FragmentStatePagerAdapter {

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 1) {
                return TrendingFragment.newInstance(WEEK);
            }
            else if (position == 2) {
                return TrendingFragment.newInstance(MONTH);
            }
            else if (position == 3) {
                return TrendingFragment.newInstance(FOREVER);
            }
            else {
                return TrendingFragment.newInstance(TODAY);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
}
