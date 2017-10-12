//Bond, Dennis
//ECEN/CS 4283 Project
//Server.java


import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.*;
import javax.swing.*;
import java.nio.*;


public class Server{

    //Port that the server listens on
    private static final int PORT = 9002;

    //Set of all the client names
    private static HashSet<String> userNames = new HashSet<String>();

    //Set of all output streams
    private static HashSet<PrintWriter> writers = new HashSet<PrintWriter>();


    //Hash maps to keep track of the user names paired with their output streams and sockets
    private static HashMap<Integer, String> userMap = new HashMap<Integer, String>();

    private static HashMap<Integer, PrintWriter> writerMap = new HashMap<Integer, PrintWriter>(); 

    private static HashMap<Integer, Socket> socketMap = new HashMap<Integer, Socket>();



    private static Socket clientSocket;
    public static String serverFileCopy;
    public static int serverFileSize;
    public static String serverFileSizeString;
    public static int directFile;


    //Fucntion to populate sockets into the hashmaps
    public static void mapSockets(Socket socket){
        int z = socketMap.size() + 1;
        socketMap.put(z, socket);

    }

    public static void main(String[] args) throws Exception{
        System.out.println("Server is online");
        System.out.println("======================================");

        ServerSocket listener = new ServerSocket(PORT);
        

        try{
            while(true){
                clientSocket = listener.accept();
                mapSockets(clientSocket);

                //System.out.println(clientSocket);
                new Handler(clientSocket).start();
                
            }
        } finally {
                listener.close();
        }
    }

    
    private static class Handler extends Thread{
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;


        private FileOutputStream fos = null;
        private BufferedOutputStream bos = null;

       

        public Handler(Socket socket){
            this.socket = socket;
        }

        //Function to populate user names into the hashmap
        public void mapUsers(String name){
            
            int x = (userMap.size()+1);

            userMap.put(x, name);
            //System.out.println(name + " " + x);
            
        }

        //Function to populate writters into the hashmap
        public void mapWriter(PrintWriter writer){

            int y = writerMap.size()+1;

            writerMap.put(y, writer);


        }


