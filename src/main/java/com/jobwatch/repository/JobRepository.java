package com.jobwatch.repository;

import com.jobwatch.model.Job;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends MongoRepository<Job, String> {
    
    // Basic search queries
    List<Job> findByCompanyContainingIgnoreCase(String company);
    List<Job> findByTitleContainingIgnoreCase(String title);
    List<Job> findByLocationContainingIgnoreCase(String location);
    List<Job> findBySource(String source);
    
    // Tag-based queries
    List<Job> findByTagsContaining(String tag);
    List<Job> findByTagsIn(List<String> tags);
    
    // Remote job queries
    List<Job> findByIsRemoteTrue();
    List<Job> findByIsRemoteFalse();
    
    // Experience level queries
    List<Job> findByExperienceLevel(String experienceLevel);
    List<Job> findByExperienceLevelIn(List<String> experienceLevels);
    
    // Job type queries
    List<Job> findByJobType(String jobType);
    List<Job> findByJobTypeIn(List<String> jobTypes);
    
    // Date-based queries
    List<Job> findByCreatedAtAfter(LocalDateTime dateTime);
    List<Job> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Match score queries
    List<Job> findByMatchScoreGreaterThan(double score);
    List<Job> findByMatchScoreBetween(double minScore, double maxScore);
    
    // Combined queries
    List<Job> findByCompanyContainingIgnoreCaseAndIsRemoteTrue(String company);
    List<Job> findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(String title, String location);
    List<Job> findByTagsContainingAndMatchScoreGreaterThan(String tag, double score);
    
    // Custom queries using @Query annotation
    @Query("{ 'title': { $regex: ?0, $options: 'i' }, 'company': { $regex: ?1, $options: 'i' } }")
    List<Job> findByTitleAndCompanyRegex(String titleRegex, String companyRegex);
    
    @Query("{ 'tags': { $in: ?0 }, 'matchScore': { $gte: ?1 } }")
    List<Job> findByTagsInAndMinMatchScore(List<String> tags, double minScore);
    
    @Query("{ 'source': ?0, 'createdAt': { $gte: ?1 } }")
    List<Job> findBySourceAndCreatedAfter(String source, LocalDateTime dateTime);
    
    @Query("{ 'description': { $regex: ?0, $options: 'i' } }")
    List<Job> findByDescriptionContaining(String keyword);
    
    @Query("{ 'isRemote': ?0, 'tags': { $in: ?1 } }")
    List<Job> findByRemoteStatusAndTags(boolean isRemote, List<String> tags);
    
    // Statistical queries
    @Query(value = "{ 'source': ?0 }", count = true)
    long countBySource(String source);
    
    @Query(value = "{ 'isRemote': true }", count = true)
    long countRemoteJobs();
    
    @Query(value = "{ 'matchScore': { $gte: ?0 } }", count = true)
    long countByMinMatchScore(double minScore);
    
    // Aggregation-style queries
    @Query("{ 'company': ?0 }")
    List<Job> findAllByCompany(String company);
    
    @Query(value = "{ 'tags': { $in: ?0 } }", sort = "{ 'matchScore': -1 }")
    List<Job> findByTagsInOrderByMatchScoreDesc(List<String> tags);
    
    @Query(value = "{ 'isRemote': true }", sort = "{ 'createdAt': -1 }")
    List<Job> findRemoteJobsOrderByCreatedAtDesc();
    
    // Existence queries
    boolean existsByTitleAndCompanyAndApplyUrl(String title, String company, String applyUrl);
    boolean existsByApplyUrl(String applyUrl);
    
    // Deletion queries
    void deleteBySource(String source);
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
    long deleteByMatchScoreLessThan(double score);
    
    // Optional queries for single results
    Optional<Job> findFirstByCompanyOrderByMatchScoreDesc(String company);
    Optional<Job> findFirstByTagsContainingOrderByCreatedAtDesc(String tag);
    Optional<Job> findTopByOrderByMatchScoreDesc();
    
    // Pagination support (works with Pageable parameter)
    // Example: repository.findByIsRemoteTrue(PageRequest.of(0, 10))
    org.springframework.data.domain.Page<Job> findByIsRemoteTrue(org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Job> findByTagsContaining(String tag, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Job> findByMatchScoreGreaterThan(double score, org.springframework.data.domain.Pageable pageable);
    
    // Advanced search method (can be used for full-text search)
    @Query("{ $or: [ " +
           "{ 'title': { $regex: ?0, $options: 'i' } }, " +
           "{ 'company': { $regex: ?0, $options: 'i' } }, " +
           "{ 'description': { $regex: ?0, $options: 'i' } }, " +
           "{ 'tags': { $regex: ?0, $options: 'i' } } " +
           "] }")
    List<Job> searchJobs(String searchTerm);
    
    // Get jobs within a specific match score range, sorted by score
    @Query(value = "{ 'matchScore': { $gte: ?0, $lte: ?1 } }", 
           sort = "{ 'matchScore': -1, 'createdAt': -1 }")
    List<Job> findJobsInScoreRange(double minScore, double maxScore);
    
    // Find recent high-quality jobs
    @Query("{ 'matchScore': { $gte: ?0 }, 'createdAt': { $gte: ?1 } }")
    List<Job> findRecentHighQualityJobs(double minScore, LocalDateTime since);
}