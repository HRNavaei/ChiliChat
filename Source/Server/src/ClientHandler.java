import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;

public class ClientHandler extends Thread implements Serializable{
    Server server;
    BufferedReader input;
    OutputStream output;
    User user;
    Map<String, byte[]> unPwdMap;
    byte[] salt;
    boolean forceLogoff,isOnline,loggedIn,sessionLogin,sessionExpired;

    public ClientHandler(Server server, Socket socket) throws IOException {
        //ServerSettings
        this.server = server;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = socket.getOutputStream();
        //ClientSettings
        isOnline=true;
        forceLogoff=false;
        loggedIn=false;
        sessionLogin=false;
        //Variables
        unPwdMap=server.getUnPwdMap();
        salt = new byte[]{102, 79, -17, -11, 120, -42, -111, -44, -15, 71, 22, -117, -91, 110, 121, 63};
    }

    public ClientHandler(Server server,Socket socket,User user) throws IOException {
        //Socket Settings
        this.server=server;
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = socket.getOutputStream();
        //Variables
        unPwdMap=server.getUnPwdMap();
        salt = new byte[]{102, 79, -17, -11, 120, -42, -111, -44, -15, 71, 22, -117, -91, 110, 121, 63};
        //ClientSettings
        isOnline=true;
        forceLogoff=false;
        loggedIn=false;
        sessionLogin=true;
        this.user=user;
        sessionExpired= user == null;
    }

    @Override
    public void run() {
        while(isOnline){
            try {
                if (!loggedIn)
                    handleLogin();
            }catch (IOException e){
                System.out.println("A non-logged in user disconnected.");
                break;
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            try{
                handleCommand();
            } catch (IOException e){
                if(e.getMessage().equalsIgnoreCase("Connection reset by peer: socket write error")
                        || e.getMessage().equalsIgnoreCase("Connection reset")){
                    System.out.println("A user disconnected.");
                    user.setOnline(false);
                    sendDisconnectNotification(user);
                    user.getPvList().clear();
                    break;
                }
            }
        }
    }

    //Access to client output stream
    public OutputStream getClientOutput() {
        return output;
    }

    //Functions related to session login
    public void addSession(Integer sid,User user){
        server.getSessionMap().put(sid,user);
    }

    public void removeSession(Integer sid){
        server.getSessionMap().remove(sid);
    }

    public boolean handleSessionLogin(User user) throws IOException {
        if(user.isOnline()){
            if(user.getClientHandler().isSessionLogin()){
                output.write("ERROR -Option <section:Login> -Option <reason:err 13>\r\n".getBytes());
                return false;
            }
            output.write("Warning -Option <reason:wrn 1>\r\n".getBytes());
            while(true){
                String ans=input.readLine();
                if(ans.equalsIgnoreCase("y")){
                    loggedIn=false;
                    return false;
                }
                else if(ans.equalsIgnoreCase("n")){
                    removeSession(user.getSid());
                    break;
                }
            }
        }
        loggedIn=true;
        this.user=user;
        user.setClientHandler(this);
        user.setOnline(true);
        return true;
    }

    public boolean isSessionLogin() {
        return sessionLogin;
    }   //End functions related to session login

    //Functions related to non-session login
    public byte[] hash(String plainText) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(plainText.toCharArray(), salt, 65536, 128);
        return factory.generateSecret(spec).getEncoded();
    }

