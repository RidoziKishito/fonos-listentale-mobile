package com.example.fonoss.data.network.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ChatResponse {
    @SerializedName("answer")
    private String answer;

    @SerializedName("sources")
    private List<Source> sources;

    @SerializedName("tools_used")
    private List<String> toolsUsed;

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }

    public static class Source {
        @SerializedName("chapter_number")
        private int chapterNumber;

        @SerializedName("chunk_id")
        private String chunkId;

        @SerializedName("excerpt")
        private String excerpt;

        public int getChapterNumber() {
            return chapterNumber;
        }

        public void setChapterNumber(int chapterNumber) {
            this.chapterNumber = chapterNumber;
        }

        public String getChunkId() {
            return chunkId;
        }

        public void setChunkId(String chunkId) {
            this.chunkId = chunkId;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public void setExcerpt(String excerpt) {
            this.excerpt = excerpt;
        }
    }
}
