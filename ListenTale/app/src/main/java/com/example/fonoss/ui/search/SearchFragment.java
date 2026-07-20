package com.example.fonoss.ui.search;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;

@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private List<Book> allBooks;
    private List<Book> filteredBooks;
    private BookAdapter adapter;
    private RecyclerView recyclerResults;
    private TextView recentLabel;
    private FirebaseFirestore db;
    private Set<String> selectedCategories = new HashSet<>();
    private List<String> allAvailableGenres = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        
        TextInputEditText inputSearch = view.findViewById(R.id.input_search);
        recyclerResults = view.findViewById(R.id.recycler_search_results);
        recentLabel = view.findViewById(R.id.text_recent_searches_label);

        allBooks = new ArrayList<>();
        filteredBooks = new ArrayList<>();
        adapter = new BookAdapter(filteredBooks, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                hideKeyboard(inputSearch);
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_searchFragment_to_bookDetailFragment, bundle);
            }

            @Override
            public void onPlayClick(Book book) {
                hideKeyboard(inputSearch);
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_searchFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onReadClick(Book book) {
                hideKeyboard(inputSearch);
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_searchFragment_to_ebookReaderFragment, bundle);
            }
        });

        recyclerResults.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        recyclerResults.setAdapter(adapter);

        fetchAllBooks();

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        setupCategoryChips(view, inputSearch);
        
        ImageButton btnFilter = view.findViewById(R.id.btn_filter_genres);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterDialog(inputSearch));
        }
    }

    private void setupCategoryChips(View view, TextInputEditText inputSearch) {
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_categories);
        int[] chipIds = {R.id.chip_romance, R.id.chip_self_help, R.id.chip_philosophy, R.id.chip_classic, R.id.chip_mystery, R.id.chip_children};

        for (int id : chipIds) {
            Chip chip = view.findViewById(id);
            if (chip != null) {
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String category = chip.getText().toString();
                    if (isChecked) selectedCategories.add(category);
                    else selectedCategories.remove(category);
                    applyFilters(inputSearch.getText().toString());
                });
            }
        }
    }

    private void applyFilters(String searchText) {
        filteredBooks.clear();
        
        for (Book book : allBooks) {
            boolean matchesSearch = searchText.isEmpty() || 
                    book.getTitle().toLowerCase().contains(searchText.toLowerCase()) ||
                    book.getAuthor().toLowerCase().contains(searchText.toLowerCase());
            
            boolean matchesCategories = true;
            for (String cat : selectedCategories) {
                boolean hasThisGenre = false;
                if (book.getGenres() != null) {
                    for (String g : book.getGenres()) {
                        if (g.toLowerCase().contains(cat.toLowerCase())) {
                            hasThisGenre = true;
                            break;
                        }
                    }
                }
                
                // Fallback to legacy genre string if array is empty
                if (!hasThisGenre && book.getGenre() != null && book.getGenre().toLowerCase().contains(cat.toLowerCase())) {
                    hasThisGenre = true;
                }
                
                if (!hasThisGenre) {
                    matchesCategories = false;
                    break;
                }
            }

            if (matchesSearch && matchesCategories && (!searchText.isEmpty() || !selectedCategories.isEmpty())) {
                filteredBooks.add(book);
            }
        }
        showResults(searchText.isEmpty() && selectedCategories.isEmpty());
    }

    private void fetchAllBooks() {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        db.collection("books").get().addOnCompleteListener(executor, task -> {
            if (task.isSuccessful()) {
                List<Book> tempBooks = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = null;
                    try {
                        book = document.toObject(Book.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (book == null) continue;
                    book.setId(document.getId());
                    tempBooks.add(book);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        allBooks.clear();
                        allBooks.addAll(tempBooks);
                        
                        Set<String> uniqueGenres = new HashSet<>();
                        for (Book book : allBooks) {
                            if (book.getGenres() != null) {
                                uniqueGenres.addAll(book.getGenres());
                            } else if (book.getGenre() != null) {
                                uniqueGenres.add(book.getGenre());
                            }
                        }
                        allAvailableGenres.clear();
                        allAvailableGenres.addAll(uniqueGenres);
                        Collections.sort(allAvailableGenres);
                        
                        // Re-apply filters
                        View view = getView();
                        if (view != null) {
                            TextInputEditText inputSearch = view.findViewById(R.id.input_search);
                            if (inputSearch != null) {
                                applyFilters(inputSearch.getText().toString());
                            }
                        }
                    });
                }
            }
        });
    }

    private void showResults(boolean isEmpty) {
        if (filteredBooks.isEmpty() && !isEmpty) {
            // Searched/Filtered but no results
            recyclerResults.setVisibility(View.GONE);
            recentLabel.setVisibility(View.VISIBLE);
        } else if (isEmpty) {
            // Nothing searched and no categories selected
            recyclerResults.setVisibility(View.GONE);
            recentLabel.setVisibility(View.VISIBLE);
        } else {
            recyclerResults.setVisibility(View.VISIBLE);
            recentLabel.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }
    
    private void showFilterDialog(TextInputEditText inputSearch) {
        if (allAvailableGenres.isEmpty()) return;
        
        String[] genresArray = allAvailableGenres.toArray(new String[0]);
        boolean[] checkedItems = new boolean[genresArray.length];
        
        for (int i = 0; i < genresArray.length; i++) {
            if (selectedCategories.contains(genresArray[i])) {
                checkedItems[i] = true;
            }
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Filter by Genres")
                .setMultiChoiceItems(genresArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        selectedCategories.add(genresArray[which]);
                    } else {
                        selectedCategories.remove(genresArray[which]);
                    }
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    // Also sync the chips visually if they exist
                    ChipGroup chipGroup = getView() != null ? getView().findViewById(R.id.chip_group_categories) : null;
                    if (chipGroup != null) {
                        for (int i = 0; i < chipGroup.getChildCount(); i++) {
                            View child = chipGroup.getChildAt(i);
                            if (child instanceof Chip) {
                                Chip chip = (Chip) child;
                                // Need to temporarily disable listener to prevent double triggering
                                chip.setOnCheckedChangeListener(null);
                                chip.setChecked(selectedCategories.contains(chip.getText().toString()));
                                // Re-attach listener
                                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    String category = chip.getText().toString();
                                    if (isChecked) selectedCategories.add(category);
                                    else selectedCategories.remove(category);
                                    applyFilters(inputSearch.getText().toString());
                                });
                            }
                        }
                    }
                    applyFilters(inputSearch.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
