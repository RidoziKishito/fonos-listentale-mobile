package com.example.fonoss.ui.home;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;

import android.os.Bundle;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public class SeeAllFragment extends Fragment {

    private List<Book> allBooks;
    private BookAdapter adapter;
    private FirebaseFirestore db;
    private ProgressBar loadingBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_see_all, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        loadingBar = view.findViewById(R.id.loading_bar_see_all);
        
        allBooks = new ArrayList<>();
        RecyclerView recyclerView = view.findViewById(R.id.recycler_see_all);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            private final int spacing = (int) (12 * getResources().getDisplayMetrics().density);

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View itemView,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int position = parent.getChildAdapterPosition(itemView);
                if (position == RecyclerView.NO_POSITION) return;
                int column = position % 2;
                outRect.left = column == 0 ? 0 : spacing / 2;
                outRect.right = column == 0 ? spacing / 2 : 0;
                outRect.bottom = spacing;
            }
        });
        
        adapter = new BookAdapter(allBooks, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_seeAllFragment_to_bookDetailFragment, bundle);
            }

            @Override
            public void onPlayClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_seeAllFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onReadClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_seeAllFragment_to_ebookReaderFragment, bundle);
            }
        }, false); // Use card layout
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_see_all_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        fetchAllBooks();
    }

    private void fetchAllBooks() {
        loadingBar.setVisibility(View.VISIBLE);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        db.collection("books").get().addOnCompleteListener(executor, task -> {
            if (task.isSuccessful()) {
                List<Book> tempBooks = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    if (book == null) continue;
                    book.setId(document.getId());
                    tempBooks.add(book);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadingBar.setVisibility(View.GONE);
                        allBooks.clear();
                        allBooks.addAll(tempBooks);
                        adapter.notifyDataSetChanged();
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        loadingBar.setVisibility(View.GONE);
                        UiNotifier.error(getContext(), "Could not load books");
                    });
                }
            }
        });
    }
}
