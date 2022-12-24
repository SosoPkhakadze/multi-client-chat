package fop.w11pchat;

import java.io.*;
import java.net.Socket;

public class ChatClient implements Runnable{

    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private boolean done;
    @Override
    public void run() {
        try {
            client = new Socket("127.0.0.1",3000);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(),true);
            InputHandler inHandler = new InputHandler();
            Thread t = new Thread(inHandler);
            t.start();
            String iMessage;
            while ((iMessage = in.readLine())!= null){
                System.out.println(iMessage);
            }
        }
        catch (IOException e){
            shutdown();
        }
    }
    public void shutdown(){
        done = true;
        try {
            in.close();
            out.close();
            if (!client.isClosed()){
                client.close();
            }
        }catch (IOException e){

        }
    }
    class InputHandler implements Runnable{
        @Override
        public void run() {
            try {
                BufferedReader inReader = new BufferedReader(new InputStreamReader(System.in));
                while (!done){
                    String message = inReader.readLine();
                    if (message.equals("LOGOUT")){
                        out.println(message);
                        inReader.close();
                        shutdown();
                    }

                    else {
                        out.println(message);
                    }
                }
            }
            catch (IOException e){
                shutdown();
            }
        }
    }

    public static void main(String[] args) {
        ChatClient client1 = new ChatClient();
        client1.run();
    }
}