package com.example.fonoss;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EbookReaderFragment extends Fragment {

    private int currentChapter = 1;
    private int totalChapters = 1;
    private TextView textChapterTitle, textContent, textPageIndicator;
    private NestedScrollView readerScrollView;
    private LibraryViewModel libraryViewModel;
    private Book currentBook;
    private boolean isInitialLoad = true;
    private Spanned currentChapterContent;
    private BookmarkAdapter bookmarksDialogAdapter;

    private class GlideImageGetter implements Html.ImageGetter {
        @Override
        public Drawable getDrawable(String source) {
            final URLDrawable urlDrawable = new URLDrawable();
            Glide.with(EbookReaderFragment.this).asBitmap().load(source).into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    if (isAdded() && textContent != null) {
                        float width = textContent.getWidth() - textContent.getPaddingLeft() - textContent.getPaddingRight();
                        if (width <= 0) width = 800; // Fallback
                        float scale = width / resource.getWidth();
                        int newHeight = (int) (resource.getHeight() * scale);
                        BitmapDrawable drawable = new BitmapDrawable(getResources(), resource);
                        drawable.setBounds(0, 0, (int)width, newHeight);
                        urlDrawable.setDrawable(drawable);
                        urlDrawable.setBounds(0, 0, (int)width, newHeight);
                        int scrollY = readerScrollView != null ? readerScrollView.getScrollY() : 0;
                        textContent.setText(textContent.getText());
                        textContent.invalidate();
                        if (readerScrollView != null) {
                            scrollToYAfterLayout(scrollY);
                        }
                    }
                }
                @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
            return urlDrawable;
        }
    }

    private class URLDrawable extends BitmapDrawable {
        private Drawable drawable;
        @Override
        public void draw(Canvas canvas) { if (drawable != null) drawable.draw(canvas); }
        public void setDrawable(Drawable drawable) { this.drawable = drawable; }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ebook_reader, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        libraryViewModel = new ViewModelProvider(requireActivity()).get(LibraryViewModel.class);
        textChapterTitle = view.findViewById(R.id.text_chapter_title);
        textContent = view.findViewById(R.id.text_book_content);
        textPageIndicator = view.findViewById(R.id.text_page_indicator);
        readerScrollView = view.findViewById(R.id.reader_scroll_view);
        TextView textBookTitle = view.findViewById(R.id.text_reader_title);

        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
            if (currentBook != null) {
                textBookTitle.setText(currentBook.getTitle());
                libraryViewModel.markAsInProgress(currentBook);
                libraryViewModel.fetchBookmarks(currentBook.getId());
                libraryViewModel.getBookmarks().observe(getViewLifecycleOwner(), bookmarks -> {
                    if (bookmarksDialogAdapter != null && bookmarks != null) {
                        bookmarksDialogAdapter.setBookmarks(bookmarks);
                    }
                    applyHighlightsKeepingScroll(readerScrollView.getScrollY());
                });
                
                totalChapters = 0; 
                textContent.setText("Loading content...");

                // Ưu tiên nạp từ máy (CỰC NHANH cho sách đã download)
                Book localBook = libraryViewModel.loadBookLocally(currentBook.getId());
                if (localBook != null && localBook.getChapters() != null && !localBook.getChapters().isEmpty()) {
                    currentBook.setChapters(localBook.getChapters());
                    totalChapters = currentBook.getChapters().size();
                    updateChapter(false);
                } else if (currentBook.getChapters() != null && !currentBook.getChapters().isEmpty()) {
                    // Nếu sách truyền qua có sẵn chapters (từ Discover)
                    totalChapters = currentBook.getChapters().size();
                    updateChapter(false);
                } else {
                    // Cuối cùng mới tải từ Firestore (chỉ cho sách chưa download và nạp từ Library)
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("books").document(currentBook.getId())
                        .get().addOnSuccessListener(doc -> {
                            if (isAdded() && doc.exists()) {
                                List<String> cloudChapters = (List<String>) doc.get("chapters");
                                if (cloudChapters != null) {
                                    currentBook.setChapters(cloudChapters);
                                    totalChapters = cloudChapters.size();
                                    updateChapter(false);
                                }
                            }
                        });
                }

                // Listen for Firebase data and restore chapter ONLY ONCE
                libraryViewModel.getBookProgressChapter().observe(getViewLifecycleOwner(), chapterMap -> {
                    if (isInitialLoad && currentBook != null && chapterMap != null && chapterMap.containsKey(currentBook.getId())) {
                        Long savedChapter = chapterMap.get(currentBook.getId());
                        if (savedChapter != null) {
                            currentChapter = savedChapter.intValue();
                            if (totalChapters > 0) updateChapter(false);
                            isInitialLoad = false;
                        }
                    }
                });
            }
        }

        view.findViewById(R.id.button_reader_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        view.findViewById(R.id.button_bookmarks).setOnClickListener(v -> showBookmarksDialog());

        setupTextSelection();

        view.findViewById(R.id.button_next_chapter).setOnClickListener(v -> {
            if (totalChapters > 0 && currentChapter < totalChapters) {
                isInitialLoad = false; 
                currentChapter++;
                updateChapter(true); 
            }
        });

        view.findViewById(R.id.button_prev_chapter).setOnClickListener(v -> {
            if (totalChapters > 0 && currentChapter > 1) {
                isInitialLoad = false;
                currentChapter--;
                updateChapter(true);
            }
        });
    }

    private void setupTextSelection() {
        textContent.setTextIsSelectable(true);
        textContent.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                menu.add(0, 100, 0, "Bookmark").setIcon(android.R.drawable.ic_input_add);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                if (item.getItemId() == 100) {
                    int start = textContent.getSelectionStart();
                    int end = textContent.getSelectionEnd();
                    if (start > end) {
                        int temp = start;
                        start = end;
                        end = temp;
                    }
                    if (start >= 0 && end > start && end <= textContent.getText().length()) {
                        String selectedText = textContent.getText().subSequence(start, end).toString();
                        addBookmark(selectedText, start, end);
                    }
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        });
    }

    private void addBookmark(String text, int startOffset, int endOffset) {
        if (currentBook == null) return;

        int scrollY = readerScrollView.getScrollY();
        Bookmark bookmark = new Bookmark(
                null,
                currentBook.getId(),
                currentChapter - 1,
                scrollY,
                text,
                startOffset,
                endOffset,
                System.currentTimeMillis()
        );

        libraryViewModel.saveBookmark(bookmark);
        UiNotifier.success(getContext(), "Bookmark added");
        applyHighlightsKeepingScroll(scrollY);
    }

    private void showBookmarksDialog() {
        if (currentBook == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_bookmarks, null);
        dialog.setContentView(dialogView);

        RecyclerView recyclerView = dialogView.findViewById(R.id.recycler_bookmarks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        BookmarkAdapter adapter = new BookmarkAdapter(new ArrayList<>(), new BookmarkAdapter.OnBookmarkClickListener() {
            @Override
            public void onBookmarkClick(Bookmark bookmark) {
                if (bookmark.getChapterIndex() + 1 != currentChapter) {
                    currentChapter = bookmark.getChapterIndex() + 1;
                    updateChapter(false);
                }
                readerScrollView.post(() -> readerScrollView.smoothScrollTo(0, bookmark.getScrollY()));
                dialog.dismiss();
            }

            @Override
            public void onDeleteClick(Bookmark bookmark) {
                int tempY = readerScrollView.getScrollY();
                libraryViewModel.deleteBookmark(bookmark.getId());
                applyHighlightsKeepingScroll(tempY);
                UiNotifier.info(getContext(), "Bookmark removed");
            }
        });

        recyclerView.setAdapter(adapter);
        bookmarksDialogAdapter = adapter;
        dialog.setOnDismissListener(d -> {
            if (bookmarksDialogAdapter == adapter) {
                bookmarksDialogAdapter = null;
            }
        });

        List<Bookmark> currentBookmarks = libraryViewModel.getBookmarks().getValue();
        if (currentBookmarks != null) {
            adapter.setBookmarks(currentBookmarks);
        }

        dialog.show();
    }

    private void updateChapter(boolean shouldSave) {
        if (totalChapters == 0) return;

        textChapterTitle.setText(String.format(Locale.getDefault(), "Chapter %d", currentChapter));
        textPageIndicator.setText(String.format(Locale.getDefault(), "%d / %d", currentChapter, totalChapters));
        
        if (currentBook != null && currentBook.getChapters() != null && !currentBook.getChapters().isEmpty()) {
            String htmlContent = currentBook.getChapters().get(Math.min(currentChapter - 1, totalChapters - 1));

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                currentChapterContent = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY, new GlideImageGetter(), null);
            } else {
                currentChapterContent = Html.fromHtml(htmlContent, new GlideImageGetter(), null);
            }
            applyHighlightsKeepingScroll(shouldSave ? 0 : readerScrollView.getScrollY());
            
            if (shouldSave) {
                libraryViewModel.saveReadingChapter(currentBook.getId(), currentChapter);
            }
            
            if (currentChapter == totalChapters) libraryViewModel.markAsCompleted(currentBook);
        }

        if (shouldSave) scrollToYAfterLayout(0);
    }

    private void applyHighlightsKeepingScroll(int scrollY) {
        if (currentChapterContent == null) return;

        SpannableStringBuilder spannable = new SpannableStringBuilder(currentChapterContent);
        List<Bookmark> bookmarks = libraryViewModel.getBookmarks().getValue();
        if (bookmarks != null) {
            for (Bookmark bookmark : bookmarks) {
                if (currentBook == null || !currentBook.getId().equals(bookmark.getBookId())) continue;
                if (bookmark.getChapterIndex() != currentChapter - 1) continue;
                int start = bookmark.getStartOffset();
                int end = bookmark.getEndOffset();

                if (!isValidHighlightRange(start, end, spannable.length(), bookmark.getSelectedText())) {
                    String selectedText = bookmark.getSelectedText();
                    if (selectedText == null || selectedText.isEmpty()) continue;
                    int fallbackStart = spannable.toString().indexOf(selectedText);
                    if (fallbackStart < 0) continue;
                    start = fallbackStart;
                    end = fallbackStart + selectedText.length();
                }

                spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }

        textContent.setText(spannable);
        scrollToYAfterLayout(scrollY);
    }

    private boolean isValidHighlightRange(int start, int end, int contentLength, String selectedText) {
        if (start < 0 || end <= start || end > contentLength) return false;
        if (selectedText == null || selectedText.isEmpty()) return false;
        CharSequence currentText = currentChapterContent.subSequence(start, end);
        return selectedText.contentEquals(currentText);
    }

    private void scrollToYAfterLayout(int y) {
        readerScrollView.post(() -> {
            readerScrollView.scrollTo(0, y);
            readerScrollView.post(() -> readerScrollView.scrollTo(0, y));
        });
    }
}
