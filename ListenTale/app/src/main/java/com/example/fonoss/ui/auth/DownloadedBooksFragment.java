package com.example.fonoss.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fonoss.R;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.ui.library.LibraryViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DownloadedBooksFragment extends Fragment {

    private LibraryViewModel libraryViewModel;
    private BookAdapter adapter;
    private List<Book> bookList = new ArrayList<>();
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloaded_books, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        textEmpty = view.findViewById(R.id.text_empty);

        view.findViewById(R.id.button_back).setOnClickListener(v -> 
            Navigation.findNavController(v).navigateUp()
        );

        RecyclerView recyclerView = view.findViewById(R.id.recycler_downloaded_books);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new BookAdapter(bookList, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_downloadedBooksFragment_to_bookDetailFragment, bundle);
            }

            @Override
            public void onPlayClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_downloadedBooksFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onReadClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_downloadedBooksFragment_to_ebookReaderFragment, bundle);
            }
        }, true);
        
        recyclerView.setAdapter(adapter);

        libraryViewModel.getDownloadedBooks().observe(getViewLifecycleOwner(), books -> {
            bookList.clear();
            if (books != null) {
                bookList.addAll(books);
            }
            adapter.notifyDataSetChanged();
            textEmpty.setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }
}
