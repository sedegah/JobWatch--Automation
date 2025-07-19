package com.jobwatch.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jobwatch.model.Job;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class JobFetcherService {

    private static final Logger logger = LoggerFactory.getLogger(JobFetcherService.class);

    @Value("${rapidapi.key}")
    private String rapidApiKey;

    @Value("${rapidapi.host}")
    private String rapidApiHost;

    @Value("${adzuna.app_id:}")
    private String adzunaAppId;

    @Value("${adzuna.api_key:}")
    private String adzunaApiKey;

    @Value("${jobwatch.fetch.remoteok.enabled:true}")
    private boolean remoteOkEnabled;

    @Value("${jobwatch.fetch.jsearch.enabled:true}")
    private boolean jSearchEnabled;

    @Value("${jobwatch.fetch.adzuna.enabled:true}")
    private boolean adzunaEnabled;

    @Value("${jobwatch.fetch.weworkremotely.enabled:true}")
    private boolean weWorkRemotelyEnabled;

    @Value("${jobwatch.fetch.hubstaff.enabled:true}")
    private boolean hubstaffEnabled;

    @Value("${jobwatch.fetch.remotewoman.enabled:true}")
    private boolean remoteWomanEnabled;

    @Value("${jobwatch.fetch.wellfound.enabled:true}")
    private boolean wellfoundEnabled;

    @Value("${jobwatch.fetch.remotive.enabled:true}")
    private boolean remotiveEnabled;

    @Value("${jobwatch.fetch.remotecircle.enabled:true}")
    private boolean remoteCircleEnabled;

    @Value("${jobwatch.fetch.workwave.enabled:true}")
    private boolean workWaveEnabled;

    @Value("${jobwatch.fetch.aijobs.enabled:true}")
    private boolean aiJobsEnabled;

    @Value("${jobwatch.fetch.toptal.enabled:true}")
    private boolean toptalEnabled;

    @Value("${jobwatch.fetch.flexjobs.enabled:true}")
    private boolean flexJobsEnabled;

    @Value("${jobwatch.fetch.jsremotely.enabled:true}")
    private boolean jsRemotelyEnabled;

    @Value("${jobwatch.fetch.otta.enabled:true}")
    private boolean ottaEnabled;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public List<Job> fetchAll() {
        logger.info("Fetching all remote jobs...");
        List<Job> jobs = new ArrayList<>();

        try {
            jobs.addAll(fetchJobgether());
        } catch (Exception e) {
            logger.warn("Jobgether fetch failed: " + e.getMessage());
        }

        if (remoteOkEnabled) {
            try {
                jobs.addAll(fetchRemoteOK());
            } catch (Exception e) {
                logger.warn("RemoteOK fetch failed: " + e.getMessage());
            }
        }

        if (jSearchEnabled) {
            try {
                jobs.addAll(fetchAllJSearch());
            } catch (Exception e) {
                logger.warn("JSearch fetch failed: " + e.getMessage());
            }
        }

        if (adzunaEnabled && !adzunaAppId.isBlank() && !adzunaApiKey.isBlank()) {
            try {
                jobs.addAll(fetchAdzuna());
            } catch (Exception e) {
                logger.warn("Adzuna fetch failed: " + e.getMessage());
            }
        }

        if (weWorkRemotelyEnabled) {
            try {
                jobs.addAll(fetchWeWorkRemotely());
            } catch (Exception e) {
                logger.warn("WeWorkRemotely fetch failed: " + e.getMessage());
            }
        }

        if (hubstaffEnabled) {
            try {
                jobs.addAll(fetchHubstaffTalent());
            } catch (Exception e) {
                logger.warn("Hubstaff Talent fetch failed: " + e.getMessage());
            }
        }

        if (remoteWomanEnabled) {
            try {
                jobs.addAll(fetchRemoteWoman());
            } catch (Exception e) {
                logger.warn("Remote Woman fetch failed: " + e.getMessage());
            }
        }

        if (wellfoundEnabled) {
            try {
                jobs.addAll(fetchWellfound());
            } catch (Exception e) {
                logger.warn("Wellfound fetch failed: " + e.getMessage());
            }
        }

        if (remotiveEnabled) {
            try {
                jobs.addAll(fetchRemotive());
            } catch (Exception e) {
                logger.warn("Remotive fetch failed: " + e.getMessage());
            }
        }

        if (remoteCircleEnabled) {
            try {
                jobs.addAll(fetchRemoteCircle());
            } catch (Exception e) {
                logger.warn("Remote Circle fetch failed: " + e.getMessage());
            }
        }

        if (workWaveEnabled) {
            try {
                jobs.addAll(fetchWorkWave());
            } catch (Exception e) {
                logger.warn("WorkWave fetch failed: " + e.getMessage());
            }
        }

        if (aiJobsEnabled) {
            try {
                jobs.addAll(fetchAIJobs());
            } catch (Exception e) {
                logger.warn("AI Jobs fetch failed: " + e.getMessage());
            }
        }

        if (toptalEnabled) {
            try {
                jobs.addAll(fetchToptal());
            } catch (Exception e) {
                logger.warn("Toptal fetch failed: " + e.getMessage());
            }
        }

        if (flexJobsEnabled) {
            try {
                jobs.addAll(fetchFlexJobs());
            } catch (Exception e) {
                logger.warn("FlexJobs fetch failed: " + e.getMessage());
            }
        }

        if (jsRemotelyEnabled) {
            try {
                jobs.addAll(fetchJSRemotely());
            } catch (Exception e) {
                logger.warn("JS Remotely fetch failed: " + e.getMessage());
            }
        }

        if (ottaEnabled) {
            try {
                jobs.addAll(fetchOtta());
            } catch (Exception e) {
                logger.warn("Otta fetch failed: " + e.getMessage());
            }
        }

        jobs = removeDuplicates(jobs);
        logger.info("Deduplicated job count: " + jobs.size());

        List<Job> remoteJobs = new ArrayList<>();
        for (Job job : jobs) {
            String t = job.getTitle().toLowerCase();
            String l = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
            String d = job.getDescription() != null ? job.getDescription().toLowerCase() : "";

            if (t.contains("remote") || l.contains("remote") || d.contains("remote") || d.contains("work from home")) {
                job.setRemote(true);
                job.addTag("remote");
                remoteJobs.add(job);
            }
        }

        exportJobsToFile(remoteJobs);
        logger.info("Final remote jobs exported: " + remoteJobs.size());
        return remoteJobs;
    }

    private List<Job> fetchJobgether() throws IOException {
        logger.info("Scraping Jobgether...");
        List<Job> jobs = new ArrayList<>();
        String url = "https://jobgether.com/remote-jobs/ghana/software-engineer";

        Document doc = retryJsoup(url, 3);
        Elements cards = doc.select("div[class*=JobCard_card]");

        for (Element card : cards) {
            try {
                String title = card.select("h3").text();
                String company = card.select("h4").text();
                String location = "Remote - Ghana";
                String desc = card.select("div[class*=description]").text();
                String href = card.select("a[href]").attr("href");
                if (!href.startsWith("http")) {
                    href = "https://jobgether.com" + href;
                }
                String posted = LocalDateTime.now().toString();
                jobs.add(new Job(title, company, location, desc, href, "Jobgether", posted));
            } catch (Exception ignored) {
            }
        }

        logger.info("Fetched " + jobs.size() + " jobs from Jobgether");
        return jobs;
    }

    private List<Job> fetchRemoteOK() throws IOException {
        List<Job> out = new ArrayList<>();
        String[] urls = {
                "https://remoteok.io/remote-dev-jobs",
                "https://remoteok.io/remote-software-jobs",
                "https://remoteok.io/remote-programming-jobs"
        };
        for (String url : urls) {
            logger.info("Scraping RemoteOK: " + url);
            Document d = retryJsoup(url, 3);
            Elements els = d.select("tr.job");
            for (Element e : els) {
                if (!e.attr("data-tags").contains("remote"))
                    continue;
                try {
                    String title = e.select("h2").text();
                    String company = e.select("h3").text();
                    String location = e.select("div.location").text();
                    String desc = e.select("td.description").text();
                    String apply = "https://remoteok.io" + e.select("a.preventLink").attr("href");
                    String date = e.select("time").attr("datetime");
                    out.add(new Job(title, company, location, desc, apply, "RemoteOK", date));
                } catch (Exception ignored) {
                }
            }
        }
        return out;
    }

    private List<Job> fetchAllJSearch() throws IOException {
        List<Job> out = new ArrayList<>();
        String[] queries = {
                "remote software engineer", "remote full stack developer",
                "remote data engineer", "remote backend engineer"
        };
        for (String q : queries) {
            out.addAll(fetchJSearch(q));
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        return out;
    }

    private List<Job> fetchJSearch(String query) throws IOException {
        List<Job> out = new ArrayList<>();
        String url = "https://jsearch.p.rapidapi.com/search?query=" +
                query.replace(" ", "%20") + "&page=1&num_pages=1";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("X-RapidAPI-Key", rapidApiKey)
                .addHeader("X-RapidAPI-Host", rapidApiHost)
                .build();

        try (Response r = client.newCall(req).execute()) {
            if (!r.isSuccessful() || r.body() == null)
                throw new IOException("Code: " + r.code());
            JSONArray arr = new JSONObject(r.body().string()).getJSONArray("data");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (!obj.optBoolean("job_is_remote", false))
                    continue;
                String title = obj.optString("job_title");
                String comp = obj.optString("employer_name");
                String loc = obj.optString("job_city") + ", " + obj.optString("job_country");
                String desc = obj.optString("job_description");
                String app = obj.optString("job_apply_link");
                String date = obj.optString("job_posted_at_datetime_utc");
                out.add(new Job(title, comp, loc, desc, app, "JSearch", date));
            }
        }

        return out;
    }

    private List<Job> fetchAdzuna() throws IOException {
        List<Job> out = new ArrayList<>();
        String url = String.format(
                "https://api.adzuna.com/v1/api/jobs/gb/search/1?app_id=%s&app_key=%s&what=software%%20developer&where=remote",
                adzunaAppId, adzunaApiKey);

        Request req = new Request.Builder().url(url).build();

        try (Response r = client.newCall(req).execute()) {
            if (!r.isSuccessful() || r.body() == null)
                throw new IOException("Adzuna code " + r.code());
            JSONArray arr = new JSONObject(r.body().string()).getJSONArray("results");

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String title = o.optString("title");
                String comp = o.getJSONObject("company").optString("display_name");
                String loc = o.getJSONObject("location").optString("display_name");
                String desc = o.optString("description");
                String app = o.optString("redirect_url");
                String date = o.optString("created");
                out.add(new Job(title, comp, loc, desc, app, "Adzuna", date));
            }
        }

        return out;
    }

    private List<Job> fetchWeWorkRemotely() throws IOException {
        logger.info("Fetching from We Work Remotely...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://weworkremotely.com", 3);
        Elements listings = doc.select("section.jobs li");
        for (Element job : listings) {
            if (job.className().contains("view-all")) continue;
            String link = "https://weworkremotely.com" + job.selectFirst("a").attr("href");
            Document jobPage = retryJsoup(link, 2);
            String title = jobPage.selectFirst("h1").text();
            String company = jobPage.selectFirst(".company-card h2").text();
            String desc = jobPage.select("div.listing-container").text();
            String posted = LocalDateTime.now().toString();
            jobs.add(new Job(title, company, "Remote", desc, link, "WeWorkRemotely", posted));
        }
        return jobs;
    }

    private List<Job> fetchHubstaffTalent() throws IOException {
        logger.info("Fetching from Hubstaff Talent...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://talent.hubstaff.com", 3);
        Elements cards = doc.select("div.project-row");
        for (Element card : cards) {
            String title = card.select("h3").text();
            String company = "Hubstaff Client";
            String desc = card.select("p.description").text();
            String link = "https://talent.hubstaff.com/search/projects" + card.select("a").attr("href");
            String posted = LocalDateTime.now().toString();
            jobs.add(new Job(title, company, "Remote", desc, link, "Hubstaff", posted));
        }
        return jobs;
    }

    private List<Job> fetchRemoteWoman() throws IOException {
        logger.info("Fetching from Remote Woman...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://remotewoman.com", 3);
        Elements rows = doc.select(".job_listings li");
        for (Element row : rows) {
            String title = row.select(".position h3").text();
            String company = row.select(".company h4").text();
            String link = row.select("a").attr("href");
            String desc = "Remote job listed for women";
            String posted = LocalDateTime.now().toString();
            jobs.add(new Job(title, company, "Remote", desc, link, "RemoteWoman", posted));
        }
        return jobs;
    }

    private List<Job> fetchWellfound() throws IOException {
        logger.info("Fetching from Wellfound...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://wellfound.com/jobs", 3);
        Elements cards = doc.select("div.listings div.styles_component__main__");
        for (Element card : cards) {
            try {
                String title = card.select("a.styles_component__job-title__").text();
                String company = card.select("div.styles_component__company-name__").text();
                String desc = card.select("div.styles_component__description__").text();
                String link = "https://wellfound.com" + card.select("a.styles_component__job-title__").attr("href");
                jobs.add(new Job(title, company, "Remote", desc, link, "Wellfound", LocalDateTime.now().toString()));
            } catch (Exception ignored) {
            }
        }
        return jobs;
    }

    private List<Job> fetchRemotive() throws IOException {
        logger.info("Fetching from Remotive...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://remotive.io", 2);
        Elements listings = doc.select(".job-listing");
        for (Element job : listings) {
            String title = job.select(".job-title").text();
            String company = job.select(".company").text();
            String desc = job.select(".job-description").text();
            String link = "https://remotive.io" + job.select("a").attr("href");
            jobs.add(new Job(title, company, "Remote", desc, link, "Remotive", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchRemoteCircle() throws IOException {
        logger.info("Fetching from Remote Circle...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://remotecircle.com/", 3);
        Elements cards = doc.select("div.job-card");
        for (Element card : cards) {
            String title = card.select(".job-title").text();
            String company = card.select(".company-name").text();
            String desc = card.select(".job-description").text();
            String link = card.select("a").attr("href");
            jobs.add(new Job(title, company, "Remote", desc, link, "RemoteCircle", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchWorkWave() throws IOException {
        logger.info("Fetching from WorkWave...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://www.workwave.com/about/careers", 3);
        Elements rows = doc.select("div.job-listing");
        for (Element row : rows) {
            String title = row.select("h3").text();
            String link = row.select("a").attr("href");
            String desc = "WorkWave job";
            jobs.add(new Job(title, "WorkWave", "Remote", desc, link, "WorkWave", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchAIJobs() throws IOException {
        logger.info("Fetching from AI Jobs...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://theaijobboard.com/", 3);
        Elements rows = doc.select(".job-listing");
        for (Element row : rows) {
            String title = row.select("h3").text();
            String company = row.select(".company-name").text();
            String desc = "AI job posting";
            String link = row.select("a").attr("href");
            jobs.add(new Job(title, company, "Remote", desc, link, "AIJobs", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchToptal() throws IOException {
        logger.info("Fetching from Toptal...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://www.toptal.com/careers", 3);
        Elements postings = doc.select("a.career-position");
        for (Element post : postings) {
            String title = post.select("h3").text();
            String link = "https://www.toptal.com" + post.attr("href");
            jobs.add(new Job(title, "Toptal", "Remote", "Toptal role", link, "Toptal", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchFlexJobs() throws IOException {
        logger.info("Fetching from FlexJobs...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://www.flexjobs.com/search?search=developer&location=Remote", 3);
        Elements cards = doc.select("div.job");
        for (Element card : cards) {
            String title = card.select("a.job-title").text();
            String company = card.select("div.company").text();
            String desc = card.select("div.description").text();
            String link = "https://www.flexjobs.com" + card.select("a").attr("href");
            jobs.add(new Job(title, company, "Remote", desc, link, "FlexJobs", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchJSRemotely() throws IOException {
        logger.info("Fetching from JS Remotely...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup("https://jsremotely.com/", 3);
        Elements rows = doc.select("tr.job");
        for (Element row : rows) {
            String title = row.select("h2").text();
            String company = row.select("h3").text();
            String desc = "JS Remotely posting";
            String link = "https://jsremotely.com" + row.select("a").attr("href");
            jobs.add(new Job(title, company, "Remote", desc, link, "JSRemotely", LocalDateTime.now().toString()));
        }
        return jobs;
    }

    private List<Job> fetchOtta() throws IOException {
        logger.info("Fetching from Otta...");
        List<Job> jobs = new ArrayList<>();
        Document doc = retryJsoup(" https://otta.com/", 3);
        Elements cards = doc.select("div[data-testid=JobCard]");
        for (Element card : cards) {
            try {
                String title = card.select("h3[data-testid=JobCard-title]").text();
                String company = card.select("span[data-testid=JobCard-companyName]").text();
                String location = card.select("span[data-testid=JobCard-location]").text();
                if (!location.toLowerCase().contains("remote")) continue;
                String desc = card.select("p[data-testid=JobCard-description]").text();
                String link = "https://otta.com" + card.select("a[data-testid=JobCard-link]").attr("href");
                jobs.add(new Job(title, company, "Remote", desc, link, "Otta", LocalDateTime.now().toString()));
            } catch (Exception ignored) {
            }
        }
        return jobs;
    }

    private List<Job> removeDuplicates(List<Job> jobs) {
        Map<String, Job> map = new LinkedHashMap<>();
        for (Job j : jobs) {
            String key = j.getApplyUrl() != null ? j.getApplyUrl().toLowerCase()
                    : (j.getTitle() + j.getCompany() + j.getLocation()).toLowerCase();
            map.putIfAbsent(key, j);
        }
        return new ArrayList<>(map.values());
    }

    private void exportJobsToFile(List<Job> jobs) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            String fileName = "jobs_" + LocalDateTime.now().toString().replace(":", "-") + ".json";
            mapper.writeValue(new File(fileName), jobs);
            logger.info("Exported jobs to: " + fileName);
        } catch (IOException e) {
            logger.warn("Failed to export: " + e.getMessage());
        }
    }

    private Document retryJsoup(String url, int attempts) throws IOException {
        IOException lastEx = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(60_000)
                        .get();
            } catch (IOException e) {
                lastEx = e;
                logger.warn("Retry " + (i + 1) + " failed for: " + url + " → " + e.getMessage());
            }
        }
        throw lastEx != null ? lastEx : new IOException("All retries failed and no exception thrown for: " + url);
    }
}