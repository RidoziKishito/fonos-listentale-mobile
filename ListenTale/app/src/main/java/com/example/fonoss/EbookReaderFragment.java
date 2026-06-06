package com.example.fonoss;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EbookReaderFragment extends Fragment {

    private int currentChapter = 1;
    private int totalChapters = 1;
    private TextView textChapterTitle, textContent, textPageIndicator;
    private LibraryViewModel libraryViewModel;
    private Book currentBook;
    private boolean isInitialLoad = true;

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
                        textContent.setText(textContent.getText());
                        textContent.invalidate();
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
        TextView textBookTitle = view.findViewById(R.id.text_reader_title);

        if (getArguments() != null) {
            currentBook = (Book) getArguments().getSerializable("book");
            if (currentBook != null) {
                textBookTitle.setText(currentBook.getTitle());
                libraryViewModel.markAsInProgress(currentBook);
                
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

    private void updateChapter(boolean shouldSave) {
        if (totalChapters == 0) return; // Bảo vệ nếu chưa nạp xong dữ liệu

        textChapterTitle.setText(String.format(Locale.getDefault(), "Chapter %d", currentChapter));
        textPageIndicator.setText(String.format(Locale.getDefault(), "%d / %d", currentChapter, totalChapters));
        
        if (currentBook != null && currentBook.getChapters() != null && !currentBook.getChapters().isEmpty()) {
            String htmlContent = currentBook.getChapters().get(Math.min(currentChapter - 1, totalChapters - 1));
            
            // Sử dụng ImageGetter để render ảnh từ thẻ <img>
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                textContent.setText(Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY, new GlideImageGetter(), null));
            } else {
                textContent.setText(Html.fromHtml(htmlContent, new GlideImageGetter(), null));
            }
            
            if (shouldSave) {
                libraryViewModel.saveReadingChapter(currentBook.getId(), currentChapter);
            }
            
            if (currentChapter == totalChapters) libraryViewModel.markAsCompleted(currentBook);
        }

        View scrollView = getView() != null ? getView().findViewById(R.id.reader_scroll_view) : null;
        if (scrollView != null) scrollView.scrollTo(0, 0);
    }
}
