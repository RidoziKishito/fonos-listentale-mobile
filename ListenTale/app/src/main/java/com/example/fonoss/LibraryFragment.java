package com.example.fonoss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;
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
    private TabLayout tabLayout;
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

        tabLayout = view.findViewById(R.id.tab_layout_library);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateListForTab(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Observe all lists for real-time updates from Firebase
        libraryViewModel.getSavedBooks().observe(getViewLifecycleOwner(), books -> { if (tabLayout.getSelectedTabPosition() == 0) adapter.updateList(books); });
        libraryViewModel.getInProgressBooks().observe(getViewLifecycleOwner(), books -> { if (tabLayout.getSelectedTabPosition() == 1) adapter.updateList(books); });
        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> { if (tabLayout.getSelectedTabPosition() == 2) adapter.updateList(books); });
        libraryViewModel.getCompletedBooks().observe(getViewLifecycleOwner(), books -> { if (tabLayout.getSelectedTabPosition() == 3) adapter.updateList(books); });

        updateListForTab(0);
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
        Set<String> currentSelectedIds = selectionsByTab.get(tabLayout.getSelectedTabPosition());
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
        Set<String> currentSelectedIds = selectionsByTab.get(tabLayout.getSelectedTabPosition());
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
            Toast.makeText(getContext(), "Please select books to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Books")
                .setMessage("Are you sure you want to remove " + totalToDelete + " items from selected categories? Only downloaded content from 'Downloaded' tab will be removed from device.")
                .setPositiveButton("Yes", (dialog, which) -> deleteSelectedBooks())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteSelectedBooks() {
        Map<Integer, List<String>> exportData = new HashMap<>();
        for (Map.Entry<Integer, Set<String>> entry : selectionsByTab.entrySet()) {
            exportData.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }

        libraryViewModel.deleteSpecificBooks(exportData);
        toggleSelectionMode(); // Exit selection mode
        Toast.makeText(getContext(), "Deletion request sent", Toast.LENGTH_SHORT).show();
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
}
