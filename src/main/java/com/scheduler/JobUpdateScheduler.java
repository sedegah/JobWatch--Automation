package com.scheduler;

import com.jobwatch.model.Job;
import com.jobwatch.repository.JobRepository;
import com.jobwatch.service.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JobUpdateScheduler {

    private final JobFetcherService fetcher;
    private final GroqService ai;
    private final ResumeParserService parser;
    private final JobMatcherService matcher;
    private final JobRepository repo;

    public JobUpdateScheduler(
        JobFetcherService fetcher,
        GroqService ai,
        ResumeParserService parser,
        JobMatcherService matcher,
        JobRepository repo
    ) {
        this.fetcher = fetcher;
        this.ai = ai;
        this.parser = parser;
        this.matcher = matcher;
        this.repo = repo;
    }

    @Scheduled(cron = "0 0 * * * *") // Every hour on the hour
    public void scheduledJobRefresh() {
        System.out.println(" [Scheduler] Running JobWatch hourly refresh...");
        System.out.println("=".repeat(60));

        try {
            repo.deleteAll();

            List<Job> jobs = fetcher.fetchAll();
            System.out.println("Fetched " + jobs.size() + " jobs");

            if (jobs.isEmpty()) return;

            ai.tag(jobs);

            repo.saveAll(jobs);

            String resume = parser.read("resume.docx");

            if (resume != null && !resume.trim().isEmpty()) {
                List<Job> matches = matcher.match(resume, jobs);
                System.out.println("Top 5 Matches:");
                matches.stream().limit(5).forEach(job ->
                    System.out.println(" OK " + job.getTitle() + " at " + job.getCompany()));
            }

        } catch (Exception e) {
            System.out.println(" [Scheduler] Error occurred: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println(" [Scheduler] JobWatch refresh completed.");
    }
}
