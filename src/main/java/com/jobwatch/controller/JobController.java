package com.jobwatch.controller;

import com.jobwatch.model.Job;
import com.jobwatch.service.JobFetcherService;
import com.jobwatch.service.JobMatcherService;
import com.jobwatch.repository.JobRepository;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobRepository jobRepository;
    private final JobFetcherService jobFetcherService;
    private final JobMatcherService jobMatcherService;

    @Autowired
    public JobController(JobRepository jobRepository,
                         JobFetcherService jobFetcherService,
                         JobMatcherService jobMatcherService) {
        this.jobRepository = jobRepository;
        this.jobFetcherService = jobFetcherService;
        this.jobMatcherService = jobMatcherService;
    }

    // Get all jobs
    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    // Search by title or description
    @GetMapping("/search")
    public List<Job> searchJobs(@RequestParam("q") String query) {
        return jobRepository.searchJobs(query);
    }

    // Get job by ID
    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable String id) {
        Optional<Job> job = jobRepository.findById(id);
        return job.map(ResponseEntity::ok)
                  .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Trigger manual fetch from all sources
    @PostMapping("/fetch")
    public ResponseEntity<List<Job>> fetchAndSaveJobs() {
        List<Job> jobs = jobFetcherService.fetchAll();
        jobs.removeIf(job -> jobRepository.existsByApplyUrl(job.getApplyUrl()));
        jobRepository.saveAll(jobs);
        return ResponseEntity.ok(jobs);
    }

    // Match jobs to a resume file
    @PostMapping("/match")
    public ResponseEntity<List<Job>> matchJobsToResume(@RequestParam(defaultValue = "resume.docx") String resumePath) {
        String resumeText = readResumeText(resumePath);
        List<Job> allJobs = jobRepository.findAll();
        List<Job> matched = jobMatcherService.match(resumeText, allJobs);
        return ResponseEntity.ok(matched);
    }

    // Get statistics
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long total = jobRepository.count();
        long remote = jobRepository.countRemoteJobs();
        long highMatch = jobRepository.countByMinMatchScore(70.0);
        return ResponseEntity.ok(new StatsResponse(total, remote, highMatch));
    }

    // Delete jobs older than given date
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupOldJobs(@RequestParam String olderThan) {
        LocalDateTime threshold = LocalDateTime.parse(olderThan);
        jobRepository.deleteByCreatedAtBefore(threshold);  // Assuming return type is void
        return ResponseEntity.ok("Old jobs deleted.");
    }

    // Utility method to read resume content
    private String readResumeText(String path) {
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resume from: " + path, e);
        }
    }

    // DTO for stats
    static class StatsResponse {
        public long total;
        public long remote;
        public long highMatch;

        public StatsResponse(long total, long remote, long highMatch) {
            this.total = total;
            this.remote = remote;
            this.highMatch = highMatch;
        }
    }
}
