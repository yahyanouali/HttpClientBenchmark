package com.example;

import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import lombok.extern.slf4j.Slf4j;

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

/**
 * Hello world!
 *
 */
@Slf4j
public class App {

    public static final int NUMBER_OF_CALLS = 10;

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        Uni.createFrom().item("Hello World!").subscribe().with(log::info);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://jsonplaceholder.typicode.com/users"))
                .version(HttpClient.Version.HTTP_2)
                .headers("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        runSynchronously(request);
        printSeparator();
        runWithPlatformThreads(request);
        printSeparator();
        runWithVirtualThreads(request);
        printSeparator();
        runWithMutiny(request);
        printSeparator();
        runWithVirtualThreadsMutiny(request);
    }

    // Variant 1: Synchronous calls
    public static void runSynchronously(HttpRequest request) throws IOException, InterruptedException {
        long startTime = System.nanoTime();
        try(HttpClient client = HttpClient.newBuilder().build()) {
            for (int i = 0; i < NUMBER_OF_CALLS; i++) {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                log.info("Synchronous call: {}", response.body().length());
            }
        }

        log.info("Synchronous Execution Time: {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }

    // Variant 2: Without Virtual Threads, using default Executors
    public static void runWithPlatformThreads(HttpRequest request) {
        long startTime = System.nanoTime();
        try (ExecutorService executor = Executors.newFixedThreadPool(10);
             HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < NUMBER_OF_CALLS; i++) {
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApplyAsync(response -> {
                            log.info("Default Thread Async call: {}", response.body().length());
                            return response;
                        }, executor);
                futures.add(future);
            }

            futures.forEach(CompletableFuture::join);
        }
        log.info("Default Thread Async Execution Time: {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }

    // Variant 3: Using Virtual Threads for asynchronous calls
    @RunOnVirtualThread
    public static void runWithVirtualThreads(HttpRequest request) {
        long startTime = System.nanoTime();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
             HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
            for (int i = 0; i < NUMBER_OF_CALLS; i++) {
                CompletableFuture<HttpResponse<String>> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApplyAsync(response -> {
                            log.info("Virtual Thread Async call: {}", response.body().length());
                            return response;
                        }, executor);
                futures.add(future);
            }

            futures.forEach(CompletableFuture::join);
        }
        log.info("Virtual Thread Async Execution Time: {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }

    // Variant 4: Using Mutiny for asynchronous processing
    public static void runWithMutiny(HttpRequest request) {
        long startTime = System.nanoTime();
        try(HttpClient client = HttpClient.newBuilder().build()) {

            List<Uni<HttpResponse<String>>> unis = new ArrayList<>();
            for (int i = 0; i < NUMBER_OF_CALLS; i++) {
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

        log.info("Mutiny Execution Time: {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
    }

    // Variant 5: Using Virtual Threads combined with Mutiny
    @RunOnVirtualThread
    public static void runWithVirtualThreadsMutiny(HttpRequest request) {
        long startTime = System.nanoTime();
        try(ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            HttpClient client = HttpClient.newBuilder().executor(executor).build()) {

            List<Uni<HttpResponse<String>>> unis = new ArrayList<>();
            for (int i = 0; i < NUMBER_OF_CALLS; i++) {
                Uni<HttpResponse<String>> uni = Uni.createFrom().completionStage(client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                        .onItem().invoke(response -> log.info("Virtual Thread + Mutiny Async call: {}", response.body().length()));
                unis.add(uni);
            }

            Uni.combine().all().unis(unis).with(responses -> {
                log.info("Virtual Thread + Mutiny combined responses: {}", responses.size());
                return responses;
            }).await().indefinitely();
        }

        log.info("Virtual Thread + Mutiny Execution Time: {} ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));

    }

    private static void printSeparator() {
        System.out.println("=========================================================================================");
    }
}
