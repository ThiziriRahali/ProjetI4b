import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class FroggerClient {
    private static final Scanner scanner = new Scanner(System.in);
    private static Socket socket;
    private static BufferedReader input;
    private static PrintWriter output;
    private static AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) {
        try {
            socket = new Socket("172.31.18.21", 12345);
            System.out.println("Connecté au serveur Frogger !");

            // flux envoyer et recevoir des données
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // rejoindre jeu
            output.println("JOIN");

            //  recevoir messages serveur
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while (running.get() && (message = input.readLine()) != null) {
                        // Si message est demande de saisie, ne pas afficher
                        if (message.startsWith("INPUT:")) {
                            String prompt = message.substring(6);
                            System.out.println(prompt);
                        } 
                        
                        else if (message.equals("MOVE")) {
                            
                            output.println("BOUGE");
                            
                        }
                        else if (message.equals("EYO")) {
                            output.println("JOIN");
                        }
                       
                        else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    if (running.get()) {
                        System.out.println("Déconnecté du serveur: " + e.getMessage());
                    }
                }
            });
            //receiveThread.setDaemon(true); // Make it a daemon thread so it doesn't prevent program exit
            receiveThread.start();

            String userInput;
            while (running.get() && (userInput = scanner.nextLine()) != null) {
                output.println(userInput);
                
                // Si l'utilisateur veut quitter
                if (userInput.equals("QUIT") || userInput.equals("x")) {
                    running.set(false);
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