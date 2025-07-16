package com.jobwatch.service;

import com.jobwatch.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobMatcherService {

    private static final Logger logger = LoggerFactory.getLogger(JobMatcherService.class);

    // Main method to compute and return sorted job matches
    public List<Job> match(String resume, List<Job> jobs) {
        logger.info(" Matching resume against {} jobs", jobs.size());

        for (Job job : jobs) {
            double score = computeMatchScore(resume, job);
            job.setMatchScore(score);
        }

        return jobs.stream()
            .sorted((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()))
            .collect(Collectors.toList());
    }

    private double computeMatchScore(String resume, Job job) {
        String resumeLower = resume.toLowerCase();
        String jobText = (job.getTitle() + " " + job.getDescription() + " " + 
                          String.join(" ", job.getTags())).toLowerCase();

        String[] skills = {
            "java", "spring", "javascript", "react", "node", "mongodb", 
            "postgresql", "docker", "kubernetes", "aws", "azure", "rest",
            "microservices", "git", "maven", "python", "angular", "vue"
        };

        int matches = 0;
        int totalSkills = skills.length;

        for (String skill : skills) {
            if (resumeLower.contains(skill) && jobText.contains(skill)) {
                matches++;
            }
        }

        double skillScore = (matches / (double) totalSkills) * 70;

        // Role-based bonus: matching seniority and title
        double titleBonus = 0;
        if (resumeLower.contains("senior") && jobText.contains("senior")) titleBonus += 10;
        if (resumeLower.contains("engineer") && jobText.contains("engineer")) titleBonus += 10;

        // Experience-based bonus (very basic)
        double experienceBonus = 0;
        if ((resumeLower.contains("3") || resumeLower.contains("years")) &&
            (jobText.contains("3") || jobText.contains("experience"))) {
            experienceBonus += 10;
        }

        double finalScore = skillScore + titleBonus + experienceBonus;

        logger.debug("Computed score for job '{}': {:.1f} (skills={}, titleBonus={}, expBonus={})",
            job.getTitle(), finalScore, skillScore, titleBonus, experienceBonus);

        return Math.min(100, finalScore);
    }
}
