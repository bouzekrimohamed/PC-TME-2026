package pc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class WordFrequency {

  private static class CounterWorker extends Thread {
	  private final File file;
	  private final long start;
	  private final long end;

	  private long totalWords = 0;
	  private final Map<String, Integer> map = new HashMap<>();

	  public CounterWorker(File file, long start, long end) {
	    this.file = file;
	    this.start = start;
	    this.end = end;
	  }

	  @Override
	  public void run() {
	    try (InputStream is = FileUtils.getRange(file, start, end);
	         Scanner scanner = new Scanner(is)) {
	      while (scanner.hasNext()) {
	        String word = cleanWord(scanner.next());
	        if (!word.isEmpty()) {
	          totalWords++;
	          map.compute(word, (w, c) -> c == null ? 1 : c + 1);
	        }
	      }
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	  }

	  public long getTotalWords() { return totalWords; }
	  public Map<String, Integer> getMap() { return map; }
  }

  /* TODO : merge two maps
   */
  public static Map<String, Integer> mergeInto(Map<String, Integer> a, Map<String, Integer> b) {
	  for (Map.Entry<String, Integer> e : b.entrySet()) {
		    String w = e.getKey();
		    int v = e.getValue();
		    a.put(w, a.getOrDefault(w, 0) + v);
		  }
		  return a;

  }

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
    } else if (mode.equals("hash2")) {
    	  long totalWords = 0;
    	  Map<String, Integer> map = new HashMap<>();
    	  try (Scanner scanner = new Scanner(file)) {
    	    while (scanner.hasNext()) {
    	      String word = cleanWord(scanner.next());
    	      if (!word.isEmpty()) {
    	        totalWords++;

    	        Integer c = map.get(word);
    	        if (c == null) {
    	          map.put(word, 1);
    	        } else {
    	          map.put(word, c + 1);
    	        }
    	      }
    	    }
    	  }
    	  printResults(totalWords, map);
    } else if (mode.equals("range")) {
      // Sequential full-file processing with hash map + use of getRange
      long totalWords = 0;
      Map<String, Integer> map = new HashMap<>();
      try (Scanner scanner = new Scanner(FileUtils.getRange(file, 0, fileSize))) {
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
        // Calcul des indices de découpage pour N morceaux 
        long[] parts = FileUtils.partition(file, numThreads);
        long totalWords = 0;
        Map<String, Integer> map = new HashMap<>();

        // On boucle sur chaque segment de la partition 
        for (int i = 0; i < numThreads; i++) {
        	long start = parts[i];
        	long end = parts[i + 1];

        	try (InputStream is = FileUtils.getRange(file, start, end);
        	     Scanner scanner = new Scanner(is)) {

        	 

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

    } else if (mode.equals("shard")) {
      // Multi-threaded, per-thread local maps, merge after
      // Based on partition + using CounterWorker
    	long[] parts = FileUtils.partition(file, numThreads);
      // create one thread per partition element
    	  List<CounterWorker> workers = new ArrayList<>();
    	  for (int i = 0; i < numThreads; i++) {
    	    CounterWorker w = new CounterWorker(file, parts[i], parts[i + 1]);
    	    workers.add(w);
    	    w.start();
    	  }
      // join all threads
    	  for (CounterWorker w : workers) {
    		    try {
    		      w.join();
    		    } catch (InterruptedException e) {
    		      throw new RuntimeException(e);
    		    }
    		  }
      // collect and merge results
    	  long totalWords = 0;
    	  Map<String, Integer> map = new HashMap<>();
    	  for (CounterWorker w : workers) {
    	    totalWords += w.getTotalWords();
    	    mergeInto(map, w.getMap());
    	  }
      // printResults
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