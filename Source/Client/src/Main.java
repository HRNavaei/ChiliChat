import java.io.IOException;

public class Main{

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost",50000);
        client.start();
    }
}
