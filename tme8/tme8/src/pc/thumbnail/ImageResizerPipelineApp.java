package pc.thumbnail;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ImageResizerPipelineApp {

    private static final File POISON_FILE = new File("__POISON__");
    private static final LoadedImage POISON_LOADED = new LoadedImage(POISON_FILE, null);
    private static final ResizedImage POISON_RESIZED = new ResizedImage(POISON_FILE, null);

    private static class LoadedImage {
        private final File source;
        private final BufferedImage image;

        private LoadedImage(File source, BufferedImage image) {
            this.source = source;
            this.image = image;
        }
    }

    private static class ResizedImage {
        private final File source;
        private final BufferedImage image;

        private ResizedImage(File source, BufferedImage image) {
            this.source = source;
            this.image = image;
        }
    }

    public static void main(String[] args) {
        File inputFolder = new File("input_images");
        File outputFolder = new File("output_images");

        if (!outputFolder.exists() && !outputFolder.mkdirs()) {
            System.err.println("Failed to create the output folder.");
            return;
        }

        File[] imageFiles = ImageUtils.findImageFiles(inputFolder);
        if (imageFiles.length == 0) {
            System.out.println("No images to process.");
            return;
        }

        BlockingQueue<File> toLoad = new LinkedBlockingQueue<>();
        BlockingQueue<LoadedImage> toResize = new LinkedBlockingQueue<>();
        BlockingQueue<ResizedImage> toSave = new LinkedBlockingQueue<>();

        Thread loader = new Thread(() -> loadStage(imageFiles, toLoad, toResize), "loader-stage");
        Thread resizer = new Thread(() -> resizeStage(toResize, toSave), "resizer-stage");
        Thread saver = new Thread(() -> saveStage(outputFolder, toSave), "saver-stage");

        loader.start();
        resizer.start();
        saver.start();

        try {
            for (File imageFile : imageFiles) {
                toLoad.put(imageFile);
            }
            toLoad.put(POISON_FILE);

            loader.join();
            resizer.join();
            saver.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Pipeline interrupted.");
            return;
        }

        System.out.println("Pipeline image resizing completed.");
    }

    private static void loadStage(File[] imageFiles, BlockingQueue<File> toLoad, BlockingQueue<LoadedImage> toResize) {
        try {
            while (true) {
                File file = toLoad.take();
                if (file == POISON_FILE) {
                    toResize.put(POISON_LOADED);
                    return;
                }

                BufferedImage image = ImageUtils.loadImage(file);
                if (image != null) {
                    toResize.put(new LoadedImage(file, image));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void resizeStage(BlockingQueue<LoadedImage> toResize, BlockingQueue<ResizedImage> toSave) {
        try {
            while (true) {
                LoadedImage loaded = toResize.take();
                if (loaded == POISON_LOADED) {
                    toSave.put(POISON_RESIZED);
                    return;
                }

                BufferedImage resized = ImageUtils.resizeImage(loaded.image);
                toSave.put(new ResizedImage(loaded.source, resized));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void saveStage(File outputFolder, BlockingQueue<ResizedImage> toSave) {
        try {
            while (true) {
                ResizedImage resized = toSave.take();
                if (resized == POISON_RESIZED) {
                    return;
                }

                File outputFile = new File(outputFolder, resized.source.getName());
                ImageUtils.saveImage(resized.image, outputFile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
