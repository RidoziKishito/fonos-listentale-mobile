package com.example.fonoss.ui.library;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.auth.UserViewModel;
import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.adapter.PlaylistAdapter;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.model.Playlist;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@AndroidEntryPoint
public class LibraryFragment extends Fragment {

    private LibraryViewModel libraryViewModel;
    private UserViewModel userViewModel;
    private BookAdapter adapter;
    private PlaylistAdapter playlistAdapter;
    private List<Book> currentList = new ArrayList<>();
    private int currentTabPosition = 0;
    private final TextView[] tabButtons = new TextView[4];
    private final Map<Integer, Set<String>> selectionsByTab = new HashMap<>();
    private boolean isSelectionMode = false;
    private TextView buttonToggleSelect;
    private ImageButton buttonAddPlaylist;
    private View layoutSelectionControls;
    private CheckBox checkboxSelectAll;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        for (int i = 0; i < 4; i++) selectionsByTab.put(i, new HashSet<>());
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        libraryViewModel.fetchLibraryData();
        
        recyclerView = view.findViewById(R.id.recycler_library);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        buttonToggleSelect = view.findViewById(R.id.button_toggle_select);
        buttonAddPlaylist = view.findViewById(R.id.button_add_playlist);
        layoutSelectionControls = view.findViewById(R.id.layout_selection_controls);
        checkboxSelectAll = view.findViewById(R.id.checkbox_select_all);
        View buttonDelete = view.findViewById(R.id.button_delete_selected);

