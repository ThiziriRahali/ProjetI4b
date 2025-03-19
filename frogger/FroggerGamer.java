import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FroggerGamer {
    // Constantes et variables existantes
    public static final String FINISH_LINE_CHAR = "🏁";
    public static final String WALL_CHAR = "🧱";
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String FROG_CHAR = "🐸";
    private static String FROGACT = FROG_CHAR;
    private static final String ROAD_CHAR = ".";
    private static final String TERRE_PLEIN_CHAR = "🌱";
    private static final String FROG_WIN = "🤴";
    private static int frogX;
    private static int frogY;
    private static volatile boolean running;
    private static Obstacle[] obstacles;
    private static int LIVES_MAX = 3;
    public static int nbVieActuel;
    private static final String MESSAGE = "Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ";    
    private static volatile boolean paused = false;
    private static volatile boolean gagne = false;
    public static Arrivals A = new Arrivals();
    private static final int DIFFICULTE = 500; 
    
    // Nouvelles variables pour le multijoueur
    private static Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<Socket, PlayerInfo> players = new ConcurrentHashMap<>();
    private static int nextPlayerId = 1;
    
    // Classe pour gérer les informations de chaque joueur
    static class PlayerInfo {
        int id;
        int frogX;
        int frogY;
        int lives;
        String frogChar;
        boolean isPlaying;
        
        public PlayerInfo(int id) {
            this.id = id;
            this.frogX = WIDTH / 2;
            this.frogY = HEIGHT - 1;
            this.lives = LIVES_MAX;
            this.frogChar = FROG_CHAR + id;
            this.isPlaying = true;
        }
    }
    
    // Classe pour gérer chaque client connecté
    static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader input;
        private PrintWriter output;
        private PlayerInfo player;
        
        public ClientHandler(Socket socket, PlayerInfo player) {
            this.socket = socket;
            this.player = player;
            try {
                this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                this.output = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        public void sendMessage(String message) {
            output.println(message);
        }
        
        public void requestInput(String prompt) {
            output.println("INPUT:" + prompt);
        }
        
        public void requestMove() {
            output.println("MOVE");
        }
        
        @Override
        public void run() {
            try {
                String message;
                while ((message = input.readLine()) != null) {
                    if (message.equals("JOIN")) {
                        // Envoyer le menu au client
                        afficherMenuPrincipalClient(this);
                    } else if (message.equals("QUIT")) {
                        break;
                    } else if (player.isPlaying) {
                        // Traiter les commandes de déplacement
                        
                            // Traiter les choix de menu
                            try {
                                int choice = Integer.parseInt(message);
                                processMenuChoice(this, choice);
                            } catch (NumberFormatException e) {
                                sendMessage("Entrée invalide, veuillez réessayer.");
                            }
                        
                    }
                }
            } catch (IOException e) {
                System.out.println("Client déconnecté : " + e.getMessage());
            } finally {
                try {
                    removeClient(socket);
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static void removeClient(Socket socket) {
        clients.remove(socket);
        players.remove(socket);
    }
    
    private static void processMenuChoice(ClientHandler client, int choice) {
        switch (choice) {
            case 1:
                startGameForClient(client);
                break;
            case 2:
                client.sendMessage("Mode multijoueur déjà activé !");
                afficherMenuPrincipalClient(client);
                break;
            case 3:
                parametrerJeuClient(client);
                break;
            case 4:
                client.sendMessage("Déconnexion...");
                break;
            default:
                client.sendMessage("Choix invalide. Veuillez réessayer.");
                afficherMenuPrincipalClient(client);
        }
    }
    
    private static void afficherMenuPrincipalClient(ClientHandler client) {
        client.sendMessage("\n=== Menu Principal ===");
        client.sendMessage("1. Mode Solo");
        client.sendMessage("2. Mode Multijoueur");
        client.sendMessage("3. Paramètres");
        client.sendMessage("4. Quitter");
        client.requestInput("Veuillez choisir une option : ");
    }
    
    private static void parametrerJeuClient(ClientHandler client) {
        client.sendMessage("\n=== Paramètres du Jeu ===");
        client.sendMessage("1. Modifier le nombre de vies (Actuel : " + LIVES_MAX + ")");
        client.sendMessage("4. Retour au menu principal");
        client.requestInput("Votre choix : ");
    }
    
    private static void startGameForClient(ClientHandler client) {
        PlayerInfo player = players.get(client.socket);
        if (player != null) {
            player.isPlaying = true;
            player.lives = LIVES_MAX;
            player.frogX = WIDTH / 2;
            player.frogY = HEIGHT - 1;
            
            // Démarrer le jeu pour ce client
            Thread gameThread = new Thread(() -> {
                while (player.isPlaying && player.lives > 0) {
                    renderForClient(client, player);
                    client.requestMove();
                    try {
                        Thread.sleep(DIFFICULTE);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            gameThread.start();
        }
    }
    
    private static void renderForClient(ClientHandler client, PlayerInfo player) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vies restantes : ").append(player.lives).append("\n");
        
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Arrivals.isWPosition(x, y)) {
                    sb.append(FROG_WIN);
                } else if (y == 0) {
                    if(x % 5 == 0) {
                        sb.append(FINISH_LINE_CHAR);
                    } else {
                        sb.append(WALL_CHAR);
                    }
                } else if (x == player.frogX && y == player.frogY) {
                    sb.append(player.frogChar);
                } else if (isPlayerAt(x, y, player)) {
                    sb.append("🐸"); // Autre joueur
                } else if (y == HEIGHT / 2) {
                    sb.append(TERRE_PLEIN_CHAR);
                } else if (isObstacleAt(x, y)) {
                    sb.append(Obstacle.OBSTACLE_CHAR);
                } else {
                    sb.append(ROAD_CHAR);
                }
                sb.append("\t");
            }
            sb.append("\n");
        }
        client.sendMessage(sb.toString());
    }
    
    private static boolean isPlayerAt(int x, int y, PlayerInfo currentPlayer) {
        for (PlayerInfo player : players.values()) {
            if (player != currentPlayer && player.frogX == x && player.frogY == y) {
                return true;
            }
        }
        return false;
    }
    
    private static void updatePlayer(PlayerInfo player, char move) {
        switch (move) {
            case 'z': if (player.frogY > 1 ^ (player.frogY == 1 && player.frogX % 5 == 0)) player.frogY--; break;
            case 's': if (player.frogY < HEIGHT - 1) player.frogY++; break;
            case 'q': if (player.frogX > 0) player.frogX--; break;
            case 'd': if (player.frogX < WIDTH - 1) player.frogX++; break;
            case 'x': player.isPlaying = false; break;
        }
        
        // Vérifier si le joueur a atteint la ligne d'arrivée
        if (player.frogY == 0 && player.frogX % 5 == 0) {
            Arrivals.addWPosition(player.frogX, player.frogY);
            ClientHandler client = getClientForPlayer(player);
            if (client != null) {
                client.sendMessage("🎉 Félicitations ! Un prince est apparu à cette place !");
            }
            
            // Vérifier si tous les emplacements sont remplis
            if (Arrivals.GlobalWin()) {
                broadcastToAll("🏆 TOUS les emplacements sont remplis ! LE JEU EST TERMINÉ ! 🏆");
                resetGame();
            }
        }
        
        // Vérifier les collisions avec les obstacles
        checkCollisionForPlayer(player);
    }
    
    private static ClientHandler getClientForPlayer(PlayerInfo player) {
        for (Map.Entry<Socket, PlayerInfo> entry : players.entrySet()) {
            if (entry.getValue() == player) {
                return clients.get(entry.getKey());
            }
        }
        return null;
    }
    
    private static void checkCollisionForPlayer(PlayerInfo player) {
        if (isObstacleAt(player.frogX, player.frogY)) {
            player.lives--;
            ClientHandler client = getClientForPlayer(player);
            if (client != null) {
                client.sendMessage("💀 Un obstacle vous a écrasé ! Il vous reste " + player.lives + " vies. 💀");
            }
            
            if (player.lives <= 0) {
                if (client != null) {
                    client.sendMessage(getGameOverAscii());
                    client.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                    client.requestInput("Voulez-vous rejouer ? (y/n) : ");
                }
            } else {
                // Réinitialiser la position de la grenouille
                player.frogX = WIDTH / 2;
                player.frogY = HEIGHT - 1;
            }
        }
    }
    
    private static String getGameOverAscii() {
        return "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣠⡀⠀\n" +
               "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣤⣤⠀⠀⠀⢀⣴⣿⡶⠀⣾⣿⣿⡿⠟⠛⠁\n" +
               "⠀⠀⠀⠀⠀⠀⣀⣀⣄⣀⠀⠀⠀⠀⣶⣶⣦⠀⠀⠀⠀⣼⣿⣿⡇⠀⣠⣿⣿⣿⠇⣸⣿⣿⣧⣤⠀⠀⠀\n" +
               "⠀⠀⢀⣴⣾⣿⡿⠿⠿⠿⠇⠀⠀⣸⣿⣿⣿⡆⠀⠀⢰⣿⣿⣿⣷⣼⣿⣿⣿⡿⢀⣿⣿⡿⠟⠛⠁⠀⠀\n" +
               "⠀⣴⣿⡿⠋⠁⠀⠀⠀⠀⠀⠀⢠⣿⣿⣹⣿⣿⣿⣿⣿⣿⡏⢻⣿⣿⢿⣿⣿⠃⣼⣿⣯⣤⣴⣶⣿⡤⠀\n" +
               "⣼⣿⠏⠀⣀⣠⣤⣶⣾⣷⠄⣰⣿⣿⡿⠿⠻⣿⣯⣸⣿⡿⠀⠀⠀⠁⣾⣿⡏⢠⣿⣿⠿⠛⠋⠉⠀⠀⠀\n" +
               "⣿⣿⠲⢿⣿⣿⣿⣿⡿⠋⢰⣿⣿⠋⠀⠀⠀⢻⣿⣿⣿⠇⠀⠀⠀⠀⠙⠛⠀⠀⠉⠁⠀⠀⠀⠀⠀⠀⠀\n" +
               "⠹⢿⣷⣶⣿⣿⠿⠋⠀⠀⠈⠙⠃⠀⠀⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
               "⠀⠀⠈⠉⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣤⣤⣴⣶⣦⣤⡀⠀\n" +
               "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀⠀⠀⠀⠀⣠⡇⢰⣶⣶⣾⡿⠷⣿⣿⣿⡟⠛⣉⣿⣿⣿⠆\n" +
               "⠀⠀⠀⠀⠀⠀⢀⣤⣶⣿⣿⡎⣿⣿⣦⠀⠀⠀⢀⣤⣾⠟⢀⣿⣿⡟⣁⠀⠀⣸⣿⣿⣤⣾⣿⡿⠛⠁⠀\n" +
               "⠀⠀⠀⠀⣠⣾⣿⡿⠛⠉⢿⣦⠘⣿⣿⡆⠀⢠⣾⣿⠋⠀⣼⣿⣿⣿⠿⠷⢠⣿⣿⣿⠿⢻⣿⣧⠀⠀⠀\n" +
               "⠀⠀⠀⣴⣿⣿⠋⠀⠀⠀⢸⣿⣇⢹⣿⣷⣰⣿⣿⠃⠀⢠⣿⣿⢃⣀⣤⣤⣾⣿⡟⠀⠀⠀⢻⣿⣆⠀⠀\n" +
               "⠀⠀⠀⣿⣿⡇⠀⠀⢀⣴⣿⣿⡟⠀⣿⣿⣿⣿⠃⠀⠀⣾⣿⣿⡿⠿⠛⢛⣿⡟⠀⠀⠀⠀⠀⠻⠿⠀⠀\n" +
               "⠀⠀⠀⠹⣿⣿⣶⣾⣿⣿⣿⠟⠁⠀⠸⢿⣿⠇⠀⠀⠀⠛⠛⠁⠀⠀⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
               "⠀⠀⠀⠀⠈⠙⠛⠛⠛⠋⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀";
    }
    
    private static void broadcastToAll(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }
    
    private static void resetGame() {
        Arrivals.ClearwPositions();
        for (PlayerInfo player : players.values()) {
            player.frogX = WIDTH / 2;
            player.frogY = HEIGHT - 1;
        }
    }
    
    public static void main(String[] args) {
        try {
            // Initialisation du jeu
            initGame();
            
            // Démarrer le serveur
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Serveur démarré, en attente de connexions...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle grenouille connectée !");
                
                // Créer un nouveau joueur
                PlayerInfo player = new PlayerInfo(nextPlayerId++);
                players.put(clientSocket, player);
                
                // Créer et démarrer un gestionnaire de client
                ClientHandler clientHandler = new ClientHandler(clientSocket, player);
                clients.put(clientSocket, clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void initGame() {
        // Initialiser le jeu
        running = true;
        obstacles = new Obstacle[5];
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle(i * 4, HEIGHT / 2 - 2);
            obstacles[i].start();
        }
    }
    
    // Méthodes existantes
    private static boolean isObstacleAt(int x, int y) {
        for (Obstacle obs : obstacles) {
            if (obs.getX() == x && obs.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    private static void stopAllObstacles() {
        if (obstacles != null) {
            for (Obstacle obs : obstacles) {
                if (obs != null) {
                    obs.stopObstacle();
                }
            }
        }
    }
    
    // Autres méthodes existantes...
}