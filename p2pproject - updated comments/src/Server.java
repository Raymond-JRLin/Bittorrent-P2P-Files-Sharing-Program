import java.net.*;
import java.util.*;
import java.io.*;

public class Server {
	Socket connection = null;
	public static void main(String[] args)
	{
		new Server(args[0]);
	}	

	public Server(String distFile)
	{
		//We need a try-catch because lots of errors can be thrown
		// take in a file and chunk it into pieces		
		try {
			
			BufferedReader configReader = new BufferedReader(new FileReader("config"));  //read a stream input about config to assign ports
			String line = configReader.readLine();
			String[] parts = line.split(" ");
			int serverPort = Integer.parseInt(parts[1]);	//set server port number	
			configReader.close();
			
			int subfile = 0;
			// open the file which is ready to transmit
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(distFile));

			// get the file length
			File f = new File(distFile);
			long fileSize = f.length();
			System.out.println(fileSize);  //print how long this file is

			Runtime.getRuntime().exec(new String[] { "bash", "-c", "rm -f /Users/bojiafeng/Desktop/P2PPROJECT/*.txt" }).waitFor(); //first clear all other useless file in the directory where we will output chunks 
			
			// loop for each full chunk
			long chunkSize = 102400;       //we design each chunk is 100Kb, i.e. 102400B
			// System.out.println(fileSize/chunkSize + 1);
			for (subfile = 0; subfile < (fileSize/chunkSize); subfile++) // for each chunk size, chop the file into several chunks
			{
				// open the output file
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream("/Users/bojiafeng/Desktop/P2PPROJECT/textfile" + String.format("%05d", subfile) + ".txt"));

				// write the right amount of bytes
				for (int currentByte = 0; currentByte < chunkSize; currentByte++)
				{
					// loop for loading one byte from the input file and write it to the output file, read from in and write into out
					out.write(in.read());
				}

				// close the file
				out.close();
			}

			// because the file size may not be exact multiple of 100Kb, the last chunk may be smaller than the chunk size
			// so we need to find the last chunk, thus loop for the last chunk and write into in
			if (fileSize != chunkSize * (subfile - 1))  //
			{
				// open the output file again
				BufferedOutputStream out = new BufferedOutputStream(
						new FileOutputStream("/Users/bojiafeng/Desktop/P2PPROJECT/textfile" + String.format("%05d", subfile) + ".txt"));

				// write the rest of the file
				int b;
				while ((b = in.read()) != -1)
					out.write(b);

				// close the file
				out.close();			
			}

			// close the file
			in.close();
			
			System.out.println(subfile +1);  //print how much chunks we got
			
			ServerSocket sSocket = new ServerSocket(5000);            //give server prot number 5000
			System.out.println("Server start at :" + new Date());     //print when we chopped the file and start
			while(true){
				// wait for a client to connect (accept a connetion from a client)
				Socket connection = sSocket.accept();
				System.out.println("Connection received from " + connection.getInetAddress().getHostName());
				//initialize input stream and output stream
				ObjectInputStream cis = new ObjectInputStream(connection.getInputStream());
				int clientId = ((Integer) cis.readObject()).intValue();
				
				System.out.println("Get connected from Id:" + clientId);
				
				//begin to transmit one file to one user, or more exactly, transmit different chunks to different clients
				ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream());;

				for(int j = clientId; j <= subfile; j += 5) {
				
					File myFile = new File ("/Users/bojiafeng/Desktop/P2PPROJECT/textfile"+ String.format("%05d", j) + ".txt"); //read chunks
					String name = myFile.getName();                            //store the chunk's name we got
					byte [] mybytearray  = new byte [(int)myFile.length()];    //store the chunks we received
					FileInputStream fis = new FileInputStream(myFile);
					BufferedInputStream bis = new BufferedInputStream(fis);    //create a new input stream
					bis.read(mybytearray,0,mybytearray.length);                //read bytes from input stream into mybytearray

					Object[] chunk = new Object[2];   //use a array named chunk to store received chunks' name and bytes

					chunk[0] = (Object) name;
					chunk[1] = (Object) mybytearray;
					//send chunks to output stream
					out.writeObject(chunk);
					out.flush();
					System.out.println("Sending " + name + "(" + mybytearray.length + " bytes)");  //print which chunk the server has sent and how much is the chunk
					bis.close();
				}
				
				Object[] payload = new Object[2];
				payload[0] = (Object) "END";
				payload[1] = (Object) (subfile + 1);
				out.writeObject(payload);
				out.flush();
				out.close();	
			}
		} catch (IOException exception) {
			System.out.println("Error: " + exception);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}


