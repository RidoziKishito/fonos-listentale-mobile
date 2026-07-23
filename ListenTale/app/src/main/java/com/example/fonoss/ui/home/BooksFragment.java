package com.example.fonoss.ui.home;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.ui.auth.UserViewModel;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.manager.DownloadBookDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@AndroidEntryPoint
public class BooksFragment extends Fragment {

    private List<Book> trendingBooks;
    private List<Book> recommendedBooks;
    private BookAdapter trendingAdapter;
    private BookAdapter recommendedAdapter;
    private FirebaseFirestore db;
    private ProgressBar loadingBar;
    private UserViewModel userViewModel;
    private List<Book> rawTrendingBooks = new ArrayList<>();
    private List<Book> rawRecommendedBooks = new ArrayList<>();
    private List<String> userFavoriteGenres = new ArrayList<>();
    private Map<String, Integer> savedGenreWeights = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_books, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = FirebaseFirestore.getInstance();
        loadingBar = view.findViewById(R.id.loading_bar);
        userViewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(UserViewModel.class);

        com.google.android.material.chip.ChipGroup sortChipGroup = view.findViewById(R.id.chip_group_sort);
        sortChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applyCurrentSort();
        });

        view.findViewById(R.id.button_download_manager).setOnClickListener(v ->
                new DownloadBookDialog().show(getChildFragmentManager(), "DownloadBookDialog"));
        
        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (trendingAdapter != null) trendingAdapter.setUserAccountType(type);
            if (recommendedAdapter != null) recommendedAdapter.setUserAccountType(type);
        });

        // Setup Trending
        trendingBooks = new ArrayList<>();
        RecyclerView recyclerTrending = view.findViewById(R.id.recycler_trending);
        recyclerTrending.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new BookAdapter(trendingBooks, new BookAdapter.OnBookClickListener() {
            @Override public void onBookClick(Book book) { BooksFragment.this.onBookClick(book); }
            @Override public void onPlayClick(Book book) { BooksFragment.this.onPlayClick(book); }
            @Override public void onReadClick(Book book) { BooksFragment.this.onReadClick(book); }
        });
        recyclerTrending.setAdapter(trendingAdapter);

        // Setup Recommended
        recommendedBooks = new ArrayList<>();
        RecyclerView recyclerRecommended = view.findViewById(R.id.recycler_recommended);
        recyclerRecommended.setLayoutManager(new LinearLayoutManager(getContext()));
        recommendedAdapter = new BookAdapter(recommendedBooks, new BookAdapter.OnBookClickListener() {
            @Override public void onBookClick(Book book) { BooksFragment.this.onBookClick(book); }
            @Override public void onPlayClick(Book book) { BooksFragment.this.onPlayClick(book); }
            @Override public void onReadClick(Book book) { BooksFragment.this.onReadClick(book); }
        }, true); // Horizontal style
        recyclerRecommended.setAdapter(recommendedAdapter);

        fetchBooks();

        view.findViewById(R.id.text_see_all).setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_booksFragment_to_seeAllFragment)
        );
    }

    private void onBookClick(Book book) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", book);
        Navigation.findNavController(requireView()).navigate(R.id.action_booksFragment_to_bookDetailFragment, bundle);
    }

    private void onPlayClick(Book book) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", book);
        Navigation.findNavController(requireView()).navigate(R.id.action_booksFragment_to_audioPlayerFragment, bundle);
    }

    private void onReadClick(Book book) {
        Bundle bundle = new Bundle();
        bundle.putSerializable("book", book);
        Navigation.findNavController(requireView()).navigate(R.id.action_booksFragment_to_ebookReaderFragment, bundle);
    }

    private void applyCurrentSort() {
        if (getView() == null) return;
        com.google.android.material.chip.ChipGroup sortChipGroup = getView().findViewById(R.id.chip_group_sort);
        int checkedId = sortChipGroup.getCheckedChipId();

        Comparator<Book> comparator;
        if (checkedId == R.id.chip_sort_views) {
            comparator = (b1, b2) -> Long.compare(b2.getViews(), b1.getViews());
        } else if (checkedId == R.id.chip_sort_date) {
            comparator = (b1, b2) -> Long.compare(b2.getPublishDate(), b1.getPublishDate());
        } else {
            // Relevance
            comparator = (b1, b2) -> {
                int score1 = calculateRelevanceScore(b1);
                int score2 = calculateRelevanceScore(b2);
                if (score1 != score2) return Integer.compare(score2, score1);
                return Double.compare(b2.getRating(), b1.getRating());
            };
        }

        recommendedBooks.clear();
        recommendedBooks.addAll(rawRecommendedBooks);
        Collections.sort(recommendedBooks, comparator);
        recommendedAdapter.notifyDataSetChanged();

        // Trending section stays as rawTrendingBooks (not sorted by chips)
        trendingBooks.clear();
        trendingBooks.addAll(rawTrendingBooks);
        trendingAdapter.notifyDataSetChanged();
    }

    private int calculateRelevanceScore(Book book) {
        int score = 0;
        if (book.getGenres() != null) {
            for (String genre : book.getGenres()) {
                score += getSavedGenreWeight(genre) * 10;
            }
        }
        score += getSavedGenreWeight(book.getGenre()) * 10;

        if (userFavoriteGenres != null && !userFavoriteGenres.isEmpty()) {
            if (book.getGenres() != null) {
                for (String g : book.getGenres()) {
                    if (containsGenre(userFavoriteGenres, g)) score++;
                }
            }
            if (containsGenre(userFavoriteGenres, book.getGenre())) {
                score++;
            }
        }
        return score;
    }

    private void fetchBooks() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    userFavoriteGenres = new ArrayList<>();
                    savedGenreWeights = new HashMap<>();
                    if (documentSnapshot.exists()) {
                        Object genresObj = documentSnapshot.get("favoriteGenres");
                        if (genresObj instanceof List) {
                            userFavoriteGenres = (List<String>) genresObj;
                        }
                        savedGenreWeights = buildSavedGenreWeights(documentSnapshot.get("saved"));
                    }
                    fetchBooksWithGenres();
                }).addOnFailureListener(e -> {
                    savedGenreWeights = new HashMap<>();
                    fetchBooksWithGenres();
                });
        } else {
            savedGenreWeights = new HashMap<>();
            fetchBooksWithGenres();
        }
    }

    private void fetchBooksWithGenres() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        db.collection("books").get().addOnCompleteListener(executor, task -> {
            if (task.isSuccessful()) {
                List<Book> tempTrending = new ArrayList<>();
                List<Book> tempRecommended = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = null;
                    try {
                        book = document.toObject(Book.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (book == null) continue;
                    book.setId(document.getId());
                    
                    String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
                    if (title.contains("sherlock") || title.contains("romeo") || title.contains("gatsby")) {
                        tempTrending.add(book);
                    } else {
                        tempRecommended.add(book);
                    }
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                        rawTrendingBooks.clear();
                        rawTrendingBooks.addAll(tempTrending);
                        rawRecommendedBooks.clear();
                        rawRecommendedBooks.addAll(tempRecommended);
                        applyCurrentSort();
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (loadingBar != null) loadingBar.setVisibility(View.GONE);
                        UiNotifier.error(getContext(), "Could not load books");
                    });
                }
            }
        });
    }

    private Map<String, Integer> buildSavedGenreWeights(Object savedObj) {
        Map<String, Integer> weights = new HashMap<>();
        if (!(savedObj instanceof List)) return weights;

        for (Object item : (List<?>) savedObj) {
            if (!(item instanceof Map)) continue;

            Map<?, ?> bookMap = (Map<?, ?>) item;
            addGenreWeight(weights, bookMap.get("genre"));
            addGenreWeight(weights, bookMap.get("gender"));

            Object genresObj = bookMap.get("genres");
            if (genresObj instanceof List) {
                for (Object genre : (List<?>) genresObj) {
                    addGenreWeight(weights, genre);
                }
            }
        }
        return weights;
    }

    private void addGenreWeight(Map<String, Integer> weights, Object genreObj) {
        if (!(genreObj instanceof String)) return;

        String genre = normalizeGenre((String) genreObj);
        if (genre.isEmpty()) return;

        Integer current = weights.get(genre);
        weights.put(genre, current == null ? 1 : current + 1);
    }

    private int getSavedGenreWeight(String genre) {
        if (genre == null || savedGenreWeights == null) return 0;

        Integer weight = savedGenreWeights.get(normalizeGenre(genre));
        return weight == null ? 0 : weight;
    }

    private boolean containsGenre(List<String> genres, String targetGenre) {
        if (genres == null || targetGenre == null) return false;

        String normalizedTarget = normalizeGenre(targetGenre);
        for (String genre : genres) {
            if (normalizedTarget.equals(normalizeGenre(genre))) return true;
        }
        return false;
    }

    private String normalizeGenre(String genre) {
        return genre == null ? "" : genre.trim().toLowerCase(Locale.US);
    }
}
