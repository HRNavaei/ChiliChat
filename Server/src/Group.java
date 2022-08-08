import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class Group implements Serializable {
    String groupID,groupName;
    Set<User> userSet;
    User admin;

    public Group(String groupName,String groupID,User admin) {
        this.groupID=groupID;
        this.groupName=groupName;
        userSet = new HashSet<>();
        userSet.add(admin);
        this.admin=admin;
    }

    public void addUser(User user) throws IOException {
        userSet.add(user);
        for(User user2:userSet){
            user2.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:join> -Option <user:%s> -Option <group-name:%s>\n\r\n",user.getUserID(),groupName).getBytes());
        }
    }

    public Set<User> getUserList() {
        return userSet;
    }

    public String getGroupID() {
        return groupID;
    }

    public void sendMsg(String sender,String msg) throws IOException {
        for(User user:this.getUserList()){
            if(!user.isOnline)
                continue;
            user.getClientHandler().getClientOutput().write(String.format("GM -Option <from:%s> -Option <to:%s> -Option <message_len:%d> -Option <message_body:%s>\r\n",
                    sender,groupID,msg.getBytes().length-2,msg).getBytes());
        }
    }

    public boolean removeUser(User user) throws IOException {
        boolean isRemoved=userSet.remove(user);
        if(!isRemoved)
            return false;
        user.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:left> -Option <user:You> -Option <group-name:%s>\r\n",groupName).getBytes());
        for(User user2:this.getUserList()){
            user2.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:left> -Option <user:%s> -Option <group-name:%s>\r\n",user.getUserID(),groupName).getBytes());
        }
        return true;
    }

    public void sendOfflineNotification(User offlineUser) throws IOException {
        for(User user:this.getUserList()){
            if(user.isOnline())
                user.getClientHandler().getClientOutput().write(String.format("Notification -Option <type:offline-group> -Option <user:%s> -Option <group-name:%s>\r\n",offlineUser.getUserID(),groupName).getBytes());
        }
    }

    public boolean includeUser(User user){
        return userSet.contains(user);
    }

    public String getAdminUserID() {
        return admin.getUserID();
    }

    public String getGroupName() {
        return groupName;
    }
}