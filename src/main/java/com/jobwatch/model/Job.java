package com.jobwatch.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.*;

@Document(collection = "jobs")
public class Job {

    @Id
    private String id;

    @Indexed
    private String title;

    @Indexed
    private String company;

    private String location;
    private String description;
    private String applyUrl;

    @Indexed
    private String source;

    private String postedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Indexed
    private List<String> tags = new ArrayList<>();

    private double matchScore = 0.0;
    private String salaryRange;
    private String jobType;
    private String experienceLevel;
    private boolean isRemote = false;

    public Job() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Job(String title, String company, String location, String description,
               String applyUrl, String source, String postedDate) {
        this();
        this.title = title;
        this.company = company;
        this.location = location;
        this.description = description;
        this.applyUrl = applyUrl;
        this.source = source;
        this.postedDate = postedDate;
        this.createdAt = parsePostedDate(postedDate);
        this.tags = extractTagsFromDescription(description);
        this.isRemote = computeRemoteStatus(location, description);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        touch();
    }

    public String getCompany() { return company; }
    public void setCompany(String company) {
        this.company = company;
        touch();
    }

    public String getLocation() { return location; }
    public void setLocation(String location) {
        this.location = location;
        this.isRemote = computeRemoteStatus(location, this.description);
        touch();
    }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
        this.isRemote = computeRemoteStatus(this.location, description);
        this.tags = extractTagsFromDescription(description);
        touch();
    }

    public String getApplyUrl() { return applyUrl; }
    public void setApplyUrl(String applyUrl) {
        this.applyUrl = applyUrl;
        touch();
    }

    public String getSource() { return source; }
    public void setSource(String source) {
        this.source = source;
        touch();
    }

    public String getPostedDate() { return postedDate; }
    public void setPostedDate(String postedDate) {
        this.postedDate = postedDate;
        touch();
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
        touch();
    }

    public double getMatchScore() { return matchScore; }
    public void setMatchScore(double matchScore) { this.matchScore = matchScore; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) {
        this.salaryRange = salaryRange;
        touch();
    }

    public String getJobType() { return jobType; }
    public void setJobType(String jobType) {
        this.jobType = jobType;
        touch();
    }

    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
        touch();
    }

    public boolean isRemote() { return isRemote; }

    public void setRemote(boolean remote) {
        this.isRemote = remote;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addTag(String tag) {
        if (tag != null && !tag.trim().isEmpty() && !this.tags.contains(tag)) {
            this.tags.add(tag);
            touch();
        }
    }

    public void removeTag(String tag) {
        if (this.tags.remove(tag)) {
            touch();
        }
    }

    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public String getShortDescription() {
        if (description == null || description.length() <= 200) return description;
        return description.substring(0, 200) + "...";
    }

    public boolean isHighMatch() { return matchScore >= 70.0; }
    public boolean isMediumMatch() { return matchScore >= 50.0 && matchScore < 70.0; }

    public String getMatchLevel() {
        if (matchScore >= 80.0) return "Excellent";
        if (matchScore >= 70.0) return "Good";
        if (matchScore >= 50.0) return "Medium";
        if (matchScore >= 30.0) return "Low";
        return "Poor";
    }

    private LocalDateTime parsePostedDate(String postedDateStr) {
        try {
            return LocalDateTime.parse(postedDateStr); // ISO-8601
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private boolean computeRemoteStatus(String location, String description) {
        String loc = location != null ? location.toLowerCase() : "";
        String desc = description != null ? description.toLowerCase() : "";
        return loc.contains("remote") || loc.contains("anywhere") ||
               desc.contains("remote") || desc.contains("work from home");
    }

    private List<String> extractTagsFromDescription(String description) {
        List<String> keywords = List.of("java", "python", "react", "node", "spring", "docker", "aws", "typescript", "sql");
        List<String> foundTags = new ArrayList<>();
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            for (String keyword : keywords) {
                if (lowerDesc.contains(keyword)) {
                    foundTags.add(keyword);
                }
            }
        }
        return foundTags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Job job)) return false;
        return Objects.equals(title, job.title) &&
               Objects.equals(company, job.company) &&
               Objects.equals(applyUrl, job.applyUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, company, applyUrl);
    }

    @Override
    public String toString() {
        return String.format("Job{title='%s', company='%s', location='%s', source='%s', matchScore=%.1f%%}",
                title, company, location, source, matchScore);
    }

    public String toDetailedString() {
        return String.format("""
                Job Details:
                  Title: %s
                  Company: %s
                  Location: %s
                  Source: %s
                  Match Score: %.1f%%
                  Tags: %s
                  Remote: %s
                  Apply URL: %s
                """, title, company, location, source, matchScore,
                String.join(", ", tags), isRemote, applyUrl);
    }
}
