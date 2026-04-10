package com.bite.judge.sandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class ManageDockerPool {

    private static final Logger log = LoggerFactory.getLogger(ManageDockerPool.class);

    @Value("${judge.pool.size:3}")
    private int poolSize;

    @Value("${judge.docker-image:eclipse-temurin:17-jdk-alpine}")
    private String dockerImage;

    private final List<String> containerNames = new ArrayList<>();
    private final BlockingQueue<String> availableContainers = new LinkedBlockingQueue<>();

    @PostConstruct
    public void initPool() {
        log.info("Start creating docker pool. size={}, image={}", poolSize, dockerImage);
        for (int i = 0; i < poolSize; i++) {
            String name = "oj-judge-pool-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            String command = String.format(
                    "docker run -d --rm --name %s --entrypoint tail %s -f /dev/null",
                    name,
                    dockerImage
            );
            try {
                ExecResult result = exec(command);
                if (result.exitCode == 0) {
                    containerNames.add(name);
                    availableContainers.offer(name);
                    log.info("Docker container created: {} ({})", name, result.stdout.trim());
                } else {
                    log.error("Failed to create docker container: {} error={}", name, result.stderr);
                }
            } catch (Exception e) {
                log.error("Create docker container exception: {}", name, e);
            }
        }
        log.info("Docker pool init finished. created={}", containerNames.size());
    }

    @PreDestroy
    public void destroyPool() {
        log.info("Start destroying docker pool. count={}", containerNames.size());
        for (String containerName : containerNames) {
            String command = "docker rm -f " + containerName;
            try {
                ExecResult result = exec(command);
                if (result.exitCode == 0) {
                    log.info("Docker container removed: {}", containerName);
                } else {
                    log.warn("Failed to remove docker container: {} error={}", containerName, result.stderr);
                }
            } catch (Exception e) {
                log.warn("Remove docker container exception: {}", containerName, e);
            }
        }
        availableContainers.clear();
        containerNames.clear();
        log.info("Docker pool destroyed.");
    }

    public String borrowContainer(long timeoutMs) throws InterruptedException {
        String containerName = availableContainers.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (containerName == null) {
            log.warn("Borrow container timeout. timeoutMs={}", timeoutMs);
        }
        return containerName;
    }

    public void returnContainer(String containerName) {
        if (containerName == null || containerName.isBlank()) {
            return;
        }
        availableContainers.offer(containerName);
    }

    private ExecResult exec(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        int exitCode = process.waitFor();
        return new ExecResult(exitCode, stdout, stderr);
    }

    private String readStream(java.io.InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private static class ExecResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private ExecResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
