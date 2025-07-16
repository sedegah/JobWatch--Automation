package com.jobwatch.service;

import com.jobwatch.model.Job;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class GroqService {

    private static final Logger logger = LoggerFactory.getLogger(GroqService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    public void tag(List<Job> jobs) {
        for (Job job : jobs) {
            try {
                List<String> tags = generateTagsWithRetries(job, 3);
                job.setTags(tags);
                logger.info("Tagged job '{}' with {}", job.getTitle(), tags);
            } catch (Exception e) {
                logger.warn("Groq tagging failed for '{}'. Using fallback. Reason: {}", job.getTitle(), e.getMessage());
                job.setTags(generateFallbackTags(job));
            }
        }
    }

    private List<String> generateTagsWithRetries(Job job, int maxRetries) throws IOException, InterruptedException {
        int attempt = 0;
        while (attempt < maxRetries) {
            try {
                return generateTags(job);
            } catch (IOException e) {
                if (e.getMessage().contains("429")) {
                    int waitTime = (int) Math.pow(2, attempt + 1) * 1000;
                    logger.warn("Rate limited by Groq for '{}'. Retrying in {}ms", job.getTitle(), waitTime);
                    Thread.sleep(waitTime);
                    attempt++;
                } else {
                    throw e;
                }
            }
        }
        throw new IOException("Max retries reached for Groq tag generation.");
    }

    private List<String> generateTags(Job job) throws IOException {
        String prompt = String.format(
            "Generate 3 to 5 relevant short tags (like technologies, job type, experience level) for the following job. Return only comma-separated values:\n\n" +
            "Job Title: %s\nCompany: %s\nDescription: %s",
            job.getTitle(), job.getCompany(),
            job.getDescription().length() > 500
                ? job.getDescription().substring(0, 500) + "..."
                : job.getDescription()
        );

        JSONObject body = new JSONObject();
        body.put("model", "llama3-70b-8192");
        body.put("temperature", 0.7);
        body.put("max_tokens", 50);

        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.put(userMessage);
        body.put("messages", messages);

        Request request = new Request.Builder()
            .url(GROQ_URL)
            .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
            .addHeader("Authorization", "Bearer " + groqApiKey)
            .addHeader("Content-Type", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Groq API error: " + response.code());
            }

            String content = new JSONObject(response.body().string())
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            return Arrays.asList(content.split(",\\s*"));
        }
    }

    private List<String> generateFallbackTags(Job job) {
        List<String> tags = new ArrayList<>();
        String text = (job.getTitle() + " " + job.getDescription()).toLowerCase();

        if (text.contains("java")) tags.add("Java");
        if (text.contains("spring")) tags.add("Spring");
        if (text.contains("python")) tags.add("Python");
        if (text.contains("react")) tags.add("React");
        if (text.contains("node")) tags.add("Node.js");
        if (text.contains("aws")) tags.add("AWS");
        if (text.contains("docker")) tags.add("Docker");
        if (text.contains("fullstack")) tags.add("Fullstack");
        if (text.contains("remote")) tags.add("Remote");

        if (tags.isEmpty()) tags.addAll(List.of("Tech", "Software"));

        return tags;
    }
}