        adapter = new BookAdapter(currentList, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_libraryFragment_to_bookDetailFragment, bundle);
            }

            @Override
            public void onPlayClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_libraryFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onReadClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_libraryFragment_to_ebookReaderFragment, bundle);
            }

            @Override
            public void onSelectionChanged(int count) {
                updateSelectAllState();
            }
        }, true);
        adapter.setSelectedBookIds(selectionsByTab.get(0));

        playlistAdapter = new PlaylistAdapter(new PlaylistAdapter.OnPlaylistClickListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("playlist", playlist);
                Navigation.findNavController(view).navigate(R.id.action_libraryFragment_to_playlistDetailFragment, bundle);
            }

            @Override
            public void onMoreClick(Playlist playlist, View anchor) {
                showPlaylistMenu(playlist, anchor);
            }
        });
        
        recyclerView.setAdapter(adapter);

        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (adapter != null) adapter.setUserAccountType(type);
        });

        buttonToggleSelect.setOnClickListener(v -> toggleSelectionMode());
        buttonAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        checkboxSelectAll.setOnClickListener(v -> toggleSelectAll());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());

        tabButtons[0] = view.findViewById(R.id.tab_saved);
        tabButtons[1] = view.findViewById(R.id.tab_in_progress);
        tabButtons[2] = view.findViewById(R.id.tab_downloaded);
        tabButtons[3] = view.findViewById(R.id.tab_completed);
        for (int i = 0; i < tabButtons.length; i++) {
            final int position = i;
            tabButtons[i].setOnClickListener(v -> selectTab(position));
        }

        // Observe all lists for real-time updates from Firebase
        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), books -> { if (currentTabPosition == 0) updateListForTab(0); });
        libraryViewModel.getInProgressBooks().observe(getViewLifecycleOwner(), books -> { if (currentTabPosition == 1) updateListForTab(1); });
        libraryViewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> { if (currentTabPosition == 2) playlistAdapter.updateList(playlists); });
        libraryViewModel.getCompletedBooks().observe(getViewLifecycleOwner(), books -> { if (currentTabPosition == 3) updateListForTab(3); });

        selectTab(0);
    }

    private void toggleSelectionMode() {
        isSelectionMode = !isSelectionMode;
        buttonToggleSelect.setText(isSelectionMode ? "Cancel" : "Select");
        layoutSelectionControls.setVisibility(isSelectionMode ? View.VISIBLE : View.GONE);
        adapter.setSelectionMode(isSelectionMode);
        if (!isSelectionMode) {
            for (Set<String> set : selectionsByTab.values()) set.clear();
            checkboxSelectAll.setChecked(false);
        } else {
            updateSelectAllState();
        }
    }

    private void toggleSelectAll() {
        boolean checked = checkboxSelectAll.isChecked();
        Set<String> currentSelectedIds = selectionsByTab.get(currentTabPosition);
        if (currentSelectedIds == null) return;
        
        for (Book book : currentList) {
            if (checked) currentSelectedIds.add(book.getId());
            else currentSelectedIds.remove(book.getId());
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSelectAllState() {
        if (currentList.isEmpty()) {
            checkboxSelectAll.setChecked(false);
            return;
        }
        Set<String> currentSelectedIds = selectionsByTab.get(currentTabPosition);
        if (currentSelectedIds == null) return;

        boolean allSelected = true;
        for (Book book : currentList) {
            if (!currentSelectedIds.contains(book.getId())) {
                allSelected = false;
                break;
            }
        }
        checkboxSelectAll.setChecked(allSelected);
    }

    private void showDeleteConfirmation() {
        int totalToDelete = 0;
        for (Set<String> set : selectionsByTab.values()) totalToDelete += set.size();

        if (totalToDelete == 0) {
            UiNotifier.warning(getContext(), "Select books to remove first");
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm_delete, null);
        TextView message = dialogView.findViewById(R.id.text_delete_message);
        message.setText("Remove " + totalToDelete + " selected item(s) from your library categories. Downloaded files are only deleted from this device when they are selected in Downloads.");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
        dialogView.findViewById(R.id.button_cancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.button_remove).setOnClickListener(v -> {
            deleteSelectedBooks();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void deleteSelectedBooks() {
        Map<Integer, List<String>> exportData = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : selectionsByTab.entrySet()) {
            exportData.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        libraryViewModel.deleteSpecificBooks(exportData);
        toggleSelectionMode(); // Exit selection mode
        UiNotifier.success(getContext(), "Library updated");
    }

    private void updateListForTab(int position) {
        if (position == 2) {
            recyclerView.setAdapter(playlistAdapter);
            buttonAddPlaylist.setVisibility(View.VISIBLE);
            buttonToggleSelect.setVisibility(View.GONE);
            playlistAdapter.updateList(libraryViewModel.getPlaylists().getValue());
            return;
        }

        recyclerView.setAdapter(adapter);
        buttonAddPlaylist.setVisibility(View.GONE);
        buttonToggleSelect.setVisibility(View.VISIBLE);

        List<Book> list;
        switch (position) {
            case 1: list = libraryViewModel.getInProgressBooks().getValue(); break;
            case 3: list = libraryViewModel.getCompletedBooks().getValue(); break;
            default: list = libraryViewModel.getSavedBooks().getValue(); break;
        }
        currentList = list != null ? list : new ArrayList<>();
        
        // Cáº­p nháº­t giá» hÃ ng cá»§a tab má»›i vÃ o adapter
        adapter.setSelectedBookIds(selectionsByTab.get(position));
        adapter.updateList(currentList);

        if (isSelectionMode) updateSelectAllState();
    }

    private void showCreatePlaylistDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        EditText input = dialogView.findViewById(R.id.input_dialog_text);
        TextView title = dialogView.findViewById(R.id.text_dialog_title);
        title.setText("Create New Playlist");
        input.setHint("Enter playlist name");

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) libraryViewModel.createPlaylist(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPlaylistMenu(Playlist playlist, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add("Rename");
        popup.getMenu().add("Delete");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Rename")) showRenamePlaylistDialog(playlist);
            else if (item.getTitle().equals("Delete")) libraryViewModel.deletePlaylist(playlist.getId());
            return true;
        });
        popup.show();
    }

    private void showRenamePlaylistDialog(Playlist playlist) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_text, null);
        EditText input = dialogView.findViewById(R.id.input_dialog_text);
        TextView title = dialogView.findViewById(R.id.text_dialog_title);
        title.setText("Rename Playlist");
        input.setText(playlist.getName());

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) libraryViewModel.updatePlaylistName(playlist.getId(), name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectTab(int position) {
        currentTabPosition = position;
        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].setSelected(i == position);
        }
        updateListForTab(position);
    }
}



