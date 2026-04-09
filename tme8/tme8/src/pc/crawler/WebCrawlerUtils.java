package pc.crawler;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class WebCrawlerUtils {

    public static List<String> processUrl(String urlString, String baseUrl, Path outputDir)
            throws IOException, URISyntaxException {

    	System.out.println("Processing URL: " + urlString);
    	
        URL url = new URI(urlString).toURL();
        URL base = new URI(baseUrl).toURL();

        List<String> nestedUrls = new ArrayList<>();

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("User-Agent", "SimpleWebCrawler");
        connection.setRequestProperty("Referer", baseUrl); // Set referrer header
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {

            String line;
            Pattern pattern = Pattern.compile("(?i)href=[\"'](.*?)[\"']");
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                while (matcher.find()) {
                    String attrValue = matcher.group(1);

                    // Decode HTML entities like &amp; in URLs
                    attrValue = attrValue.replaceAll("&amp;", "&");
                    
                    // Resolve the URL, keeping query parameters
                    URI baseUri = url.toURI();
                    URI resolvedUri = baseUri.resolve(attrValue);
                    URL extractedUrl = resolvedUri.toURL();
                    String extractedUrlString = extractedUrl.toString();

                    // Only process URLs on the same domain
                    if (extractedUrl.getHost().equalsIgnoreCase(base.getHost())) {
                        if (extractedUrlString.toLowerCase().endsWith(".pdf")) {
                            downloadFile(extractedUrl, outputDir);
                        } else if (!nestedUrls.contains(extractedUrlString)) {
                            nestedUrls.add(extractedUrlString);
                        }
                    }
                }
            }
        } finally {
            connection.disconnect();
        }

        return nestedUrls;
    }

    private static void downloadFile(URL url, Path outputDir) throws IOException {
        String fileName = Paths.get(url.getPath()).getFileName().toString();
        
        if (fileName.isEmpty()) {
            fileName = "downloaded_file_" + System.currentTimeMillis() + ".pdf";
        }
        
        Path targetFile = outputDir.resolve(fileName);

        if (Files.exists(targetFile)) {
            System.out.println("File already downloaded: " + targetFile + " (skipping)");
            return;
        }

        Files.createDirectories(outputDir);
        
        try (InputStream in = url.openStream();
             OutputStream out = Files.newOutputStream(targetFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            Files.deleteIfExists(targetFile);
            System.out.println("Could not download PDF from : " + url.toString());
        }

        System.out.println("Downloaded PDF: " + url.toString());
    }
}
