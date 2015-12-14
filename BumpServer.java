//Electif Application Concurrente Mobile 2015
//BUMP - Chat organisé par rooms

//Jeanne Bonnet
//Raphaël Moulin
//Damien Turlay
//Quentin Zakoian


package bumpserver;


//Importation des bibliothèques nécessaires
import java.awt.Color;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;


//CLASS : Serveur de l'application BUMP
public class BumpServer extends JFrame {
	
	//Définition des variables principales
	JLabel L_PortBr;
	JLabel L_Info;
	JTextField TF_Port;
	JButton B_ToggleConnect;
	JTextPane TA_ListRooms;
	JTextPane TA_ListPeople;
	JScrollPane SP_ScrollRooms;
	JScrollPane SP_ScrollPeople;
	StyledDocument SD_ListRoomsDoc;
	StyledDocument SD_ListPeopleDoc;
	SimpleAttributeSet SAS_bold = new SimpleAttributeSet();
	SimpleAttributeSet SAS_info = new SimpleAttributeSet();
	SimpleAttributeSet SAS_info_bold = new SimpleAttributeSet();
	SimpleAttributeSet SAS_error = new SimpleAttributeSet();
	SimpleAttributeSet SAS_error_bold = new SimpleAttributeSet();

	static ServerSocket serverSocket;
	static Socket socket; 
	static int clientId[] = new int[100];
	static int canalId[] = new int[100];
	static int clientCanal[] = new int[clientId.length];
	static ObjectInputStream input[] = new ObjectInputStream[clientId.length];
	static ObjectOutputStream output[] = new ObjectOutputStream[clientId.length];
	static String nicks[] = new String[clientId.length];
	static String canaux[] = new String[canalId.length];
	static String message;
	static String who;
	static String type;
	static int canal;
	static boolean signal;
    
	
	//Constructeur du serveur
	BumpServer() {
		
		//Construction de la fenêtre 
    	super("BUMP - Server");
    	setSize(330, 540);
    	setLocationRelativeTo(null);
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	setResizable(false);
		
		//Mise en place de l'interface
    	createComponente();
    	createScrollBar();
    	createInterface();
    	setComponente();
    	setListener();
		
		//Initialisation du contenu
    	setVisible(true);
		emptyClients();
		emptyCanaux();
	}
    
	
	//Création des textes et des boutons
	private void createComponente() {
    	L_PortBr = new JLabel("Port:");    
    	L_Info = new JLabel("En attente de connexion.");    
    	TF_Port = new JTextField("45678");
    	B_ToggleConnect = new JButton("Connexion");
	}
    
	
	//Création des champs de texte scrollables
	private void createScrollBar() {
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
		B_ToggleConnect.setBounds(170, 10, 150, 20);
    	SP_ScrollRooms.setBounds(10, 40, 150, 430);
    	SP_ScrollPeople.setBounds(170, 40, 150, 430);
		L_Info.setBounds(10, 480, 310, 20);
		
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
	private void setComponente() {
    	Container content = getContentPane();
    	content.setLayout(null);
    	content.add(L_PortBr);
    	content.add(TF_Port);
    	content.add(B_ToggleConnect);
    	content.add(SP_ScrollPeople);
    	content.add(SP_ScrollRooms);
		content.add(L_Info);
	}
    
	
	//Mise en place des évènements
	private void setListener() {
		
		//Lors du clic du bouton de (dé)connexion
    	B_ToggleConnect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int p = 0;
				boolean connected = !B_ToggleConnect.getText().equals("Connexion");
				emptyClients();
				emptyCanaux();
				
				//On cherche à se déconnecter
				if (connected) {
					try {
						serverSocket.close();
					} catch(Exception ex) { }
					
					//On ferme tous les canaux
					for(int i = 0; i < input.length; i++ ) {
						try {
							input[i].close();
							output[i].close();
						} catch(Exception ex) { }
					}
					
					B_ToggleConnect.setText("Connexion");
					L_Info.setText("En attente de connexion.");
				}
				
				//On cherche à se connecter
				else {
					try {
						String port = TF_Port.getText();
						p = Integer.parseInt(port);
						serverSocket = new ServerSocket(p, 100);
						signal = true;
					} catch(NumberFormatException | IOException ex) {
						signal = false;
						L_Info.setText("Echec de la connexion, port indisponible?");
					}
					
					//Si le socket est ouvert, on ouvre une première room
					//On créé ensuite un thread pour accueillir les nouveaux clients
					if(signal == true) {
						canalId[0] = 1;
						canaux[0] = "BUMP";
						AcceptNewClients ob1 = new AcceptNewClients("RunServer");
						B_ToggleConnect.setText("Déconnexion");
						L_Info.setText("Serveur connecté.");
					}
				}
				
				//Mise à jour de l'affichage des clients et des rooms
				updateClients();
				updateCanaux();
			}
		});
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
							String.format("%02d ", i), SAS_error);
						SD_ListPeopleDoc.insertString(SD_ListPeopleDoc.getLength(), 
							nicks[i]+"\n", null);
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
							String.format("%02d ", i), SAS_error);
						SD_ListRoomsDoc.insertString(SD_ListRoomsDoc.getLength(), 
							canaux[i]+"\n", null);
					} catch (Exception ex) { }
				}
			}
		}
		
		//On descend le scroll à la fin
		TA_ListRooms.setCaretPosition(TA_ListRooms.getDocument().getLength());
	}
	
	
	//Envoi d'un message pour tous les clients d'une room
	void sendMessage(int canal, String type, String who, String message) {
    	for(int i = 0; i < clientId.length; i++) {
        	if(clientId[i] == 1 &&
			   clientCanal[i] == canal) {
            	sendSom(i, type, who, message);
        	}
    	}
	}
	
	
	//Envoi d'un message à tous les clients
	void sendAll(String type, String who, String message) {
    	for(int i = 0; i < clientId.length; i++) {
        	if(clientId[i] == 1) {
            	sendSom(i, type, who, message);
        	}
    	}
	}
    

	//Envoi d'un message à un client
	//4 données principales
	// id : client concerné 
	// type : type de la donnée envoyée
	// who : qui (surnom) à envoyer la donnée
	// message : contenu du message
	void sendSom(int id, String type, String who, String message)
	{
    	try
    	{
        	output[id].writeObject(type);
        	output[id].writeObject(who);
        	output[id].writeObject(message);
        	output[id].flush();
    	} catch(Exception e) { }
	}
    
	
	//THREAD : Pour accueillir les nouveaux clients
	public class AcceptNewClients implements Runnable {
    	Thread t;
		
		//Constructeur
    	AcceptNewClients(String nameThread) {
        	startThread(nameThread);
    	}
		
		
		//Démarrage du thread
		private void startThread(String nameThread) {
			t = new Thread(this, nameThread);
			t.start();
		}
   	 
		
    	//Dès que le thread est lancé, On attend les nouveaux clients
		@Override
    	public void run() {  
        	while(true) {
            	try { 
					try {
                    	socket = serverSocket.accept();
                	} catch (Exception ex) { break; }
					
					//On recherche le premier id disponible
					//Puis on place le client dans la room principale : BUMP
                	int dispo = -1; 
                	for(int i = 0; i < clientId.length; i++)
                	{
						if(clientId[i] == 0) 
                    	{
                        	clientId[i] = 1;
							clientCanal[i] = 0;
                        	dispo = i;
                        	break;
                    	}
                	}
					
					//Plus de place...
					if (dispo == -1)
						break;
					
					//On attribue un stream d'entrée et de sortie pour chaque nouveau client
                	output[dispo] = new ObjectOutputStream(socket.getOutputStream());
                	output[dispo].flush();
                	input[dispo] = new ObjectInputStream(socket.getInputStream());
					
					//On recupère les premières informations sur le client
					try {
						input[dispo].readObject(); //canal
						input[dispo].readObject(); //type = "connexion"
						nicks[dispo] = (String) input[dispo].readObject();
						input[dispo].readObject(); //message = ""
					} catch (IOException | ClassNotFoundException ex1) {
						nicks[dispo] = "Client_" + dispo;
					}

					//Message de retour contenant notamment l'id attribué
                	sendSom(dispo, "connexion", ""+dispo, ""+clientCanal[dispo]);
					
					//Message sur la room pour rajouter le nouveau client chez tout le monde
                	sendMessage(clientCanal[dispo], "newclient", ""+dispo, nicks[dispo]); 
					
					//Envoi au nouveau client de la liste des rooms
                   	for (int i = 0; i < canalId.length; i++) {
						if (canalId[i] != 0)
							sendSom(dispo, "canal", ""+i, canaux[i]);
					}
					
					//On place le client dans la room principale
					//Principalement pour la mise à jour de l'affichage du client
					sendSom(dispo, "changecanal", ""+dispo, ""+clientCanal[dispo]);
										
					//Envoi au nouveau client de la liste des clients présents dans la room
					for (int i = 0; i < clientId.length; i++) {
						if (clientId[i] != 0 && clientCanal[i] == clientCanal[dispo])
							sendSom(dispo, "client", ""+i, nicks[i]);
					}
					
					//Mise à jour de la liste des clients
					updateClients();
                   	
					//On ouvre un thread pour la réception des messages de ce nouveau client
                	ReceiveMsgs ob2 = new ReceiveMsgs(dispo, "StartReceiveMsgs_" + dispo);
               	 
            	}
            	catch(IOException e) { }             
        	}
    	}
	}

    
	//THREAD : Nécessaire à la reception des messages de chaque client
	public class ReceiveMsgs implements Runnable
	{
    	int C_id;
    	ReceiveMsgs ob1;
    	Thread t;
   	 
		//Constructeur
    	ReceiveMsgs(int C_id, String nameThread) {
        	this.C_id = C_id;
			startThread(nameThread);
    	}
		
		
		//Lancement du thread
		private void startThread(String nameThread) {
        	t = new Thread(this, nameThread);
        	t.start();
    	}
		
		
		//Dès que le thread est lancé, On attend les messages 
		@Override
    	public void run() {
        	do {    
				//Si un problème apparait, cela signifie que le client s'est déconnecté
            	try {    
                	canal = Integer.parseInt((String) input[C_id].readObject());
					type = (String) input[C_id].readObject();
                	who = (String) input[C_id].readObject(); 
                	message = (String) input[C_id].readObject();
            	} catch(IOException | ClassNotFoundException e) { 
					type = "deconnexion"; 
				}
				
				//Dans le cas d'une déconnexion...
            	if(type.equals("deconnexion")) {
					
					//On avertit tous les autres clients de la room
                	sendMessage(clientCanal[C_id], "deconnexion", ""+C_id, who); 
               	 
					//Réinitialisation de toutes les données liées au client
                	clientId[C_id] = 0; 
					clientCanal[C_id] = 0;
                	nicks[C_id] = "";
                	try {
                    	input[C_id].close();
                    	output[C_id].close();
                	} catch(Exception ex) { }
					
					//Mise à jour de la liste des clients
					updateClients();
            	}
				
				//Un client veut créer une room
				else if (message.startsWith("/create")) {
					sendSom(C_id, "message", who, message); 
					
					//Le nom de la room est vide
					if (message.substring(7).trim().isEmpty()) {
						sendSom(C_id, "error", "canal", "empty");
					}
					
					
					else {
						
						//On cherche un id disponible pour cette room
						int dispo = -1; 
						for(int i = 0; i < canalId.length; i++)
						{
							if(canalId[i] == 0) 
							{
								canalId[i] = 1;
								canaux[i] = message.substring(8);
								dispo = i;
								break;
							}
						}
						
						//Si un id est disponible, on avertit tous les clients
						if (dispo != -1)
							sendAll("newcanal", ""+dispo, canaux[dispo]);
					}
					
					//Mise à jour des rooms
					updateCanaux();
				}
				
				//Le client veut rejoindre une room
				else if (message.startsWith("/join")) {
					sendSom(C_id, "message", who, message); 
					
					//Le client doit spécifier l'id de la room
					try {
						
						//On regarde si la room demandé existe
						canal = Integer.parseInt(message.substring(5).trim());
						int dispo = -1;
						for (int i = 0; i < canalId.length; i++) {
							if (canal == i && canalId[i] == 1) {
								dispo = i;
								break;
							}
						}
						
						//Si non, il y a une erreur
						if (dispo == -1)
							sendSom(C_id, "error", "canal", "invalid");
						
						else {
							canal = clientCanal[C_id];
							clientCanal[C_id] = dispo;
							
							//On avertit les clients de l'ancienne room que le client est parti
							sendMessage(canal, "deconnexion", ""+C_id, who);
							
							//On change la room du client
							sendSom(C_id, "changecanal", ""+C_id, ""+dispo);
							
							//On avertit les clients de cette nouvelle room
							sendMessage(clientCanal[C_id], "newclient", ""+C_id, who); 
							
							//Mise à jour de la liste des clients pour le nouveau client
							for (int i = 0; i < clientId.length; i++) {
								if (clientId[i] != 0 && clientCanal[i] == clientCanal[C_id])
									sendSom(C_id, "client", ""+i, nicks[i]);
							}
						}
					} catch (Exception ex) {
						sendSom(C_id, "error", "canal", "invalid");
					}
				}
				
				//Le message saisi est une commande invalide
				else if (message.startsWith("/")) {
					sendSom(C_id, "message", who, message); 
					sendSom(C_id, "error", "command", "unknown");
				}
				
				//Dans tous les autres cas, on transmet le message aux clients de la room
				else
                	sendMessage(clientCanal[C_id], "message", who, message); 
				
        	} while(!type.equals("deconnexion"));
    	}
	}

	//Lancement du serveur
	public static void main(String args[]) {
    	BumpServer bumpServer;
		bumpServer = new BumpServer();
	}	
}