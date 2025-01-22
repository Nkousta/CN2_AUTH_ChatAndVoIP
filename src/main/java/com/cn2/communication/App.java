package com.cn2.communication;


import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;


public class App extends Frame implements WindowListener, ActionListener {


	/*

	 * 

	 * Definition of the app's fields

	 * 

	 */


	static TextField inputTextField;
	static JTextArea textArea;
	static JFrame frame;
	static JButton sendButton;
	static JTextField meesageTextField;


	public static Color gray;
	final static String newline = "\n";
	static JButton callButton;
	static DatagramSocket messageSocket; // DatagramSocket για UDP επικοινωνία

	static DatagramSocket voiceSocket; // DatagramSocket για UDP επικοινωνία
	static String remoteIP = "192.168.244.75"; // IP του απομακρυσμένου υπολογιστή // REPLACE WITH PEER'S IP
	static String localIP; // IP του τοπικού υπολογιστή
	static int localPortMessage = 12311; // Port used to receive messages
	static int remotePortMessage = 12310; // Port used to send messages (Where the peer is listening) // REPLACE WITH

											// PEER'S PORT
	static int localPortVoice = 12313; // Port used to receive voice data
	static int remotePortVoice = 12312; // Port used to send voice data


	static boolean isCalling = false; // Κατάσταση VoIP κλήσης
	static TargetDataLine microphone; // Είσοδος μικροφώνου
	static SourceDataLine speaker; // Έξοδος ηχείου


	/**

	 * 

	 * Construct the app's frame and initialize important parameters

	 * 

	 */


	public App(String title) {


		/*

		 * 

		 * 1. Defining the components of the GUI

		 * 

		 */


		// Setting up the characteristics of the frame


		super(title);


		gray = new Color(254, 254, 254);
		setBackground(gray);
		setLayout(new FlowLayout());
		addWindowListener(this);


		// Setting up the TextField and the TextArea


		inputTextField = new TextField();
		inputTextField.setColumns(20);


		// Setting up the TextArea.


		textArea = new JTextArea(10, 40);
		textArea.setLineWrap(true);
		textArea.setEditable(false);


		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);


		// Setting up the buttons


		sendButton = new JButton("Send");
		callButton = new JButton("Call");


