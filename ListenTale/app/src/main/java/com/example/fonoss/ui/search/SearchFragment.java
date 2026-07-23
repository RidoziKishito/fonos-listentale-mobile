package com.example.fonoss.ui.search;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.auth.UserViewModel;
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
import android.widget.CheckBox;
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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;

@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private List<Book> allBooks;
    private List<Book> filteredBooks;
    private BookAdapter adapter;
    private RecyclerView recyclerResults;
    private TextView recentLabel;
    private UserViewModel userViewModel;
    private FirebaseFirestore db;
    private Set<String> selectedCategories = new HashSet<>();
    private List<String> allAvailableGenres = new ArrayList<>();
    private int selectedDurationFilter = DURATION_ANY;
    private double selectedMinRating = 0.0;
    private static final int DURATION_ANY = 1000;
    private static final int DURATION_UNDER_1_HOUR = 1001;
    private static final int DURATION_1_TO_3_HOURS = 1002;
    private static final int DURATION_3_TO_6_HOURS = 1003;
    private static final int DURATION_OVER_6_HOURS = 1004;
    private static final int RATING_ANY = 2000;
    private static final int RATING_3_PLUS = 2001;
    private static final int RATING_4_PLUS = 2002;
    private static final int RATING_4_5_PLUS = 2003;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = FirebaseFirestore.getInstance();
        userViewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
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

        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (adapter != null) adapter.setUserAccountType(type);
        });

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

            boolean matchesDuration = matchesDurationFilter(book);
            boolean matchesRating = book.getRating() >= selectedMinRating;
            boolean hasActiveFilters = !searchText.isEmpty()
                    || !selectedCategories.isEmpty()
                    || selectedDurationFilter != DURATION_ANY
                    || selectedMinRating > 0.0;

            if (matchesSearch && matchesCategories && matchesDuration && matchesRating && hasActiveFilters) {
                filteredBooks.add(book);
            }
        }
        showResults(!hasAnyActiveFilter(searchText));
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
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding / 2, padding, 0);

        TextView genreTitle = createFilterTitle("Genres");
        content.addView(genreTitle);

        List<CheckBox> genreChecks = new ArrayList<>();
        for (String genre : allAvailableGenres) {
            CheckBox checkBox = new CheckBox(requireContext());
            checkBox.setText(genre);
            checkBox.setChecked(selectedCategories.contains(genre));
            genreChecks.add(checkBox);
            content.addView(checkBox);
        }

        TextView durationTitle = createFilterTitle("Listening duration");
        content.addView(durationTitle);

        RadioGroup durationGroup = new RadioGroup(requireContext());
        durationGroup.setOrientation(RadioGroup.VERTICAL);
        addRadioButton(durationGroup, DURATION_ANY, "Any duration");
        addRadioButton(durationGroup, DURATION_UNDER_1_HOUR, "Under 1 hour");
        addRadioButton(durationGroup, DURATION_1_TO_3_HOURS, "1 - 3 hours");
        addRadioButton(durationGroup, DURATION_3_TO_6_HOURS, "3 - 6 hours");
        addRadioButton(durationGroup, DURATION_OVER_6_HOURS, "Over 6 hours");
        durationGroup.check(selectedDurationFilter);
        content.addView(durationGroup);

        TextView ratingTitle = createFilterTitle("Minimum rating");
        content.addView(ratingTitle);

        RadioGroup ratingGroup = new RadioGroup(requireContext());
        ratingGroup.setOrientation(RadioGroup.VERTICAL);
        addRadioButton(ratingGroup, RATING_ANY, "Any rating");
        addRadioButton(ratingGroup, RATING_3_PLUS, "3.0+");
        addRadioButton(ratingGroup, RATING_4_PLUS, "4.0+");
        addRadioButton(ratingGroup, RATING_4_5_PLUS, "4.5+");
        ratingGroup.check(getRatingFilterId(selectedMinRating));
        content.addView(ratingGroup);

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(content);

        new AlertDialog.Builder(requireContext())
                .setTitle("Search filters")
                .setView(scrollView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    selectedCategories.clear();
                    for (CheckBox checkBox : genreChecks) {
                        if (checkBox.isChecked()) {
                            selectedCategories.add(checkBox.getText().toString());
                        }
                    }
                    selectedDurationFilter = durationGroup.getCheckedRadioButtonId();
                    selectedMinRating = getRatingValue(ratingGroup.getCheckedRadioButtonId());

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
                .setNeutralButton("Clear", (dialog, which) -> {
                    selectedCategories.clear();
                    selectedDurationFilter = DURATION_ANY;
                    selectedMinRating = 0.0;
                    syncCategoryChips(inputSearch);
                    applyFilters(inputSearch.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private TextView createFilterTitle(String text) {
        TextView title = new TextView(requireContext());
        title.setText(text);
        title.setTextColor(getResources().getColor(R.color.slate_900, requireContext().getTheme()));
        title.setTextSize(16);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 16, 0, 4);
        return title;
    }

    private void addRadioButton(RadioGroup group, int id, String text) {
        RadioButton radioButton = new RadioButton(requireContext());
        radioButton.setId(id);
        radioButton.setText(text);
        group.addView(radioButton);
    }

    private int getRatingFilterId(double minRating) {
        if (minRating >= 4.5) return RATING_4_5_PLUS;
        if (minRating >= 4.0) return RATING_4_PLUS;
        if (minRating >= 3.0) return RATING_3_PLUS;
        return RATING_ANY;
    }

    private double getRatingValue(int ratingFilterId) {
        switch (ratingFilterId) {
            case RATING_3_PLUS:
                return 3.0;
            case RATING_4_PLUS:
                return 4.0;
            case RATING_4_5_PLUS:
                return 4.5;
            default:
                return 0.0;
        }
    }

    private boolean hasAnyActiveFilter(String searchText) {
        return !searchText.isEmpty()
                || !selectedCategories.isEmpty()
                || selectedDurationFilter != DURATION_ANY
                || selectedMinRating > 0.0;
    }

    private boolean matchesDurationFilter(Book book) {
        if (selectedDurationFilter == DURATION_ANY) return true;

        double durationHours = parseDurationHours(book.getDuration());
        if (durationHours < 0) return false;

        switch (selectedDurationFilter) {
            case DURATION_UNDER_1_HOUR:
                return durationHours < 1.0;
            case DURATION_1_TO_3_HOURS:
                return durationHours >= 1.0 && durationHours <= 3.0;
            case DURATION_3_TO_6_HOURS:
                return durationHours > 3.0 && durationHours <= 6.0;
            case DURATION_OVER_6_HOURS:
                return durationHours > 6.0;
            default:
                return true;
        }
    }

    private double parseDurationHours(String duration) {
        if (duration == null) return -1;

        String value = duration.trim().toLowerCase();
        if (value.isEmpty()) return -1;

        double hours = 0;
        double minutes = 0;
        java.util.regex.Matcher hourMatcher = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*(h|hr|hrs|hour|hours)")
                .matcher(value);
        if (hourMatcher.find()) {
            hours = Double.parseDouble(hourMatcher.group(1));
        }

        java.util.regex.Matcher minuteMatcher = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*(m|min|mins|minute|minutes)")
                .matcher(value);
        if (minuteMatcher.find()) {
            minutes = Double.parseDouble(minuteMatcher.group(1));
        }

        if (hours == 0 && minutes == 0) {
            java.util.regex.Matcher colonMatcher = java.util.regex.Pattern
                    .compile("^(\\d+):(\\d{1,2})$")
                    .matcher(value);
            if (colonMatcher.find()) {
                hours = Double.parseDouble(colonMatcher.group(1));
                minutes = Double.parseDouble(colonMatcher.group(2));
            }
        }

        if (hours == 0 && minutes == 0) return -1;
        return hours + (minutes / 60.0);
    }

    private void syncCategoryChips(TextInputEditText inputSearch) {
        ChipGroup chipGroup = getView() != null ? getView().findViewById(R.id.chip_group_categories) : null;
        if (chipGroup == null) return;

        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            View child = chipGroup.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                chip.setOnCheckedChangeListener(null);
                chip.setChecked(selectedCategories.contains(chip.getText().toString()));
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    String category = chip.getText().toString();
                    if (isChecked) selectedCategories.add(category);
                    else selectedCategories.remove(category);
                    applyFilters(inputSearch.getText().toString());
                });
            }
        }
    }
}
