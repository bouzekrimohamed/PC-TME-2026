package pc.mandelbrot.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

public class MandelbrotViewer {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Mandelbrot Viewer");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Create the MandelbrotPanel
            MandelbrotPanel mandelbrotPanel = new MandelbrotPanel();

            // Create the SettingsPanel with mandelbrotPanel reference
            SettingsPanel settingsPanel = new SettingsPanel(mandelbrotPanel);

            // Use JSplitPane to place SettingsPanel to the right of MandelbrotPanel
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(mandelbrotPanel),
                    settingsPanel);
            splitPane.setDividerLocation(800); // Initial divider position

            frame.add(splitPane);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
