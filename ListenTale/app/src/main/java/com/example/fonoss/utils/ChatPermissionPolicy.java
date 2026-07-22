package com.example.fonoss.utils;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChatPermissionPolicy {

    public enum UserTier {
        NORMAL,
        PREMIUM
    }

    @Inject
    public ChatPermissionPolicy() {
    }

    public UserTier getCurrentUserTier() {
        // Default to NORMAL. Later when user subscription API / Auth is linked, return actual tier.
        return UserTier.NORMAL;
    }

    public boolean canCreateNewSession(int currentSessionCount) {
        UserTier tier = getCurrentUserTier();
        if (tier == UserTier.PREMIUM) {
            return true;
        }
        // Normal users can create up to 10 sessions locally
        return currentSessionCount < 10;
    }

    public int getMaxBooksPerSession() {
        return Integer.MAX_VALUE; // Unlimited books per session
    }

    public boolean canSendMessage(int dailyMessageCount) {
        UserTier tier = getCurrentUserTier();
        if (tier == UserTier.PREMIUM) {
            return true;
        }
        return dailyMessageCount < 50; // 50 messages per day for normal users
    }
}
