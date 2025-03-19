import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try {
            // Se connecter au serveur à l'adresse localhost sur le port 12345
            Socket socket = new Socket("localhost", 12345);

            // Créer des flux pour envoyer et recevoir des données
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);

            // Envoyer un message au serveur
            output.println("Bonjour serveur!");

            // Lire la réponse du serveur
            String response = input.readLine();
            System.out.println("Réponse du serveur: " + response);

            // Fermer les ressources
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}