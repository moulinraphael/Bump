//Electif Application Concurrente Mobile 2015
//BUMP - Chat organisé par rooms

//Jeanne Bonnet
//Raphaël Moulin
//Damien Turlay
//Quentin Zakoian


package bumpclient;


//Importation des bibliothèques nécessaires
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.JTextPane;


//CLASS : Client de l'application BUMP
public class BumpClient extends JFrame {
	
	//Définition des variables principales
	JLabel L_PortBr;
	JLabel L_IP;
	JTextField TF_Port;
	JTextField TF_Message;
	JTextField TF_IP;
	JTextField TF_Nick;
	JButton B_ToggleConnect;
	JTextPane TA_ListRooms;
	JTextPane TA_ListPeople;
	JTextPane TA_Chat;
	JScrollPane SP_ScrollChat;
	JScrollPane SP_ScrollRooms;
	JScrollPane SP_ScrollPeople;
	StyledDocument SD_ChatDoc;
	StyledDocument SD_ListRoomsDoc;
	StyledDocument SD_ListPeopleDoc;
	SimpleAttributeSet SAS_bold = new SimpleAttributeSet();
	SimpleAttributeSet SAS_info = new SimpleAttributeSet();
	SimpleAttributeSet SAS_info_bold = new SimpleAttributeSet();
	SimpleAttributeSet SAS_error = new SimpleAttributeSet();
	SimpleAttributeSet SAS_error_bold = new SimpleAttributeSet();
	
	static Socket joinSocket = null;
	static ObjectOutputStream input;
	static ObjectInputStream output;	
	static String who;
	static String type;
	static String message;
	static String ip;
	static int clientId[] = new int[100];
	static String nicks[] = new String[clientId.length];
	static int canalId[] = new int[100];
	static String canaux[] = new String[canalId.length];
	static int canal = 0;
	static int id;
	
	
	//Constructeur du client
	BumpClient() {
		
		//Construction de la fenêtre 
		super("BUMP - Client");
		setSize(840, 540);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);
		
		//Mise en place de l'interface
		createComponent();
		createScrollBar();
		createInterface();
		setComponent();
		setListener();
		
