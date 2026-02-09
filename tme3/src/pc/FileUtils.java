package pc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Utility methods for range-based file access (used in "partition" and "shard"
 * modes).
 */
class FileUtils {

  /**
   * Partitions the file into approximately equal byte ranges, adjusted to word
  * boundaries.
   *
   * @param f        the file to partition
   * @param numParts number of parts
   * @return array of offsets [0, p1, p2, ..., size]
   * @throws IOException if file access fails
   */
  public static long[] partition(File f, int numParts) throws IOException {
    long time = System.currentTimeMillis();
    long size = Files.size(Paths.get(f.getPath()));
    long[] offsets = new long[numParts + 1];
    long partSize = size / numParts;

    try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
      offsets[0] = 0;
      for (int i = 1; i < numParts; i++) {
        long approx = partSize * i;
        raf.seek(approx);

        // Skip forward to the next token boundary (whitespace)
        while (raf.getFilePointer() < size) {
          int c = raf.read();
          if (c == -1)
            break;
          if (Character.isWhitespace(c)) {
            offsets[i] = raf.getFilePointer();
            break;
          }
        }
        if (offsets[i] == 0) {
          offsets[i] = approx; // fallback if no boundary found
        }
      }
      offsets[numParts] = size;
    }
    System.out.println("Computed partition of " + size + " B  into " + numParts + " in "
        + (System.currentTimeMillis() - time) + " ms");
    return offsets;
  }

  /**
   * Returns an InputStream that reads only the portion of the file from start
   * (inclusive) to end (exclusive).
   *
   * @param f     the file to read from
   * @param start starting byte offset (inclusive)
   * @param end   ending byte offset (exclusive)
   * @return an InputStream reading exactly the requested range
   * @throws IOException if the file cannot be opened or seek fails
   */
  public static InputStream getRange(File f, long start, long end) throws IOException {
    if (start < 0 || end <= start || end > Files.size(Paths.get(f.getPath()))) {
      throw new IllegalArgumentException("Invalid range: start=" + start + ", end=" + end);
    }

    RandomAccessFile raf = new RandomAccessFile(f, "r");
    raf.seek(start);

    // Wrap the channel in an InputStream starting at the sought position
    InputStream channelStream = Channels.newInputStream(raf.getChannel());

    // Limit the readable bytes to (end - start)
    long limit = end - start;
    return new BufferedInputStream(new LimitedInputStream(channelStream, raf, limit));
  }

}