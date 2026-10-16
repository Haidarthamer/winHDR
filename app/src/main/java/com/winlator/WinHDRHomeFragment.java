package com.winlator;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class WinHDRHomeFragment extends Fragment {
    private LinearLayout continueRow;
    private LinearLayout recentlyAddedRow;
    private TextView emptyText;
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_winhdr_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        continueRow = view.findViewById(R.id.llHomeContinue);
        recentlyAddedRow = view.findViewById(R.id.llHomeRecentlyAdded);
        emptyText = view.findViewById(R.id.TVHomeEmpty);

        view.findViewById(R.id.btnHomeContainers).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ContainersFragment()));
        view.findViewById(R.id.btnHomeGames).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment(true)));
        view.findViewById(R.id.btnHomeSeeAll).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment(false)));
        view.findViewById(R.id.btnHomeControls).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new InputControlsFragment(0)));
        view.findViewById(R.id.btnHomeSettings).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new SettingsFragment()));

        loadHomeShortcuts();
    }

    public void setSearchQuery(String query) {
        searchQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (isAdded() && continueRow != null) loadHomeShortcuts();
    }

    private void loadHomeShortcuts() {
        if (!isAdded() || continueRow == null || recentlyAddedRow == null) return;
        continueRow.removeAllViews();
        recentlyAddedRow.removeAllViews();

        ArrayList<Shortcut> shortcuts = new ContainerManager(requireContext()).loadShortcuts(null);
        Collections.sort(shortcuts, (left, right) ->
                Long.compare(right.file.lastModified(), left.file.lastModified()));

        ArrayList<Shortcut> filtered = new ArrayList<>();
        for (Shortcut shortcut : shortcuts) {
            String name = shortcut.name == null ? "" : shortcut.name.toLowerCase(Locale.ROOT);
            String containerName = shortcut.container.getName() == null ? "" :
                    shortcut.container.getName().toLowerCase(Locale.ROOT);
            if (searchQuery.isEmpty() || name.contains(searchQuery) || containerName.contains(searchQuery)) {
                filtered.add(shortcut);
            }
        }

        int quickCount = Math.min(4, filtered.size());
        for (int i = 0; i < quickCount; i++) {
            continueRow.addView(createShortcutCard(filtered.get(i)));
        }
        int recentCount = Math.min(8, filtered.size());
        for (int i = 0; i < recentCount; i++) {
            recentlyAddedRow.addView(createShortcutCard(filtered.get(i)));
        }

        if (emptyText != null) {
            emptyText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
            if (!searchQuery.isEmpty()) emptyText.setText("No shortcuts match \"" + searchQuery + "\".");
            else emptyText.setText("Your shortcut library is empty. Create a container and install or add a Windows app to get started.");
        }
    }

    private View createShortcutCard(Shortcut shortcut) {
        final android.content.Context context = requireContext();
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(ContextCompat.getDrawable(context, R.drawable.winhdr_game_card));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(dp(146), dp(184));
        cardParams.setMargins(0, 0, dp(10), 0);
        card.setLayoutParams(cardParams);

        FrameLayout artwork = new FrameLayout(context);
        GradientDrawable artworkBackground = new GradientDrawable();
        artworkBackground.setColor(Color.rgb(16, 35, 59));
        artworkBackground.setCornerRadius(dp(6));
        artwork.setBackground(artworkBackground);
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(88));
        artworkParams.bottomMargin = dp(7);
        card.addView(artwork, artworkParams);

        ImageView icon = new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        icon.setPadding(dp(14), dp(12), dp(14), dp(12));
        if (shortcut.icon != null) icon.setImageBitmap(shortcut.icon);
        else icon.setImageResource(shortcut.file.isDirectory()
                ? R.drawable.container_folder : R.drawable.container_file_link);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        artwork.addView(icon, iconParams);

        TextView title = new TextView(context);
        title.setText(shortcut.name);
        title.setTextColor(Color.rgb(241, 246, 255));
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        TextView subtitle = new TextView(context);
        subtitle.setText(shortcut.container.getName());
        subtitle.setTextColor(Color.rgb(129, 149, 178));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        subtitle.setMaxLines(1);
        subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16));
        subtitleParams.topMargin = dp(1);
        card.addView(subtitle, subtitleParams);

        Button launch = new Button(context);
        launch.setText(shortcut.file.isDirectory() ? "Open" : "Play  →");
        launch.setTextAllCaps(false);
        launch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        launch.setTextColor(Color.WHITE);
        launch.setPadding(dp(4), 0, dp(4), 0);
        launch.setMinHeight(dp(29));
        launch.setMinimumHeight(dp(29));
        launch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(28, 98, 185)));
        LinearLayout.LayoutParams launchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30));
        launchParams.topMargin = dp(4);
        card.addView(launch, launchParams);

        View.OnClickListener openListener = v -> launchShortcut(shortcut);
        card.setOnClickListener(openListener);
        launch.setOnClickListener(openListener);
        return card;
    }

    private void launchShortcut(Shortcut shortcut) {
        if (shortcut.file.isDirectory()) {
            ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment(false));
            return;
        }
        Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
        intent.putExtra("container_id", shortcut.container.id);
        intent.putExtra("shortcut_path", shortcut.file.getPath());
        startActivity(intent);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