		//Initialisation du contenu
		setVisible(true);
		emptyClients();
		emptyCanaux();
 	}
	
	
	//Création des textes et des boutons
	private void createComponent() {
		L_PortBr = new JLabel("Port:");
		TF_Port = new JTextField("45678");
		L_IP = new JLabel("IP:");
		TF_IP = new JTextField("127.0.0.1");
		B_ToggleConnect = new JButton("Connexion");
		TF_Nick = new JTextField("Surnom");
		TF_Message = new JTextField();
	}
	
	
	//Création des champs de texte scrollables
	private void createScrollBar() { 
		TA_Chat = new JTextPane(); 
		SD_ChatDoc = TA_Chat.getStyledDocument();
		TA_Chat.setEditable(false);
    	SP_ScrollChat = new JScrollPane(TA_Chat);
    	SP_ScrollChat.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
   	 
    	TA_ListPeople = new JTextPane();
		SD_ListPeopleDoc = TA_ListPeople.getStyledDocument();
    	TA_ListPeople.setEditable(false);
    	SP_ScrollPeople = new JScrollPane(TA_ListPeople);
    	SP_ScrollPeople.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
   	 
    	TA_ListRooms = new JTextPane();
		SD_ListRoomsDoc = TA_ListRooms.getStyledDocument();
    	TA_ListRooms.setEditable(false);
    	SP_ScrollRooms = new JScrollPane(TA_ListRooms);
    	SP_ScrollRooms.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	}
	
	
	//Positionnement des élements de l'interface
	//Création des styles
	private void createInterface() {
		L_PortBr.setBounds(10, 10, 50, 20);
		TF_Port.setBounds(40, 10, 120, 20);
		L_IP.setBounds(170, 10, 20, 20);
		TF_IP.setBounds(190, 10, 480, 20);
		B_ToggleConnect.setBounds(680, 10, 150, 20);
    	SP_ScrollRooms.setBounds(10, 40, 150, 430);
    	SP_ScrollChat.setBounds(170, 40, 500, 430);
    	SP_ScrollPeople.setBounds(680, 40, 150, 430);
		TF_Nick.setBounds(10, 480, 150, 20);
		TF_Message.setBounds(170, 480, 660, 20);
		
    	StyleConstants.setBold(SAS_bold, true);
    	StyleConstants.setItalic(SAS_info, true);
    	StyleConstants.setBold(SAS_info_bold, true);
    	StyleConstants.setForeground(SAS_info_bold, Color.GREEN);
    	StyleConstants.setForeground(SAS_info, Color.GREEN);
    	StyleConstants.setItalic(SAS_error, true);
    	StyleConstants.setBold(SAS_error_bold, true);
    	StyleConstants.setForeground(SAS_error_bold, Color.RED);
    	StyleConstants.setForeground(SAS_error, Color.RED);
	}
	
	
	//Insertion des éléments dans la fenêtre
	private void setComponent() {
		Container content = getContentPane();
		content.setLayout(null);
		content.add(L_PortBr);
		content.add(TF_Port);
		content.add(L_IP);
		content.add(TF_IP);
		content.add(B_ToggleConnect);
		content.add(TF_Nick);
    	content.add(SP_ScrollChat);
    	content.add(SP_ScrollPeople);
    	content.add(SP_ScrollRooms);
		content.add(TF_Message);
		
		TF_Message.setEditable(false);
	}
	
	
	//Mise en place des évènements
	private void setListener() {
		
		//Lors du clic du bouton de (dé)connexion
		B_ToggleConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean connected = !B_ToggleConnect.getText().equals("Connexion");
				emptyClients();
				emptyCanaux();
				
				//On cherche à se déconnecter
				if (connected) {
					
					//Fermeture de la connexion
					try {
						joinSocket.close();
						output.close();
						input.close();
					} catch(Exception ex) { }
					
					joinSocket = null;
					B_ToggleConnect.setText("Connexion");
					TF_Nick.setEditable(true);
					TF_Message.setEditable(false);
				}
				
				//On cherche à se connecter
				else {
					String port = TF_Port.getText();
					int p;
					p = Integer.parseInt(port);
					ip = TF_IP.getText();
					
					
					try {
						joinSocket = new Socket(ip, p);
					} catch(Exception ex) {
						printMessage("<Erreur> ", SAS_error_bold);
						printMessageln("Serveur non disponible!", SAS_error);
					}
					
					//Si la connexion est ouverte, on instancie le thread de communication
					if(joinSocket != null) {
						ConnectServer ob1 = new ConnectServer();
						B_ToggleConnect.setText("Déconnexion");
						TF_Nick.setEditable(false);
						TF_Message.setEditable(true);
					}
				}
				
				//Mise à jour de l'affichage des clients et des rooms
				updateClients();
				updateCanaux();
			}
		});
		
		
		//Lors de l'écriture dans le champ de message
		TF_Message.addKeyListener(
			new KeyListener() {
				@Override
				public void keyPressed(KeyEvent e) {
					int keycode = e.getKeyCode();
					
					//Le client appuie sur la touche ENTREE
					if(keycode == KeyEvent.VK_ENTER) {
						sendData("message", TF_Nick.getText(), TF_Message.getText());
						TF_Message.setText("");
					}
				}
				
				@Override
				public void keyReleased(KeyEvent e) { }
				@Override
				public void keyTyped(KeyEvent e) { }
			}
		);
	}
	
	
	//Vidage des clients
	void emptyClients() {
		for (int i = 0; i < clientId.length; i++) {
			clientId[i] = 0;
			nicks[i] = "";
		}
	}
	
	
	//Vidage des rooms
	void emptyCanaux() {
		for (int i = 0; i < canalId.length; i++) {
			canalId[i] = 0;
			canaux[i] = "";
		}
	}
	
	
	//Mise à jour de l'affichage des clients
	void updateClients() {
		boolean connected = !B_ToggleConnect.getText().equals("Connexion");
		TA_ListPeople.setText("");
		
		//Les clients ne sont affichés que si le serveur est connecté
		if (connected) {
			for (int i = 0; i < clientId.length; i++) {
				if (clientId[i] != 0) {
					try {
						SD_ListPeopleDoc.insertString(SD_ListPeopleDoc.getLength(), 
							String.format("%02d ", i), i == id ? SAS_error_bold : SAS_error);
						SD_ListPeopleDoc.insertString(SD_ListPeopleDoc.getLength(), 
							nicks[i]+"\n", i == id ? SAS_bold : null);
					} catch (Exception ex) { }
				}
			}
		}
		
		//On descend le scroll à la fin
		TA_ListPeople.setCaretPosition(TA_ListPeople.getDocument().getLength());
	}
	
	
	//Mise à jour de l'affichage des rooms
	void updateCanaux() {
		boolean connected = !B_ToggleConnect.getText().equals("Connexion");
		TA_ListRooms.setText("");
		
		
		//Les rooms ne sont affichées que si le serveur est connecté
		if (connected) {
			for (int i = 0; i < canalId.length; i++) {
				if (canalId[i] != 0) {
					try {
						SD_ListRoomsDoc.insertString(SD_ListRoomsDoc.getLength(), 
							String.format("%02d ", i), i == canal ? SAS_error_bold : SAS_error);
						SD_ListRoomsDoc.insertString(SD_ListRoomsDoc.getLength(), 
							canaux[i]+"\n", i == canal ? SAS_bold : null);
					} catch (Exception ex) { }
				}
			}
		}
		
		//On descend le scroll à la fin
		TA_ListRooms.setCaretPosition(TA_ListRooms.getDocument().getLength());
	}
	
	
	//Envoi de données vers le serveur
	// canal : id de la room du client 
	// type : type de la donnée envoyée
	// who : qui (surnom) à envoyer la donnée
	// message : contenu du message
	void sendData(String type, String who, String message) {
		try { 
			input.writeObject(""+canal);
			input.writeObject(type);
			input.writeObject(who);
			input.writeObject(message);
			input.flush(); 
		} catch(Exception e) { }
	}
	
	
	//Affichage d'un message dans la zone de messages
	void printMessage(String message, SimpleAttributeSet style) {
		try {
			SD_ChatDoc.insertString(SD_ChatDoc.getLength(), message, style);
		} catch (Exception ex) { }
		TA_Chat.setCaretPosition(TA_Chat.getDocument().getLength());
	}
	
	
	//Affichage d'un message avec saut de ligne
	void printMessageln(String message, SimpleAttributeSet style) {
		printMessage(message+"\n", style);
	}
	
	
	//THREAD : Pour ouvrir la communication avec le serveur
	class ConnectServer implements Runnable {
		ConnectServer ob1;
		Thread t;
		
		//Constructeur
		ConnectServer() {
			startThread();
		}
		
		
		//Démarrage du thread
		private void startThread() {
			t = new Thread(this, "RunServer");
			t.start();
		}
		
		
		//Dès que le thread est lancé, on lance la communication
		@Override
		public void run() {
			try {
				try {
					
					//Ouverture des canaux entrant et sortant
					input = new ObjectOutputStream(joinSocket.getOutputStream());
					input.flush();
					output = new ObjectInputStream(joinSocket.getInputStream());
					
					sendData("connexion", TF_Nick.getText(), "");
					
					//Récupération de l'id du client et du canal (par défaut 0 "BUMP")
					try {
						output.readObject(); // type == "connexion"
						id = Integer.parseInt((String) output.readObject());
						canal = Integer.parseInt((String) output.readObject());
						
					} catch(IOException | ClassNotFoundException ex) { }
					
					//On vide les clients et les rooms affichées
					emptyClients();
					emptyCanaux();
					updateClients();
					updateCanaux();

					
				} catch(IOException | NumberFormatException e) { }
				
				//Lancement du thread pour la réception de tous les messages suivants
				ReceiveMessage message1 = new ReceiveMessage();
			} catch (Exception ex) { }
		}
	}	
	
	
	//THREAD : Pour la réception des messages
	class ReceiveMessage implements Runnable {
		ReceiveMessage ob1;
		Thread t;
		
		//Constructeur
		ReceiveMessage() {
			startThread();
		}
		
		
		//Démarrage du thread
		private void startThread() {
			t = new Thread(this, "ReceiveMessage");
			t.start();
		}
		
		
		//Dès que le thread est lancé, on attend les nouveaux messages
		@Override
		public void run() {
			try {
				while (true) {	
					try {	
						type = (String) output.readObject(); 
						who = (String) output.readObject(); 
						message = (String) output.readObject(); 
						
						//En fonction du type de message on effectue diverses actions
						switch (type) {
							
							//C'est un message simple, on l'affiche
							case "message":
								printMessage("<" + who + "> ", SAS_bold);
								printMessageln(message, null);
								break;
							
							//Un nouveau client arrive sur la room
							//pas de break !
							case "newclient":
								
								if (Integer.parseInt(who) == id)
									break;
								
								printMessage("<" + message + "> ", SAS_bold);
								printMessageln("Je suis tout nouveau", SAS_info);						
							//Mise à jour de la liste des clients connectés
							//Appelée notamment dès que l'on arrive sur une room
							//newclient effectue aussi ces actions
							case "client":
								clientId[Integer.parseInt(who)] = 1;
								nicks[Integer.parseInt(who)] = message;
								updateClients();
								break;
							
							//Une nouvelle room est créée
							//pas de break !
							case "newcanal":
								printMessage("<Server> ", SAS_bold);
								printMessageln("Un nouveau canal vient d'être créé : "+message, SAS_info);
								
							//Mise à jour de la liste des rooms
							//Notamment à la connexion
							//newcanal effectue aussi ces actions
							case "canal":
								canalId[Integer.parseInt(who)] = 1;
								canaux[Integer.parseInt(who)] = message;
								updateCanaux();
								break;
								
							//Changement de canal
							//Mise à jour des clients notamment
							case "changecanal":
								
								if (Integer.parseInt(who) != id)
									break;
								
								canal = Integer.parseInt(message);
								TA_Chat.setText("");
								printMessage("<Server> ", SAS_bold);
								printMessageln("Bienvenue sur le canal : "+canaux[canal], SAS_info);
								updateCanaux();
								emptyClients();
								break;
								
							//Déconnexion d'un client de la room
							case "deconnexion":
								
								if (Integer.parseInt(who) == id)
									break;
								
								printMessage("<" + nicks[Integer.parseInt(who)] + "> ", SAS_bold);
								printMessageln("Au revoir les copains !", SAS_info);
								clientId[Integer.parseInt(who)] = 0;
								nicks[Integer.parseInt(who)] = "";
								updateClients();
								break;
							
							//Une erreur s'est produite
							case "error":
								printMessage("<Server> ", SAS_bold);
								
								//Pour la commande create
								if (who.equals("canal") && message.equals("empty"))
									printMessageln("Le nom du canal ne peut être vide", SAS_error);
								
								//La commande est inconnue
								else if (who.equals("command") && message.equals("unknown"))
									printMessageln("La commande est inconnue", SAS_error);
								
								//Pour la commande join
								else if (who.equals("canal") && message.equals("invalid"))
									printMessageln("Le canal saisi est invalide", SAS_error);
								
								//Autre erreur
								else
									printMessageln("Une erreur vient de se produire", SAS_error);
								break;
							default:
								break;
						}
						
						
					} catch(IOException | ClassNotFoundException ex) { 
						
						//Le serveur s'est déconnecté
						printMessage("<Info> ", SAS_info_bold);
						printMessageln("Vous êtes déconnecté", SAS_info);
						emptyClients();
						emptyCanaux();
						updateClients();
						updateCanaux();
						TF_Nick.setEditable(true);
						TF_Message.setEditable(false);
						B_ToggleConnect.setText("Connexion");
						
						//On ferme tous les canaux
						try {
							output.close();
							input.close();
							joinSocket.close();
						} catch(Exception ex2) { }
						
						break;
					}
				}
			} catch (Exception ex) { }
		}
	}
	
	//Lancement du client
	public static void main(String args[]) {
		BumpClient bumpClient;
		bumpClient = new BumpClient();
	}
}
