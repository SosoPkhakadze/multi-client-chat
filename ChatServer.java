package fop.w11pchat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements Runnable{
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private ExecutorService pool;
    private boolean done;
    public  ChatServer(){
        connections = new ArrayList<>();
        done = false;
    }
    @Override
    public void run() {
        try {
            server = new ServerSocket(3000);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }
    public void shutdown(){
        done = true;
        pool.shutdown();
        try {
            if (!server.isClosed())
                server.close();
            for (ConnectionHandler ch:connections) {
                ch.shutdown();
            }
        }catch (IOException e){

        }
    }
    class ConnectionHandler implements Runnable{
        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        public String getNickname() {
            return nickname;
        }
        private String nickname;
        private String time;
        public ConnectionHandler(Socket client){
            this.client = client;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(),true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Enter the username: ");
                while (true) {
                    nickname = in.readLine();
                    long count = 0;
                    if (!connections.isEmpty()) {
                        count = connections.stream()
                                .map(ConnectionHandler::getNickname)
                                .filter(Objects::nonNull).filter(n -> n.equals(nickname))
                                .count();
                    }
                    if (count == 2) {
                        out.println("Error: This username is already in use. Please enter a new one!");
                    } else if (nickname.isEmpty() || nickname.startsWith(" ")) {
                        out.println("Error: Please enter a name that is not used and is not empty and does not start with a space.");
                    } else {
                        break;
                    }
                }
                System.out.println(nickname + " connected !");
                out.println("""
                        Hallo ! Welcome to the chatroom.Instructions:
                        1. Simply type the message to send broadcast to all active client.
                        2. Type '@username<space>yourmessage' without quotes to send message to desired client.
                        3. Type 'WHOIS' without quotes to see list of active clients.
                        4. Type 'LOGOUT' without quotes to logoff from server.
                        5. Type 'PINGU' without quotes to request a random penguin fact.
                        6. Type '/nick<space>newusername' to change your username to new username.""".indent(1));
                time = String.valueOf(LocalTime.now());
                broadcast(time + "  ***  " + nickname +" has joined the chat room ***");
                System.out.println(time + "  ***  " + nickname +" has joined the chat room ***");
                String message;
                while ((message = in.readLine())!= null){
                    if (message.equals("LOGOUT")){
                        broadcast(nickname + " left the chat!");
                        shutdown();
                    if(connections.isEmpty()){
                        server.close();
                    }
                    }
                    else if (message.startsWith("@")) {
                        String[] parts = message.split(" ", 2);
                        String name = parts[0].substring(1);
                        String text = parts[1];
                        int count = 0;
                        for (ConnectionHandler client : connections) {
                            if (client.nickname.equals(name)) {
                                client.sendMessage("[private] "+nickname+":"+" "+message.substring(message.indexOf(" ") + 1));
                                out.println("[private] " + text);
                                break;
                            }
                        }
                        if (count == connections.size()) System.out.println("No client with the according nickname!");
                    }
                    if (message.equals("WHOIS")) {
                        LocalTime time = LocalTime.now();
                        out.println("List of the users connected at " + time + " :");
                        int x = 0;
                        for (int i = 0; i < connections.size(); i++) {
                            var connection = connections.get(i);
                            x = 1 + i;
                            out.println("\n" + x + ") " + connection.nickname + " since " + connection.time);
                        }
                    }

                    else if(message.equals("PINGU")){
                        List<String> facts = List.of(
                                "Penguins are flightless birds.",
                                "There are 17 species of penguins.",
                                "Penguins have an insulating layer of fat called blubber.",
                                "Penguins can swim up to speeds of 15 mph.",
                                "Penguins mate for life and build nests with pebbles.","Penguins are found in every continent except for Antarctica.",
                                "The largest penguin species is the emperor penguin, which can reach up to 4 feet tall and weigh over 100 pounds.",
                                "Penguins have a gland above their eyes that filters salt from seawater, allowing them to drink it.",
                                "Penguins mate for life and often engage in elaborate courtship rituals.",
                                "Penguins have a layer of insulating feathers and a layer of down feathers to keep them warm in cold water.",
                                "Penguins are excellent swimmers and can reach speeds of up to 15 mph in the water.",
                                "Penguins have a special adaptation called counter shading, which helps them blend in with their surroundings and avoid predators.",
                                "Penguins have a unique vocalization called a bray, which they use to communicate with each other.",
                                "Penguins have a gland on their feet that produces an oil used to waterproof their feathers.",
                                "Penguins are social animals and often live in large colonies called rookeries."
                        );
                        Random rand = new Random();
                        int index = rand.nextInt(facts.size());
                        String fact = facts.get(index);
                        out.println(fact);
                        broadcast(fact);
                    }
                    else if (message.startsWith("/nick ")){
                        String[] messageSplit = message.split(" ", 2);
                        long count = 0;
                            count = connections.stream()
                                    .map(ConnectionHandler::getNickname)
                                    .filter(Objects::nonNull).filter(n -> n.equals(messageSplit[1]))
                                    .count();
                        if (count >= 1) {
                            out.println("Error: This username is already in use. Please enter a new one!");
                        }
                        else if((!messageSplit[1].startsWith(" ") && !messageSplit[1].isEmpty()) && messageSplit.length == 2){
                            broadcast(nickname+" renamed themselves to "+ messageSplit[1]);
                            System.out.println(nickname+" renamed themselves to "+ messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Successfully changed nickname to " +nickname);
                        } else{
                            out.println("No nickname provided!");
                        }
                    }
                    else if(!message.startsWith("@") && !message.startsWith("WHOIS") && !message.startsWith("LOGOUT") && !message.startsWith("/nick "))
                    {
                        out.println(message);
                        broadcast(nickname + ": " + message);
                    }
                }
            }catch (IOException e){
                shutdown();
            }
        }

        public void sendMessage(String message){
            out.println(message);
        }
        public void broadcast(String message){
            for (ConnectionHandler ch:connections) {
                if (ch!=null && !this.time.equals(ch.time)){
                    ch.sendMessage(message);
                }
            }
        }
        public void shutdown(){
            try {
                in.close();
                out.close();
                connections.remove(this);
                if (!client.isClosed()){
                    client.close();
                }
            }
            catch (IOException e){

            }
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.run();
    }
}
