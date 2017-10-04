import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

public class Client {

	ArrayList<String> check_list_1 = new ArrayList<String>();

	private int listeningPort;
	private int serverPort;
	private int uploadPort;
	private int downloadPort;
	
	private int totalSize = 0;
	
	private int clientId;

	private Socket c1;

	public Client(int serverPort, int listeningPort, int uploadPort, int downloadPort, int clientId) {
		this.serverPort = serverPort;
		this.listeningPort = listeningPort;
		this.uploadPort = uploadPort;
		this.downloadPort = downloadPort;
		this.clientId = clientId;
	}

	public void Start() {
		
		int bytesRead;
		int current = 0;
		// try to receive the initial chunks from server
		try {
			// set up socket to connect to server
			c1 = new Socket("127.0.0.1", serverPort);
			System.out.println("Connected !!");
			ObjectOutputStream fos0 = new ObjectOutputStream(c1.getOutputStream());   //initialize output stream
			fos0.writeObject((Object) this.clientId);
			fos0.flush();
			
			ObjectInputStream is = new ObjectInputStream(c1.getInputStream());        //initialize input stream
			
			
			File peerChunkDir = new File("Client" + this.clientId);    //create a new file directory to save received chunks for each client
			System.out.println(peerChunkDir.getAbsolutePath());
			
			if(peerChunkDir.exists()) {
				Runtime.getRuntime().exec(new String[] {"bash", "-c", "rm -rf " + peerChunkDir.getAbsolutePath() + "/*.txt"}).waitFor();//if this directory existed, clear txt files first				
			} else {                
				peerChunkDir.mkdirs();                                 //if there is no such directory, create it
			}
			
			while(true) {
				Object[] rcv = (Object[]) is.readObject();             //receive chunks, use array rcv, i.e. receive, to store stream received from socket
				String name = (String) rcv[0];                         //similar to chunk in server, store chunks' name
				if(name.equals("END")) 
				{
					this.totalSize = ((Integer) rcv[1]).intValue();    //if the name of chunk is END, means receive all the chunks
					System.out.println("Total chunk size is " + this.totalSize);      //print total size, i.e. original file size
					break;
				}
				
				byte[] mybytearray = (byte[]) rcv[1];                  //similar to chunk in server, store chunks' bytes
				
				System.out.println("Begin receiving:" + name);         //as long as we receive a chunk, print what we received
				
				if(!check_list_1.contains(name)) {
					// create a new file directory and chunk
					FileOutputStream fos = new FileOutputStream("Client" + this.clientId + "/" + name, false);   //whatever, overwrite
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					bos.write(mybytearray);                            //stream write the bytes
					bos.flush();
					bos.close();
					fos.flush();
					fos.close();
					System.out.println("File " + "chunk " + name);
					check_list_1.add(name);
				}
			}
			
			System.out.print("check_list_1: ");
			for (String c: check_list_1) {
				System.out.print(c + " ");                             //print what we have already had
			}
			System.out.println();

		} catch (IOException ex) {
			ex.printStackTrace();

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Start a new thread at background, to send chunks to next neighbor
		ClientThread t1 = new ClientThread(listeningPort, check_list_1, this.clientId);
		t1.start();

		// ask for chunks from preceding neighbor
		Socket t;
		while(true) {
			try {
				t = new Socket("127.0.0.1", downloadPort);
				break;
			} catch (IOException e) {
				System.out.println("Try again in 5sec.");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		try {
			ObjectOutputStream fos2 = new ObjectOutputStream(t.getOutputStream());
			ObjectInputStream fis1 = new ObjectInputStream(t.getInputStream());
			// keep trying to connect until successful connection
			while (true) {
				
				fos2.writeObject(check_list_1);    //stream write chunks, send chunks from chechlist, i.e. this client has, to the output stream
				fos2.flush();
				fos2.reset();

				// ArrayList<Integer> input_check_list =
				// (ArrayList<Integer>)(fos1.readObject());
				// TODO: Compare input_check_list and check_list -> request_list
				// TODO: for(int i: request_list)

				Object[] rcv = (Object[]) fis1.readObject();
				String name = (String) rcv[0];
				byte[] chunk = (byte[]) rcv[1];
				System.out.println("Received " + name);      //print which chunks did this client receive
				if((!name.equals("NOTHING")) && (!check_list_1.contains(name))) {
					FileOutputStream fos = new FileOutputStream("Client" + this.clientId + "/" + name, false); //create or overwrite chunks under client directory
					BufferedOutputStream bos = new BufferedOutputStream(fos);
					bos.write(chunk);
					bos.close();
					fos.close();
					check_list_1.add(name);                  //add the name of chunks we received into checklist
					
					
				}
				// chunks save
				
				// end for
				if(check_list_1.size() == this.totalSize) {                     //chechk whether we got every chunks
					System.out.println("WE HAVE EVERYTHING!");                  //if so, print info to inform
					File combine = new File("peer" + this.clientId + ".txt");   //create recovery txt file named peerId
					if(!combine.exists()) {
						Runtime.getRuntime().exec(
							new String[] {"bash", "-c", "cat Client" + this.clientId + "/*.txt > " + "peer" + this.clientId + ".txt"}).waitFor();//if there is a same name original file, delete it and concatenate chunks to recover a new file
					}
				}
			}
			//TODO: check if we have all chunks
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		// My client id
		int myClientId = Integer.parseInt(args[0]);

		// Set port based on id
		Client c;
		
		try {
			BufferedReader configReader = new BufferedReader(new FileReader("config"));
			String line = configReader.readLine();	           //read config file to know port number
			String[] parts = line.split(" ");
			int serverPort = Integer.parseInt(parts[1]);
			
			while(line != null) {
				line = configReader.readLine();
				parts = line.split(" ");
				int clientId = Integer.parseInt(parts[0]);
				int clientPort = Integer.parseInt(parts[2]);
				int uploadPort = Integer.parseInt(parts[3]);
				int downloadPort = Integer.parseInt(parts[4]);             //get client ID, port number, upload and download port number
				if (myClientId == clientId) {
					c = new Client(serverPort, clientPort, uploadPort, downloadPort, myClientId);      //define a new client 
					c.Start();
					break;
				}
			}
					
			configReader.close();
		
//		switch (myClientId) {
//		case 0:
//			c = new Client(5000, 9000, 9001, 9004, myClientId);
//			break;
//		case 1:
//			c = new Client(5000, 9001, 9002, 9000, myClientId);
//			break;
//		case 2:
//			c = new Client(5000, 9002, 9003, 9001, myClientId);
//			break;
//		case 3:
//			c = new Client(5000, 9003, 9004, 9002, myClientId);
//			break;
//		default:
//			c = new Client(5000, 9004, 9000, 9003, myClientId);
//			break;
//		}
//
//		c.Start();
} catch (Exception ex) {
			
		}

	}
}