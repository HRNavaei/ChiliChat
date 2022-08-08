import java.io.*;
import java.util.*;

public class UserHandler extends Thread{
    Client client;
    Scanner stdInSc;
    String userID;

    public UserHandler(Client client){
        this.client=client;
        this.stdInSc = new Scanner(new InputStreamReader(System.in));
    }

    @Override
    public void run() {
        System.out.println("\nEnter the desired option:\n  1. Pv List\n  2. Group List\n" +
                "  3. Send PM\n  4. Send GM\n  5. Create Group\n  6. Add to Group\n" +
                "  7. Group User List\n  8. Group Admin\n  9. Leave Group\n  10. Is-Online\n  11. Logoff\n  12. Close\n");
        try {
            while (true) {
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.print("> ");
                String input= stdInSc.next();
                int option;
//                if(input.length()>2 || !Character.isDigit(input.charAt(0)) || !Character.isDigit(input.charAt(1))){
//                    option=-1;
//                }
//                else option=Integer.parseInt(input);
                try{
                    option = Integer.parseInt(input);
                }catch (Exception e){
                    option = -1;
                }
                switch (option) {
                    case 1:
                        pvList();
                        break;
                    case 2:
                        groupList();
                        break;
                    case 3:
                        sendPM();
                        break;
                    case 4:
                        sendGM();
                        break;
                    case 5:
                        createGroup();
                        break;
                    case 6:
                        addToGroup();
                        break;
                    case 7:
                        groupUserList();
                        break;
                    case 8:
                        groupAdmin();
                        break;
                    case 9:
                        leaveGroup();
                        break;
                    case 10:
                        isOnline();
                        break;
                    case 11:
                        logOff();
                        return;
                    case 12:
                        close();
                        break;
                    default:
                        System.out.println("Bad input. Try again.");
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loginMenu() throws IOException {
        System.out.println("Enter the desired option:\n  1. Sign-up\n  2. Sign-in");
        while(true) {
            int option = stdInSc.nextInt();
            switch (option) {
                case 1:
                    signUp();
                    return;
                case 2:
                    signIn();
                    return;
                default:
                    System.out.println("Bad input. Try again.");
            }
        }
    }

    public void signUp() throws IOException {
        String userID, pwd;
        System.out.print("Username: ");
        userID=stdInSc.next();

        System.out.print("Password: ");
        pwd=stdInSc.next();

        client.clientOut.write(String.format("Make -Option <user:%s> -Option <pass:%s>\r\n",userID,pwd).getBytes());
    }

    public void signIn() throws IOException {
        String userID, pwd;
        System.out.print("Username: ");
        userID=stdInSc.next();
        System.out.print("Password: ");
        pwd=stdInSc.next();

        client.clientOut.write(String.format("Connect -Option <user:%s> -Option <pass:%s>\r\n",userID,pwd).getBytes());
    }

    public void groupList() throws IOException {
        client.clientOut.write("GroupList\n".getBytes());
    }

    public void pvList() throws IOException {
        client.clientOut.write("PvList\n".getBytes());
    }

    public void sendPM() throws IOException {
        System.out.print("Receipt Username: ");
        String userID=stdInSc.next();
        System.out.printf("Enter your message:\n>");
        stdInSc.nextLine();
        String msg=stdInSc.nextLine();
        client.clientOut.write(String.format("PM -Option <message_len:%d> -Option <to:%s> -Option <message_body:\"%s\">\n",msg.length(),userID,msg).getBytes());
    }

    public void sendGM() throws IOException {
        System.out.print("Recipient Group ID: ");
        String groupID=stdInSc.next();
        System.out.printf("Enter your message:\n >");
        stdInSc.nextLine();
        String msg=stdInSc.nextLine();
        client.clientOut.write(String.format("GM -Option <to:%s> -Option <message_len:%d> -Option <message_body:\"%s\">\n",groupID,msg.length(),msg).getBytes());
    }

    public void createGroup() throws IOException{
        String groupName,groupID;
        System.out.print("Group Name: ");
        groupName=stdInSc.next();
        System.out.print("Group ID: ");
        groupID=stdInSc.next();
        client.clientOut.write(String.format("CG -Option <gname:%s> -Option <gid:%s>\n",groupName,groupID).getBytes());
    }

    public void addToGroup() throws IOException {
        System.out.print("Group ID: ");
        String groupID=stdInSc.next();
        client.clientOut.write(String.format("Group -Option <user:%s> -Option <gid:%s>\n", userID,groupID).getBytes());
    }

    public void groupUserList() throws IOException {
        System.out.print("Group ID: ");
        String groupID=stdInSc.next();
        client.clientOut.write(String.format("Users -Option <group:%s>\n",groupID).getBytes());
    }

    public void groupAdmin() throws IOException {
        System.out.print("Group ID: ");
        String groupID=stdInSc.next();
        client.clientOut.write(String.format("Admin -Option <group:%s>\n",groupID).getBytes());
    }

    public void leaveGroup() throws IOException {
        System.out.print("Group ID: ");
        String groupID=stdInSc.next();
        client.clientOut.write(String.format("Leave -Option <id:%s>\n",groupID).getBytes());
    }

    public void isOnline() throws IOException {
        System.out.print("username: ");
        String userID=stdInSc.next();
        client.clientOut.write(String.format("Is-Online -Option <user:%s>\n",userID).getBytes());
    }

    public void logOff() throws IOException {
        client.clientOut.write("Logoff\n".getBytes());
    }

    public void close() throws IOException {
        client.clientOut.write("End\n".getBytes());
        System.exit(0);
    }
}