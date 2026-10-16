package com.winlator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class WinHDRHomeFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_winhdr_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btnHomeContainers).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ContainersFragment()));
        view.findViewById(R.id.btnHomeGames).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment()));
        view.findViewById(R.id.btnHomeSettings).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new SettingsFragment()));
        view.findViewById(R.id.btnHomeControls).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new InputControlsFragment(0)));
    }
}
