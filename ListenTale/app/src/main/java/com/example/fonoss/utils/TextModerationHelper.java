package com.example.fonoss.utils;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

public class TextModerationHelper {

    private static Set<String> badWords;

    private static void loadBadWords(Context context) {
        if (badWords != null) return;
        badWords = new HashSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("bad_words.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (!line.isEmpty()) {
                    badWords.add(line);
                    // Add unaccented version for Vietnamese words
                    String unaccented = removeAccents(line);
                    if (!unaccented.equals(line)) {
                        badWords.add(unaccented);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isTextAppropriate(Context context, String text) {
        if (text == null || text.trim().isEmpty()) return true;
        
        loadBadWords(context);
        
        String normalizedText = text.toLowerCase();
        String unaccentedText = removeAccents(normalizedText);

        // Check if the username contains any bad word
        for (String badWord : badWords) {
            // We check both the original normalized text and the unaccented version
            // For example, "óc chó" or "oc cho"
            // To prevent false positives, we might want to check for exact word matches 
            // instead of substring if the bad word is very short.
            // But for a simple filter, substring matching often works for profanity.
            
            // For 2-letter bad words (like "đm", "vl", "cc", "đb"), doing substring matching
            // can flag innocent words (like "đam mê"). Let's check whole words instead.
            
            String[] words = normalizedText.split("\\s+");
            String[] unaccentedWords = unaccentedText.split("\\s+");
            
            for (String w : words) {
                if (w.equals(badWord) || w.contains(badWord) && badWord.length() > 3) {
                    return false;
                }
            }
            
            for (String w : unaccentedWords) {
                if (w.equals(badWord) || w.contains(badWord) && badWord.length() > 3) {
                    return false;
                }
            }
            
            // Check the entire text for multi-word profanity
            if (normalizedText.contains(badWord) || unaccentedText.contains(badWord)) {
                return false;
            }
        }
        return true;
    }

    private static String removeAccents(String s) {
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replace('đ', 'd').replace('Đ', 'D');
    }
}
