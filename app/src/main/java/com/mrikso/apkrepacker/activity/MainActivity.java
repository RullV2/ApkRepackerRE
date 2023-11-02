package com.mrikso.apkrepacker.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.view.MenuItem;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import com.google.android.material.bottomnavigation.BottomNavigationView;


import com.masrull.update.UpdaterAssistant;
import com.mrikso.apkrepacker.App;
import com.mrikso.apkrepacker.R;
import com.mrikso.apkrepacker.fragment.AboutFragment;
import com.mrikso.apkrepacker.fragment.AppsFragment;
import com.mrikso.apkrepacker.fragment.OnBackPressedListener;
import com.mrikso.apkrepacker.fragment.ProjectsFragment;
import com.mrikso.apkrepacker.fragment.SettingsFragment;
import com.mrikso.apkrepacker.ui.preferences.InitPreference;
import com.mrikso.apkrepacker.utils.AppExecutor;
import com.mrikso.apkrepacker.utils.FragmentNavigator;

public class MainActivity extends BaseActivity implements BottomNavigationView.OnNavigationItemSelectedListener, FragmentNavigator.FragmentFactory {
    private Preference kuts;
    private ProjectsFragment projectFragment;
    private FragmentNavigator mFragmentNavigator;
    private static int f775a;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        sayaupdate();
    
        Toast.makeText(getApplicationContext(),"Beta Test",Toast.LENGTH_SHORT).show();
        App.setContext(this);
        AppExecutor.getInstance().getDiskIO().execute(() -> {
            new InitPreference().init(this);
        });

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(this);
        mFragmentNavigator = new FragmentNavigator(savedInstanceState, getSupportFragmentManager(), R.id.container_main, this);
        projectFragment = mFragmentNavigator.findFragmentByTag(ProjectsFragment.TAG);
        if (savedInstanceState == null)
            mFragmentNavigator.switchTo(ProjectsFragment.TAG);

        //FragmentUtils.replace(projectFragment, getSupportFragmentManager(), R.id.container_main, ProjectsFragment.TAG, false);
    
    }
  
  

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                mFragmentNavigator.switchTo(ProjectsFragment.TAG);
                //FragmentUtils.replace(projectFragment, getSupportFragmentManager(), R.id.container_main, ProjectsFragment.TAG, false);
                break;
            case R.id.navigation_apps:
                mFragmentNavigator.switchTo(AppsFragment.TAG);
                // FragmentUtils.replace(new AppsFragment(), getSupportFragmentManager(), R.id.container_main, AppsFragment.TAG, false);
                break;
            case R.id.navigation_settings:
                mFragmentNavigator.switchTo(SettingsFragment.TAG);
                // FragmentUtils.replace(new SettingsFragment(), getSupportFragmentManager(), R.id.container_main, SettingsFragment.TAG, false);
                break;
            case R.id.navigation_about:
                mFragmentNavigator.switchTo(AboutFragment.TAG);
                // FragmentUtils.replace(new AboutFragment(), getSupportFragmentManager(), R.id.container_main, AboutFragment.TAG, false);
                break;
        }
        return true;
    }
  
   public void writeUpdateChannelPreference(String channel) {
        SharedPreferences.Editor editor = getSharedPreferences("com.mrikso.apkrepacker_preferences", 0).edit();
        editor.apply();
    }

	public void sayaupdate(){
		if (getCurrentChannel() == null) {
            writeUpdateChannelPreference("stable");
            writeUpdatesOnStartup(true);
        }
        if (getStartupUpdates()) {
            new UpdaterAssistant(this, false).checkForUpdates();
        }
    }
    public void writeUpdatesOnStartup(boolean shouldCheck) {
        SharedPreferences.Editor editor = getSharedPreferences("com.mrikso.apkrepacker_preferences", 0).edit();
        editor.putBoolean("updates_on_startup", shouldCheck);
        editor.apply();
    }

    public String getCurrentChannel() {
        SharedPreferences prefs = getSharedPreferences("com.mrikso.apkrepacker_preferences", 0);
        return prefs.getString("channel", null);
    }

    public boolean getStartupUpdates() {
        SharedPreferences prefs = getSharedPreferences("com.mrikso.apkrepacker_preferences", 0);
        return prefs.getBoolean("updates_on_startup", true);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        OnBackPressedListener backPressedListener = null;
        for (Fragment fragment : fm.getFragments()) {
            if (fragment instanceof OnBackPressedListener) {
                backPressedListener = (OnBackPressedListener) fragment;
                break;
            }
        }
        if (backPressedListener != null) {
            backPressedListener.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentNavigator.writeStateToBundle(outState);
    }

    @Override
    public Fragment createFragment(String tag) {
        switch (tag) {
            case ProjectsFragment.TAG:
                return getProjectFragment();
            case AppsFragment.TAG:
                return new AppsFragment();
            case SettingsFragment.TAG:
                return new SettingsFragment();
            case AboutFragment.TAG:
                return new AboutFragment();
        }

        throw new IllegalArgumentException("Unknown fragment tag: " + tag);
    }

    private ProjectsFragment getProjectFragment() {
        if (projectFragment == null)
            projectFragment = new ProjectsFragment();
        return projectFragment;
    }
  
}