package com.example.fonoss;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

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
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new BookAdapter(allBooks, book -> {
            Bundle bundle = new Bundle();
            bundle.putSerializable("book", book);
            Navigation.findNavController(view).navigate(R.id.action_seeAllFragment_to_bookDetailFragment, bundle);
        }, true); // Use horizontal layout
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.button_see_all_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        fetchAllBooks();
    }

    private void fetchAllBooks() {
        loadingBar.setVisibility(View.VISIBLE);
        db.collection("books").get().addOnCompleteListener(task -> {
            loadingBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                allBooks.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Book book = document.toObject(Book.class);
                    book.setId(document.getId());
                    allBooks.add(book);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
