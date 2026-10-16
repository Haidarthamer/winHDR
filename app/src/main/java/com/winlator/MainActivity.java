package com.winlator;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;
import com.winlator.contentdialog.AboutDialog;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.LocaleHelper;
import com.winlator.core.PreloaderDialog;
import com.winlator.xenvironment.RootFSInstaller;

import java.io.File;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final boolean DEBUG_MODE = false; // FIXME change to false
    public static final @IntRange(from = 1, to = 19) byte CONTAINER_PATTERN_COMPRESSION_LEVEL = 9;
    public static final byte PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;
    public static final byte OPEN_FILE_REQUEST_CODE = 2;
    public static final byte EDIT_INPUT_CONTROLS_REQUEST_CODE = 3;
    public static final byte OPEN_DIRECTORY_REQUEST_CODE = 4;
    private DrawerLayout drawerLayout;
    private boolean permanentNavigation = false;
    public final PreloaderDialog preloaderDialog = new PreloaderDialog(this);
    private boolean editInputControls = false;
    private int selectedProfileId;
    private Callback<Uri> openFileCallback;
    private SharedPreferences preferences;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppUtils.setActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        drawerLayout = findViewById(R.id.DrawerLayout);
        permanentNavigation = findViewById(R.id.PermanentNavigationRoot) != null;
        NavigationView navigationView = findViewById(R.id.NavigationView);
        navigationView.setNavigationItemSelectedListener(this);

        setSupportActionBar(findViewById(R.id.Toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(!permanentNavigation);
        actionBar.setDisplayShowTitleEnabled(!permanentNavigation);

        SearchView globalSearch = findViewById(R.id.GlobalSearch);
        if (globalSearch != null) {
            globalSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    applyGlobalSearch(query);
                    return true;
                }
                @Override public boolean onQueryTextChange(String newText) {
                    applyGlobalSearch(newText);
                    return true;
                }
            });
        }
        View topControls = findViewById(R.id.btnTopControls);
        if (topControls != null) topControls.setOnClickListener(v -> showFragment(new InputControlsFragment(0)));
        View topSettings = findViewById(R.id.btnTopSettings);
        if (topSettings != null) topSettings.setOnClickListener(v -> showFragment(new SettingsFragment()));

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Intent intent = getIntent();
        editInputControls = intent.getBooleanExtra("edit_input_controls", false);
        if (editInputControls) {
            selectedProfileId = intent.getIntExtra("selected_profile_id", 0);
            if (!permanentNavigation) actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_back);
            onNavigationItemSelected(navigationView.getMenu().findItem(R.id.menu_item_input_controls));
            navigationView.setCheckedItem(R.id.menu_item_input_controls);
        }
        else {
            boolean showShortcutsFirst = preferences.getBoolean("show_shortcuts_first", false);
            int selectedMenuItemId = intent.getIntExtra("selected_menu_item_id", 0);
            int menuItemId = selectedMenuItemId > 0 ? selectedMenuItemId : R.id.menu_item_home;

            if (!permanentNavigation) actionBar.setHomeAsUpIndicator(R.drawable.icon_action_bar_menu);
            onNavigationItemSelected(navigationView.getMenu().findItem(menuItemId));
            navigationView.setCheckedItem(menuItemId);
            if (!requestAppPermissions()) RootFSInstaller.installIfNeeded(this);

            int containerId = intent.getIntExtra("container_id", 0);
            String startPath = intent.getStringExtra("start_path");
            if (containerId > 0 && startPath != null) {
                showFragment(new ContainerFileManagerFragment(containerId, startPath));
            }
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.setSystemLocale(newBase));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                RootFSInstaller.installIfNeeded(this);
            }
            else finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MainActivity.OPEN_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (openFileCallback != null) {
                openFileCallback.call(data.getData());
                openFileCallback = null;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ((newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ||
            newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) && currentFragment instanceof BaseFileManagerFragment) {
            ((BaseFileManagerFragment)currentFragment).onOrientationChanged();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragment != null && currentFragment.isVisible()) {
            if (currentFragment instanceof ShortcutDetailsFragment) {
                showFragment(new ShortcutsFragment(false));
                return;
            }
            if (currentFragment instanceof BaseFileManagerFragment) {
                BaseFileManagerFragment fileManagerFragment = (BaseFileManagerFragment)currentFragment;
                if (fileManagerFragment.onBackPressed()) return;
            }
            else if (currentFragment instanceof ContainersFragment && !permanentNavigation) {
                finish();
                return;
            }
        }

        if (permanentNavigation) {
            showFragment(new WinHDRHomeFragment());
            NavigationView nav = findViewById(R.id.NavigationView);
            if (nav != null) nav.setCheckedItem(R.id.menu_item_home);
        }
        else showFragment(new ContainersFragment());
    }

    private void applyGlobalSearch(String query) {
        if (currentFragment instanceof ShortcutsFragment) {
            ((ShortcutsFragment) currentFragment).setSearchQuery(query);
        }
        else if (currentFragment instanceof WinHDRHomeFragment) {
            ((WinHDRHomeFragment) currentFragment).setSearchQuery(query);
        }
    }

    private void openDesktop() {
        ContainerManager manager = new ContainerManager(this);
        if (manager.getContainers().isEmpty()) {
            showFragment(new ContainersFragment());
            return;
        }
        Container container = manager.getContainers().get(0);
        File desktop = new File(container.getUserDir(), "Desktop");
        if (!desktop.exists()) desktop.mkdirs();
        showFragment(new ContainerFileManagerFragment(container.id, desktop.getAbsolutePath()));
    }

    public void setOpenFileCallback(Callback<Uri> openFileCallback) {
        this.openFileCallback = openFileCallback;
    }

    private boolean requestAppPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) return false;

        String[] permissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == R.id.menu_item_add ||
            itemId == R.id.menu_item_home ||
            itemId == R.id.menu_item_view_style ||
            itemId == R.id.menu_item_new_folder) {
            return super.onOptionsItemSelected(menuItem);
        }
        else {
            if (editInputControls) {
                setResult(RESULT_OK);
                finish();
            }
            else {
                if (currentFragment instanceof BaseFileManagerFragment) {
                    BaseFileManagerFragment fileManagerFragment = (BaseFileManagerFragment)currentFragment;
                    if (fileManagerFragment.onOptionsMenuClicked()) return true;
                }
                if (!permanentNavigation) drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        switch (item.getItemId()) {
            case R.id.menu_item_home:
                showFragment(new WinHDRHomeFragment());
                break;
            case R.id.menu_item_shortcuts:
                preferences.edit().putBoolean("show_shortcuts_first", true).apply();
                showFragment(new ShortcutsFragment(false));
                break;
            case R.id.menu_item_games:
                preferences.edit().putBoolean("show_shortcuts_first", true).apply();
                showFragment(new ShortcutsFragment(true));
                break;
            case R.id.menu_item_desktop:
                openDesktop();
                break;
            case R.id.menu_item_containers:
                preferences.edit().putBoolean("show_shortcuts_first", false).apply();
                showFragment(new ContainersFragment());
                break;
            case R.id.menu_item_input_controls:
                showFragment(new InputControlsFragment(selectedProfileId));
                break;
            case R.id.menu_item_settings:
                showFragment(new SettingsFragment());
                break;
            case R.id.menu_item_about:
                (new AboutDialog(this)).show();
                break;
        }
        return true;
    }

    public void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(R.id.FLFragmentContainer, fragment)
            .commit();

        if (!permanentNavigation) drawerLayout.closeDrawer(GravityCompat.START);
        currentFragment = fragment;
        NavigationView navigationView = findViewById(R.id.NavigationView);
        if (navigationView != null) {
            int selectedId = 0;
            if (fragment instanceof WinHDRHomeFragment) selectedId = R.id.menu_item_home;
            else if (fragment instanceof ShortcutsFragment) {
                selectedId = ((ShortcutsFragment) fragment).isGamesOnly()
                        ? R.id.menu_item_games : R.id.menu_item_shortcuts;
            }
            else if (fragment instanceof ContainerFileManagerFragment) selectedId = R.id.menu_item_desktop;
            else if (fragment instanceof ContainersFragment || fragment instanceof ContainerDetailFragment)
                selectedId = R.id.menu_item_containers;
            else if (fragment instanceof InputControlsFragment) selectedId = R.id.menu_item_input_controls;
            else if (fragment instanceof SettingsFragment) selectedId = R.id.menu_item_settings;
            if (selectedId != 0) navigationView.setCheckedItem(selectedId);
        }
    }
}