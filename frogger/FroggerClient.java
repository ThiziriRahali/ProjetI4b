import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FroggerClient {
    private static Scanner scanner = new Scanner(System.in);
    private static Socket socket;
    private static BufferedReader input;
    private static PrintWriter output;
    private static boolean running = true;

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
                    while (running && (message = input.readLine()) != null) {
                        // Si le message est une demande de saisie, ne pas l'afficher
                        if (message.startsWith("INPUT:")) {
                            String prompt = message.substring(6);
                            System.out.print(prompt);
                        } 
                        // Si c'est une commande de déplacement, ne pas l'afficher
                        else if (message.equals("MOVE")) {
                            System.out.print("Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ");
                            String choiceMov = scanner.nextLine();
                            output.println (choiceMov);
                        }
                        // Sinon afficher le message (tableau de jeu, etc.)
                        else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Déconnecté du serveur.");
                    running = false;
                }
            });
            receiveThread.start();

            // Lire les entrées du joueur et les envoyer au serveur
            while (running) {
                String userInput = scanner.nextLine();
                output.println(userInput);
                
                // Si l'utilisateur veut quitter
                if (userInput.equals("QUIT")) {
                    running = false;
                    break;
                }
            }

            // Fermer les ressources
            socket.close();
            scanner.close();
        } catch (IOException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }
}