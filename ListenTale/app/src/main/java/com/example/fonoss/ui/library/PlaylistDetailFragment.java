package com.example.fonoss.ui.library;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.auth.UserViewModel;
import com.example.fonoss.utils.UiNotifier;
import com.example.fonoss.adapter.BookAdapter;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.model.Playlist;

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
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public class PlaylistDetailFragment extends Fragment {

    private Playlist playlist;
    private LibraryViewModel libraryViewModel;
    private UserViewModel userViewModel;
    private BookAdapter adapter;
    private List<Book> bookList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            playlist = (Playlist) getArguments().getSerializable("playlist");
        }

        if (playlist == null) {
            Navigation.findNavController(view).navigateUp();
            return;
        }

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        
        TextView textName = view.findViewById(R.id.text_playlist_name);
        textName.setText(playlist.getName());

        view.findViewById(R.id.button_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        view.findViewById(R.id.button_add_books).setOnClickListener(v -> showBookPickerDialog());

        RecyclerView recyclerView = view.findViewById(R.id.recycler_playlist_books);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new BookAdapter(bookList, new BookAdapter.OnBookClickListener() {
            @Override
            public void onBookClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                if (playlist != null) {
                    bundle.putSerializable("playlist", playlist);
                }
                Navigation.findNavController(view).navigate(R.id.action_playlistDetailFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onPlayClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                if (playlist != null) {
                    bundle.putSerializable("playlist", playlist);
                }
                Navigation.findNavController(view).navigate(R.id.action_playlistDetailFragment_to_audioPlayerFragment, bundle);
            }

            @Override
            public void onReadClick(Book book) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", book);
                Navigation.findNavController(view).navigate(R.id.action_playlistDetailFragment_to_ebookReaderFragment, bundle);
            }
        }, true);
        
        recyclerView.setAdapter(adapter);

        userViewModel.getAccountType().observe(getViewLifecycleOwner(), type -> {
            if (adapter != null) adapter.setUserAccountType(type);
        });

        libraryViewModel.getPlaylists().observe(getViewLifecycleOwner(), playlists -> {
            if (playlists != null) {
                for (Playlist p : playlists) {
                    if (p.getId().equals(playlist.getId())) {
                        this.playlist = p;
                        updateUI(view);
                        break;
                    }
                }
            }
        });

        updateUI(view);
    }

    private void showBookPickerDialog() {
        BookPickerDialog dialog = new BookPickerDialog();
        dialog.setListener(selectedBooks -> {
            if (!selectedBooks.isEmpty()) {
                libraryViewModel.addBooksToPlaylist(playlist.getId(), selectedBooks);
                UiNotifier.success(getContext(), "Adding books to playlist...");
            }
        });
        dialog.show(getChildFragmentManager(), "BookPicker");
    }

    private void updateUI(View view) {
        bookList = playlist.getBooks() != null ? playlist.getBooks() : new ArrayList<>();
        adapter.updateList(bookList);
        view.findViewById(R.id.text_empty_playlist).setVisibility(bookList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
