package pc.crawler;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WebCrawlerParallel {

    private static final CrawlTask POISON_PILL = new CrawlTask("__POISON__", -1);

    private static class CrawlTask {
        private final String url;
        private final int depth;

        private CrawlTask(String url, int depth) {
            this.url = url;
            this.depth = depth;
        }
    }

    public static void main(String[] args) {
        String baseUrl = "https://www-licence.ufr-info-p6.jussieu.fr/lmd/licence/2023/ue/LU3IN001-2023oct/index.php";
        Path outputDir = Paths.get("tmp", "crawler");

        int maxDepth = parseOrDefault(args, 0, 2);
        int workerCount = parseOrDefault(args, 1, Runtime.getRuntime().availableProcessors());

        BlockingQueue<CrawlTask> queue = new LinkedBlockingQueue<>();
        ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
        ActivityMonitor activityMonitor = new ActivityMonitor();
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);

        long startNanos = System.nanoTime();

        try {
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }

            visitedUrls.put(baseUrl, Boolean.TRUE);
            activityMonitor.taskStarted();
            queue.put(new CrawlTask(baseUrl, maxDepth));

            for (int i = 0; i < workerCount; i++) {
                executor.submit(() -> workerLoop(queue, visitedUrls, activityMonitor, baseUrl, outputDir));
            }

            activityMonitor.awaitCompletion();

            for (int i = 0; i < workerCount; i++) {
                queue.put(POISON_PILL);
            }
        } catch (IOException e) {
            System.err.println("Error initializing crawler: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Crawler interrupted.");
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        System.out.println("Parallel crawling completed.");
        System.out.println("Depth=" + maxDepth + ", workers=" + workerCount + ", elapsed=" + elapsedMillis + " ms");
    }

    private static void workerLoop(
            BlockingQueue<CrawlTask> queue,
            ConcurrentHashMap<String, Boolean> visitedUrls,
            ActivityMonitor activityMonitor,
            String baseUrl,
            Path outputDir) {
        while (true) {
            CrawlTask task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (task == POISON_PILL) {
                return;
            }

            try {
                List<String> extractedUrls = WebCrawlerUtils.processUrl(task.url, baseUrl, outputDir);

                if (task.depth > 0) {
                    for (String extractedUrl : extractedUrls) {
                        if (visitedUrls.putIfAbsent(extractedUrl, Boolean.TRUE) == null) {
                            activityMonitor.taskStarted();
                            queue.put(new CrawlTask(extractedUrl, task.depth - 1));
                        }
                    }
                }
            } catch (URISyntaxException | IOException e) {
                System.err.println("Error while processing " + task.url + ": " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } finally {
                activityMonitor.taskCompleted();
            }
        }
    }

    private static int parseOrDefault(String[] args, int index, int defaultValue) {
        if (args.length <= index) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(args[index]);
            return value >= 0 ? value : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
