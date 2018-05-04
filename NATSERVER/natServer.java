import java.io.*;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class natServer {

    // The server socket.
    private static ServerSocket serverSocket = null;
    // The client socket.
    private static Socket clientSocket = null;

    // Thie server can accept up to maxClientsCount clients' connections.
    private static final int maxClientsCount = 10;
    private static final clientThread[] threads = new clientThread[maxClientsCount];

    public static String ipadd[][] = new String[maxClientsCount][2];

    public static void main(String args[]) {

        // The default port number.
        int portNumber = 2222;

        if (args.length < 1) {
            System.out.println("Usage: java natServer <portNumber>\n"
                    + "Now using port number=" + portNumber);
        } else {
            portNumber = Integer.valueOf(args[0]).intValue();
        }

    /*
     * Open a server socket on the portNumber (default 2222). Note that we can
     * not choose a port less than 1023 if we are not privileged users (root).
     */
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.out.println(e);
        }

    /*
     * Create a client socket for each connection and pass it to a new client
     * thread.
     */
        while (true) {
            try {
                clientSocket = serverSocket.accept();
                int i = 0;
                for (i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == null) {
                        (threads[i] = new clientThread(clientSocket, threads)).start();

                        ipadd[i][1] = "1.0.0." + (i + 1);
                        break;
                    }
                }
                if (i == maxClientsCount) {
                    PrintStream os = new PrintStream(clientSocket.getOutputStream());
                    os.println("Server too busy. Try later.");
                    os.close();
                    clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}


class clientThread extends Thread {

    private String clientName = null;
    private BufferedReader is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final clientThread[] threads;
    private int maxClientsCount;

    public clientThread(Socket clientSocket, clientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        clientThread[] threads = this.threads;

        try {

            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            os = new PrintStream(clientSocket.getOutputStream());
            String name;
            while (true) {
                os.println("Enter your name.");
                name = is.readLine().trim();
                if (name.indexOf('@') == -1) {
                    break;
                } else {
                    os.println("The name should not contain '@' character.");
                }
            }

            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;

                        natServer.ipadd[i][0] = name;
                        for (int k = 0; k <= i; k++) {

                            System.out.print(natServer.ipadd[k][0] + " ");
                            System.out.print(natServer.ipadd[k][1] + "\n");

                        }

                        break;
                    }
                }

            }
      /* Start exchange of data. */
            while (true) {
                String line = is.readLine();
                if (line.startsWith("/quit")) {
                    break;
                }

                String namec = "", wordsc = "";

        /* If the message is private sent it to the given client. */
                if (line.startsWith("@")) {


                    String[] words = line.split("\\s", 2);
                    if (words.length > 1 && words[1] != null) {
                        words[1] = words[1].trim();
                        if (!words[1].isEmpty()) {
                            synchronized (this) {
                                for (int i = 0; i < maxClientsCount; i++) {


                                    if (threads[i] != null && threads[i] != this
                                            && threads[i].clientName != null
                                            && threads[i].clientName.equals(words[0])) {


                                        for (int z = 0; z < maxClientsCount; z++) {
                                            if ((natServer.ipadd[z][0]).equals(name)) {
                                                namec = natServer.ipadd[z][1];
                                                break;
                                            }
                                        }

                                        for (int z = 0; z < maxClientsCount; z++) {
                                            if ((natServer.ipadd[z][0]).equals((words[0].substring(1, (words[0].length()))))) {
                                                wordsc = natServer.ipadd[z][1];
                                                break;
                                            }
                                        }

                                        System.out.println("\nSource IP ||  Data  || Destination IP");
                                        System.out.println(namec + "   || " + words[1] + " || " + wordsc);
                                        System.out.println("after changing the Source IP address..");
                                        System.out.println("Source IP ||  Data  || Destination IP");
                                        System.out.println("1.0.0.0   || " + words[1] + " || " + natServer.ipadd[i][1]);
                                        System.out.println("Forwarding data..");

                                        threads[i].os.println("\nSource IP ||  Data  || Destination IP");
                                        threads[i].os.println("1.0.0.0   || " + words[1] + " || " + natServer.ipadd[i][1] + "\n");

                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
          /* The message is public, broadcast it to all other clients. */
                    synchronized (this) {

                        for (int z = 0; z < maxClientsCount; z++) {
                            if ((natServer.ipadd[z][0]).equals(name)) {
                                namec = natServer.ipadd[z][1];
                                break;
                            }
                        }

                        System.out.println("\nSource IP ||  Data  || Destination IP");
                        System.out.println(namec + "   || " + line + " || broadcast");
                        System.out.println("after changing the Source IP address..");
                        System.out.println("Source IP ||  Data  ||Destination IP");
                        System.out.println("1.0.0.0   || " + line + "  || broadcast");
                        System.out.println("Forwarding data..");

                        for (int i = 0; i < maxClientsCount; i++) {
                            if (threads[i] != null && threads[i].clientName != null) {

                                threads[i].os.println("<" + name + "> " + line);

                                threads[i].os.println("\nSource IP ||  Data  || Destination IP");
                                threads[i].os.println("1.0.0.0   || " + line + " || broadcast" + "\n");

                            }
                        }
                    }
                }
            }
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientName != null) {
                        threads[i].os.println("*** The user " + name
                                + " is disconnected.. ***");
                    }
                }
            }


      /*
       * Clean up. Set the current thread variable to null so that a new client
       * could be accepted by the server.
       */
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }
      /*
       * Close the output stream, close the input stream, close the socket.
       */
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }
}
