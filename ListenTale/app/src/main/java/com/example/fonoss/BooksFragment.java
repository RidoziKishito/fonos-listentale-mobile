package com.example.fonoss;

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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class BooksFragment extends Fragment {

    private List<Book> trendingBooks;
    private List<Book> recommendedBooks;
    private BookAdapter trendingAdapter;
    private BookAdapter recommendedAdapter;
    private FirebaseFirestore db;
    private ProgressBar loadingBar;

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

        view.findViewById(R.id.button_download_manager).setOnClickListener(v ->
                new DownloadBookDialog().show(getChildFragmentManager(), "DownloadBookDialog"));
        
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

    private void fetchBooks() {
        if (loadingBar != null) loadingBar.setVisibility(View.VISIBLE);
        
        db.collection("books").get().addOnCompleteListener(task -> {
            if (loadingBar != null) loadingBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                trendingBooks.clear();
                recommendedBooks.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    if (book == null) continue;
                    book.setId(document.getId());
                    
                    // Logic separation
                    String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
                    if (title.contains("sherlock") || title.contains("romeo") || title.contains("gatsby")) {
                        trendingBooks.add(book);
                    } else {
                        recommendedBooks.add(book);
                    }
                }
                trendingAdapter.notifyDataSetChanged();
                recommendedAdapter.notifyDataSetChanged();
            } else {
                UiNotifier.error(getContext(), "Could not load books");
            }
        });
    }
}
