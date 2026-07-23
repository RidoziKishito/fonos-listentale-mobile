package com.example.fonoss.data.network;

import com.example.fonoss.data.network.model.ChatRequest;
import com.example.fonoss.data.network.model.ChatResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChatApiService {
    @POST("chat")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
}
