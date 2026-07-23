package com.example.fonoss.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.fonoss.R;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.model.ChatSession;
import com.example.fonoss.utils.UiNotifier;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatFragment extends Fragment {

    private ChatViewModel chatViewModel;
    private RecyclerView recyclerMessages;
    private ChatMessageAdapter messageAdapter;
    private EditText editTextInput;
    private ImageButton buttonSend;
    private TextView textSessionTitle, textActiveBookName;
    private ImageView imageActiveBookIcon;
    private View buttonChangeOrAddBook;
    private View chatBookContextBar;

    private Book initialBook;
    private String initialBookId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chatViewModel = new ViewModelProvider(requireActivity()).get(ChatViewModel.class);

        // Bind Views
        recyclerMessages = view.findViewById(R.id.recycler_chat_messages);
        editTextInput = view.findViewById(R.id.edit_text_chat_input);
        buttonSend = view.findViewById(R.id.button_send_message);
        textSessionTitle = view.findViewById(R.id.text_chat_session_title);
        textActiveBookName = view.findViewById(R.id.text_active_book_name);
        imageActiveBookIcon = view.findViewById(R.id.image_active_book_icon);
        buttonChangeOrAddBook = view.findViewById(R.id.button_change_or_add_book);
        chatBookContextBar = view.findViewById(R.id.chat_book_context_bar);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);
        messageAdapter = new ChatMessageAdapter();
        recyclerMessages.setAdapter(messageAdapter);

        // Check Navigation Arguments (from BookDetail / EbookReader / AudioPlayer)
        if (getArguments() != null) {
            initialBook = (Book) getArguments().getSerializable("book");
            if (initialBook != null) {
                initialBookId = initialBook.getId();
            } else {
                initialBookId = getArguments().getString("bookId");
            }
        }

        // Setup Observers
        observeViewModel();

        // Check initial state
        chatViewModel.loadSessions();
        if (initialBookId != null && !initialBookId.isEmpty()) {
            showSessionHistoryDialogWithBookContext(initialBook, initialBookId);
        } else {
            // Opened from central Footer tab
            if (chatViewModel.getCurrentSession().getValue() == null) {
                showSessionHistoryDialog();
            }
        }

        // Listeners
        view.findViewById(R.id.button_chat_back).setOnClickListener(v -> {
            if (!Navigation.findNavController(v).navigateUp()) {
                Navigation.findNavController(v).navigate(R.id.booksFragment);
            }
        });

        view.findViewById(R.id.button_chat_sessions_history).setOnClickListener(v -> showSessionHistoryDialog());

        buttonChangeOrAddBook.setOnClickListener(v -> showBookPickerDialog());

        buttonSend.setOnClickListener(v -> {
            String text = editTextInput.getText().toString();
            if (!text.trim().isEmpty()) {
                chatViewModel.sendMessage(text);
                editTextInput.setText("");
            }
        });
    }

    private void observeViewModel() {
        chatViewModel.getCurrentSession().observe(getViewLifecycleOwner(), session -> {
            if (session != null) {
                textSessionTitle.setText(session.getTitle());
            } else {
                textSessionTitle.setText("Chat with AI");
            }
            updateInputState();
        });

        chatViewModel.getActiveBook().observe(getViewLifecycleOwner(), book -> {
            if (book != null) {
                textActiveBookName.setText("Asking about: " + book.getTitle());
                Glide.with(this)
                        .load(book.getCoverUrl())
                        .circleCrop()
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(imageActiveBookIcon);
            } else {
                textActiveBookName.setText("You have to choose a book");
                imageActiveBookIcon.setImageResource(android.R.drawable.ic_menu_gallery);
            }
            updateInputState();
        });

        chatViewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            messageAdapter.setMessages(messages);
            if (messages != null && !messages.isEmpty()) {
                recyclerMessages.smoothScrollToPosition(messages.size() - 1);
            }
        });

        chatViewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                UiNotifier.info(getContext(), msg);
            }
        });

        chatViewModel.getIsGenerating().observe(getViewLifecycleOwner(), isGenerating -> {
            updateInputState();
        });
    }

    private void updateInputState() {
        Book activeBook = chatViewModel.getActiveBook().getValue();
        ChatSession session = chatViewModel.getCurrentSession().getValue();
        Boolean isGenerating = chatViewModel.getIsGenerating().getValue();
        if (isGenerating == null) isGenerating = false;

        boolean hasActiveBookAndSession = (session != null && activeBook != null);

        // Always keep chatBookContextBar VISIBLE so user can tap Change/Add Book + button anytime
        if (chatBookContextBar != null) {
            chatBookContextBar.setVisibility(View.VISIBLE);
        }

        if (isGenerating) {
            editTextInput.setEnabled(false);
            buttonSend.setEnabled(false);
            buttonSend.setImageResource(android.R.drawable.ic_media_pause);
            buttonSend.setAlpha(0.4f);
        } else if (hasActiveBookAndSession) {
            editTextInput.setEnabled(true);
            buttonSend.setEnabled(true);
            buttonSend.setImageResource(android.R.drawable.ic_menu_send);
            buttonSend.setAlpha(1.0f);
        } else {
            editTextInput.setEnabled(false);
            buttonSend.setEnabled(false);
            buttonSend.setImageResource(android.R.drawable.ic_menu_send);
            buttonSend.setAlpha(0.4f);
        }
    }

    private void showSessionHistoryDialogWithBookContext(Book book, String bookId) {
        chatViewModel.loadSessions();
        List<ChatSession> sessionList = chatViewModel.getSessions().getValue();

        if (sessionList == null || sessionList.isEmpty()) {
            String title = (book != null) ? "Ask about: " + book.getTitle() : "New Conversation";
            chatViewModel.createNewSession(title, bookId);
            return;
        }

        ChatSessionPickerDialog dialog = new ChatSessionPickerDialog(
                sessionList,
                new ChatSessionPickerDialog.OnSessionSelectedListener() {
                    @Override
                    public void onSessionSelected(ChatSession session) {
                        chatViewModel.selectSession(session.getSessionId());
                        chatViewModel.addBookToCurrentSession(bookId);
                        chatViewModel.setActiveBookForCurrentSession(bookId);
                    }

                    @Override
                    public void onCreateNewSessionClicked() {
                        String title = (book != null) ? "Ask about: " + book.getTitle() : "New Conversation";
                        chatViewModel.createNewSession(title, bookId);
                    }

                    @Override
                    public void onSessionDeleted(ChatSession session) {
                        chatViewModel.deleteSession(session.getSessionId());
                    }
                }
        );
        dialog.show(getChildFragmentManager(), "ChatSessionPickerDialog");
    }

    private void showSessionHistoryDialog() {
        chatViewModel.loadSessions();
        List<ChatSession> sessionList = chatViewModel.getSessions().getValue();
        if (sessionList == null || sessionList.isEmpty()) {
            showBookPickerForNewSession();
            return;
        }

        ChatSessionPickerDialog dialog = new ChatSessionPickerDialog(
                sessionList,
                new ChatSessionPickerDialog.OnSessionSelectedListener() {
                    @Override
                    public void onSessionSelected(ChatSession session) {
                        chatViewModel.selectSession(session.getSessionId());
                    }

                    @Override
                    public void onCreateNewSessionClicked() {
                        showBookPickerForNewSession();
                    }

                    @Override
                    public void onSessionDeleted(ChatSession session) {
                        chatViewModel.deleteSession(session.getSessionId());
                    }
                }
        );
        dialog.show(getChildFragmentManager(), "ChatSessionPickerDialog");
    }

    private void showBookPickerForNewSession() {
        Book active = chatViewModel.getActiveBook().getValue();
        String currentId = (active != null) ? active.getId() : null;

        BookPickerBottomSheet bottomSheet = new BookPickerBottomSheet(
                chatViewModel.getAvailableBooks().getValue(),
                currentId,
                book -> {
                    chatViewModel.createNewSession("Ask about: " + book.getTitle(), book.getId());
                }
        );
        bottomSheet.show(getChildFragmentManager(), "BookPickerBottomSheet");
    }

    private void showBookPickerDialog() {
        Book active = chatViewModel.getActiveBook().getValue();
        String currentId = (active != null) ? active.getId() : null;

        BookPickerBottomSheet bottomSheet = new BookPickerBottomSheet(
                chatViewModel.getAvailableBooks().getValue(),
                currentId,
                book -> {
                    ChatSession currentSession = chatViewModel.getCurrentSession().getValue();
                    if (currentSession == null) {
                        chatViewModel.createNewSession("Ask about: " + book.getTitle(), book.getId());
                    } else {
                        chatViewModel.addBookToCurrentSession(book.getId());
                        chatViewModel.setActiveBookForCurrentSession(book.getId());
                    }
                }
        );
        bottomSheet.show(getChildFragmentManager(), "BookPickerBottomSheet");
    }
}
