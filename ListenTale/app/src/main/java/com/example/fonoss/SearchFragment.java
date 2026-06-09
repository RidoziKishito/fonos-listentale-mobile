package com.example.fonoss;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchFragment extends Fragment {

    private List<Book> allBooks;
    private List<Book> filteredBooks;
    private BookAdapter adapter;
    private RecyclerView recyclerResults;
    private TextView recentLabel;
    private FirebaseFirestore db;
    private Set<String> selectedCategories = new HashSet<>();

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
                if (book.getGenre() == null || !book.getGenre().toLowerCase().contains(cat.toLowerCase())) {
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
        db.collection("books").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allBooks.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    book.setId(document.getId());
                    allBooks.add(book);
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
}
