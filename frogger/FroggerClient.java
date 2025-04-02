import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FroggerClient {
    private static final Scanner scanner = new Scanner(System.in);
    private static Socket socket;
    private static BufferedReader input;
    private static PrintWriter output;
    private static boolean running = true;

    public static void main(String[] args) {
        try {
            socket = new Socket("localhost", 12345);
            System.out.println("Connecté au serveur Frogger !");

            // flux envoyer et recevoir des données
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // rejoindre jeu
            output.println("JOIN");

            //  recevoir messages serveur
            // Thread receiveThread = new Thread(() -> {
            //     try {
            //         String message;
            //         while (running && (message = input.readLine()) != null) {
            //             if (message.startsWith("INPUT:")) {
            //                 String prompt = message.substring(6);
            //                 System.out.println(prompt);
            //             } 
                        
            //             else if (message.equals("MOVE")) {
                            
            //                 output.println("BOUGE");
                            
            //             }
                       
            //             else {
            //                 System.out.println(message);
            //             }
            //         }
            //     } catch (IOException e) {
            //         if (running) {
            //             System.out.println("Déconnecté du serveur: " + e.getMessage());
            //         }
            //     }
            // });
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while (running && (message = input.readLine()) != null) {
                        
                        if (message.startsWith("INPUT:")) {
                            String prompt = message.substring(6);
                            System.out.println(prompt);
                        } else if (message.equals("MOVE")) {
                            output.println("BOUGE");
                        } else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Déconnecté du serveur: " + e.getMessage());
                    }
                }
            });
           
            receiveThread.start();

            String userInput;
            while (running && (userInput = scanner.nextLine()) != null) {
                output.println(userInput);
                
                if (userInput.equals("QUIT") || userInput.equals("x")) {
                    running = false;
                    break;
                }
            }

            System.out.println("Fermeture de la connexion...");
            socket.close();
            scanner.close();
        } catch (IOException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Erreur lors de la fermeture du socket : " + e.getMessage());
                }
            }
        }
    }
}