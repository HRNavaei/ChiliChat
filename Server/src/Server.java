import java.io.*;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server extends Thread implements Serializable{
    int port;
    Integer sidCounter;
    Set<User> userSet;
    Set<Group> groupSet;
    Map<Integer, User> sessionMap;
    Map<String, byte[]> unPwdMap;

    public Server(int port) {
        this.port=port;
        sidCounter=0;
        userSet=new HashSet<>();
        groupSet = new HashSet<>();
        unPwdMap=new HashMap<>();
        sessionMap=new HashMap<>();
    }

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

    public void addSession(Integer sid,User user){
        sessionMap.put(sid,user);
    }

    public void removeSession(Integer sid){
        sessionMap.remove(sid);
    }

    public Map<String, byte[]> getUnPwdMap() {
        return unPwdMap;
    }

    public void addUser(User user) {
        userSet.add(user);
    }

    public User searchUser(String userID){
        for(User user:userSet){
            if(user.getUserID().equalsIgnoreCase(userID)){
                return user;
            }
        }
        return null;
    }

    public void addGroup(Group group) {
        groupSet.add(group);
    }

    public Group searchGroup(String groupName){
        for(Group group:groupSet){
            if(group.getGroupID().equals(groupName)){
                return group;
            }
        }
        return null;
    }

    public Integer getSidCounter() {
        return sidCounter++;
    }

    public void sendOfflineNotification(User user) {
        try {
            for (Group group: user.getGroupList())
                group.sendOfflineNotification(user);
            for(User user1:user.getPvList()){
                if(!user1.isOnline)
                    continue;
                user1.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:offline-pv> -Option <user:%s>\r\n",user.getUserID()).getBytes());
                user1.getPvList().remove(user);
            }
        } catch (IOException e) {
            System.out.println("sending notification failed");
            e.printStackTrace();
        }
    }
}