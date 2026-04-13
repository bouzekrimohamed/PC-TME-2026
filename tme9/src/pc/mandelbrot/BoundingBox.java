package pc.mandelbrot;

public class BoundingBox {
	public final double xmin;
	public final double xmax;
	public final double ymin;
	public final double ymax;
	public final int width;
	public final int height;

	public BoundingBox(double xmin, double xmax, double ymin, double ymax, int width, int height) {
		this.xmin = xmin;
		this.xmax = xmax;
		this.ymin = ymin;
		this.ymax = ymax;
		this.width = width;
		this.height = height;
	}
}
