package pc.mandelbrot.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;

import pc.mandelbrot.BoundingBox;
import pc.mandelbrot.MandelbrotCalculator;

public class MandelbrotPanel extends JPanel implements MouseListener, MouseMotionListener, PropertyChangeListener {
    private static final long serialVersionUID = 1L; // to please compiler
	private BufferedImage image;
    private BoundingBox boundingBox;
    private int maxIterations;

    private Rectangle selection = null;
    private Point dragStart = null;

    private final Settings settings = Settings.getInstance();

    public MandelbrotPanel() {
        // Initialize from Settings
        maxIterations = settings.getMaxIterations();
        boundingBox = new BoundingBox(-2.5, 1, -1, 1, settings.getPanelWidth(), settings.getPanelHeight());

        // Update Settings with initial bounding box
        settings.setXmin(boundingBox.xmin);
        settings.setXmax(boundingBox.xmax);
        settings.setYmin(boundingBox.ymin);
        settings.setYmax(boundingBox.ymax);

        // Set preferred size based on settings
        setPreferredSize(new Dimension(settings.getPanelWidth(), settings.getPanelHeight()));

        // Add mouse listeners
        addMouseListener(this);
        addMouseMotionListener(this);

        // Listen to settings changes
        settings.addPropertyChangeListener(this);

        // Compute the initial image
        computeImage();
    }

    /**
     * Computes the Mandelbrot set and updates the image buffer.
     */
    private void computeImage() {
        int width = boundingBox.width;
        int height = boundingBox.height;

        // Create the image buffer
        int[] imageBuffer = new int[width * height];

        // Compute the Mandelbrot set using the parallel method
        MandelbrotCalculator.parCompute(boundingBox, maxIterations, imageBuffer);

        // Create the BufferedImage from the image buffer
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, width, height, imageBuffer, 0, width);

        // Repaint the panel to display the new image
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            // Draw the image
            g.drawImage(image, 0, 0, null);
        }

        // Draw the selection rectangle if present
        if (selection != null) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.WHITE);
            g2.draw(selection);
        }
    }

    // MouseListener methods
    @Override
    public void mousePressed(MouseEvent e) {
        dragStart = e.getPoint();
        selection = new Rectangle();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (selection != null && selection.width > 0 && selection.height > 0) {
            // Calculate new bounding box based on the selection
            int panelWidth = getWidth();
            int panelHeight = getHeight();

            // Determine desired aspect ratio
            double aspectRatio = (double) panelWidth / panelHeight;

            // Adjust the selection rectangle to maintain aspect ratio
            int selWidth = selection.width;
            int selHeight = selection.height;
            double selAspectRatio = (double) selWidth / selHeight;

            if (selAspectRatio > aspectRatio) {
                // Selection is wider than the panel aspect ratio, adjust height
                int newHeight = (int) (selWidth / aspectRatio);
                if (selection.y + newHeight > panelHeight) {
                    newHeight = panelHeight - selection.y;
                }
                selection.height = newHeight;
            } else {
                // Selection is taller than the panel aspect ratio, adjust width
                int newWidth = (int) (selHeight * aspectRatio);
                if (selection.x + newWidth > panelWidth) {
                    newWidth = panelWidth - selection.x;
                }
                selection.width = newWidth;
            }

            // Now recalculate new bounding box based on adjusted selection
            double xScale = (boundingBox.xmax - boundingBox.xmin) / panelWidth;
            double yScale = (boundingBox.ymax - boundingBox.ymin) / panelHeight;

            double newXmin = boundingBox.xmin + selection.x * xScale;
            double newXmax = boundingBox.xmin + (selection.x + selection.width) * xScale;

            // Correct the y-coordinate mapping
            double newYmax = boundingBox.ymax - selection.y * yScale;
            double newYmin = boundingBox.ymax - (selection.y + selection.height) * yScale;

            // Update the bounding box with the new coordinates and same dimensions
            boundingBox = new BoundingBox(newXmin, newXmax, newYmin, newYmax, panelWidth, panelHeight);

            // Update settings with new bounding box values
            settings.setXmin(boundingBox.xmin);
            settings.setXmax(boundingBox.xmax);
            settings.setYmin(boundingBox.ymin);
            settings.setYmax(boundingBox.ymax);

            // Clear the selection
            selection = null;
            dragStart = null;

            // Recompute the Mandelbrot set with the new bounding box
            computeImage();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Optional: Implement double-click to reset the view
        if (e.getClickCount() == 2) {
            resetView();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Not needed
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Not needed
    }

    // MouseMotionListener methods
    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart != null) {
            Point dragEnd = e.getPoint();
            selection.setBounds(Math.min(dragStart.x, dragEnd.x), Math.min(dragStart.y, dragEnd.y),
                    Math.abs(dragStart.x - dragEnd.x), Math.abs(dragStart.y - dragEnd.y));
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Not needed
    }

    /**
     * Resets the view to the initial bounding box.
     */
    public void resetView() {
        boundingBox = new BoundingBox(-2.5, 1, -1, 1, getWidth(), getHeight());

        // Update settings
        settings.setXmin(boundingBox.xmin);
        settings.setXmax(boundingBox.xmax);
        settings.setYmin(boundingBox.ymin);
        settings.setYmax(boundingBox.ymax);

        computeImage();
    }

    /**
     * Handles property changes from Settings.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();

        switch (property) {
            case "panelWidth":
            case "panelHeight":
                // Update panel size
                int newWidth = settings.getPanelWidth();
                int newHeight = settings.getPanelHeight();
                setPreferredSize(new Dimension(newWidth, newHeight));
                revalidate(); // Notify parent container of size change

                // Update bounding box dimensions
                boundingBox = new BoundingBox(boundingBox.xmin, boundingBox.xmax, boundingBox.ymin, boundingBox.ymax,
                        newWidth, newHeight);

                // Recompute the image with new dimensions
                computeImage();
                break;

            case "maxIterations":
                // Update maxIterations and recompute
                maxIterations = settings.getMaxIterations();
                computeImage();
                break;

            default:
                break;
        }
    }
}
