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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Service
public class JobFetcherService {

    private static final Logger logger = Logger.getLogger(JobFetcherService.class.getName());

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
            logger.warning("Jobgether fetch failed: " + e.getMessage());
        }

        if (remoteOkEnabled) {
            try {
                jobs.addAll(fetchRemoteOK());
            } catch (Exception e) {
                logger.warning("RemoteOK fetch failed: " + e.getMessage());
            }
        }

        if (jSearchEnabled) {
            try {
                jobs.addAll(fetchAllJSearch());
            } catch (Exception e) {
                logger.warning("JSearch fetch failed: " + e.getMessage());
            }
        }

        if (adzunaEnabled && !adzunaAppId.isBlank() && !adzunaApiKey.isBlank()) {
            try {
                jobs.addAll(fetchAdzuna());
            } catch (Exception e) {
                logger.warning("Adzuna fetch failed: " + e.getMessage());
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
            logger.warning("Failed to export: " + e.getMessage());
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
                logger.warning("Retry " + (i + 1) + " failed for: " + url + " → " + e.getMessage());
            }
        }
        throw lastEx != null ? lastEx : new IOException("All retries failed and no exception thrown for: " + url);
    }
}
