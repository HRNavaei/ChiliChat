/**
 * The Server class fulfill the connection process from clients and
 * keeps the main fields of the program for keeping
 * users, groups, sessions and username-passwords
 * as well as their getter methods for having access to them
 * from other classes.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread implements Serializable{
    private int port;
    private Integer sidCounter;
    private Set<User> userSet;
    Set<Group> groupSet;
    private Map<Integer, User> sessionMap;
    private Map<String, byte[]> unPwdMap;

    public Server(int port) {
        this.port=port;
        sidCounter=0;
        userSet=new HashSet<>();
        groupSet = new HashSet<>();
        unPwdMap=new HashMap<>();
        sessionMap=new HashMap<>();
    }

    // Main method of the class which handles every client attempting to connect to server.
    @Override
    public void run() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true){
            System.out.println("Ready for a client to connect...");
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Client connected successfully via "+clientSocket+".");

            User user;
            ClientHandler clientHandler=null;
            try {
                Integer sid=Integer.parseInt(new BufferedReader(new InputStreamReader(clientSocket.getInputStream())).readLine());
                System.out.println(sid);
                if(sid==-1){
                    clientHandler = new ClientHandler(this,clientSocket);
                }
                else {
                    user=sessionMap.get(sid);
                    if(user==null) System.out.println("null user");
                    else System.out.println(user.getUserID());
                    clientHandler=new ClientHandler(this,clientSocket,user);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            clientHandler.start();
        }
    }

    public Integer getSidCounter() {
        return sidCounter++;
    }

    public Set<User> getUserSet() {
        return userSet;
    }

    public Set<Group> getGroupSet() {
        return groupSet;
    }

    public Map<Integer, User> getSessionMap() {
        return sessionMap;
    }

    public Map<String, byte[]> getUnPwdMap() {
        return unPwdMap;
    }
}