        private void saveFile(Socket clientSocket, String fileName) throws IOException{
            //Remove file size
            String size = "";

            //System.out.println(fileName);

            int sizeIndex = fileName.lastIndexOf('|');
            if(sizeIndex >= 0){
                size = fileName.substring(sizeIndex+1);
            }

            //System.out.println(sizeIndex);

            String temp = fileName.substring(0, sizeIndex);
            fileName = temp;

            //System.out.println(fileName);

            int fileSize = Integer.parseInt(size);
            serverFileSize = fileSize;

            serverFileSizeString = Integer.toString(serverFileSize);

            //Get extension
            String ext1 = "";

            int index = fileName.lastIndexOf('.');
            if(index >= 0){
                ext1 = fileName.substring(index+1);
            }

            //Remove extension
            String newName = fileName.substring(0, index);

            serverFileCopy = newName + "_serverCopy." + ext1;

            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            FileOutputStream fos = new FileOutputStream(serverFileCopy);
            byte buffer [] = new byte[fileSize];
            

            int read =0; 
            int totalRead = 0;
            int remaining = fileSize;
            while((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0){
                totalRead += read;
                remaining -= read;
                System.out.println("Read: " + totalRead + " bytes.");
                fos.write(buffer, 0, read);
            }
            
            fos.flush();
      
        }

        private void sendFile(Socket socket, String fileName, int fileSize) throws IOException{
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            FileInputStream fis = new FileInputStream(fileName);

            byte [] buffer = new byte[fileSize];

            while(fis.read(buffer) > 0){
                dos.write(buffer);
            }

            fis.close();
            dos.flush();
        }

        public void run(){
            try{
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);



                while(true){
                    out.println("SUBMITNAME");
                    name = in.readLine();

                    if(name.equals(null) || name.equals("") || name.equals("null")){
                        continue;
                    }

                    else{
                        synchronized(userNames){
                            if(!userNames.contains(name)){
                                userNames.add(name);
                                mapUsers(name);
                                break;
                            }
                        }
                    }
                }

                out.println("NAMEACCEPTED");
                writers.add(out);
                mapWriter(out);

                //Accept messeges from this client and broadcast them
                while(true){
                    String input = in.readLine();
                    if(input == null){
                       return;
                    }

//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                    //@ for direct messages
                    else if(input.charAt(0) == '@'){
                            //Search for username

                            String directName = "";

                            //Removes the userName from the message   
                            for(int i=1; i < input.length(); i++){
                                if(input.charAt(i) != ' '){
                                    directName += input.charAt(i);
                                }
                                else{
                                    // System.out.println(directName);
                                    break;
                                }   
                            }

                            int keyDM = 0;
                            int keySend =0;

                            //Checks to see if it is a valid userName
                            if(userNames.contains(directName) == true){
                                String newInput = input.substring(directName.length() + 2, input.length());
                                
                                //Search hashmap for dm name to get key
                                
                                for(int i=1; i <= userMap.size(); i++){
                                   
                                    String test = userMap.get(i);
                                    if(test.equals(directName)){

                                        keyDM = i;
                                      
                                        //Use key to find printwriter for dm name
                                        PrintWriter dmWrite = writerMap.get(keyDM);
                                        dmWrite.println("DM      " + name + ": " + newInput);

                                        break;
                                    }


                                    
                                }

                                for(int i=1; i <= userMap.size(); i++){
                                  
                                    String test = userMap.get(i);
                                    if(test.equals(name)){

                                        keySend = i;
                                       
                                        //Use key to find printwriter for dm name
                                        PrintWriter dmWrite = writerMap.get(keySend);
                                        dmWrite.println("DM      " + name + ": " + newInput);

                                        break;
                                    }
                             
                                }
                            }
                            //Not a valid userName
                            //Send error message to sender
                            else{
                                String invalid = "INVALID USER";

                                for(int i=1; i<=userMap.size(); i++){
                                    
                                    String test = userMap.get(i);

                                    if(test.equals(name)){
                                        int keyInvalid = 0;
                                        keyInvalid = i;

                                        PrintWriter invalidWrite = writerMap.get(keyInvalid);
                                         invalidWrite.println("INVALID " + name + ": " + invalid);
                                    }
                                }
                                
                            }
                        
                    }//End @
//@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
                    
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                    //! for excluding a user
                    else if(input.charAt(0) == '!'){
                        //Search for user name

                        String excludeName = "";

                        //Removes the userName from the message   
                        for(int i=1; i < input.length(); i++){
                            if(input.charAt(i) != ' '){
                                excludeName += input.charAt(i);
                            }
                            else{
                                // System.out.println(directName);
                                break;
                            } 

                        }

                        int key = 0;

                        //Checks to see if it is a valid userName
                        if(userNames.contains(excludeName) == true){
                            String newInput = input.substring(excludeName.length() + 2, input.length());
                            
                            //Search hashmap for exclude name to get key
                            for(int i=1; i <= userMap.size(); i++){
                                
                                String test = userMap.get(i);
                                if(test.equals(excludeName)){

                                    key = i;
                                
                                    //Use key to find printwriter for dm name
                                    for(int j=1; j<=userMap.size(); j++){
                                        if(j!=key){
                                            PrintWriter dmWrite = writerMap.get(j);
                                            dmWrite.println("DM      " + name + ": " + newInput);
                                        }
                                    }
                                    break;
                                }
                                
                            }
                         
                        }     
                        //Not a valid userName
                        //Send error message to sender
                        else{
                            String invalid = "INVALID USER";

                            for(int i=1; i<=userMap.size(); i++){
                                
                                String test = userMap.get(i);

                                if(test.equals(name)){
                                    int keyInvalid = 0;
                                    keyInvalid = i;

                                    PrintWriter invalidWrite = writerMap.get(keyInvalid);
                                     invalidWrite.println("INVALID " + name + ": " + invalid);
                                }
                            }
                            
                        }

                    }//End of !
//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
                    //& 
                    //send file to everyone
                    else if(input.charAt(0) == '&'){
                        //Create case to launch file browser in client side
                        out.println("SFILE   ");
                        
                                                  
                    }//End of &

                    else if(input.startsWith("FILENAME")){
                        //System.out.println(input.substring(8) + "\n");

                        String testName = input.substring(8);

                        if(testName.startsWith("NULL")){
                            //System.out.println(testName);
                            out.println("CANCEL  " + "File Transfer canceled.");
                        }
                        else{
                            //Search for sender's socket
                            out.println("SENDIT");

                            for(int i =1; i<= userMap.size(); i++){
                                String test = userMap.get(i);

                                if(test.equals(name)){
                                    int match = 0;
                                    match = i;

                                    saveFile(socketMap.get(match), testName);
                                }
                            }
                        }

                    }

                    else if(input.startsWith("DONE")){
                            //Tell clients to get ready to receive file

                        for(PrintWriter writer : writers){
                            writer.println("READY?  " + serverFileCopy + "|" + serverFileSizeString);
                        }
                            // System.out.println("Test");
                            //out.println("READY?  " + serverFileCopy);
                            
                    }
                    else if(input.startsWith("GOFORIT")){
                        //Send file to all users

                        for(int i =1; i<= userMap.size(); i++){
                                String test = userMap.get(i);

                                if(test.equals(name)){
                                    int match = 0;
                                    match = i;
                                    
                                    sendFile(socketMap.get(i), serverFileCopy, serverFileSize);
                                    //System.out.println("Send to: " + i);
                                }
                        }

                        out.println("DONE");
                    }//End of file transfer to all
//&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&

//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    //~
                    //Direct file transfer
                    else if(input.charAt(0)== '~' ){
                        //Search for user name

                        String excludeName = "";

                        //Removes the userName from the message   
                        for(int i=1; i < input.length(); i++){
                            if(input.charAt(i) != ' '){
                                excludeName += input.charAt(i);
                            }
                            else{
                                // System.out.println(directName);
                                break;
                            } 

                        }

                        int key = 0;

                        //Checks to see if it is a valid userName
                        if(userNames.contains(excludeName) == true){
                            //String newInput = input.substring(excludeName.length() + 2, input.length());
                            
                            //Search hashmap for exclude name to get key
                            for(int i=1; i <= userMap.size(); i++){
                                // System.out.println("MAP @: " + userMap.get(i));
                                // System.out.println("SIZE: " + userMap.size());

                                String test = userMap.get(i);
                                if(test.equals(excludeName)){

                                    key = i;
                                    directFile = key;
                                    break;
                                }
                            }

                            //Contact sender for filename and file
                            out.println("DFILE   ");
                                
                        }

        
                        //Not a valid userName
                        //Send error message to sender
                        else{
                            String invalid = "INVALID USER";

                            for(int i=1; i<=userMap.size(); i++){
                                
                                String test = userMap.get(i);

                                if(test.equals(name)){
                                    int keyInvalid = 0;
                                    keyInvalid = i;

                                    PrintWriter invalidWrite = writerMap.get(keyInvalid);
                                     invalidWrite.println("INVALID " + name + ": " + invalid);
                                }
                            }
                            
                        }

                    
                   } //End of ~

                    
                    else if(input.startsWith("DILENAME")){
                        //System.out.println(input.substring(8) + "\n");

                        String testName = input.substring(8);

                        if(testName.startsWith("NULL")){
                            //System.out.println(testName);
                            out.println("CANCEL  " + "File Transfer canceled.");
                        }
                        else{
                            //Search for sender's socket
                            out.println("DENDIT");

                            for(int i =1; i<= userMap.size(); i++){
                                String test = userMap.get(i);

                                if(test.equals(name)){
                                    int match = 0;
                                    match = i;

                                    saveFile(socketMap.get(match), testName);
                                }
                            }
                        }

                    }

                    else if(input.startsWith("DOND")){
                        //Tell client to get ready to receive file
                        PrintWriter writer = writerMap.get(directFile);
                        writer.println("READYD  " + serverFileCopy + "|" + serverFileSizeString);
        
                    }
                    else if(input.startsWith("DOFORIT")){
                        //Send file to the one user              
                        sendFile(socketMap.get(directFile), serverFileCopy, serverFileSize);                                   
                        out.println("DONE");
                    }
                        
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                    else{
                        for(PrintWriter writer : writers){
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                    }
                }
            }catch(IOException e){
                System.out.println(e);
            }finally{
                    //Remove client name after logout
                    if(name != null){
                        userNames.remove(name);
                    }
                    if(out != null){
                        writers.remove(out);
                    }
                    try{
                        socket.close();
                    }catch (IOException e){

                    }
                }

            }
        }
    }