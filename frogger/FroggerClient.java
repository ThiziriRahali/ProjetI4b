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
            // Se connecter au serveur à l'adresse localhost sur le port 12345
            socket = new Socket("localhost", 12345);
            System.out.println("Connecté au serveur Frogger !");

            // Créer des flux pour envoyer et recevoir des données
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer une commande pour rejoindre le jeu
            output.println("JOIN");

            // Thread pour recevoir les messages du serveur (tableau de jeu, etc.)
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while (running.get() && (message = input.readLine()) != null) {
                        // Si le message est une demande de saisie, ne pas l'afficher
                        if (message.startsWith("INPUT:")) {
                            String prompt = message.substring(6);
                            System.out.print(prompt);
                        } 
                        // Si c'est une commande de déplacement, ne pas l'afficher
                        else if (message.equals("MOVE")) {
                            System.out.print("Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ");
                            // Don't read input here, it will be read in the main thread
                        }
                        // Sinon afficher le message (tableau de jeu, etc.)
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
            receiveThread.setDaemon(true); // Make it a daemon thread so it doesn't prevent program exit
            receiveThread.start();

            // Lire les entrées du joueur et les envoyer au serveur
            String userInput;
            while (running.get() && (userInput = scanner.nextLine()) != null) {
                output.println(userInput);
                
                // Si l'utilisateur veut quitter
                if (userInput.equals("QUIT") || userInput.equals("x")) {
                    running.set(false);
                    break;
                }
            }

            // Fermer les ressources
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