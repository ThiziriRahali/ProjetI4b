import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            // Créer un serveur qui écoute sur le port 12345
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Serveur démarré, en attente de connexion...");

            // Attendre la connexion du client
            Socket socket = serverSocket.accept();
            System.out.println("Client connecté !");

            // Création des flux pour recevoir et envoyer des données
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            // Lire les messages du client
            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Message du client: " + message);
                
                // Répondre au client
                output.println("Serveur reçu: " + message);
            }

            // Fermer les ressources
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
