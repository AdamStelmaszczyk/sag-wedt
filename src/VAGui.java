import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/*
 * View Agent Gui
 */
class VAGui extends JFrame {
	private static final long serialVersionUID = 1L;

	private VA myAgent;

	private JCheckBox isLocalNameCheckBox;
	private JTextField nameTextField;
	private JTextArea relationsTextArea;

	VAGui(VA a) {
		super(a.getLocalName());

		myAgent = a;

		ActionListener getRelationsListener = new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				relationsTextArea.setText("");
				String name = nameTextField.getText().trim();
				myAgent.getRelations(name, !isLocalNameCheckBox.isSelected());
			}
		};

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BorderLayout());
		JPanel northFlowPanel = new JPanel();
		northFlowPanel.add(new JLabel("NAME:"));
		isLocalNameCheckBox = new JCheckBox();
		isLocalNameCheckBox
				.setToolTipText("Select if the name is not the GUID.");
		northFlowPanel.add(isLocalNameCheckBox);
		northPanel.add(northFlowPanel, BorderLayout.WEST);
		nameTextField = new JTextField();
		nameTextField.addActionListener(getRelationsListener);
		northPanel.add(nameTextField, BorderLayout.CENTER);
		getContentPane().add(northPanel, BorderLayout.NORTH);

		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		centerPanel.add(new JLabel("Relations:"), BorderLayout.NORTH);
		relationsTextArea = new JTextArea();
		relationsTextArea.setEditable(false);
		centerPanel
				.add(new JScrollPane(relationsTextArea), BorderLayout.CENTER);
		getContentPane().add(centerPanel, BorderLayout.CENTER);

		JPanel southPanel = new JPanel();
		JButton getRelationsButton = new JButton("Get relations");
		getRelationsButton.addActionListener(getRelationsListener);
		southPanel.add(getRelationsButton);
		JButton clearRelationsButton = new JButton("Clear");
		clearRelationsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				relationsTextArea.setText("");
			}
		});
		southPanel.add(clearRelationsButton);
		getContentPane().add(southPanel, BorderLayout.SOUTH);

		// Make the agent terminate when the user closes the GUI
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		});
	}

	public void showGui() {
		setSize(600, 400);
		super.setVisible(true);
	}

	public void appendRelations(final String relations) {
		relationsTextArea.append(relations);
	}

}
