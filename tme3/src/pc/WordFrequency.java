package pc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class WordFrequency {

  public static void main(String[] args) throws IOException {
    String filename = args.length > 0 ? args[0] : "data/WarAndPeace.txt";
    String mode = args.length > 1 ? args[1] : "hash";
    int numThreads = args.length > 2 ? Integer.parseInt(args[2]) : 4;

    File file = new File(filename);
    if (!file.exists() || !file.canRead()) {
      System.err.println("Could not open '" + filename + "'. Please provide a readable text file.");
      System.exit(2);
    }

    long fileSize = file.length();
    System.out.println("Preparing to parse " + filename + " (mode=" + mode + ", N=" + numThreads + "), containing "
        + fileSize + " bytes");

    long startTime = System.nanoTime();

    if (mode.equals("hash")) {
      // Sequential full-file processing with hash map
      long totalWords = 0;
      Map<String, Integer> map = new HashMap<>();
      try (Scanner scanner = new Scanner(file)) {
        while (scanner.hasNext()) {
          String word = cleanWord(scanner.next());
          if (!word.isEmpty()) {
            totalWords++;
            map.compute(word, (w, c) -> c == null ? 1 : c + 1);
          }
        }
      }
      printResults(totalWords, map);
    } else if (mode.equals("partition")) {
      // Single-threaded, loop over ranges with single map
      long[] parts = FileUtils.partition(file, numThreads);
      long totalWords = 0;
      Map<String, Integer> map = new HashMap<>();

      for (int i = 0; i < numThreads; i++) {
        try (Scanner scanner = new Scanner(FileUtils.getRange(file, parts[i], parts[i + 1]))) {
          while (scanner.hasNext()) {
            String word = cleanWord(scanner.next());
            if (!word.isEmpty()) {
              totalWords++;
              map.compute(word, (w, c) -> c == null ? 1 : c + 1);
            }
          }
        }
      }
      printResults(totalWords, map);
    } else {
      System.err.println("Unknown mode: " + mode);
      System.exit(1);
    }

    long endTime = System.nanoTime();
    long durationMs = (endTime - startTime) / 1_000_000;
    System.out.println("Total runtime: " + durationMs + " ms for mode " + mode);
  }

  private static void printResults(long totalWords, Map<String, Integer> map) {
    System.out.println("Total words: " + totalWords);
    System.out.println("Unique words: " + map.size());

    List<Map.Entry<String, Integer>> wordList = new ArrayList<>(map.entrySet());
    wordList.sort((e1, e2) -> {
      if (!e1.getValue().equals(e2.getValue())) {
        return Integer.compare(e2.getValue(), e1.getValue()); // desc freq
      } else {
        return e1.getKey().compareTo(e2.getKey()); // asc alpha
      }
    });

    for (Map.Entry<String, Integer> entry : wordList.subList(0, Math.min(5, wordList.size()))) {
      System.out.println(entry.getValue() + " " + entry.getKey());
    }
  }

  private static String cleanWord(String word) {
    return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
  }
}