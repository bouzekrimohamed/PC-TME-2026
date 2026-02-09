package pc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * A decorator InputStream that limits the number of bytes that can be read. It
 * also closes the underlying RandomAccessFile when closed.
 */
class LimitedInputStream extends FilterInputStream {
  private final RandomAccessFile raf; // to close it properly
  private long remaining;

  public LimitedInputStream(InputStream in, RandomAccessFile raf, long limit) {
    super(in);
    this.raf = raf;
    this.remaining = limit;
  }
  
  @Override
  public int available() throws IOException {
    return Math.min(super.available(), (int)remaining);
  }

  @Override
  public int read() throws IOException {
    if (remaining <= 0) {
      return -1;
    }
    int b = super.read();
    if (b != -1) {
      remaining--;
    }
    return b;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (remaining <= 0) {
      return -1;
    }
    int toRead = (int) Math.min(len, remaining);
    int read = super.read(b, off, toRead);
    if (read > 0) {
      remaining -= read;
    }
    return read;
  }

  @Override
  public long skip(long n) throws IOException {
    if (remaining <= 0) {
      return 0;
    }
    long toSkip = Math.min(n, remaining);
    long skipped = super.skip(toSkip);
    remaining -= skipped;
    return skipped;
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      raf.close(); // Ensure the RAF is closed even if channelStream fails
    }
  }
  
  @Override
  public boolean markSupported() {
    return false;
  }
}