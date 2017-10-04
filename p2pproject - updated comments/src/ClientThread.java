import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public final class ClientThread extends Thread {

	private int port;
	private ArrayList<String> check_list_1;
	private int clientId;

	public ClientThread(int port, ArrayList<String> check_list, int clientId) {
		
		this.port = port;
		this.clientId = clientId;
		System.out.println(port);
		this.check_list_1 = check_list;
	}

	public void run(){
		try {
			ServerSocket ssocket = new ServerSocket(port);
			System.out.println("Client listening...");
			Socket connection = ssocket.accept();                                                // keep listening until successful connection
			
			
			System.out.println("Client get connected.");
			ObjectInputStream fis1 = new ObjectInputStream(connection.getInputStream());         //initialize input and output stream
			ObjectOutputStream fos2 = new ObjectOutputStream(connection.getOutputStream());
			while (true) {
				ArrayList<String> input_check_list = (ArrayList<String>) fis1.readObject();      //receive chunks neighbor gave
				System.out.println("Neighbor has " + input_check_list);
				System.out.println("I have " + check_list_1);
				Thread.sleep(3000);
				// Listening command
				// String command = (String)(fos1.readObject());
				// if(command == "LIST") {
				// TODO Send check list
				// } else {
				boolean sentSth = false;                          								 //does not sent yet
				for (String i: check_list_1) {                                                   //traversal checklist which is I have already had
					if (input_check_list.contains(i)) {
						System.out.println("Neighbor already have " + i);                        //if true, means neighbor already have the chunk I have
					} else {
						File myFile1 = new File(
								"Client" + clientId + "/" + i);                                  //if not, save the chunk into clientId file directory
						assert(myFile1.exists());
						FileInputStream fis = new FileInputStream(myFile1);
						BufferedInputStream bis = new BufferedInputStream(fis);
						// Buffer
						byte[] myreadbyte = new byte[1000000];
						int ch = bis.read(myreadbyte);                                           // Valid data length
						assert(ch != -1);
						// Copy valid content from buffer to new array
						byte[] mysendbyte = Arrays.copyOf(myreadbyte, ch);
						// Send out
						Object[] payload = new Object[2];
						payload[0] = (Object) (i);
						payload[1] = (Object) mysendbyte;
 						fos2.writeObject(payload);                                               //write fos2 into payload, send out payload with name and bytes
						fos2.flush();
						bis.close();
						fis.close();
						System.out.println("I have sent " + i);
						sentSth = true;
					}
				}
				if(!sentSth) {
					Object[] payload = new Object[2];                                            //end traversal, set name equal to nothing to end chunks in client
					payload[0] = (Object) "NOTHING";
					payload[1] = (Object) new byte[1];
					fos2.writeObject(payload);
					fos2.flush();
					System.out.println("Notify neighbor that I have nothing to send");           //has sent all chunks
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		

	}
}

