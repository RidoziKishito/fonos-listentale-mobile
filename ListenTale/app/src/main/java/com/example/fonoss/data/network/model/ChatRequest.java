package com.example.fonoss.data.network.model;

import com.google.gson.annotations.SerializedName;

public class ChatRequest {
    @SerializedName("book_id")
    private String bookId;

    @SerializedName("query")
    private String query;

    @SerializedName("llm_provider")
    private String llmProvider;

    public ChatRequest(String bookId, String query) {
        this.bookId = bookId;
        this.query = query;
        this.llmProvider = "deepseek";
    }

    public ChatRequest(String bookId, String query, String llmProvider) {
        this.bookId = bookId;
        this.query = query;
        this.llmProvider = (llmProvider != null && !llmProvider.isEmpty()) ? llmProvider : "deepseek";
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }
}
