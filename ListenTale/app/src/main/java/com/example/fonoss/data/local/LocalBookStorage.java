package com.example.fonoss.data.local;

import com.example.fonoss.data.model.Book;

import android.content.Context;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalBookStorage {
    private LocalBookStorage() {}

    public static void save(Context context, Book book) throws Exception {
        FileOutputStream fos = context.openFileOutput(fileName(book.getId()), Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(book);
        List<String> chapters = book.getChapters();
        oos.writeObject(chapters == null ? null : new ArrayList<>(chapters));
        oos.close();
        fos.close();
    }

    public static Book load(Context context, String bookId) throws Exception {
        FileInputStream fis = context.openFileInput(fileName(bookId));
        ObjectInputStream ois = new ObjectInputStream(fis);
        Book book = (Book) ois.readObject();
        try {
            Object chapters = ois.readObject();
            if (chapters instanceof List) {
                book.setChapters((List<String>) chapters);
            }
        } catch (EOFException ignored) {
            // Older local files only contain the Book object.
        }
        ois.close();
        fis.close();
        return book;
    }

    private static String fileName(String bookId) {
        return "book_" + bookId + ".dat";
    }
}


