package com.example.fonoss.data.repository;

import android.content.Context;

import com.example.fonoss.data.local.ChatLocalStorage;
import com.example.fonoss.data.model.ChatMessage;
import com.example.fonoss.data.model.ChatSession;
import com.example.fonoss.utils.ChatPermissionPolicy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ChatRepository {

    private final Context context;
    private final ChatPermissionPolicy permissionPolicy;

    @Inject
    public ChatRepository(@ApplicationContext Context context, ChatPermissionPolicy permissionPolicy) {
        this.context = context;
        this.permissionPolicy = permissionPolicy;
    }

    public List<ChatSession> getAllSessions() {
        return ChatLocalStorage.loadSessions(context);
    }

    public ChatSession getSessionById(String sessionId) {
        List<ChatSession> sessions = getAllSessions();
        for (ChatSession session : sessions) {
            if (session.getSessionId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }

    public ChatSession createNewSession(String title, String initialBookId) {
        List<ChatSession> sessions = getAllSessions();
        if (!permissionPolicy.canCreateNewSession(sessions.size())) {
            return null;
        }

        ChatSession newSession = new ChatSession(title, initialBookId);
        sessions.add(0, newSession);
        ChatLocalStorage.saveSessions(context, sessions);
        return newSession;
    }

    public void updateSession(ChatSession updatedSession) {
        List<ChatSession> sessions = getAllSessions();
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getSessionId().equals(updatedSession.getSessionId())) {
                updatedSession.setUpdatedAt(System.currentTimeMillis());
                sessions.set(i, updatedSession);
                break;
            }
        }
        ChatLocalStorage.saveSessions(context, sessions);
    }

    public void deleteSession(String sessionId) {
        List<ChatSession> sessions = getAllSessions();
        Iterator<ChatSession> iterator = sessions.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getSessionId().equals(sessionId)) {
                iterator.remove();
                break;
            }
        }
        ChatLocalStorage.saveSessions(context, sessions);
    }

    public List<ChatMessage> getMessagesForSession(String sessionId) {
        return ChatLocalStorage.loadMessages(context, sessionId);
    }

    public void addMessageToSession(String sessionId, ChatMessage message) {
        List<ChatMessage> messages = getMessagesForSession(sessionId);
        messages.add(message);
        ChatLocalStorage.saveMessages(context, sessionId, messages);

        // Update session last message & timestamp
        ChatSession session = getSessionById(sessionId);
        if (session != null) {
            session.setLastMessage(message.getContent());
            session.setUpdatedAt(System.currentTimeMillis());
            updateSession(session);
        }
    }
}
