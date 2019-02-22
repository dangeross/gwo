import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

class gwoDebugWindow extends JFrame implements ActionListener {
	private JTextArea text;
	private JButton close;
	
	public gwoDebugWindow(String data) {
		super("Debug Window");
		
		close = new JButton("Close");
		close.addActionListener(this);
		close.setActionCommand("$close");
			
		text = new JTextArea(data, 10, 30);
		text.setLineWrap(true);
		text.setBorder(BorderFactory.createLineBorder(Color.gray));
		JScrollPane textScrollPane = new JScrollPane(text,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		JPanel footerPane = new JPanel();
		footerPane.setLayout(new FlowLayout(FlowLayout.CENTER));
		footerPane.add(close);

		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add("Center", textScrollPane);
		contentPane.add("South", footerPane);

		setContentPane(contentPane);
		pack();
		setVisible(true);
	}	


	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("$close")) {
			setVisible(false);
			dispose();
		}
	}
}