    public boolean signUp(String userID, String password) throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
        String lowerCaseUn = userID.toLowerCase();
        System.out.println("Pass: "+password);
        if (lowerCaseUn.length() < 6) {
            output.write("ERROR -Option <section:Login> -Option <reason:err 1>\r\n".getBytes());
            return false;
        }
        if (password.length() < 6) {
            output.write("ERROR -Option <section:Login> -Option <reason:err 2>\r\n".getBytes());
            return false;
        }
        if (unPwdMap.containsKey(lowerCaseUn)) {
            output.write("ERROR -Option <section:Login> -Option <reason:err 3>\r\n".getBytes());
            return false;
        }
        for (char ch : lowerCaseUn.toCharArray()) {
            if (!(Character.isLetter(ch) || Character.isDigit(ch))) {
                output.write("ERROR -Option <section:Login> -Option <reason:err 4>\r\n".getBytes());
                return false;
            }
        }
        byte[] hashedPwd = hash(password);
        unPwdMap.put(lowerCaseUn, hashedPwd);
        user = new User(userID);
        addUser(user);
        user.setClientHandler(this);
        user.setOnline(true);
        loggedIn=true;
        user.setSid(server.getSidCounter());
        addSession(user.getSid(),user);
        output.write(String.format("User Accepted -Option <user_name:%s> -Option <SID:%d>\r\n", lowerCaseUn,user.getSid()).getBytes());
        return true;
    }

    public boolean signIn(String userID, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        byte[] hashedPwd = hash(password), storedPwd;

        if ((storedPwd = unPwdMap.get(userID.toLowerCase())) == null) {
            output.write("ERROR -Option <section:Login> -Option <reason:err 5>\r\n".getBytes());
            return false;
        }

        if (Arrays.equals(storedPwd, hashedPwd)) {
            User user=searchUser(userID);
            if(user.isOnline()){
                System.out.println("online");
                output.write("Warning -Option <reason:wrn 1>\r\n".getBytes());

                String ans=input.readLine();
                if(ans.equalsIgnoreCase("n"))
                    return false;
                else if(ans.equalsIgnoreCase("y"))
                    server.getSessionMap().remove(user.getSid());
            }
            else{
                System.out.println("offline");
                removeSession(user.getSid());
            }

            user.setClientHandler(this);
            user.setOnline(true);
            user.setSid(server.getSidCounter());
            loggedIn=true;
            addSession(user.getSid(),user);
            output.write(String.format("Connected -Option <user_name:%s> -Option <SID:%d>\r\n", userID,user.getSid()).getBytes());
            this.user=user;
            return true;
        } else {
            output.write("ERROR -Option <section:Login> -Option <reason:err 6>\r\n".getBytes());
            return false;
        }
    }

    public void addUser(User user) {
        server.getUserSet().add(user);
    }   //* Create a new user account

    public void handleLogin() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        if(sessionLogin){
            if(sessionExpired)
                output.write("ERROR -Option <section:Login> -Option <reason:err 14>\r\n".getBytes());
            else if(handleSessionLogin(user)){
                output.write(String.format("App-Msg -Option <section:Login> -Option <msg:Successful-Session> -Option <user_name:%s>\r\n",user.getUserID()).getBytes());
                return;
            }
        }
        output.write("App-Msg -Option <section:Login> -Option <msg:Ready>\r\n".getBytes());
        while (true) {
            String line=input.readLine();
            String[] tokens = line.split(" ");

            switch (tokens[0]) {
                case "Make":
                    System.out.println("1");
                    tokens = line.split(" ", 5);
                    if(signUp(tokens[2].substring(6, tokens[2].length() - 1), tokens[4].substring(6, tokens[4].length() - 1)))
                        return;
                    break;
                case "Connect":
                    System.out.println("2");
                    tokens = line.split(" ", 5);
                    if (signIn(tokens[2].substring(6, tokens[2].length() - 1), tokens[4].substring(6, tokens[4].length() - 1))){
                        output.write(String.format("App-Msg -Option <section:Login> -Option <msg:Successful> -Option <user:%s>\r\n",user.getUserID()).getBytes());
                        return;
                    }
                    break;
            }
        }
    }   //* Handle login operation
    //End functions related to non-session login

    //Search a user based on UserID
    public User searchUser(String userID){
        for(User user: server.getUserSet()){
            if(user.getUserID().equalsIgnoreCase(userID)){
                return user;
            }
        }
        return null;
    }

    //Main function of this class - handles commands received from user
    public void handleCommand() throws IOException {
        while (true) {
            System.out.println("main loop");
            String line=input.readLine(),msg;
            String[] tokens = line.split(" ");

            switch (tokens[0]){
                case "Group":
                    System.out.println("3");
                    addToGroup(tokens[4].substring(5,tokens[4].length()-1)); break;
                case "CG":
                    System.out.println("4");
                    createGroup(tokens[2].substring(7,tokens[2].length()-1),tokens[4].substring(5,tokens[4].length()-1)); break;
                case "Users":
                    getGroupUsers(tokens[2].substring(7,tokens[2].length()-1)); break;
                case "Admin":
                    getGroupAdmin(tokens[2].substring(7,tokens[2].length()-1)); break;
                case "Is-Online":
                    isOnline(tokens[2].substring(6, tokens[2].length() - 1)); break;
                case "GM":
                    System.out.println("5");
                    tokens=line.split(" ",7);
                    msg=tokens[6].substring(14,tokens[6].length()-1);
                    sendGM(tokens[2].substring(4,tokens[2].length()-1),msg); break;
                case "PM":
                    System.out.println("6");
                    tokens=line.split(" ",7);
                    msg=tokens[6].substring(14,tokens[6].length()-1);
                    sendPM(tokens[4].substring(4,tokens[4].length()-1),msg); break;
                case "Leave":
                    System.out.println(7);
                    leaveGroup(tokens[2].substring(4,tokens[2].length()-1)); break;
                case "End":
                    System.out.println(8);
                    user.setOnline(false);
                    sendDisconnectNotification(user);
                    isOnline=false;
                    user.pvList.clear();
                    return;
                case "Logoff":
                    System.out.println(9);
                    logOff();
                    return;
                case "GroupList":
                    sendGroupList(); break;
                case "PvList":
                    sendPvList(); break;
                default:
                    System.out.print("Unknown Command: "+tokens[0]+'\n');
                    break;
            }
        }
    }

    //Check if a user is online or not
    public void  isOnline(String userID) throws IOException {
        User user=searchUser(userID);
        if(user==null){
            output.write("ERROR -Option <section:Is-Online> -Option <reason:err 9>\r\n".getBytes());
            return;
        }
        output.write(String.format("Is-Online -Option <%s> -Option <%d>\r\n",userID,user.isOnline() ? 1:0).getBytes());
    }

    //Notify other users that a user is disconnected
    public void sendDisconnectNotification(User user) {
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

    //Group related functions
    public Group searchGroup(String groupName){
        for(Group group:server.getGroupSet()){
            if(group.getGroupID().equals(groupName)){
                return group;
            }
        }
        return null;
    }

    public void createGroup(String groupName,String groupID) throws IOException {
        System.out.println("createGroup");

        if(searchGroup(groupID)!=null){
            output.write("ERROR -Option <section:CG> -Option <reason:err 7>\r\n".getBytes());
            return;
        }
        Group group=new Group (groupName,groupID,user);
        user.getGroupList().add(group);
        server.getGroupSet().add(group);
        output.write(String.format("CG -Option <gname:\"%s\">\r\n",groupName).getBytes());
    }

    public void addToGroup(String groupID) throws IOException {
        System.out.println(groupID);
        Group group = searchGroup(groupID);
        if(group==null){
            output.write("ERROR -Option <section:Group> -Option <reason:err 8>\r\n".getBytes());
            return;
        }
        if(group.includeUser(user)){
            output.write("ERROR -Option <section:Group> -Option <reason:err 12>\r\n".getBytes());
            return;
        }
        group.addUser(user);
        for(User user2:group.getUserSet()){
            if(user2.isOnline()){
                user2.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:join> -Option <user:%s> -Option <group-name:%s>\n\r\n",user.getUserID(),group.getGroupName()).getBytes());
            }
        }
        user.getGroupList().add(group);
        output.write(String.format("Hi <%s>, welcome to the chat room.\r\n",user.getUserID()).getBytes());
    }

    public void getGroupUsers(String groupID) throws IOException {
        Group group = searchGroup(groupID);
        if(group==null){
            output.write("ERROR -Option <section:User-List> -Option <reason:err 8>\r\n".getBytes());
            return;
        }
        if(!group.includeUser(user)){
            output.write("ERROR -Option <section:User-List> -Option <reason:err 11>\r\n".getBytes());
            return;
        }
        Set<User> groupUserSet = group.getUserSet();
        StringBuilder groupUsersLog = new StringBuilder();
        groupUsersLog.append("USERS_LIST:\r\n");
        for(User user:groupUserSet){
            groupUsersLog.append('<'+user.getUserID()+">|");
        }
        groupUsersLog.deleteCharAt(groupUsersLog.length()-1);
        groupUsersLog.append("\r\n");
        output.write(groupUsersLog.toString().getBytes());
    }

    public void sendGroupList() throws IOException {
        StringBuilder groupList = new StringBuilder();
        groupList.append("GROUP_LIST:\r\n");
        for(Group group: user.groupList){
            groupList.append('<'+group.groupName+"><"+group.groupID+">|");
        }
        groupList.deleteCharAt(groupList.length()-1);
        groupList.append("\r\n");
        output.write(groupList.toString().getBytes());
    }

    public void getGroupAdmin(String groupID) throws IOException {
        Group group = searchGroup(groupID);
        if(group==null){
            output.write("ERROR -Option <section:Admin> -Option <reason:err 8>\r\n".getBytes());
            return;
        }
        if(!group.includeUser(user)){
            output.write("ERROR -Option <section:Admin> -Option <reason:err 11>\r\n".getBytes());
            return;
        }
        output.write(String.format("Admin -Option <%s> -Option <%s>\r\n",group.getGroupName(),group.getAdminUserID()).getBytes());
    }

    public void sendGM(String groupID,String msg) throws IOException {
        Group group = searchGroup(groupID);
        if(group==null){
            output.write("ERROR -Option <section:GM> -Option <reason:err 8>\r\n".getBytes());
            return;
        }
        if(!group.includeUser(user)){
            output.write("ERROR -Option <section:GM> -Option <reason:err 11>\r\n".getBytes());
            return;
        }
        group.sendMsg(user.getUserID(),msg);
    }

    public void leaveGroup(String id) throws IOException {
        System.out.println("leaveGroup");
        Group group = searchGroup(id);

        if(group==null)
            output.write("ERROR -Option <section:Leave> -Option <reason:err 10>\r\n".getBytes());
        else if(!group.removeUser(this.user))
            output.write("ERROR -Option <section:Leave> -Option <reason:err 11>\r\n".getBytes());
        else user.getGroupList().remove(group);
    }
    //End group related functions

    //PM related functions
    public void sendPvList() throws IOException {
        StringBuilder pvList = new StringBuilder();
        pvList.append("PV_LIST:\r\n");
        for(User user: user.pvList){
            pvList.append('<'+user.userID+">|");
        }
        pvList.deleteCharAt(pvList.length()-1);
        pvList.append("\r\n");
        output.write(pvList.toString().getBytes());
    }

    public void sendPM(String userID,String msg) throws IOException {
        User user = searchUser(userID);
        if(user==null){
            output.write("ERROR -Option <section:PM> -Option <reason:err 9>\r\n".getBytes());
            return;
        }
        if(!user.isOnline){
            output.write("ERROR -Option <section:PM> -Option <reason:err 15>\r\n".getBytes());
            return;
        }
        user.getClientHandler().getClientOutput().write(String.format("PM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> -Option <message_body:%s>\r\n",this.user.getUserID(),userID,msg.length()-2,msg).getBytes());
        this.user.getPvList().add(user);
        user.getPvList().add(this.user);
    }
    //End PM related functions

    //Logoff related functions
    public void logOff() throws IOException {
        user.setOnline(false);
        sendDisconnectNotification(user);
        user.pvList.clear();
        if(!forceLogoff)
            output.write("Warning -Option <reason:wrn 2>\r\n".getBytes());
        removeSession(user.getSid());
        user=null;
        isOnline=true;
        loggedIn=false;
        sessionLogin=false;
    }

    public void forceLogoff() throws IOException {
        output.write("Warning -Option <reason:wrn 3>\r\n".getBytes());
        forceLogoff=true;
    }
    //End logoff related functions
}