package pc.crawler;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.io.*;


public class WebCrawler {
    public static void main(String[] args) {
        // Hardcoded base URL to start crawling from
        String baseUrl = "https://www-licence.ufr-info-p6.jussieu.fr/lmd/licence/2023/ue/LU3IN001-2023oct/index.php";
        
        // Hardcoded output directory where downloaded pages will be saved
        Path outputDir = Paths.get("/tmp/crawler/");
        
        try {
            // Ensure the output directory exists; create it if it doesn't
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
                System.out.println("Created output directory: " + outputDir.toAbsolutePath());
            }

            // Process the initial URL (depth 0)
            System.out.println("Processing (Depth 0): " + baseUrl);
            List<String> extractedUrls = Collections.emptyList();
            try {
    			extractedUrls = WebCrawlerUtils.processUrl(baseUrl, baseUrl, outputDir);
    		} catch (URISyntaxException|IOException e) {
    			System.err.println("Error during crawling: " + e.getMessage());
    		}
            		
            // Check if there are URLs extracted to process at depth 1
            if (extractedUrls.isEmpty()) {
                System.out.println("No URLs found to process at depth 1.");
            } else {
                // Process each extracted URL (depth 1)
            	for (String url : extractedUrls) {
            		System.out.println("Processing (Depth 1): " + url);
            		try {
            			WebCrawlerUtils.processUrl(url, baseUrl, outputDir);
            		} catch (URISyntaxException|IOException e) {
            			System.err.println("Error during crawling: " + e.getMessage());
            		}
            	}
            }

            System.out.println("Sequential crawling completed successfully.");

        } catch (IOException e) {
            // Handle exceptions that may occur during crawling
            System.err.println("Error during crawling: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
