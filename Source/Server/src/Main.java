import java.io.*;
import java.util.Scanner;

public class Main {
    static Server server;

    public static void main(String... args) throws IOException {
        try {
            server = (Server)new ObjectInputStream(new FileInputStream("server")).readObject();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (FileNotFoundException e){
            server = new Server(50000);
        }
        server.start();

        ShutDownTask shutDownTask = new ShutDownTask(server);
        Runtime.getRuntime().addShutdownHook(shutDownTask);

        Scanner stdInSc = new Scanner(System.in);
        while(true){
            String input=stdInSc.nextLine();
            if(input.equalsIgnoreCase("shutdown") || input.equalsIgnoreCase("shut down"))
                System.exit(0);
            else
                System.out.printf("Unrecognized Command: \"%s\"%n", input);
        }
    }
}

class ShutDownTask extends Thread {
    Server server;
    public ShutDownTask(Server server){
        this.server=server;
    }

    @Override
    public void run() {
        System.out.println("Shutting down the server...");
        try {
            new ObjectOutputStream(new FileOutputStream(new File("server"))).writeObject(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}