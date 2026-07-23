package com.example.fonoss.ui.player;

import com.example.fonoss.R;
import dagger.hilt.android.AndroidEntryPoint;

import com.example.fonoss.ui.library.LibraryViewModel;
import com.example.fonoss.data.model.Book;
import com.example.fonoss.service.AudioService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import java.util.Locale;
import java.util.Map;
import java.util.List;

import com.example.fonoss.data.model.Bookmark;
import com.example.fonoss.data.model.Playlist;
import com.example.fonoss.data.recommendation.RecommendationEngine;
import com.example.fonoss.manager.QueueManager;
import com.example.fonoss.adapter.BookmarkAdapter;
import com.example.fonoss.utils.UiNotifier;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;

@AndroidEntryPoint
public class AudioPlayerFragment extends Fragment {

    private ImageButton fabPlayPause;
    private Slider playerSlider;
    private TextView textCurrentTime, textTotalTime, textTimerCountdown, textCurrentSpeed;
    private TextView textPlayerTitle, textPlayerAuthor;
    private ImageView imagePlayerArtwork, imagePlayerBg;
    private ImageButton buttonTimer;
    
    private AudioService audioService;
    private boolean isBound = false;
    private Book currentBook;
    private LibraryViewModel libraryViewModel;
    private BookmarkAdapter bookmarksDialogAdapter;
    
