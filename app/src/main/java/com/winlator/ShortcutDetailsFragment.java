package com.winlator;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.Shortcut;

import java.io.File;

public class ShortcutDetailsFragment extends Fragment {
    private final int containerId;
    private final String shortcutPath;
    private Container container;
    private Shortcut shortcut;

    public ShortcutDetailsFragment(int containerId, String shortcutPath) {
        this.containerId = containerId;
        this.shortcutPath = shortcutPath;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shortcut_details, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ContainerManager manager = new ContainerManager(requireContext());
        container = manager.getContainerById(containerId);
        File file = shortcutPath == null ? null : new File(shortcutPath);
        if (container == null || file == null || !file.exists()) {
            ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment(false));
            return;
        }

        shortcut = new Shortcut(container, file);
        ImageView icon = view.findViewById(R.id.IVShortcutIcon);
        if (shortcut.icon != null) icon.setImageBitmap(shortcut.icon);
        else icon.setImageResource(file.isDirectory()
                ? R.drawable.container_folder : R.drawable.container_file_link);

        ((TextView) view.findViewById(R.id.TVShortcutName)).setText(shortcut.name);
        ((TextView) view.findViewById(R.id.TVShortcutContainer))
                .setText("Container  ·  " + container.getName());
        ((TextView) view.findViewById(R.id.TVShortcutKind))
                .setText(file.isDirectory() ? "Folder in Windows workspace" : "Launchable Windows shortcut");
        ((TextView) view.findViewById(R.id.TVShortcutPath)).setText("Location\n" + file.getAbsolutePath());

        Button play = view.findViewById(R.id.BTPlayShortcut);
        Button folder = view.findViewById(R.id.BTOpenFolder);
        if (file.isDirectory()) play.setText("Open directory");
        play.setOnClickListener(v -> {
            if (shortcut.file.isDirectory()) {
                ((MainActivity) requireActivity()).showFragment(
                        new ContainerFileManagerFragment(container.id, shortcut.file.getAbsolutePath()));
            }
            else {
                Intent intent = new Intent(requireActivity(), XServerDisplayActivity.class);
                intent.putExtra("container_id", container.id);
                intent.putExtra("shortcut_path", shortcut.file.getPath());
                startActivity(intent);
            }
        });
        folder.setOnClickListener(v -> {
            File target = shortcut.file.isDirectory() ? shortcut.file : shortcut.file.getParentFile();
            if (target != null) {
                ((MainActivity) requireActivity()).showFragment(
                        new ContainerFileManagerFragment(container.id, target.getAbsolutePath()));
            }
        });
        view.findViewById(R.id.BTBackLibrary).setOnClickListener(v ->
                ((MainActivity) requireActivity()).showFragment(new ShortcutsFragment(false)));

        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(shortcut.name);
        }
    }
}
