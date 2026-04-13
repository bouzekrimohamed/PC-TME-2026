package pc.mandelbrot.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class Settings {
    private static Settings instance = null;

    private int panelWidth;
    private int panelHeight;
    private int maxIterations;

    private double xmin;
    private double xmax;
    private double ymin;
    private double ymax;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // Private constructor to prevent instantiation
    private Settings() {
        // Default settings
        panelWidth = 800;
        panelHeight = 600;
        maxIterations = 5000;

        xmin = -2.5;
        xmax = 1;
        ymin = -1;
        ymax = 1;
    }

    // Method to retrieve the singleton instance
    public static synchronized Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    // Getters and Setters with Property Change Notifications

    public int getPanelWidth() {
        return panelWidth;
    }

    public void setPanelWidth(int panelWidth) {
        int oldWidth = this.panelWidth;
        this.panelWidth = panelWidth;
        pcs.firePropertyChange("panelWidth", oldWidth, panelWidth);
    }

    public int getPanelHeight() {
        return panelHeight;
    }

    public void setPanelHeight(int panelHeight) {
        int oldHeight = this.panelHeight;
        this.panelHeight = panelHeight;
        pcs.firePropertyChange("panelHeight", oldHeight, panelHeight);
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        int oldIterations = this.maxIterations;
        this.maxIterations = maxIterations;
        pcs.firePropertyChange("maxIterations", oldIterations, maxIterations);
    }

    public double getXmin() {
        return xmin;
    }

    public void setXmin(double xmin) {
        double oldXmin = this.xmin;
        this.xmin = xmin;
        pcs.firePropertyChange("xmin", oldXmin, xmin);
    }

    public double getXmax() {
        return xmax;
    }

    public void setXmax(double xmax) {
        double oldXmax = this.xmax;
        this.xmax = xmax;
        pcs.firePropertyChange("xmax", oldXmax, xmax);
    }

    public double getYmin() {
        return ymin;
    }

    public void setYmin(double ymin) {
        double oldYmin = this.ymin;
        this.ymin = ymin;
        pcs.firePropertyChange("ymin", oldYmin, ymin);
    }

    public double getYmax() {
        return ymax;
    }

    public void setYmax(double ymax) {
        double oldYmax = this.ymax;
        this.ymax = ymax;
        pcs.firePropertyChange("ymax", oldYmax, ymax);
    }

    // Methods to add and remove property change listeners
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }
}
