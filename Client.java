//Bond, Dennis
//ECEN/CS 4283 Project
//Client.java


import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import java.lang.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {

    BufferedReader in;
    PrintWriter out;


    FileInputStream fis = null;
    BufferedInputStream bis = null;

    FileOutputStream fos = null;
    BufferedOutputStream bos = null;
    OutputStream os = null;


    JFrame frame = new JFrame("Chat Window");
    JTextField textField = new JTextField(50);
    JTextArea messageArea = new JTextArea(10, 50);

    public File file;
    public int fileSize;
    public String fileSizeString;
    private String clientFileCopy;

    private static JFileChooser jFileChooser = new JFileChooser();

   
    public Client() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // Add Listeners
        textField.addActionListener(new ActionListener() {
          
            public void actionPerformed(ActionEvent e) {
                out.println(textField.getText());
                textField.setText("");
            }
        });
    }

    
    //Prompt for and return the address of the server. 
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Chat/Server Project",
            JOptionPane.QUESTION_MESSAGE);
    }

    
    //Prompt for and return the desired screen name. 
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter a user name:",
            "User name creation",
            JOptionPane.PLAIN_MESSAGE);
    }

    //Returns the file name that was slected or null if canceled
    public String selectFile(Socket socket) {
    	jFileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
		
		int result = jFileChooser.showOpenDialog(null);
	
	
		if (result == JFileChooser.APPROVE_OPTION) {
		    File selectedFile = jFileChooser.getSelectedFile();
		    file = selectedFile;
		    //System.out.println("Selected file: " + selectedFile.getAbsolutePath());

		    //Get filesize
		    fileSize = (int)file.length();
		    //System.out.println(fileSize);

		    fileSizeString = Integer.toString(fileSize);
            //System.out.println(fileSizeString);

		    
		    return selectedFile.getAbsolutePath();  
		}

		return "NULL";
		
    }

    //Sends the file to the server
    public void sendFile(Socket socket, File fileName, int fileSize) throws IOException{
    	 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		 FileInputStream fis = new FileInputStream(fileName.getAbsolutePath());

		    byte [] buffer = new byte[fileSize];
		



		    while(fis.read(buffer) > 0){
		    	dos.write(buffer);

		    }

		     fis.close();
		     dos.flush();

    }

    //Saves the file sent by the server
    private void saveFile(Socket socket, String fileName) throws IOException{
    	 //Remove file size
            String size = "";

            int sizeIndex = fileName.lastIndexOf('|');
            if(sizeIndex >= 0){
                size = fileName.substring(sizeIndex+1);
            }


            String temp = fileName.substring(0, sizeIndex);
            fileName = temp;

            int currentFileSize = Integer.parseInt(size);

            fileSizeString = Integer.toString(currentFileSize);	



    	Path p = Paths.get(fileName);
    	String pureFile = p.getFileName().toString();

    	//Get extension
        String ext1 = "";

        int index = fileName.lastIndexOf('.');
        if(index >= 0){
            ext1 = fileName.substring(index+1);
        }

        //Remove last bit of file
        int cut = 12 + ext1.length();
    	String newName = pureFile.substring(0, pureFile.length() - cut);

    	clientFileCopy = newName + "_userCopy." + ext1;



    	DataInputStream dis = new DataInputStream(socket.getInputStream());
    	FileOutputStream fos = new FileOutputStream(clientFileCopy);
    	
    	//System.out.println(clientFileCopy);
    	//System.out.println("save");
    	byte buffer [] = new byte[currentFileSize];

    
    	int read = 0;
    	int totalRead = 0;
    	int remaining = currentFileSize;
    	while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0){
    		totalRead += read;
    		remaining -= read;
    		System.out.println("Read: " + totalRead + " bytes.");
    		fos.write(buffer, 0 , read);
    	}

    	fos.flush();
    }


    
     //Connects to the server then enters the processing loop.  
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = "";

        //Make sure serverAddress is not empty
        while(true){
        	serverAddress = getServerAddress();
        	if(serverAddress.equals(null) || serverAddress.equals("") || serverAddress.equals("null")){
        		continue;
        	}
        	else{
        		break;
        	}
        }
        
        Socket socket = new Socket(serverAddress, 9002);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } 
            
            else if(line.startsWith("DM")){
				messageArea.append(line.substring(8) + "\n");
				
            }

            else if(line.startsWith("INVALID")){
            	messageArea.append(line.substring(8) + "\n");
            }
            //\\///\//\\/\/\\/\/\/\/\/\/\/\\/\/\/\/\/\/\/\/\/\/\/\/\/
            //File transfer to all
            else if(line.startsWith("SFILE")){
            	//File transfer stuff here
            	
            	out.println("FILENAME" + selectFile(socket) + "|" + fileSizeString);
            }

            else if(line.startsWith("SENDIT")){
            	sendFile(socket, file, fileSize);
            	out.println("DONE");
            }
            else if(line.startsWith("CANCEL")){
            	messageArea.append(line.substring(8) + "\n");
            }
            else if(line.startsWith("READY?")){
            	clientFileCopy = line.substring(8);
            	//System.out.println("Ready");
            	out.println("GOFORIT");

            	saveFile(socket, clientFileCopy);
            }
            else if(line.startsWith("DONE")){
            	//Transfer complete.
            	messageArea.append("File transfer complete." + "\n");
            }
            //\\/\/\\/\\/\/\/\/\/\//\/\/\\/\/\/\/\/\/\/\//\/\/\/\/\/\
            //Direct file transfer
            else if(line.startsWith("DFILE")){
            	//File transfer stuff here
            	
            	out.println("DILENAME" + selectFile(socket) + "|" + fileSizeString);
            }
            else if(line.startsWith("DENDIT")){
            	sendFile(socket, file, fileSize);
            	out.println("DOND");
            }
             else if(line.startsWith("READYD")){
            	clientFileCopy = line.substring(8);
            	//System.out.println("Ready");
            	out.println("DOFORIT");

            	saveFile(socket, clientFileCopy);
            }


        }           
    }
      
    public static void main(String[] args) throws Exception {
        Client client = new Client();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}