    private CountDownTimer sleepTimer;
    private long timerMillisRemaining = 0;
    private boolean isTimerRunning = false;
    private boolean isTimerPaused = false;
    private TextView dialogRemainingText;
    private boolean isPositionRestored = false;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (isBound && audioService != null) {
                updateUI();
                checkPlaybackStatus();
                if (audioService.isPlaying()) {
                    handler.postDelayed(this, (long) (1000 / audioService.getPlaybackSpeed()));
                }
            }
        }
    };

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.LocalBinder binder = (AudioService.LocalBinder) service;
            audioService = binder.getService();
            isBound = true;

            audioService.setListener(isPlaying -> {
                updatePlayPauseIcon();
                if (isPlaying) {
                    handler.removeCallbacks(updateProgressTask);
                    handler.post(updateProgressTask);
                }
            });

            Book serviceBook = audioService.getCurrentBook();
            
            if (currentBook != null) {
                if (serviceBook != null && serviceBook.getId().equals(currentBook.getId())) {
                    currentBook = serviceBook;
                    isPositionRestored = true;
                } else {
                    audioService.playBook(currentBook);
                    libraryViewModel.markAsInProgress(currentBook);
                    
                    Map<String, Long> posMap = libraryViewModel.getBookProgressPos().getValue();
                    if (getArguments() != null && getArguments().containsKey("chapterIndex")) {
                        int targetChapterIndex = getArguments().getInt("chapterIndex");
                        int totalChapters = (currentBook != null && currentBook.getChapters() != null && !currentBook.getChapters().isEmpty()) 
                                            ? currentBook.getChapters().size() : 10;
                        int totalDur = audioService.getTotalDuration();
                        if (totalDur > 0 && totalChapters > 0) {
                            int seekSec = (targetChapterIndex * totalDur) / totalChapters;
                            audioService.seekTo(seekSec);
                            isPositionRestored = true;
                        }
                    } else if (!isPositionRestored && posMap != null && posMap.containsKey(currentBook.getId())) {
                        Long savedPos = posMap.get(currentBook.getId());
                        if (savedPos != null) {
                            audioService.seekTo(savedPos.intValue());
                            isPositionRestored = true;
                        }
                    }
                }
                setupSmartQueue();
            } else if (serviceBook != null) {
                currentBook = serviceBook;
                isPositionRestored = true;
                setupSmartQueue();
            }

            if (currentBook != null) bindBookData();
            updatePlayPauseIcon();
            updateUI();
            updateSpeedUI(audioService.getPlaybackSpeed());
            handler.removeCallbacks(updateProgressTask);
            handler.post(updateProgressTask);
        }
        @Override public void onServiceDisconnected(ComponentName name) { isBound = false; audioService = null; }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audio_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        
        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
        }

        fabPlayPause = view.findViewById(R.id.fab_play_pause);
        playerSlider = view.findViewById(R.id.player_slider);
        textCurrentTime = view.findViewById(R.id.text_player_current_time);
        textTotalTime = view.findViewById(R.id.text_player_total_time);
        buttonTimer = view.findViewById(R.id.button_player_timer);
        textTimerCountdown = view.findViewById(R.id.text_timer_countdown);
        textCurrentSpeed = view.findViewById(R.id.text_current_speed);
        textPlayerTitle = view.findViewById(R.id.text_player_title);
        textPlayerAuthor = view.findViewById(R.id.text_player_author);
        imagePlayerArtwork = view.findViewById(R.id.image_player_artwork);
        imagePlayerBg = view.findViewById(R.id.image_player_bg);

        view.findViewById(R.id.button_player_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
        fabPlayPause.setOnClickListener(v -> togglePlay());
        view.findViewById(R.id.button_player_rewind_15).setOnClickListener(v -> seekBySeconds(-15));
        view.findViewById(R.id.button_player_forward_30).setOnClickListener(v -> seekBySeconds(30));
        
        view.findViewById(R.id.button_player_prev).setOnClickListener(v -> {
            if (isBound && audioService != null) {
                audioService.playPreviousBook();
                updateUI();
            }
        });
        
        view.findViewById(R.id.button_player_next).setOnClickListener(v -> {
            if (isBound && audioService != null) {
                audioService.playNextBook(null, null);
                updateUI();
            }
        });

        playerSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser && isBound && audioService != null) {
                int targetSec = (int) (value * audioService.getTotalDuration() / 100.0f);
                audioService.seekTo(targetSec);
                saveCurrentPositionImmediately(false);
                updateUI();
            }
        });
        view.findViewById(R.id.layout_speed_control).setOnClickListener(this::showSpeedMenu);
        textCurrentSpeed.setOnClickListener(this::showSpeedMenu);
        buttonTimer.setOnClickListener(v -> showTimerDialog());
        textTimerCountdown.setOnClickListener(v -> showTimerDialog());

        View btnAddBookmark = view.findViewById(R.id.button_player_add_bookmark);
        if (btnAddBookmark != null) {
            btnAddBookmark.setOnClickListener(v -> showAddBookmarkDialog());
        }

        View btnBookmarksList = view.findViewById(R.id.button_player_bookmarks);
        if (btnBookmarksList != null) {
            btnBookmarksList.setOnClickListener(v -> showBookmarksDialog());
        }

        View btnAiChat = view.findViewById(R.id.button_player_ai_chat);
        if (btnAiChat != null) {
            btnAiChat.setOnClickListener(v -> {
                if (currentBook == null) return;
                Bundle bundle = new Bundle();
                bundle.putSerializable("book", currentBook);
                Navigation.findNavController(v).navigate(R.id.action_audioPlayerFragment_to_chatFragment, bundle);
            });
        }
        
        if (currentBook != null) bindBookData();

        libraryViewModel.getBookProgressPos().observe(getViewLifecycleOwner(), posMap -> {
            if (!isPositionRestored && isBound && audioService != null && currentBook != null) {
                if (posMap != null && posMap.containsKey(currentBook.getId())) {
                    Long savedPos = posMap.get(currentBook.getId());
                    if (savedPos != null && audioService.getCurrentPosition() == 0) {
                        audioService.seekTo(savedPos.intValue());
                        isPositionRestored = true;
                        updateUI();
                    }
                }
            }
        });
    }

    private void bindBookData() {
        if (currentBook == null) return;
        textPlayerTitle.setText(currentBook.getTitle());
        textPlayerAuthor.setText(currentBook.getAuthor());
        Glide.with(this).load(currentBook.getCoverUrl()).placeholder(android.R.drawable.ic_menu_gallery).into(imagePlayerArtwork);
        Glide.with(this).load(currentBook.getCoverUrl()).into(imagePlayerBg);

        libraryViewModel.fetchBookmarks(currentBook.getId());
        libraryViewModel.getBookmarks().observe(getViewLifecycleOwner(), bookmarks -> {
            if (bookmarksDialogAdapter != null && bookmarks != null) {
                bookmarksDialogAdapter.setBookmarks(bookmarks);
            }
        });
    }

    private int lastSavedPos = -1;

    private void checkPlaybackStatus() {
        if (isBound && audioService != null && currentBook != null) {
            int pos = audioService.getCurrentPosition();
            int duration = audioService.getTotalDuration();
            
            if (pos > 0 && (lastSavedPos < 0 || Math.abs(pos - lastSavedPos) >= 3)) {
                lastSavedPos = pos;
                libraryViewModel.savePlaybackPosition(currentBook.getId(), pos);
            }
            
            if (duration > 0 && pos >= duration - 3) {
                libraryViewModel.markAsCompleted(currentBook);
            }

            audioService.accumulateActiveListening();
            long deltaSec = audioService.consumePendingListeningSeconds();
            if (deltaSec > 0 && libraryViewModel != null) {
                libraryViewModel.addListeningDuration(deltaSec);
            }
        }
    }

    private void saveCurrentPositionImmediately(boolean forceCloudSync) {
        if (isBound && audioService != null && currentBook != null) {
            int pos = audioService.getCurrentPosition();
            if (pos >= 0) {
                lastSavedPos = pos;
                libraryViewModel.savePlaybackPosition(currentBook.getId(), pos, forceCloudSync);
            }
            audioService.accumulateActiveListening();
            long deltaSec = audioService.consumePendingListeningSeconds();
            if (deltaSec > 0 && libraryViewModel != null) {
                libraryViewModel.addListeningDuration(deltaSec);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentPositionImmediately(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        saveCurrentPositionImmediately(true);
        if (handler != null && updateProgressTask != null) {
            handler.removeCallbacks(updateProgressTask);
        }
    }

    private void togglePlay() {
        if (isBound && audioService != null) {
            audioService.togglePlayPause();
            saveCurrentPositionImmediately(!audioService.isPlaying());
            updatePlayPauseIcon();
            if (audioService.isPlaying()) {
                handler.removeCallbacks(updateProgressTask);
                handler.post(updateProgressTask);
            }
        }
    }

    private void seekBySeconds(int seconds) {
        if (!isBound || audioService == null) return;

        int duration = audioService.getTotalDuration();
        int currentPosition = audioService.getCurrentPosition();
        int targetPosition = currentPosition + seconds;
        if (duration > 0) {
            targetPosition = Math.min(duration, targetPosition);
        }
        targetPosition = Math.max(0, targetPosition);

        audioService.seekTo(targetPosition);
        saveCurrentPositionImmediately(false);
        updateUI();
    }

    private void showSpeedMenu(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        String currentSpeedStr = String.format(Locale.getDefault(), "%.1fx", (isBound && audioService != null) ? audioService.getPlaybackSpeed() : 1.0f);
        String[] speeds = {"0.5x", "1.0x", "1.5x", "2.0x"};
        for (String s : speeds) {
            android.view.MenuItem item = popup.getMenu().add(s);
            if (s.equals(currentSpeedStr)) item.setEnabled(false);
        }
        popup.setOnMenuItemClickListener(item -> {
            float speed = Float.parseFloat(item.getTitle().toString().replace("x", ""));
            if (isBound && audioService != null) audioService.setPlaybackSpeed(speed);
            updateSpeedUI(speed);
            return true;
        });
        popup.show();
    }

    private void updateSpeedUI(float speed) {
        textCurrentSpeed.setText(String.format(Locale.getDefault(), "%.1fx", speed));
    }

    private void showTimerDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_timer_picker, null);
        NumberPicker hPicker = view.findViewById(R.id.picker_hours), mPicker = view.findViewById(R.id.picker_minutes), sPicker = view.findViewById(R.id.picker_seconds);
        hPicker.setMinValue(0); hPicker.setMaxValue(23);
        mPicker.setMinValue(0); mPicker.setMaxValue(59);
        sPicker.setMinValue(0); sPicker.setMaxValue(59);
        mPicker.setValue(30);

        dialogRemainingText = view.findViewById(R.id.text_remaining_info);
        com.google.android.material.button.MaterialButton btnSet = view.findViewById(R.id.button_set_timer), btnPauseResume = view.findViewById(R.id.button_pause_resume_timer), btnStop = view.findViewById(R.id.button_stop_timer);
        View layoutControls = view.findViewById(R.id.layout_timer_controls);

        if (isTimerRunning) {
            dialogRemainingText.setVisibility(View.VISIBLE);
            dialogRemainingText.setText("Remaining: " + textTimerCountdown.getText());
            layoutControls.setVisibility(View.VISIBLE);
            btnSet.setText("Set New Timer");
            btnPauseResume.setText(isTimerPaused ? "Continue" : "Pause");
            btnPauseResume.setOnClickListener(v -> { if (isTimerPaused) resumeSleepTimer(); else pauseSleepTimer(); dialog.dismiss(); });
            btnStop.setOnClickListener(v -> { stopSleepTimer(); dialog.dismiss(); });
        }
        btnSet.setOnClickListener(v -> {
            long totalMillis = (hPicker.getValue() * 3600L + mPicker.getValue() * 60L + sPicker.getValue()) * 1000;
            if (totalMillis > 0) { startSleepTimer(totalMillis); dialog.dismiss(); }
        });
        dialog.setOnDismissListener(d -> dialogRemainingText = null);
        dialog.setContentView(view);
        dialog.show();
    }

    private void startSleepTimer(long millis) {
        if (sleepTimer != null) sleepTimer.cancel();
        isTimerRunning = true; isTimerPaused = false;
        timerMillisRemaining = millis;
        buttonTimer.setVisibility(View.INVISIBLE);
        textTimerCountdown.setVisibility(View.VISIBLE);
        runTimer();
    }

    private void runTimer() {
        sleepTimer = new CountDownTimer(timerMillisRemaining, 1000) {
            @Override
            public void onTick(long millis) {
                timerMillisRemaining = millis;
                long h = millis / 3600000, m = (millis % 3600000) / 60000, s = (millis % 60000) / 1000;
                String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
                textTimerCountdown.setText(time);
                if (dialogRemainingText != null) dialogRemainingText.setText("Remaining: " + time);
            }
            @Override
            public void onFinish() { stopSleepTimer(); if (isBound && audioService != null) audioService.pause(); updatePlayPauseIcon(); }
        }.start();
    }

    private void pauseSleepTimer() { if (sleepTimer != null) sleepTimer.cancel(); isTimerPaused = true; }
    private void resumeSleepTimer() { isTimerPaused = false; runTimer(); }
    private void stopSleepTimer() { if (sleepTimer != null) sleepTimer.cancel(); isTimerRunning = false; buttonTimer.setVisibility(View.VISIBLE); textTimerCountdown.setVisibility(View.GONE); }

    private void updatePlayPauseIcon() {
        if (isBound && audioService != null && audioService.isPlaying()) fabPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        else fabPlayPause.setImageResource(android.R.drawable.ic_media_play);
    }

    private void updateUI() {
        if (isBound && audioService != null) {
            Book serviceBook = audioService.getCurrentBook();
            if (serviceBook != null && (currentBook == null || !serviceBook.getId().equals(currentBook.getId()))) {
                currentBook = serviceBook;
                bindBookData();
            }
            int pos = audioService.getCurrentPosition();
            int duration = audioService.getTotalDuration();
            textCurrentTime.setText(formatTime(pos));
            textTotalTime.setText(formatTime(duration));
            if (duration > 0) {
                playerSlider.setValue(Math.max(0f, Math.min(100f, (float) pos / duration * 100)));
            } else {
                playerSlider.setValue(0f);
            }
        }
    }

    private String formatTime(int seconds) {
        if (seconds < 0) return "00:00";
        int h = seconds / 3600;
        int m = (seconds % 3600) / 60;
        int s = seconds % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    private void showAddBookmarkDialog() {
        if (!isBound || audioService == null || currentBook == null) {
            UiNotifier.warning(getContext(), "Audio chưa sẵn sàng");
            return;
        }
        final int bookmarkPos = Math.max(0, audioService.getCurrentPosition());

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_audio_bookmark, null);
        dialog.setContentView(dialogView);

        TextView textTimestampInfo = dialogView.findViewById(R.id.text_bookmark_timestamp_info);
        TextInputEditText inputNote = dialogView.findViewById(R.id.input_bookmark_note);
        View btnCancel = dialogView.findViewById(R.id.button_cancel_bookmark);
        View btnSave = dialogView.findViewById(R.id.button_save_bookmark);

        String timeFormatted = formatTime(bookmarkPos);
        textTimestampInfo.setText("Vị trí: " + timeFormatted);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String note = inputNote != null && inputNote.getText() != null ? inputNote.getText().toString().trim() : "";
            Bookmark bookmark = new Bookmark(
                    null,
                    currentBook.getId(),
                    bookmarkPos,
                    note,
                    System.currentTimeMillis()
            );
            libraryViewModel.saveBookmark(bookmark);
            UiNotifier.success(getContext(), "Đã lưu mốc ghi nhớ (" + timeFormatted + ")");
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showBookmarksDialog() {
        if (currentBook == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bookmarks, null);
        dialog.setContentView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_bookmarks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        BookmarkAdapter adapter = new BookmarkAdapter(new java.util.ArrayList<>(), new BookmarkAdapter.OnBookmarkClickListener() {
            @Override
            public void onBookmarkClick(Bookmark bookmark) {
                if (bookmark.isAudioBookmark() && isBound && audioService != null) {
                    audioService.seekTo(bookmark.getAudioPosition());
                    saveCurrentPositionImmediately(false);
                    updateUI();
                    UiNotifier.info(getContext(), "Chuyển đến " + formatTime(bookmark.getAudioPosition()));
                    dialog.dismiss();
                }
            }

            @Override
            public void onDeleteClick(Bookmark bookmark) {
                libraryViewModel.deleteBookmark(bookmark.getId());
                UiNotifier.info(getContext(), "Đã xóa mốc ghi nhớ");
            }
        });

        recyclerView.setAdapter(adapter);
        bookmarksDialogAdapter = adapter;
        dialog.setOnDismissListener(d -> {
            if (bookmarksDialogAdapter == adapter) {
                bookmarksDialogAdapter = null;
            }
        });

        java.util.List<Bookmark> currentBookmarks = libraryViewModel.getBookmarks().getValue();
        if (currentBookmarks != null) {
            adapter.setBookmarks(currentBookmarks);
        }

        dialog.show();
    }

    @Override public void onStart() {
        super.onStart();
        Intent intent = new Intent(requireContext(), AudioService.class);
        ContextCompat.startForegroundService(requireContext(), intent);
        requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override public void onResume() {
        super.onResume();
        if (isBound) { 
            updateUI(); 
            updatePlayPauseIcon(); 
            handler.removeCallbacks(updateProgressTask);
            handler.post(updateProgressTask); 
        }
    }

    @Override public void onStop() {
        super.onStop();
        if (isBound) {
            if (currentBook != null && audioService != null) {
                int pos = audioService.getCurrentPosition();
                if (pos > 0) libraryViewModel.savePlaybackPosition(currentBook.getId(), pos);
                audioService.setListener(null);
            }
            requireContext().unbindService(connection);
        }
        handler.removeCallbacks(updateProgressTask);
    }

    private void setupSmartQueue() {
        if (!isBound || audioService == null || currentBook == null) return;

        RecommendationEngine.UserProfileContext context = buildUserProfileContext();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("books").get().addOnSuccessListener(snapshot -> {
                    List<Book> allBooks = new java.util.ArrayList<>();
                    if (snapshot != null) {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            Book b = doc.toObject(Book.class);
                            if (b != null) {
                                b.setId(doc.getId());
                                allBooks.add(b);
                            }
                        }
                    }
                    if (isAdded() && audioService != null && currentBook != null) {
                        audioService.setAllBooksAndContext(allBooks, context);

                        Playlist playlistArg = null;
                        if (getArguments() != null) {
                            playlistArg = (Playlist) getArguments().getSerializable("playlist");
                        }

                        if (playlistArg != null && playlistArg.getBooks() != null && !playlistArg.getBooks().isEmpty()) {
                            int startIndex = 0;
                            for (int i = 0; i < playlistArg.getBooks().size(); i++) {
                                Book pb = playlistArg.getBooks().get(i);
                                if (pb != null && currentBook.getId().equals(pb.getId())) {
                                    startIndex = i;
                                    break;
                                }
                            }
                            audioService.getQueueManager().setQueue(playlistArg.getBooks(), startIndex, QueueManager.QueueMode.PLAYLIST);
                        } else {
                            // Outside Playlist: Reset queue and populate recommended/series books
                            audioService.getQueueManager().setQueueFromBook(currentBook, allBooks, context);
                        }
                    }
                });
    }

    private RecommendationEngine.UserProfileContext buildUserProfileContext() {
        java.util.Set<String> savedBookIds = new java.util.HashSet<>();
        java.util.Set<String> inProgressBookIds = new java.util.HashSet<>();
        java.util.Set<String> completedBookIds = new java.util.HashSet<>();

        List<Book> saved = libraryViewModel != null ? libraryViewModel.getSavedBooks().getValue() : null;
        if (saved != null) {
            for (Book b : saved) if (b != null && b.getId() != null) savedBookIds.add(b.getId());
        }

        List<Book> inProg = libraryViewModel != null ? libraryViewModel.getInProgressBooks().getValue() : null;
        if (inProg != null) {
            for (Book b : inProg) if (b != null && b.getId() != null) inProgressBookIds.add(b.getId());
        }

        List<Book> comp = libraryViewModel != null ? libraryViewModel.getCompletedBooks().getValue() : null;
        if (comp != null) {
            for (Book b : comp) if (b != null && b.getId() != null) completedBookIds.add(b.getId());
        }

        RecommendationEngine.UserProfileContext ctx = new RecommendationEngine.UserProfileContext();
        ctx.setSavedBookIds(savedBookIds);
        ctx.setInProgressBookIds(inProgressBookIds);
        ctx.setCompletedBookIds(completedBookIds);
        if (currentBook != null) ctx.setLastListenedBook(currentBook);

        return ctx;
    }

    @Override public void onDestroy() { if (sleepTimer != null) sleepTimer.cancel(); super.onDestroy(); }
}



