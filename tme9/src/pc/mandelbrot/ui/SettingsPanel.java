package pc.mandelbrot.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SettingsPanel extends JPanel implements PropertyChangeListener {
    private static final long serialVersionUID = 1L; // to please compiler
	private final Settings settings = Settings.getInstance();

    private JTextField xminField;
    private JTextField xmaxField;
    private JTextField yminField;
    private JTextField ymaxField;
    private JTextField deltaXField;
    private JTextField deltaYField;

    public SettingsPanel(MandelbrotPanel mandelbrotPanel) {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Panel Width Control
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Panel Width:"), gbc);

        gbc.gridx = 1;
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(settings.getPanelWidth(), 100, 2000, 100));
        add(widthSpinner, gbc);

        // Panel Height Control
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Panel Height:"), gbc);

        gbc.gridx = 1;
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(settings.getPanelHeight(), 100, 2000, 100));
        add(heightSpinner, gbc);

        // Max Iterations Control
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Max Iterations:"), gbc);

        gbc.gridx = 1;
        // Remove the upper limit by setting max to null
        JSpinner iterationsSpinner = new JSpinner(
                new SpinnerNumberModel(settings.getMaxIterations(), 100, null, 100));
        add(iterationsSpinner, gbc);

        // Apply Button
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton applyButton = new JButton("Apply");
        add(applyButton, gbc);

        // Bounding Box Display
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 1;

        // X Min
        gbc.gridx = 0;
        gbc.gridy = 4;
        add(new JLabel("X Min:"), gbc);

        gbc.gridx = 1;
        xminField = new JTextField(15);
        xminField.setEditable(false);
        add(xminField, gbc);

        // X Max
        gbc.gridx = 0;
        gbc.gridy = 5;
        add(new JLabel("X Max:"), gbc);

        gbc.gridx = 1;
        xmaxField = new JTextField(15);
        xmaxField.setEditable(false);
        add(xmaxField, gbc);

        // Delta X
        gbc.gridx = 0;
        gbc.gridy = 6;
        add(new JLabel("Delta X:"), gbc);

        gbc.gridx = 1;
        deltaXField = new JTextField(15);
        deltaXField.setEditable(false);
        add(deltaXField, gbc);

        // Y Min
        gbc.gridx = 0;
        gbc.gridy = 7;
        add(new JLabel("Y Min:"), gbc);

        gbc.gridx = 1;
        yminField = new JTextField(15);
        yminField.setEditable(false);
        add(yminField, gbc);

        // Y Max
        gbc.gridx = 0;
        gbc.gridy = 8;
        add(new JLabel("Y Max:"), gbc);

        gbc.gridx = 1;
        ymaxField = new JTextField(15);
        ymaxField.setEditable(false);
        add(ymaxField, gbc);

        // Delta Y
        gbc.gridx = 0;
        gbc.gridy = 9;
        add(new JLabel("Delta Y:"), gbc);

        gbc.gridx = 1;
        deltaYField = new JTextField(15);
        deltaYField.setEditable(false);
        add(deltaYField, gbc);

        // Reset Button
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton resetButton = new JButton("Reset View");
        add(resetButton, gbc);

        // Listeners to update settings
        widthSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newWidth = (int) widthSpinner.getValue();
                settings.setPanelWidth(newWidth);
            }
        });

        heightSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newHeight = (int) heightSpinner.getValue();
                settings.setPanelHeight(newHeight);
            }
        });

        iterationsSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int newIterations = (int) iterationsSpinner.getValue();
                settings.setMaxIterations(newIterations);
            }
        });

        // Apply Button Action
        applyButton.addActionListener(e -> {
            // Trigger property changes if necessary
            settings.setPanelWidth((int) widthSpinner.getValue());
            settings.setPanelHeight((int) heightSpinner.getValue());
            settings.setMaxIterations((int) iterationsSpinner.getValue());
        });

        // Reset Button Action
        resetButton.addActionListener(e -> {
            mandelbrotPanel.resetView();
        });

        // Initialize bounding box fields
        updateBoundingBoxFields();

        // Add property change listener
        settings.addPropertyChangeListener(this);
    }

    /**
     * Updates the bounding box fields and calculates deltaX and deltaY.
     */
    private void updateBoundingBoxFields() {
        xminField.setText(String.format("%.10f", settings.getXmin()));
        xmaxField.setText(String.format("%.10f", settings.getXmax()));
        yminField.setText(String.format("%.10f", settings.getYmin()));
        ymaxField.setText(String.format("%.10f", settings.getYmax()));

        double deltaX = settings.getXmax() - settings.getXmin();
        double deltaY = settings.getYmax() - settings.getYmin();

        deltaXField.setText(String.format("%.10f", deltaX));
        deltaYField.setText(String.format("%.10f", deltaY));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String property = evt.getPropertyName();

        switch (property) {
            case "xmin":
            case "xmax":
            case "ymin":
            case "ymax":
                updateBoundingBoxFields();
                break;
            default:
                break;
        }
    }
}
