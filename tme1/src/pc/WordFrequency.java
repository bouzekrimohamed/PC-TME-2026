package pc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class WordFrequency {

    private static class WordCount {
        private final String word;
        private int count;

        public WordCount(String word, int count) {
            this.word = word;
            this.count = count;
        }

        public String getWord() {
            return word;
        }

        public int getCount() {
            return count;
        }

        public void increment() {
            count++;
        }
    }

    public static void main(String[] args) throws IOException {
        // Allow filename as optional first argument, default to WarAndPeace.txt
        // Optional second argument is mode (e.g., "list" or "listfreq").
        String filename = args.length > 0 ? args[0] : "data/WarAndPeace.txt";
        String mode = args.length > 1 ? args[1] : "count";

        // Check if file is readable
        File file = new File(filename);
        if (!file.exists() || !file.canRead()) {
            System.err.println("Could not open '" + filename + "'. Please provide a readable text file as the first argument.");
            System.err.println("Usage: java WordFrequency [path/to/textfile] [mode]");
            System.exit(2);
        }

        long fileSize = file.length();

        System.out.println("Preparing to parse " + filename + " (mode=" + mode + "), containing " + fileSize + " bytes");

        long start = System.nanoTime();

        Scanner scanner = new Scanner(file);

        if (mode.equals("count")) {
            long totalWords = 0;
            while (scanner.hasNext()) {
                String word = cleanWord(scanner.next());
                if (!word.isEmpty()) {
                    totalWords++;
                }
            }
            System.out.println("Total words: " + totalWords);

        } else if (mode.equals("list")) {
            long totalWords = 0;
            List<String> words = new ArrayList<>();
            while (scanner.hasNext()) {
                String word = cleanWord(scanner.next());
                if (!word.isEmpty()) {
                    totalWords++;

                    // si le mot n'est pas encore présent, on l'ajoute
                    if (!words.contains(word)) {
                        words.add(word);
                    }
                }
            }
            System.out.println("Total words: " + totalWords);
            System.out.println("Unique words: " + words.size());

        } else if (mode.equals("listfreq")) {
            long totalWords = 0;
            List<WordCount> words = new ArrayList<>();
            while (scanner.hasNext()) {
                String word = cleanWord(scanner.next());
                if (!word.isEmpty()) {
                    totalWords++;

                    // chercher si déjà présent
                    WordCount found = null;
                    for (WordCount wc : words) {
                        if (wc.getWord().equals(word)) {
                            found = wc;
                            break;
                        }
                    }

                    if (found != null) {
                        found.increment();
                    } else {
                        words.add(new WordCount(word, 1));
                    }
                }
            }
            System.out.println("Total words: " + totalWords);
            System.out.println("Unique words: " + words.size());

            // tri: fréquence décroissante, puis mot alphabétique croissant
            words.sort((a, b) -> {
                int cmp = Integer.compare(b.getCount(), a.getCount()); // décroissant
                if (cmp != 0) return cmp;
                return a.getWord().compareTo(b.getWord()); // alphabétique croissant
            });

            // afficher top 5 (si moins de 5 mots distincts, on affiche tout)
            int k = Math.min(5, words.size());
            for (int i = 0; i < k; i++) {
                WordCount wc = words.get(i);
                System.out.println(wc.getCount() + " " + wc.getWord());
            }

        } else if (mode.equals("tree")) {
            long totalWords = 0;
            Map<String, Integer> map = new TreeMap<>();
            while (scanner.hasNext()) {
                String word = cleanWord(scanner.next());
                if (!word.isEmpty()) {
                    totalWords++;

                    map.put(word, map.getOrDefault(word, 0) + 1);
                }
            }
            System.out.println("Total words: " + totalWords);
            System.out.println("Unique words: " + map.size());

            // extraire dans une liste d'entries
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());

            // tri: fréquence décroissante, puis mot alphabétique croissant
            entries.sort((e1, e2) -> {
                int cmp = Integer.compare(e2.getValue(), e1.getValue()); // décroissant
                if (cmp != 0) return cmp;
                return e1.getKey().compareTo(e2.getKey()); // alphabétique croissant
            });

            int k = Math.min(5, entries.size());
            for (int i = 0; i < k; i++) {
                Map.Entry<String, Integer> e = entries.get(i);
                System.out.println(e.getValue() + " " + e.getKey());
            }

        } else if (mode.equals("hash")) {
            long totalWords = 0;
            Map<String, Integer> map = new HashMap<>();
            while (scanner.hasNext()) {
                String word = cleanWord(scanner.next());
                if (!word.isEmpty()) {
                    totalWords++;

                    map.put(word, map.getOrDefault(word, 0) + 1);
                }
            }
            System.out.println("Total words: " + totalWords);
            System.out.println("Unique words: " + map.size());

            // extraire dans une liste d'entries
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());

            // tri: fréquence décroissante, puis mot alphabétique croissant
            entries.sort((e1, e2) -> {
                int cmp = Integer.compare(e2.getValue(), e1.getValue()); // décroissant
                if (cmp != 0) return cmp;
                return e1.getKey().compareTo(e2.getKey()); // alphabétique croissant
            });

            int k = Math.min(5, entries.size());
            for (int i = 0; i < k; i++) {
                Map.Entry<String, Integer> e = entries.get(i);
                System.out.println(e.getValue() + " " + e.getKey());
            }

        } else {
            System.err.println("Unknown mode '" + mode + "'. Supported modes: count, list, listfreq, tree, hash");
            System.exit(1);
        }

        scanner.close();

        long end = System.nanoTime();
        long durationMs = (end - start) / 1_000_000;
        System.out.println("Total runtime (wall clock) : " + durationMs + " ms for mode " + mode);
    }

    private static String cleanWord(String word) {
        return word.replaceAll("[^a-zA-Z]", "").toLowerCase();
    }
}
