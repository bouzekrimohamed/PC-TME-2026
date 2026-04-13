package pc.mandelbrot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Main {
	public static void main(String[] args) {
		// Define image parameters
		int width = 800;
		int height = 600;
		int maxIterations = 5000;

		// Define output file name
		String outputFileName = "mandelbrot.png";

		// Define the bounding box for the Mandelbrot set
		BoundingBox bbox = new BoundingBox(-2.5, 1, -1, 1, width, height);

		// Create an array to hold the image pixels
		int[] imageBuffer = new int[width * height];

		long startTime = System.currentTimeMillis();
		// Compute the Mandelbrot set using the parallel method
		MandelbrotCalculator.parCompute(bbox, maxIterations, imageBuffer);

		// Create a BufferedImage from the image buffer
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height, imageBuffer, 0, width);

		// Save the BufferedImage to a file
		try {
			File outputFile = new File(outputFileName);
			ImageIO.write(image, "png", outputFile);
			System.out.println("Mandelbrot image saved as " + outputFileName);
		} catch (IOException e) {
			System.err.println("Error saving the Mandelbrot image.");
			e.printStackTrace();
		}
		System.out.println("Execution time: " + (System.currentTimeMillis() - startTime) + "ms");
	}
}
