package com.example.fonoss.data.local;

import android.content.Context;
import android.util.Log;

import com.example.fonoss.data.model.ChatMessage;
import com.example.fonoss.data.model.ChatSession;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatLocalStorage {

    private static final String SESSIONS_FILE_NAME = "chat_sessions.dat";
    private static final String TAG = "ChatLocalStorage";

    private ChatLocalStorage() {}

    public static synchronized List<ChatSession> loadSessions(Context context) {
        try {
            FileInputStream fis = context.openFileInput(SESSIONS_FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<ChatSession> sessions = (List<ChatSession>) ois.readObject();
            ois.close();
            fis.close();
            if (sessions != null) {
                // Sort sessions by updatedAt descending (newest first)
                Collections.sort(sessions, (s1, s2) -> Long.compare(s2.getUpdatedAt(), s1.getUpdatedAt()));
                return sessions;
            }
        } catch (Exception e) {
            Log.d(TAG, "No existing sessions file or read error: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static synchronized void saveSessions(Context context, List<ChatSession> sessions) {
        try {
            FileOutputStream fos = context.openFileOutput(SESSIONS_FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(sessions);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving chat sessions", e);
        }
    }

    public static synchronized List<ChatMessage> loadMessages(Context context, String sessionId) {
        String fileName = "messages_" + sessionId + ".dat";
        try {
            FileInputStream fis = context.openFileInput(fileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<ChatMessage> messages = (List<ChatMessage>) ois.readObject();
            ois.close();
            fis.close();
            if (messages != null) {
                return messages;
            }
        } catch (Exception e) {
            Log.d(TAG, "No existing messages for session " + sessionId + ": " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public static synchronized void saveMessages(Context context, String sessionId, List<ChatMessage> messages) {
        String fileName = "messages_" + sessionId + ".dat";
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(messages);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving messages for session " + sessionId, e);
        }
    }
}