		/*

		 * 

		 * 2. Adding the components to the GUI

		 * 

		 */


		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);


		/*

		 * 

		 * 3. Linking the buttons to the ActionListener

		 * 

		 */


		sendButton.addActionListener(this);
		callButton.addActionListener(this);


		if (messageSocket == null) {
			try {

				messageSocket = new DatagramSocket(localPortMessage); // Δημιουργία DatagramSocket
				voiceSocket = new DatagramSocket(localPortVoice); // Δημιουργία DatagramSocket
			} catch (SocketException e) {

				e.printStackTrace();

				return;

			}

		}


	}


	/**

	 * 

	 * The main method of the application. It continuously listens for

	 * 

	 * new messages.

	 * 

	 */


	public static void main(String[] args) {


		/*
		 * 
		 * 1. Create the app's window
		 * 
		 */


		App app = new App("CN2 - AUTH"); // TODO: You can add the title that will displayed on the Window of the App

											// here


		app.setSize(500, 250);
		app.setVisible(true);


		/*
		 * 
		 * 2.
		 * 
		 */


		try {


			System.out.println("Chat application started for messaging on port: " + localPortMessage);


			new Thread(() -> {

				while (true) {

					receiveMessage();

				}

			}).start();


		} catch (Exception e) {

			e.printStackTrace();

		}


	}


	/**

	 * 

	 * The method that corresponds to the Action Listener. Whenever an action is

	 * performed

	 * 

	 * (i.e., one of the buttons is clicked) this method is executed.

	 * 

	 */


	@Override


	public void actionPerformed(ActionEvent e) {


		/*

		 * 

		 * Check which button was clicked.

		 * 

		 */


		if (e.getSource() == sendButton) {

			String message = inputTextField.getText();

			try {

				if (!message.isEmpty()) {

					sendMessage(message);


					SwingUtilities.invokeLater(() -> {
						textArea.append("You: " + message + "\n");

					});

					inputTextField.setText(""); // Clear the input field

				}

			} catch (Exception ex) { // Use a different variable name for the catch block

				ex.printStackTrace();

			}

		}


		else if (e.getSource() == callButton) {


			// The "Call" button was clicked


			if (!isCalling) {


				startCall();
				callButton.setText("End Call");


			} else {


				stopCall();
				callButton.setText("Call");


			}


		}


	}


	private static void receiveMessage() {


		try {


			byte[] buffer = new byte[1024];


			DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
			messageSocket.receive(receivePacket); // Λήψη μηνύματος
			String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
			SwingUtilities.invokeLater(() -> {
				textArea.append("Friend: " + receivedMessage + "\n");

			});


		} catch (IOException e) {

			e.printStackTrace();


		}


	}


	private static void sendMessage(String message) {


		try {

			if (messageSocket == null) { // Έλεγχος αν το socket είναι null
				messageSocket = new DatagramSocket(); // Ξεκινήστε το socket εάν είναι απαραίτητο

			}

			if (remoteIP == null || remotePortMessage == 0) {

				throw new IllegalStateException("Remote IP ή Port δεν έχουν οριστεί!");

			}


			byte[] buffer = message.getBytes(); // Μετατροπή μηνύματος σε byte array
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length,

					new InetSocketAddress(remoteIP, remotePortMessage)); // Δημιουργία πακέτου

			messageSocket.send(packet); // Αποστολή πακέτου


		} catch (IOException e) {

			e.printStackTrace(); // Εκτύπωση σφαλμάτων

		}

	}


	private static void startCall() {

		isCalling = true; // Θέτουμε την κατάσταση της κλήσης σε ενεργή

		try {


			// Ορισμός μορφής ήχου με συχνότητα δειγματοληψίας 8000 samples/sec, (ii)


			// μέγεθος δείγματος 8 bits (1 byte), (iii) μονοφωνικό κανάλι (1 κανάλι) και

			// (iv) signed δείγματα.


			AudioFormat format = new AudioFormat(8000.0f, 8, 1, true, true);
			DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format); // είσοδος (μικρόφωνο)
			DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, format); // έξοδος (ηχείο)

			microphone = (TargetDataLine) AudioSystem.getLine(targetInfo); // Λήψη γραμμής μικροφώνου
			speaker = (SourceDataLine) AudioSystem.getLine(sourceInfo); // Λήψη γραμμής ηχείου
			microphone.open(format); // Άνοιγμα μικροφώνου
			speaker.open(format); // Άνοιγμα ηχείου
			microphone.start(); // Εκκίνηση εγγραφής μικροφωνου
			speaker.start(); // Εκκίνηση αναπαραγωγής ηχείου

			// αποστολή δεδομένων ήχου


			Thread sendAudio = new Thread(() -> {
				try {

					byte[] buffer = new byte[1024]; // Buffer για δεδομένα ήχου

					while (isCalling) {


						int bytesRead = microphone.read(buffer, 0, buffer.length); // Ανάγνωση δεδομένων από το

																					// μικρόφωνο
						DatagramPacket packet = new DatagramPacket(buffer, bytesRead,
								new InetSocketAddress(remoteIP, remotePortVoice)); // Δημιουργία πακέτου UDP

						voiceSocket.send(packet); // Αποστολή πακέτου

					}


				} catch (IOException e) {

					e.printStackTrace(); // Καταγραφή σφαλμάτων


				}


			});


			// λήψη δεδομένων ήχου


			Thread receiveAudio = new Thread(() -> {

				try {

					byte[] buffer = new byte[1024]; // Buffer για δεδομένα ήχου

					while (isCalling) {

						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);


						voiceSocket.receive(packet); // Λήψη πακέτου UDP
						speaker.write(packet.getData(), 0, packet.getLength()); // Αναπαραγωγή των δεδομένων από το

																				// ηχείο
					}


				} catch (IOException e) {


					e.printStackTrace(); // Καταγραφή σφαλμάτων


				}


			});


			sendAudio.start();
			receiveAudio.start();


			textArea.append("Call started.\n"); // η κλήση ξεκίνησε


		} catch (LineUnavailableException e) {

			e.printStackTrace(); // σε περίπτωση μη διαθέσιμης γραμμής


		}


	}


	private static void stopCall() {


		isCalling = false;


		if (microphone != null)
			microphone.close(); // Κλείσιμο μικροφώνου


		if (speaker != null)
			speaker.close(); // Κλείσιμο ηχείου

		textArea.append("Call ended.\n"); // η κλήση τερματίστηκε


	}


	/**

	 * 

	 * These methods have to do with the GUI. You can use them if you wish to define

	 * 

	 * what the program should do in specific scenarios (e.g., when closing the

	 * 

	 * window).

	 * 

	 */


	@Override


	public void windowActivated(WindowEvent e) {


		// TODO Auto-generated method stub


	}


	@Override


	public void windowClosed(WindowEvent e) {


		// TODO Auto-generated method stub


	}


	@Override


	public void windowClosing(WindowEvent e) {


		// TODO Auto-generated method stub


		dispose();
		System.exit(0);


	}


	@Override


	public void windowDeactivated(WindowEvent e) {


		// TODO Auto-generated method stub


	}


	@Override


	public void windowDeiconified(WindowEvent e) {


		// TODO Auto-generated method stub


	}


	@Override


	public void windowIconified(WindowEvent e) {


		// TODO Auto-generated method stub


	}


	@Override


	public void windowOpened(WindowEvent e) {


		// TODO Auto-generated method stub


	}


}