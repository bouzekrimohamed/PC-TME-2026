package pc.thumbnail;

import java.awt.image.BufferedImage;
import java.io.File;

public class ImageResizerApp {

	public static void main(String[] args) {
		// Hardcoded input and output folders
		File inputFolder = new File("input_images");
		File outputFolder = new File("output_images");

		// Ensure the output directory exists
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdirs()) {
				System.err.println("Failed to create the output folder.");
				return;
			}
		}

		// Find image files in the input folder
		File[] imageFiles = ImageUtils.findImageFiles(inputFolder);

		treatImages(outputFolder, imageFiles);

		System.out.println("Image resizing completed.");
	}

  public static void treatImages(File outputFolder, File[] imageFiles) {
    // process each image
    for (File file : imageFiles) {
      // Load the image
      BufferedImage originalImage = ImageUtils.loadImage(file);
      if (originalImage != null) {
        // Resize the image
        BufferedImage resizedImage = ImageUtils.resizeImage(originalImage);

        // Create the output file
        File outputFile = new File(outputFolder, file.getName());

        // Save the resized image
        ImageUtils.saveImage(resizedImage, outputFile);
      }
    }
  }
}
