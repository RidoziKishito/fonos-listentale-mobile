package com.example.fonoss.data.network;

import com.example.fonoss.BuildConfig;

public class SupabaseConfig {
    public static String getSupabaseUrl() {
        String url = BuildConfig.SUPABASE_URL;
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    public static String getSupabaseKey() {
        return BuildConfig.SUPABASE_KEY != null ? BuildConfig.SUPABASE_KEY : "";
    }
}
