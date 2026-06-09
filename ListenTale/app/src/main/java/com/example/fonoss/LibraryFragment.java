package com.example.fonoss;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
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

public class LibraryFragment extends Fragment {

    private LibraryViewModel libraryViewModel;
    private BookAdapter adapter;
    private List<Book> currentList = new ArrayList<>();
    private int currentTabPosition = 0;
    private final TextView[] tabButtons = new TextView[4];
    private final Map<Integer, Set<String>> selectionsByTab = new HashMap<>();
    private boolean isSelectionMode = false;
    private TextView buttonToggleSelect;
    private View layoutSelectionControls;
    private CheckBox checkboxSelectAll;

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
        libraryViewModel.fetchLibraryData();
        
        RecyclerView recyclerView = view.findViewById(R.id.recycler_library);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        buttonToggleSelect = view.findViewById(R.id.button_toggle_select);
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
        
        recyclerView.setAdapter(adapter);

        buttonToggleSelect.setOnClickListener(v -> toggleSelectionMode());
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
        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> { if (currentTabPosition == 2) updateListForTab(2); });
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
        List<Book> list;
        switch (position) {
            case 1: list = libraryViewModel.getInProgressBooks().getValue(); break;
            case 2: list = libraryViewModel.getDownloadedBooks().getValue(); break;
            case 3: list = libraryViewModel.getCompletedBooks().getValue(); break;
            default: list = libraryViewModel.getSavedBooks().getValue(); break;
        }
        currentList = list != null ? list : new ArrayList<>();
        
        // Cập nhật giỏ hàng của tab mới vào adapter
        adapter.setSelectedBookIds(selectionsByTab.get(position));
        adapter.updateList(currentList);

        if (isSelectionMode) updateSelectAllState();
    }

    private void selectTab(int position) {
        currentTabPosition = position;
        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].setSelected(i == position);
        }
        updateListForTab(position);
    }
}
