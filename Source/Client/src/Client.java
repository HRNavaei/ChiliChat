import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client extends Thread {
    Socket socket;
    OutputStream clientOut;
    BufferedReader clientIn;
    UserHandler userHandler;
    boolean loggedIn;
    List<String> errorList;
    List<String> warningList;

    public Client(String host,int port) throws IOException {
        socket = new Socket(host,port);
        clientOut = socket.getOutputStream();
        clientIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        loggedIn=false;
        errorList=new ArrayList<>();
        warningList=new ArrayList<>();
        fillErrorList();
        fillWarningList();
    }

    public void fillErrorList() {
        errorList.add("Username should be at least 6 characters."); //1
        errorList.add("Password should be at least 6 characters."); //2
        errorList.add("Username already taken."); //3
        errorList.add("Username can only contain digits and letters."); //4
        errorList.add("username not found"); //5
        errorList.add("incorrect password"); //6
        errorList.add("group id already taken"); //7
        errorList.add("group not found"); //8
        errorList.add("user not found"); //9
        errorList.add("not any group with this id"); //10
        errorList.add("you aren't in this group"); //11
        errorList.add("you are already in this group"); //12
        errorList.add("ChiliChat is already running"); //13
        errorList.add("Session is expired"); //14
        errorList.add("The user is offline"); //15
    }

    public void fillWarningList() {
        warningList.add("Only one device can log in at a time.\nDo you want to continue and force the other device to log off? (Y/N)"); //1
        warningList.add("You logged off."); //2
        warningList.add("Another device has logged in to your account. You logged off."); //3
    }

    @Override
    public void run(){
        try {
            clientOut.write((((String)new ObjectInputStream(new FileInputStream("sid")).readObject())+'\n').getBytes());
        }catch (FileNotFoundException e){
            try {
                clientOut.write("-1\n".getBytes());
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            handleResponse();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleResponse() throws IOException{
        while(true){
            String cmd=clientIn.readLine();
            handleCmd(cmd);
        }
    }

    public void handleCmd(String cmd) throws IOException {
        String cmd0=cmd.split(" ",2)[0];
        switch (cmd0){
            case "User":
                handleSignup(cmd.split(" ")); break;
            case "Connected":
                handleSignin(cmd.split(" ")); break;
            case "USERS_LIST:":
                cmd=clientIn.readLine();
                handleUserList(cmd.split("\\|")); break;
            case "GROUP_LIST:":
                cmd=clientIn.readLine();
                handleGroupList(cmd.split("\\|")); break;
            case "PV_LIST:":
                cmd=clientIn.readLine();
                handlePvList(cmd.split("\\|")); break;
            case "Admin":
                handleGroupAdmin(cmd.split(" ")); break;
            case "Is-Online":
                handleIsOnline(cmd.split(" ")); break;
            case "GM":
                handleGM(cmd.split(" ",9)); break;
            case "PM":
                handlePM(cmd.split(" ",9)); break;
            case "CG":
                handleCG(cmd.split(" ")); break;
            case "Hi":
                handleGroupWelcome(cmd.split(" ")); break;
            case "ERROR":
                handleError(cmd.split(" ")); break;
            case "Warning":
                handleWarning(cmd.split(" ")); break;
            case "Notification":
                handleNotification(cmd.split(" ")); break;
            case "App-Msg":
                handleAppMsg(cmd.split(" ")); break;
        }
    }

    public void handleAppMsg(String[] cmd) throws IOException {
        switch (cmd[2].substring(9,cmd[2].length()-1)){
            case "Login":
                String status=cmd[4].substring(5,cmd[4].length()-1);
                if(status.equals("Ready")){
                    userHandler=new UserHandler(this);
                    userHandler.loginMenu();
                }
                else if(status.equals("Successful")){
                    loggedIn=true;
                }
                else if(status.equals("Successful-Session")){
                    loggedIn=true;
                    userHandler = new UserHandler(this);
                    userHandler.start();
                    System.out.printf("Signed in successfully as @%s\n",cmd[6].substring(11,cmd[6].length()-1).toLowerCase());
                }
                break;
        }
    }

    public void handleSignup(String[] cmd) throws IOException {
        userHandler.userID=cmd[3].substring(11,cmd[3].length()-1);
        new ObjectOutputStream(new FileOutputStream(new File("sid"))).writeObject(cmd[5].substring(5,cmd[5].length()-1));
        System.out.printf("\nYou have successfully signed up as @%s.\nWelcome to ChiliChat!\n",userHandler.userID);
        userHandler.start();
    }

    public void handleSignin(String[] cmd) throws IOException {
        new ObjectOutputStream(new FileOutputStream(new File("sid"))).writeObject(cmd[4].substring(5,cmd[4].length()-1));
        System.out.printf("\nSigned in successfully as @%s\n",cmd[2].substring(11,cmd[2].length()-1).toLowerCase());
        userHandler.start();
    }

    public void handleCG(String[] cmd){
        System.out.printf("Group \"%s\" created successfully\n",cmd[2].substring(8,cmd[2].length()-2));
    }

    public void handleGM(String[] cmd){
        System.out.printf("Message in group %s_from %s: %s\n> ",cmd[4].substring(4,cmd[4].length()-1),cmd[2].substring(6,cmd[2].length()-1),cmd[8].substring(14,cmd[8].length()-1));
    }

    public void handlePM(String[] cmd){
        System.out.printf("Message from %s: %s\n> ",cmd[2].substring(6,cmd[2].length()-1),cmd[8].substring(14,cmd[8].length()-1));
    }

    public void handleUserList(String[] cmd){
        for(int i=0;i< cmd.length;i++){
            System.out.print(cmd[i].substring(1,cmd[i].length()-1));
            if(i!= cmd.length-1){
                System.out.print(", ");
            }
        }
        System.out.println();
    }

    public void handleGroupList(String[] cmd) {
        System.out.println("Group List:\n");
        if(!cmd[0].equalsIgnoreCase("")) {
            for (int i = 0; i < cmd.length; i++) {
                String[] groupInfo=cmd[i].split("<");
                String groupName=groupInfo[1].substring(0,groupInfo[1].length()-1),groupID=groupInfo[2].substring(0,groupInfo[2].length()-1);
                System.out.printf("%s(@%s)",groupName,groupID);
                if (i != cmd.length - 1) {
                    System.out.print(", ");
                }
            }
        }
        System.out.println();
    }

    public void handlePvList(String[] cmd) {
        System.out.println("Pv List:\n");
        if(!cmd[0].equalsIgnoreCase("")) {
            for (int i = 0; i < cmd.length; i++) {
                System.out.print(cmd[i].substring(1, cmd[i].length() - 1));
                if (i != cmd.length - 1) {
                    System.out.print(", ");
                }
            }
        }
        System.out.println();
    }

    public void handleGroupAdmin(String[] cmd){
        System.out.println((String.format("Admin of group \"%s\": \"%s\"",cmd[2].substring(1,cmd[2].length()-1),cmd[4].substring(1,cmd[4].length()-1))));
    }

    public void handleIsOnline(String[] cmd) {
        boolean isOnline=cmd[4].substring(1,cmd[4].length()-1).equals("1") ? true:false;
        System.out.println(String.format("%s is %s.",cmd[2].substring(1,cmd[2].length()-1),isOnline ? "online":"offline"));
    }

    public void handleGroupWelcome(String[] cmd){
        System.out.printf("Hi %s, welcome to this group.\n",cmd[1].substring(1,cmd[1].length()-2));
    }

    public void handleNotification(String[] cmd){
        switch (cmd[2].substring(6,cmd[2].length()-1)){
            case "join":
                System.out.printf("\"%s\" joined the group \"%s\"\n",cmd[4].substring(6,cmd[4].length()-1),cmd[6].substring(12,cmd[6].length()-1)); break;
            case "left":
                System.out.printf("\"%s\" left the group \"%s\"\n",cmd[4].substring(6,cmd[4].length()-1),cmd[6].substring(12,cmd[6].length()-1)); break;
            case "offline-pv":
                System.out.printf("User \"%s\" is now offline\n",cmd[4].substring(6,cmd[4].length()-1)); break;
            case "offline-group":
                System.out.printf("User \"%s\" in group \"%s\" is now offline\n",cmd[4].substring(6,cmd[4].length()-1),cmd[6].substring(12,cmd[6].length()-1)); break;
        }
    }

    public void handleError(String[] cmd) throws IOException {
        int errorCode = 0;
        switch (cmd[2].substring(9, cmd[2].length() - 1)) {
            case "Group":
                System.out.print("Add to Group Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "GM":
                System.out.print("Group Message Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "PM":
                System.out.print("Private Message Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "CG":
                System.out.print("Group Creating Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "Login":
                System.out.print("Login Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                if(errorCode!=13 && errorCode!=14){
                    userHandler=new UserHandler(this);
                    userHandler.loginMenu();
                }
                break;
            case "Leave":
                System.out.print("Leaving Group Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "User-List":
                System.out.print("Getting Group User List Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "Admin":
                System.out.print("Getting Group Admin Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
            case "Is-Online":
                System.out.print("Checking Online Statue Failed: ");
                errorCode = Integer.parseInt(cmd[5].substring(0, cmd[5].length() - 1));
                System.out.println(errorList.get(errorCode - 1));
                break;
        }

        if (errorCode == 13) {
            System.out.println("Press any key to exit...");
            System.in.read();
            System.exit(-1);
        }
        System.out.println();
    }

    public void handleWarning(String[] cmd) throws IOException {
        int warningCode = Integer.parseInt(cmd[3].substring(0, cmd[3].length() - 1));
        System.out.println(warningList.get(warningCode - 1));
        if (warningCode == 1) {
            String ans;
            while(true){
                ans=userHandler.stdInSc.next();
                if(!(ans.equalsIgnoreCase("y") || ans.equalsIgnoreCase("n"))){
                    System.out.println("Wrong input, Try again.");
                    continue;
                }
                else break;
            }

            clientOut.write((ans+'\n').getBytes());

            if(ans.equalsIgnoreCase("n")){
                userHandler.stop();
                userHandler=new UserHandler(this);
                userHandler.loginMenu();
            }
        }
        if (warningCode == 3){
            userHandler.logOff();
            userHandler.stop();
        }
    }
}