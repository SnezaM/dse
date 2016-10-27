package network.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import management.Benutzerverwaltung;
import management.MyException;
import management.Produktgruppeverwaltung;
import management.Produktverwaltung;

/**
 * 
 * Diese Klasse stellt den eigentlichen Serverprozess dar. Mittels der Generalisierungsbeziehung
 * von Thread wird die Nebenl�ufigkeit erm�glicht.
 *
 */
public class SimpleServer extends Thread {

	private static Benutzerverwaltung benver = Benutzerverwaltung.getInstance();
	private static Produktverwaltung prodver = Produktverwaltung.getinstance();
	private static Produktgruppeverwaltung prodgruver = Produktgruppeverwaltung.getinstance();
	
	
	private Socket clientSocket;

	private OutputStream rawOut;
	private ObjectOutputStream outObj;
	private DataOutputStream outData;

	private InputStream rawIn;
	private DataInputStream inData;

	public SimpleServer(Socket clientsocket) {
		this.clientSocket = clientsocket;
		start();
	}

	public static void main(String[] args) {
		try {
			@SuppressWarnings("resource")
			ServerSocket serverSocket = new ServerSocket(1234);
			System.out.println("Server gestartet(Port " + serverSocket.getLocalPort() + ")");
			try {
				while (true) {
					System.out.println("Server wartet auf eine neue Verbindung!");
					
					new SimpleServer(serverSocket.accept());
				}
			} catch (IOException e) {
				System.err.println("SimpleServer:main: Connetion Failed");
				System.exit(1);
			}

		} catch (IOException e) {
			System.err.println("SimpleServer:main: " + e.getMessage());
			System.exit(1);
		}
	}

	public void run() {
		System.out.println("Neuer Thread erfolgreich gestartet(f�r IP-Adresse: "+this.clientSocket.getInetAddress()+")!");

		try {
		//anlegen der Streams(out und dann in)
			rawOut = this.clientSocket.getOutputStream();
			outObj = new ObjectOutputStream(rawOut);
			outData = new DataOutputStream(rawOut);

			rawIn = this.clientSocket.getInputStream();
			inData = new DataInputStream(rawIn);

			
			String eingabe;
			try{
				while((eingabe = inData.readUTF())!=null && !eingabe.equals("ende")){
					String[] eingabeGesplittet = eingabe.trim().split("-");
					String anfrage = eingabeGesplittet[0];
		
					System.out.println("Befehl von Client("+this.clientSocket.getInetAddress()+"): "+anfrage);
					if (anfrage.equals("ende")){
						System.out.println("Client("+this.clientSocket.getInetAddress()+") beendete Verbindung mittels Eingabe 'ende'");
						break;
					}
		
					
					if (anfrage.equals("getBenutzer")){
						outObj.writeObject(benver.getBenutzerListe());
						continue;
					}
					
					
					//Loginpr�fung
					if (anfrage.equals("pruefeLogin")) {
						try{
							String username = eingabeGesplittet[1];
							String password = eingabeGesplittet[2];
							if(username == null || password == null) throw  new MyException("Ein Parameter ist null"); 
							if(!benver.pruefeLogin(username, password)) throw new MyException("Userdaten stimmen nicht mit Passwortdaten �berein("+username+"/"+password+")");
							outData.writeBoolean(true);
						}catch(MyException e){
							System.err.println("SimpleServer:pruefeLogin:"+e.getMessage());
							outData.writeBoolean(false);
						}
						continue;
					}
					
					
					//getProdukte
					if (anfrage.equals("getProdukte")){
						outObj.writeObject(prodver.getAlleProdukt());
						continue;
					}
				
					//Get benutzer nach Usernamen
					if(anfrage.equals("getBenutzerByUname")){
						System.out.println("Client("+this.clientSocket.getInetAddress()+"): "+anfrage+", Uname: "+eingabeGesplittet[1]);
						outObj.writeObject(benver.getBenByUsername(eingabeGesplittet[1]));
						continue;
					}
				}
			}catch(Exception e){
				//Da Port geschlossen wird, w�hrend er noch abh�rt, wird eine Exception geworfen, welche hiermit abgefangen wird
			}

			System.out.println("Server: Beende Connection mit Client("+this.clientSocket.getInetAddress()+")");
			outObj.flush();
			outObj.close();
			outData.close();
			rawOut.close();

			//inObj.close();
			rawIn.close();
		}catch(IOException e)
		{
			System.err.println("SimpleServer:run: bei Client("+this.clientSocket.getInetAddress()+") " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}

}
