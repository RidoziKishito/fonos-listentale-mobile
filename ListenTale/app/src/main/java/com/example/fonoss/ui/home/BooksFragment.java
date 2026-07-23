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

        View btnUpgradeHeader = view.findViewById(R.id.button_upgrade_header);
        if (btnUpgradeHeader != null) {
            btnUpgradeHeader.setOnClickListener(v -> showUpgradeDialog());
        }

        view.findViewById(R.id.button_download_manager).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_booksFragment_to_downloadedBooksFragment));
        
        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (trendingAdapter != null) trendingAdapter.setUserAccountType(type);
            if (recommendedAdapter != null) recommendedAdapter.setUserAccountType(type);
            if (btnUpgradeHeader != null) {
                if ("PREMIUM".equals(type)) {
                    btnUpgradeHeader.setVisibility(View.GONE);
                } else {
                    btnUpgradeHeader.setVisibility(View.VISIBLE);
                }
            }
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

    private List<Book> userInProgressBooks = new ArrayList<>();
    private Map<String, Long> userProgressPositions = new HashMap<>();

    private void fetchBooks() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    userFavoriteGenres = new ArrayList<>();
                    savedGenreWeights = new HashMap<>();
                    userInProgressBooks = new ArrayList<>();
                    userProgressPositions = new HashMap<>();
                    if (documentSnapshot.exists()) {
                        Object genresObj = documentSnapshot.get("favoriteGenres");
                        if (genresObj instanceof List) {
                            userFavoriteGenres = (List<String>) genresObj;
                        }
                        savedGenreWeights = buildSavedGenreWeights(documentSnapshot.get("saved"));
                        
                        Object posObj = documentSnapshot.get("progressPositions");
                        if (posObj instanceof Map) {
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) posObj).entrySet()) {
                                if (entry.getKey() instanceof String && entry.getValue() instanceof Number) {
                                    userProgressPositions.put((String) entry.getKey(), ((Number) entry.getValue()).longValue());
                                }
                            }
                        }
                        userInProgressBooks = parseBookList(documentSnapshot.get("inProgress"));
                    }
                    fetchBooksWithGenres();
                }).addOnFailureListener(e -> {
                    savedGenreWeights = new HashMap<>();
                    userInProgressBooks = new ArrayList<>();
                    userProgressPositions = new HashMap<>();
                    fetchBooksWithGenres();
                });
        } else {
            savedGenreWeights = new HashMap<>();
            userInProgressBooks = new ArrayList<>();
            userProgressPositions = new HashMap<>();
            fetchBooksWithGenres();
        }
    }

    private void fetchBooksWithGenres() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        db.collection("books").get().addOnCompleteListener(executor, task -> {
            if (task.isSuccessful()) {
                List<Book> allBooks = new ArrayList<>();
                Map<String, Book> allBooksMap = new HashMap<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = null;
                    try {
                        book = document.toObject(Book.class);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (book == null) continue;
                    book.setId(document.getId());
                    
                    // Compute individual progress if position recorded
                    if (userProgressPositions != null && userProgressPositions.containsKey(book.getId())) {
                        long posSec = userProgressPositions.get(book.getId());
                        int totalSec = parseDurationToSeconds(book.getDuration());
                        if (totalSec > 0) {
                            int pct = (int) Math.min(100, Math.max(1, (posSec * 100) / totalSec));
                            book.setProgressPercent(pct);
                        } else if (posSec > 0) {
                            book.setProgressPercent((int) Math.min(100, posSec / 60));
                        }
                    }
                    allBooks.add(book);
                    allBooksMap.put(book.getId(), book);
                }

                List<Book> tempTrending = new ArrayList<>();
                if (userInProgressBooks != null && !userInProgressBooks.isEmpty()) {
                    for (Book inProg : userInProgressBooks) {
                        Book fullBook = allBooksMap.get(inProg.getId());
                        if (fullBook != null) {
                            if (inProg.getProgressPercent() > 0) fullBook.setProgressPercent(inProg.getProgressPercent());
                            tempTrending.add(fullBook);
                        } else {
                            tempTrending.add(inProg);
                        }
                    }
                } else {
                    // Fallback to top database books only if user has no listening history yet
                    int count = Math.min(5, allBooks.size());
                    for (int i = 0; i < count; i++) {
                        Book b = allBooks.get(i);
                        if (b.getProgressPercent() == 0) {
                            b.setProgressPercent(35 + (i * 15) % 50);
                        }
                        tempTrending.add(b);
                    }
                }

                List<Book> tempRecommended = new ArrayList<>(allBooks);

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

    private List<Book> parseBookList(Object listObj) {
        List<Book> books = new ArrayList<>();
        if (!(listObj instanceof List)) return books;
        for (Object item : (List<?>) listObj) {
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                Book b = new Book();
                if (map.get("id") instanceof String) b.setId((String) map.get("id"));
                if (map.get("title") instanceof String) b.setTitle((String) map.get("title"));
                if (map.get("author") instanceof String) b.setAuthor((String) map.get("author"));
                if (map.get("coverUrl") instanceof String) b.setCoverUrl((String) map.get("coverUrl"));
                if (map.get("genre") instanceof String) b.setGenre((String) map.get("genre"));
                if (map.get("duration") instanceof String) b.setDuration((String) map.get("duration"));
                if (map.get("pages") instanceof String) b.setPages((String) map.get("pages"));
                if (map.get("rating") instanceof Number) b.setRating(((Number) map.get("rating")).doubleValue());
                
                if (map.get("audio_link") instanceof String) b.setAudio_link((String) map.get("audio_link"));
                else if (map.get("audio_url") instanceof String) b.setAudio_link((String) map.get("audio_url"));
                else if (map.get("audioUrl") instanceof String) b.setAudio_link((String) map.get("audioUrl"));

                int percent = 0;
                if (map.get("progressPercent") instanceof Number) {
                    percent = ((Number) map.get("progressPercent")).intValue();
                } else if (map.get("progress") instanceof Number) {
                    percent = ((Number) map.get("progress")).intValue();
                } else if (userProgressPositions != null && b.getId() != null && userProgressPositions.containsKey(b.getId())) {
                    long posSec = userProgressPositions.get(b.getId());
                    int totalSec = parseDurationToSeconds(b.getDuration());
                    if (totalSec > 0) percent = (int) Math.min(100, Math.max(1, (posSec * 100) / totalSec));
                }
                b.setProgressPercent(percent);
                books.add(b);
            }
        }
        return books;
    }

    private int parseDurationToSeconds(String durationStr) {
        if (durationStr == null || durationStr.isEmpty()) return 0;
        try {
            int totalSec = 0;
            String s = durationStr.toLowerCase().trim();
            if (s.contains("h")) {
                String[] parts = s.split("h");
                totalSec += Integer.parseInt(parts[0].trim()) * 3600;
                if (parts.length > 1 && parts[1].contains("m")) {
                    String minPart = parts[1].replace("m", "").trim();
                    if (!minPart.isEmpty()) totalSec += Integer.parseInt(minPart) * 60;
                }
            } else if (s.contains("m")) {
                String minPart = s.replace("m", "").trim();
                if (!minPart.isEmpty()) totalSec += Integer.parseInt(minPart) * 60;
            } else {
                totalSec = Integer.parseInt(s);
            }
            return totalSec;
        } catch (Exception e) {
            return 0;
        }
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

    private com.google.firebase.firestore.ListenerRegistration paymentListener;

    private void showUpgradeDialog() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        androidx.appcompat.app.AlertDialog loadingDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage("Generating payment request...")
                .setCancelable(false)
                .show();

        userViewModel.startUpgradeRequest(new com.example.fonoss.data.repository.UserRepository.UpgradeRequestCallback() {
            @Override
            public void onSuccess(org.json.JSONObject result) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    try {
                        String qrUrl = result.getString("qrUrl");
                        String paymentCode = result.getString("paymentCode");
                        String bank = result.getString("bank");
                        String accountNo = result.getString("accountNumber");
                        String accountName = result.getString("accountName");
                        int amount = result.getInt("amount");

                        displayRealUpgradeDialog(qrUrl, paymentCode, bank, accountNo, accountName, amount);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(getContext(), "Error parsing response", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    loadingDialog.dismiss();
                    android.widget.Toast.makeText(getContext(), "Failed to create request: " + message, android.widget.Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void displayRealUpgradeDialog(String qrUrl, String paymentCode, String bank, String accountNo, String accountName, int amount) {
        if (!isAdded()) return;
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(getLayoutInflater().inflate(R.layout.dialog_upgrade_account, null))
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        dialog.show();

        android.widget.ImageView qrImage = dialog.findViewById(R.id.image_qr_code);
        android.widget.TextView memoText = dialog.findViewById(R.id.text_payment_memo);
        android.widget.TextView bankText = dialog.findViewById(R.id.text_bank_info);
        android.widget.TextView accountNoText = dialog.findViewById(R.id.text_account_number);
        android.widget.TextView accountNameText = dialog.findViewById(R.id.text_account_name);
        View btnPaid = dialog.findViewById(R.id.button_paid);
        View btnCancel = dialog.findViewById(R.id.button_cancel);

        if (memoText != null) memoText.setText("Memo: " + paymentCode);
        if (bankText != null) bankText.setText("Bank: " + bank);
        if (accountNoText != null) accountNoText.setText("Account: " + accountNo);
        if (accountNameText != null) accountNameText.setText("Holder: " + accountName);

        if (qrImage != null) com.bumptech.glide.Glide.with(this).load(qrUrl).into(qrImage);

        if (btnCancel != null) btnCancel.setOnClickListener(view -> {
            if (paymentListener != null) paymentListener.remove();
            dialog.dismiss();
        });

        if (btnPaid != null) btnPaid.setOnClickListener(view -> {
            btnPaid.setEnabled(false);
            android.widget.Toast.makeText(getContext(), "Verifying payment simulation...", android.widget.Toast.LENGTH_SHORT).show();
            
            userViewModel.verifyAndUpgrade(aVoid -> {
                if (paymentListener != null) paymentListener.remove();
                dialog.dismiss();
                if (isAdded()) {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Success (Simulation)")
                            .setMessage("Your account has been upgraded to Premium for testing. Enjoy!")
                            .setPositiveButton("Awesome", null)
                            .show();
                }
            }, e -> {
                btnPaid.setEnabled(true);
                if (isAdded()) {
                    android.widget.Toast.makeText(getContext(), "Simulation failed.", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        });

        paymentListener = userViewModel.listenToPaymentStatus(paymentCode, aVoid -> {
            if (paymentListener != null) paymentListener.remove();
            dialog.dismiss();
            if (isAdded()) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Upgrade Successful!")
                        .setMessage("Welcome to Premium! Your account has been upgraded successfully.")
                        .setPositiveButton("Explore Premium", null)
                        .show();
            }
        });

        dialog.setOnDismissListener(d -> {
            if (paymentListener != null) paymentListener.remove();
        });
    }
}
