package com.example.fonoss.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.fonoss.data.model.Book;
import com.example.fonoss.data.model.ChatMessage;
import com.example.fonoss.data.model.ChatSession;
import com.example.fonoss.data.repository.BookRepository;
import com.example.fonoss.data.repository.ChatRepository;
import com.example.fonoss.utils.ChatPermissionPolicy;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ChatViewModel extends ViewModel {

    private final ChatRepository chatRepository;
    private final BookRepository bookRepository;
    private final ChatPermissionPolicy permissionPolicy;
    private final FirebaseFirestore db;

    private final MutableLiveData<List<ChatSession>> sessions = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<ChatSession> currentSession = new MutableLiveData<>();
    private final MutableLiveData<List<ChatMessage>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Book>> availableBooks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Book> activeBook = new MutableLiveData<>();
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();

    @Inject
    public ChatViewModel(ChatRepository chatRepository, BookRepository bookRepository, ChatPermissionPolicy permissionPolicy) {
        this.chatRepository = chatRepository;
        this.bookRepository = bookRepository;
        this.permissionPolicy = permissionPolicy;
        this.db = bookRepository.getDb();
        loadAvailableBooks();
    }

    public LiveData<List<ChatSession>> getSessions() {
        return sessions;
    }

    public LiveData<ChatSession> getCurrentSession() {
        return currentSession;
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return messages;
    }

    public LiveData<List<Book>> getAvailableBooks() {
        return availableBooks;
    }

    public LiveData<Book> getActiveBook() {
        return activeBook;
    }

    public LiveData<String> getToastMessage() {
        return toastMessage;
    }

    public void loadSessions() {
        List<ChatSession> list = chatRepository.getAllSessions();
        sessions.setValue(list);
    }

    public void loadAvailableBooks() {
        db.collection("books").get().addOnSuccessListener(querySnapshots -> {
            List<Book> list = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshots.getDocuments()) {
                Book b = doc.toObject(Book.class);
                if (b != null) {
                    b.setId(doc.getId());
                    list.add(b);
                }
            }
            availableBooks.postValue(list);
        });
    }

    public void selectSession(String sessionId) {
        ChatSession session = chatRepository.getSessionById(sessionId);
        if (session != null) {
            currentSession.setValue(session);
            loadMessagesForSession(sessionId);
            updateActiveBookForSession(session);
        }
    }

    public void createOrSelectSessionWithBook(String bookId, Book targetBook) {
        loadSessions();
        List<ChatSession> currentList = sessions.getValue();
        ChatSession foundSession = null;

        if (currentList != null) {
            for (ChatSession s : currentList) {
                if (s.getBookIds() != null && s.getBookIds().contains(bookId)) {
                    foundSession = s;
                    break;
                }
            }
        }

        if (foundSession == null) {
            String title = (targetBook != null) ? "Ask about: " + targetBook.getTitle() : "New Conversation";
            foundSession = chatRepository.createNewSession(title, bookId);
        }

        if (foundSession != null) {
            selectSession(foundSession.getSessionId());
            if (targetBook != null) {
                activeBook.setValue(targetBook);
            }
        } else {
            toastMessage.setValue("Reached maximum conversation limit!");
        }
    }

    public void createNewSession(String title, String initialBookId) {
        List<ChatSession> currentList = chatRepository.getAllSessions();
        if (!permissionPolicy.canCreateNewSession(currentList.size())) {
            toastMessage.setValue("Your account has reached the conversation limit.");
            return;
        }

        ChatSession newSession = chatRepository.createNewSession(title, initialBookId);
        if (newSession != null) {
            loadSessions();
            selectSession(newSession.getSessionId());
        }
    }

    public void deleteSession(String sessionId) {
        chatRepository.deleteSession(sessionId);
        loadSessions();
        ChatSession current = currentSession.getValue();
        if (current != null && current.getSessionId().equals(sessionId)) {
            currentSession.setValue(null);
            messages.setValue(new ArrayList<>());
            activeBook.setValue(null);
        }
    }

    public void addBookToCurrentSession(String bookId) {
        ChatSession session = currentSession.getValue();
        if (session == null || bookId == null) return;

        session.addBookId(bookId);
        setActiveBookForCurrentSession(bookId);
    }

    public void setActiveBookForCurrentSession(String bookId) {
        ChatSession session = currentSession.getValue();
        if (session == null || bookId == null) return;

        session.setActiveBookId(bookId);
        chatRepository.updateSession(session);
        currentSession.setValue(session);

        fetchBookById(bookId, book -> {
            if (book != null) {
                activeBook.setValue(book);
            }
        });
    }

    public void sendMessage(String text) {
        ChatSession session = currentSession.getValue();
        Book book = activeBook.getValue();
        if (session == null || text == null || text.trim().isEmpty()) return;

        String currentBookId = (book != null) ? book.getId() : session.getActiveBookId();
        if (currentBookId == null || currentBookId.isEmpty()) {
            toastMessage.setValue("Please select a book before asking a question!");
            return;
        }

        // Add divider message when user enters message for a new book context
        List<ChatMessage> currentMsgs = messages.getValue();
        boolean needDivider = true;
        if (currentMsgs != null && !currentMsgs.isEmpty()) {
            for (int i = currentMsgs.size() - 1; i >= 0; i--) {
                ChatMessage m = currentMsgs.get(i);
                if (currentBookId.equals(m.getBookId())) {
                    needDivider = false;
                    break;
                }
            }
        }
        if (needDivider) {
            String bookTitle = (book != null) ? book.getTitle() : "Book";
            ChatMessage sysMsg = new ChatMessage(session.getSessionId(), currentBookId, ChatMessage.SENDER_SYSTEM, bookTitle);
            chatRepository.addMessageToSession(session.getSessionId(), sysMsg);
        }

        // 1. Add User Message
        ChatMessage userMsg = new ChatMessage(session.getSessionId(), currentBookId, ChatMessage.SENDER_USER, text.trim());
        chatRepository.addMessageToSession(session.getSessionId(), userMsg);
        loadMessagesForSession(session.getSessionId());

        // 2. Mock AI response (will be replaced with backend API integration)
        String bookTitle = (book != null) ? book.getTitle() : "this book";
        String mockReply = "Thank you for asking about \"" + bookTitle + "\". I am Fonos AI Assistant. An automated response for \"" 
                + text.trim() + "\" is being processed. (Backend API Integration Ready)";

        ChatMessage aiMsg = new ChatMessage(session.getSessionId(), currentBookId, ChatMessage.SENDER_AI, mockReply);
        chatRepository.addMessageToSession(session.getSessionId(), aiMsg);
        loadMessagesForSession(session.getSessionId());

        loadSessions(); // refresh updated order
    }

    private void loadMessagesForSession(String sessionId) {
        List<ChatMessage> list = chatRepository.getMessagesForSession(sessionId);
        messages.setValue(list);
    }

    private void updateActiveBookForSession(ChatSession session) {
        String activeId = session.getActiveBookId();
        if (activeId != null && !activeId.isEmpty()) {
            fetchBookById(activeId, book -> activeBook.setValue(book));
        } else if (!session.getBookIds().isEmpty()) {
            String firstId = session.getBookIds().get(0);
            fetchBookById(firstId, book -> {
                activeBook.setValue(book);
                session.setActiveBookId(firstId);
                chatRepository.updateSession(session);
            });
        } else {
            activeBook.setValue(null);
        }
    }

    public void fetchBookById(String bookId, OnBookLoadedCallback callback) {
        db.collection("books").document(bookId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Book b = doc.toObject(Book.class);
                if (b != null) {
                    b.setId(doc.getId());
                    callback.onBookLoaded(b);
                    return;
                }
            }
            callback.onBookLoaded(null);
        }).addOnFailureListener(e -> callback.onBookLoaded(null));
    }

    public interface OnBookLoadedCallback {
        void onBookLoaded(Book book);
    }
}
