import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class User implements Serializable {
    String userID;
    ClientHandler clientHandler;
    boolean isOnline;
    Integer sid;
    Set<User> pvList;
    Set<Group> groupList;

    public User(String userID){
        this.userID=userID;
        pvList=new HashSet<>();
        groupList=new HashSet<>();
    }

    public String getUserID() {
        return userID;
    }

    public ClientHandler getClientHandler() {
        return clientHandler;
    }

    public void setClientHandler(ClientHandler clientHandler) throws IOException {
        if(isOnline)
            this.clientHandler.forceLogoff();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.clientHandler = clientHandler;
    }

    public Integer getSid() {
        return sid;
    }

    public void setSid(Integer sid) {
        this.sid = sid;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        if(!online)
            this.clientHandler=null;
        isOnline = online;
    }

    public Set<User> getPvList() {
        return pvList;
    }

    public Set<Group> getGroupList() {
        return groupList;
    }
}