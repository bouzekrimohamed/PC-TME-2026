package pc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    } else if (mode.equals("naive")) {
      long[] parts = FileUtils.partition(file, numThreads);
      long[] totalWords = new long[1];
      Map<String, Integer> map = new HashMap<>();

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];
        threads[i] = new Thread(new NaiveWorker(file, start, end, map, totalWords));
        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      printResults(totalWords[0], map);

    } else if (mode.equals("naive2")) {
      // BONUS Q4: lambda (ou classe anonyme) au lieu d'une classe dédiée
      long[] parts = FileUtils.partition(file, numThreads);
      final long[] totalWords = new long[1]; // partagé (FAUX -> data race, normal pour ce mode)
      final Map<String, Integer> map = new HashMap<>(); // partagé (FAUX -> data race, normal pour ce mode)
      final AtomicLong cmeCount = new AtomicLong(0); // optionnel: compter les ConcurrentModificationException

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];

        threads[i] = new Thread(() -> {
          try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
            while (scanner.hasNext()) {
              String word = cleanWord(scanner.next());
              if (!word.isEmpty()) {
                totalWords[0]++; // FAUX (data race)
                try {
                  map.compute(word, (w, c) -> c == null ? 1 : c + 1); // FAUX (data race)
                } catch (ConcurrentModificationException ex) {
                  cmeCount.incrementAndGet(); // on continue l'exécution
                }
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      if (cmeCount.get() > 0) {
        System.out.println("ConcurrentModificationException count: " + cmeCount.get());
      }
      printResults(totalWords[0], map);

    } else if (mode.equals("atomic")) {
      long[] parts = FileUtils.partition(file, numThreads);
      AtomicLong totalWords = new AtomicLong(0);
      Map<String, Integer> map = new HashMap<>();

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];

        threads[i] = new Thread(() -> {
          try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
            while (scanner.hasNext()) {
              String word = cleanWord(scanner.next());
              if (!word.isEmpty()) {
                totalWords.incrementAndGet();
                map.compute(word, (w, c) -> c == null ? 1 : c + 1);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      printResults(totalWords.get(), map);

    } else if (mode.equals("synchronized")) {
      long[] parts = FileUtils.partition(file, numThreads);
      AtomicLong totalWords = new AtomicLong(0);
      Map<String, Integer> map = new HashMap<>();

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];

        threads[i] = new Thread(() -> {
          try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
            while (scanner.hasNext()) {
              String word = cleanWord(scanner.next());
              if (!word.isEmpty()) {
                totalWords.incrementAndGet();
                synchronized (map) {
                  map.compute(word, (w, c) -> c == null ? 1 : c + 1);
                }
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      printResults(totalWords.get(), map);

    } else if (mode.equals("lock")) {
      // BONUS Q8: ReentrantLock au lieu de synchronized
      long[] parts = FileUtils.partition(file, numThreads);
      AtomicLong totalWords = new AtomicLong(0);
      Map<String, Integer> map = new HashMap<>();
      final Lock lock = new ReentrantLock();

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];

        threads[i] = new Thread(() -> {
          try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
            while (scanner.hasNext()) {
              String word = cleanWord(scanner.next());
              if (!word.isEmpty()) {
                totalWords.incrementAndGet();

                lock.lock();
                try {
                  map.compute(word, (w, c) -> c == null ? 1 : c + 1);
                } finally {
                  lock.unlock();
                }
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      printResults(totalWords.get(), map);

    } else if (mode.equals("decorated")) {
      long[] parts = FileUtils.partition(file, numThreads);
      AtomicLong totalWords = new AtomicLong(0);
      Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());

      Thread[] threads = new Thread[numThreads];
      for (int i = 0; i < numThreads; i++) {
        final long start = parts[i];
        final long end = parts[i + 1];

        threads[i] = new Thread(() -> {
          try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
            while (scanner.hasNext()) {
              String word = cleanWord(scanner.next());
              if (!word.isEmpty()) {
                totalWords.incrementAndGet();
                map.compute(word, (w, c) -> c == null ? 1 : c + 1);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

        threads[i].start();
      }

      for (int i = 0; i < numThreads; i++) {
        try {
          threads[i].join();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      printResults(totalWords.get(), map);

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
        return Integer.compare(e2.getValue(), e1.getValue());
      } else {
        return e1.getKey().compareTo(e2.getKey());
      }
    });

    for (Map.Entry<String, Integer> entry : wordList.subList(0, Math.min(5, wordList.size()))) {
      System.out.println(entry.getValue() + " " + entry.getKey());
    }
  }

  static String cleanWord(String word) {
    return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
  }
}

class NaiveWorker implements Runnable {
  private final File file;
  private final long start;
  private final long end;
  private final Map<String, Integer> map;
  private final long[] totalWords;

  public NaiveWorker(File file, long start, long end, Map<String, Integer> map, long[] totalWords) {
    this.file = file;
    this.start = start;
    this.end = end;
    this.map = map;
    this.totalWords = totalWords;
  }

  @Override
  public void run() {
    try (Scanner scanner = new Scanner(FileUtils.getRange(file, start, end))) {
      while (scanner.hasNext()) {
        String word = WordFrequency.cleanWord(scanner.next());
        if (!word.isEmpty()) {
          totalWords[0]++;
          map.compute(word, (w, c) -> c == null ? 1 : c + 1);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
