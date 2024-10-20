package com.example;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Benchmark {

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/users"))
                .version(HttpClient.Version.HTTP_2)
                .headers("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        // Define different values for NUMBER_OF_CALLS
        int[] callCounts = {10, 30, 50, 70, 90};

        // Initialize a 2D array to store execution times (6 columns for the 5 methods plus call counts)
        long[][] executionTimes = new long[callCounts.length][6];

        // Execute methods for different call counts
        for (int i = 0; i < callCounts.length; i++) {
            executionTimes[i][0] = callCounts[i]; // Store the call count

            executionTimes[i][1] = runSynchronously(request, callCounts[i]);
            executionTimes[i][2] = runWithPlatformThreads(request, callCounts[i]);
            executionTimes[i][3] = runWithVirtualThreads(request, callCounts[i]);
            executionTimes[i][4] = runWithMutiny(request, callCounts[i]);
            executionTimes[i][5] = runWithVirtualThreadsMutiny(request, callCounts[i]);
        }

        // Print the results in a tabular format
        printResults(callCounts, executionTimes);
        // Save the results to a CSV file
        saveResultsToCSV(callCounts, executionTimes);
    }

    private static long runSynchronously(HttpRequest request, int numberOfCalls) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        try (HttpClient client = HttpClient.newBuilder().build()) {
            for (int i = 0; i < numberOfCalls; i++) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Synchronous call: {}", response.body().length());
            }
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    private static long runWithPlatformThreads(HttpRequest request, int numberOfCalls) {
        long startTime = System.nanoTime();
        try (ExecutorService executor = Executors.newFixedThreadPool(10);
             HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfCalls; i++) {
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApplyAsync(response -> {
                            log.info("Default Thread Async call: {}", response.body().length());
                            return response;
                        }, executor);
                futures.add(future);
            }

            futures.forEach(CompletableFuture::join);
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    @RunOnVirtualThread
    private static long runWithVirtualThreads(HttpRequest request, int numberOfCalls) {
        long startTime = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < numberOfCalls; i++) {
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApplyAsync(response -> {
                            log.info("Virtual Thread Async call: {}", response.body().length());
                            return response;
                        }, executor);
                futures.add(future);
            }

            futures.forEach(CompletableFuture::join);
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    private static long runWithMutiny(HttpRequest request, int numberOfCalls) {
        long startTime = System.nanoTime();
        try (HttpClient client = HttpClient.newBuilder().build()) {

            List<Uni<HttpResponse<String>>> unis = new ArrayList<>();
            for (int i = 0; i < numberOfCalls; i++) {
                Uni<HttpResponse<String>> uni = Uni.createFrom()
                        .completionStage(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                        .onItem().invoke(response -> log.info("Mutiny Async call: {}", response.body().length()));
                unis.add(uni);
            }

            Uni.combine().all().unis(unis).with(responses -> {
                log.info("Mutiny combined responses: {}", responses.size());
                return responses;
            }).await().indefinitely();
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    @RunOnVirtualThread
    private static long runWithVirtualThreadsMutiny(HttpRequest request, int numberOfCalls) {
        long startTime = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<Uni<HttpResponse<String>>> unis = new ArrayList<>();
            for (int i = 0; i < numberOfCalls; i++) {
                Uni<HttpResponse<String>> uni = Uni.createFrom()
                        .completionStage(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                        .onItem().invoke(response -> log.info("Virtual Thread + Mutiny Async call: {}", response.body().length()));
                unis.add(uni);
            }

            Uni.combine().all().unis(unis).with(responses -> {
                log.info("Virtual Thread + Mutiny combined responses: {}", responses.size());
                return responses;
            }).await().indefinitely();
        }
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
    }

    private static void printResults(int[] callCounts, long[][] executionTimes) {
        // Print headers
        System.out.printf("%-15s %-25s %-25s %-25s %-25s %-25s%n", "Number of Calls", "Sync Time (ms)", "Platform Thread Time (ms)", "Virtual Thread Time (ms)", "Mutiny Time (ms)", "Virtual + Mutiny Time (ms)");
        System.out.println("===========================================================================================================");
        // Print results
        for (int i = 0; i < callCounts.length; i++) {
            System.out.printf("%-15d %-25d %-25d %-25d %-25d %-25d%n",
                    executionTimes[i][0],
                    executionTimes[i][1],
                    executionTimes[i][2],
                    executionTimes[i][3],
                    executionTimes[i][4],
                    executionTimes[i][5]);
        }
    }

    private static void saveResultsToCSV(int[] callCounts, long[][] executionTimes) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("benchmark_results.csv"))) {
            // Write header
            writer.write("Number of Calls, Sync Time (ms), Platform Thread Time (ms), Virtual Thread Time (ms), Mutiny Time (ms), Virtual + Mutiny Time (ms)");
            writer.newLine();

            // Write data
            for (int i = 0; i < callCounts.length; i++) {
                writer.write(String.format("%d, %d, %d, %d, %d, %d",
                        executionTimes[i][0],
                        executionTimes[i][1],
                        executionTimes[i][2],
                        executionTimes[i][3],
                        executionTimes[i][4],
                        executionTimes[i][5]));
                writer.newLine();
            }
            log.info("Results saved to benchmark_results.csv");
        } catch (IOException e) {
            log.error("Error writing results to CSV file: {}", e.getMessage());
        }
    }
}
