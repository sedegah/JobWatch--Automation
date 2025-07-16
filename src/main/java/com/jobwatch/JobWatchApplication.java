package com.jobwatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jobwatch.model.Job;
import com.jobwatch.repository.JobRepository;
import com.jobwatch.service.*;

import java.util.List;

@SpringBootApplication
@EnableScheduling //  background scheduling
public class JobWatchApplication implements CommandLineRunner {

    @Autowired private JobFetcherService fetcher;
    @Autowired private GroqService ai;
    @Autowired private ResumeParserService parser;
    @Autowired private JobMatcherService matcher;
    @Autowired private JobRepository repo;

    public static void main(String[] args) {
        SpringApplication.run(JobWatchApplication.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println(" Starting Job Watch MVP...");
        System.out.println("=" + "=".repeat(50));

        updateJobsAndMatchResume();

        System.out.println("\n Job Watch MVP completed successfully!");
        System.out.println(" MongoDB saved at: mongodb://localhost:27017/jobwatch");
    }

    
    public void updateJobsAndMatchResume() {
        repo.deleteAll();

        System.out.println(" Fetching jobs from RemoteOK and JSearch...");
        List<Job> jobs = fetcher.fetchAll();

        if (jobs.isEmpty()) {
            System.out.println(" No jobs found. Please check your internet connection and API keys.");
            return;
        }
        System.out.println(" Found " + jobs.size() + " jobs");

        System.out.println(" Tagging jobs with OpenAI...");
        ai.tag(jobs);

        System.out.println(" Saving jobs to MongoDB...");
        repo.saveAll(jobs);

        System.out.println(" Parsing resume...");
        String resume = null;
        try {
            resume = parser.read("resume.docx");
        } catch (Exception e) {
            System.out.println("  Failed to read resume: " + e.getMessage());
        }

        if (resume != null && !resume.trim().isEmpty()) {
            System.out.println(" Finding best matches...");
            List<Job> matches = matcher.match(resume, jobs);
            displayResults(matches);
        } else {
            System.out.println("  Resume missing or empty. Skipping matching step.");
        }

        displayStatistics(jobs);
    }

    private void displayResults(List<Job> matches) {
        System.out.println("\n TOP 10 JOB MATCHES:");
        System.out.println("=" + "=".repeat(80));

        matches.stream()
            .limit(10)
            .forEach(job -> {
                System.out.printf("%.1f%% | %s at %s%n",
                    job.getMatchScore(), job.getTitle(), job.getCompany());
                System.out.println("       " + job.getLocation());
                System.out.println("        Tags: " + String.join(", ", job.getTags()));
                System.out.println("       " + job.getApplyUrl());
                System.out.println("       Source: " + job.getSource());
                System.out.println();
            });
    }

    private void displayStatistics(List<Job> jobs) {
        System.out.println("\n JOB STATISTICS:");
        System.out.println("=" + "=".repeat(30));

        long remoteOkJobs = jobs.stream().filter(j -> "RemoteOK".equals(j.getSource())).count();
        long jSearchJobs = jobs.stream().filter(j -> "JSearch".equals(j.getSource())).count();

        System.out.println(" Total Jobs: " + jobs.size());
        System.out.println(" RemoteOK Jobs: " + remoteOkJobs);
        System.out.println("JSearch Jobs: " + jSearchJobs);

        System.out.println("\nTop Companies:");
        jobs.stream()
            .collect(java.util.stream.Collectors.groupingBy(Job::getCompany,
                java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(5)
            .forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " jobs"));

        System.out.println("\n  Most Common Tags:");
        jobs.stream()
            .flatMap(job -> job.getTags().stream())
            .collect(java.util.stream.Collectors.groupingBy(tag -> tag,
                java.util.stream.Collectors.counting()))
            .entrySet().stream()
            .sorted(java.util.Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)
            .forEach(entry -> System.out.println("   " + entry.getKey() + ": " + entry.getValue() + " jobs"));
    }
}
