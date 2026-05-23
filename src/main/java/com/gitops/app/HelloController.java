package com.gitops.app;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
public class HelloController {

    @Autowired
    private BuildProperties buildProperties;

    private static final Random RANDOM = new SecureRandom();

    // Loaded once at startup from the txt files on the classpath
    private List<String> funFacts = new ArrayList<>();
    private List<Mood>   moods    = new ArrayList<>();

    private record Mood(String face, String label) {}

    // -------------------------------------------------------------------------
    // @PostConstruct runs once after Spring has wired all dependencies.
    // We load both files here so:
    //   - It happens only once (not on every request)
    //   - Any file-not-found error surfaces at startup, not mid-request
    //   - Adding new jokes = edit the txt file, rebuild, done. No Java changes.
    // -------------------------------------------------------------------------
    @PostConstruct
    public void loadData() {
        funFacts = loadLines("fun-facts.txt");
        moods    = loadMoods("moods.txt");
    }

    /**
     * Reads every non-blank line from a classpath resource file
     * into a List<String>.
     */
    private List<String> loadLines(String filename) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new ClassPathResource(filename).getInputStream(),
                    StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line.trim());
                }
            }
        } catch (Exception e) {
            // Log and fall back gracefully — app still starts even if file is missing
            System.err.println("Warning: could not load " + filename + " — " + e.getMessage());
            lines.add("The fun facts file is missing. Someone forgot to commit it.");
        }
        return lines;
    }

    /**
     * Reads moods.txt where each line is: face|label
     * Splits on '|' and builds a List<Mood>.
     */
    private List<Mood> loadMoods(String filename) {
        List<Mood> result = new ArrayList<>();
        for (String line : loadLines(filename)) {
            String[] parts = line.split("\\|", 2);
            if (parts.length == 2) {
                result.add(new Mood(parts[0].trim(), parts[1].trim()));
            }
        }
        if (result.isEmpty()) {
            result.add(new Mood("(；一_一)", "moods.txt failed to load"));
        }
        return result;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hello")
    public ResponseEntity<Map<String, Object>> hello() {

        String builtAt = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                .withZone(ZoneId.of("UTC"))
                .format(buildProperties.getTime());

        String serverTime = DateTimeFormatter
                .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());

        String clusterTruth = funFacts.get(RANDOM.nextInt(funFacts.size()));
        Mood   mood         = moods.get(RANDOM.nextInt(moods.size()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message",       "Hello from the GitOps Pipeline!");
        response.put("application",   buildProperties.getName());
        response.put("version",       buildProperties.getVersion());
        response.put("built_at",      builtAt);
        response.put("server_time",   serverTime);
        response.put("status",        "running");
        response.put("cluster_truth", clusterTruth);
        response.put("current_mood",  mood.face());
        response.put("mood_reason",   mood.label());

        return ResponseEntity.ok(response);
    }
}