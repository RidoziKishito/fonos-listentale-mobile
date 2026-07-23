package com.example.fonoss.data.network;

import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;

import retrofit2.http.Query;

public interface SupabaseApiService {

    @POST("rest/v1/books")
    @Headers({
        "Prefer: resolution=merge-duplicates",
        "Content-Type: application/json"
    })
    Call<ResponseBody> upsertBook(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken,
        @Query("on_conflict") String onConflict,
        @Body Map<String, Object> bookData
    );
}
