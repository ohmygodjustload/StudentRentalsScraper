/**
 * An interface for the GUI component of the application.
 * This class serves as a placeholder for future GUI implementations.
 * Currently, it just allows user to input the min and max values to iterate over.
 * 
 * @author Andrew Peirce
 * Date Last Modified: 01/17/2026
 */
package gui;

import java.awt.*;
import javax.swing.*;

public class GUI {

	private JFrame frame;
	private JPanel panel;
	private JLabel minLabel;
	private JLabel maxLabel;
	private JSpinner minSpinner;
	private JSpinner maxSpinner;
	private JButton runButton;
	private SpinnerNumberModel minModel;
	private SpinnerNumberModel maxModel;

	public GUI() {
		frame = new JFrame("GUI Placeholder");
		panel = new JPanel();
		panel.setLayout(new GridLayout(3, 2, 10, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		minLabel = new JLabel("Min Value:");
		maxLabel = new JLabel("Max Value:");
		minModel = new SpinnerNumberModel(0, 0, 1000, 1);
		maxModel = new SpinnerNumberModel(900, 0, 1000, 1);
		minSpinner = new JSpinner(minModel);
		maxSpinner = new JSpinner(maxModel);
		runButton = new JButton("Run");
		panel.add(minLabel);
		panel.add(minSpinner);
		panel.add(maxLabel);
		panel.add(maxSpinner);
		panel.add(runButton);
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

	// Add functionality to the run button to run the Scraper with the specified min and max values
	public void addRunButtonListener(java.awt.event.ActionListener listener) {
		runButton.addActionListener(listener);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new GUI();
		});
		// TODO Auto-generated method stub

	}

}
