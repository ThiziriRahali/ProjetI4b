import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FroggerGamer {
    public static final String FINISH_LINE_CHAR = "🏁";
    public static final String WALL_CHAR = "🧱";
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String FROG_TET = "🦎";
    private static final String FROG_CHAR = "🐸";
    private static final String FROG_DRAG = "🐲";
    private static final String FROG_PRINCESS = "👸";
    private static final String ROAD_CHAR = ".";
    private static final String TERRE_PLEIN_CHAR = "🌱";
    private static final String FROG_WIN = "🤴";
    private static final String FROG_MEAN = "😡";
    private static Obstacle[] obstacles;
    private static final int LIVES_MAX = 3;
    public static int nbVieActuel;
    public static Arrivals A = new Arrivals();
    private static final int DIFFICULTE = 500; 
    private static volatile boolean running= true;
    
    // multijoueur
    private static Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<Socket, PlayerInfo> players = new ConcurrentHashMap<>();
    private static int nextPlayerId = 1;
    

    static class PlayerInfo {
        int id;
        int frogX;
        int frogY;
        int lives;
        String frogChar;
        boolean isPlaying;
        int cpt ;
        int niveau;
        String rang;
        
        public PlayerInfo(int id) {
            this.id = id;
            this.frogX = WIDTH / 2;
            this.frogY = HEIGHT - 1;
            this.lives = LIVES_MAX;
            this.frogChar = FROG_TET + id;
            this.isPlaying = true;
            this.niveau=0;
        }

        public String getEmojiNiveau() {
            actuEmoji(this);
            if (niveau > 0 & niveau<=2){
                return FROG_TET;
            }
            else  if (niveau > 2 & niveau <= 6){
                return FROG_CHAR; 
            }
            else  if (niveau > 7 & niveau <= 20){
                return FROG_DRAG; 
            }
            else  if (niveau > 20){
                return FROG_PRINCESS; 
            }
            return FROG_CHAR;
        }

        public void actuEmoji(PlayerInfo player){
            if (player.niveau > 0 & player.niveau<=2){
                player.frogChar = FROG_TET + id;
            }
            else  if (player.niveau > 2 & player.niveau <= 6){
                player.frogChar = FROG_CHAR+id; 
            }
            else  if (player.niveau > 7 & player.niveau <= 20){
                player.frogChar = FROG_DRAG+id; 
            }
            else  if (player.niveau > 20){
                player.frogChar = FROG_PRINCESS+id; 
            }
        }
    }
    
    //  client connecté
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
                        // Envoyer menu au client
                        afficherMenuPrincipalClient(this);
                    } else if (message.equals("QUIT")) {
                        break;
                    } else if (player.isPlaying) {
              
                            try {
                                int choice = Integer.parseInt(message);
                                processMenuChoice(this, choice);
                            } catch (NumberFormatException e) {
                                switch(message){

                                    case "y": afficherMenuPrincipalClient(this); break;
                                    case "n": break;

                                }
                                
                                updatePlayer(player, message);
                                checkCollisionForPlayer(player);
                                    
                                
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
    
    private static void sendAllMessage(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
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
        running=true;
        PlayerInfo player = players.get(client.socket);
        player.cpt=0;
        if (player != null) {
            player.isPlaying = true;
            player.lives = LIVES_MAX;
            player.frogX = WIDTH / 2;
            player.frogY = HEIGHT - 1;
            player.actuEmoji(client.player);
            
            
            
            Thread gameThread = new Thread(() -> {
                while (player.isPlaying && running) {
                    renderForClient(client, player);
                    client.requestMove();
                    client.sendMessage("Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ");
                    pause(10);
                }
            });
            gameThread.start();
        }
    }
    
    private static void renderForClient(ClientHandler client, PlayerInfo player) {
        client.sendMessage("\033[H\033[2J");
        System.out.flush();
        StringBuilder sb = new StringBuilder();
        sb.append("Vies restantes : ").append(player.lives).append("\n");
        
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Arrivals.isWPosition(x, y)) {
                    sb.append(FROG_WIN);
                } else if (y == 0) {
                    if(x % 4 == 0) {
                        sb.append(FINISH_LINE_CHAR);
                    } else {
                        sb.append(WALL_CHAR);
                    }
                } else if (x == player.frogX && y == player.frogY) {
                    sb.append(player.getEmojiNiveau());}
                 else if (isPlayerAt(x, y, player)) {
                    sb.append("☁️"); // Autre joueur
                } 
                    else if (y == HEIGHT / 2) {
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

    public static synchronized void checkAllPlayersCollisions() {
        for (PlayerInfo player : players.values()) {
            if (player.isPlaying && player.lives > 0) {
                checkCollisionForPlayer(player);
            }
        }
    }


    private static void checkCollisionForPlayer(PlayerInfo player) {
        if (isObstacleAt(player.frogX, player.frogY)) {
            
            player.lives--;
            ClientHandler client = getClientForPlayer(player);

            client.sendMessage("\033[H\033[2J");
            System.out.flush();
            
            
            if (client != null) {
                client.sendMessage("💀 Un obstacle vous a écrasé ! Il vous reste " + player.lives + " vies. 💀");
                
            }
            
            if (player.lives <= 0) {
                if (client != null) {
                    
                    client.sendMessage(GameOver());
                    client.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                    running=false;
                    askreplay(client);

                    return;
                }
            } else {
                resetFrog(player);
            }
        }
    }

    private static void askreplay(ClientHandler client){
        client.requestInput("Voulez-vous rejouer ? (y/n) : ");
        Arrivals.ClearwPositions();
    }
    
  

    private static void updatePlayer(PlayerInfo player, String move) {
       
        switch (move) {
            case "z": if (player.frogY > 1 ^ (player.frogY == 1 && player.frogX % 4 == 0)) player.frogY--; break;
            case "s": if (player.frogY < HEIGHT - 1) player.frogY++; break;
            case "q": if (player.frogX > 0) player.frogX--; break;
            case "d": if (player.frogX < WIDTH - 1) player.frogX++; break;
            case "x": player.isPlaying = false; break;
        }
    
        
        checkCollisionForPlayer(player);
        if (player.frogY == 0 && player.frogX % 4 == 0) {
            Arrivals.addWPosition(player.frogX, player.frogY);
            ClientHandler client = getClientForPlayer(player);
            if (client != null) {
                player.cpt++;
                
                sendAllMessage("🎉 Félicitations ! Un prince est apparu !");
                resetFrog(player);
            }
            
            
            if (Arrivals.GlobalWin()) {
                System.out.println("DEBUG: Victoire détectée !");
                sendAllMessage("\033[H\033[2J");
                System.out.flush();
                running = false;
                
                sendAllMessage("🏆 TOUS les emplacements sont remplis ! LE JEU EST TERMINÉ ! 🏆");
                PlayerInfo W = null;
                int gagnant = -1;
                
                for (PlayerInfo p : players.values()) {
                    if (p.cpt > gagnant) {
                        gagnant = p.cpt;
                        W = p;
                    }
                }
                getClientForPlayer(W).sendMessage(goodJob());
                
                // Announce the winner
                if (W!= null) {
                    sendAllMessage("🎖️ Le joueur " + W.id + " remporte la partie avec " + W.cpt + " arrivées ! 🎖️");
                }
                player.niveau=player.cpt;
                askreplay(client);
            }
        }
    }

    private static ClientHandler getClientForPlayer(PlayerInfo player) {
        for (Map.Entry<Socket, PlayerInfo> entry : players.entrySet()) {
            if (entry.getValue() == player) {
                return clients.get(entry.getKey());
            }
        }
        return null;
    }
    
   
    private static void resetFrog(PlayerInfo player) {
        player.frogX = WIDTH / 2;
        player.frogY = HEIGHT - 1;
    }
    private static String goodJob() {
        return "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀                    ⠀⣀⣀⣀⣀⣀⣀⣤⣤⣤⣤⣤⣤⣤⣤⣤⣶⣶⣦⠤⡤⠶\n" +
        "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣀⣀⣀⣀⣀⣀⣀⣀⣠⣤⣤⣤⣤⣤⣶⣶⣶⣶⣶⣾⣿⣿⣿⣿⣿⡿⠿⠿⠿⠿⠿⠿⠿⠿⠟⠛⠛⠛⠿⠿⠻⠋⠟⠍⣿⢿⣮⠀⠀\n" +
        "⢰⣶⣶⣿⣷⣷⣶⣷⣿⣿⡿⠿⠿⠿⠿⠿⠿⠿⠛⠛⢛⣛⣛⣛⠉⠉⠉⠉⠉⠉⠉⢉⣡⣤⣶⣶⣶⣤⣄⠀⠀⠀⣶⣶⣶⣾⣿⣿⡼⣿⣿⣶⣦⡀⠀⠀⢺⣗⠀⠀⠀\n" +
        "⢸⣿⡏⠉⠉⠀⢀⣤⣴⣶⣶⣶⣤⣴⣿⡆⠀⠀⢠⣴⣿⣿⡿⢿⣿⣿⣷⣄⠀⠀⠀⣴⣿⣿⣿⠋⠉⣿⣿⣿⣿⣆⠀⠈⣿⣿⣿⣿⣿⡇⢸⣿⣿⣿⣿⡄⠀⢸⣺⠠⠀⠀\n" +
        "⠀⣿⡇⠀⢀⣴⣿⣿⣿⠁⠈⠙⢻⣿⣿⡇⠀⣰⣿⣿⣿⣿⠀⠀⣿⣿⣿⣿⣷⡀⣸⣿⣿⣿⣿⡇⠀⢹⣿⣿⣿⣿⡇⠀⣿⣿⣿⣿⣿⡇⠸⣿⣿⣿⣿⣿⡄⢸⣸⠀⠀⠀\n" +
        "⠀⣿⣧⠀⣸⣿⣿⣿⣿⠀⠀⠀⠀⠹⣿⡇⠀⣿⣿⣿⣿⣿⠀⠀⣿⣿⣿⣿⣿⣧⣿⣿⣿⣿⣿⡇⠀⢸⣿⣿⣿⣿⣿⠀⢹⣿⣿⣿⣿⡇⠀⣿⣿⣿⣿⣿⡇⠀⢰⡧⠀⠀\n" +
        "⠀⣿⣿⢀⣿⣿⣿⣿⣿⡀⠀⢀⣀⣀⣉⣁⡀⣿⣿⣿⣿⣿⡆⠀⢹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⠀⠸⣿⣿⣿⣿⣿⠀⢸⣿⣿⣿⣿⡇⠀⣿⣿⣿⣿⣿⠁⢠⣿⡍⠀⠀\n" +
        "⠀⣿⣿⠀⣿⣿⣿⣿⣿⡇⠻⢿⣿⣿⣿⣿⠃⣿⣿⣿⣿⣿⡇⠀⢸⣿⣿⣿⣿⣿⢻⣿⣿⣿⣿⡇⠀⠀⣿⣿⣿⣿⡿⠀⢸⣿⣿⣿⣿⣧⠀⣿⣿⣿⣿⣿⡇⠀⠀⡇⢆⠀⠀\n" +
        "⠀⢻⣿⡄⢹⣿⣿⣿⣿⡇⠀⢸⣿⣿⣿⣿⠀⠹⣿⣿⣿⣿⡇⠀⢸⣿⣿⣿⣿⠃⠀⠻⣿⣿⣿⣿⠀⢀⣿⣿⣿⡿⠃⠀⣠⣿⣿⣿⣿⣿⣴⣿⣿⡿⠋⠀⠀⠀⣼⣿⠀⠀\n" +
        "⠀⢸⣿⡇⠈⠻⣿⣿⣿⣧⠀⢸⣿⣿⣿⣿⠀⠀⠙⢿⣿⣿⣷⣄⣼⣿⣿⠿⠃⠀⠀⠀⠉⠻⠿⣿⣿⣿⡿⠟⠋⠀⠀⠀⠛⠛⠛⠛⠋⠉⠈⠉⠁⢀⣀⡀⠀⠀⣸⣿⠄⠀\n" +
        "⠀⢸⣿⡇⠀⠀⠉⠛⠿⣿⣧⣿⠿⠟⠋⠁⠀⠀⠀⠀⠈⠙⠛⠛⢛⡋⣁⣀⠀⠀⠀⠀⠀⠀⣠⣤⣤⣤⣤⣤⣴⣤⣤⣶⣶⣤⣤⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
        "⠀⠀⣿⣧⠀⠀⠀⠀⠀⣀⣤⣤⣤⣤⣶⣶⣶⣦⠀⠀⠀⣠⣶⣿⣿⡷⢿⣿⣿⣷⣦⣀⠀⠀⠻⢿⣿⣿⣿⣿⣿⡏⠛⢿⣿⣿⣿⣿⣷⡄⠀⠸⣿⣿⣿⣿⣿⠀⠸⣿⡡⠀\n" +
        "⠀⠀⣿⣿⠀⠀⠀⠀⠀⠀⠸⣿⣿⣿⣿⣿⣷⠀⠀⣼⣿⣿⣿⣿⣿⠀⠀⢸⣿⣿⣿⣿⣿⡇⠀⢸⣿⣿⣿⣿⣿⣇⠀⢸⣿⣿⣿⣿⡿⠁⠀⠀⠈⣿⣿⣿⡇⠀⠀⢹⡗⡃\n" +
        "⠀⠀⣿⣿⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⠀⢠⣿⣿⣿⣿⣿⣿⠀⠀⢸⣿⣿⣿⣿⣿⣿⠀⠸⣿⣿⣿⣿⣿⣿⢾⣿⣿⣿⣿⣥⣀⠀⠀⠀⠀⢹⣿⣿⠃⠀⠀⢀⢳⠬\n" +
        "⠀⠀⢿⣿⡀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⠀⠸⣿⣿⣿⣿⣿⣿⡆⠀⢸⣿⣿⣿⣿⣿⣿⠀⠀⣿⣿⣿⣿⣿⣿⠀⠙⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠿⠿⠀⠀⠀⠀⣸⡽\n" +
        "⠀⠀⢸⣿⡇⠀⠀⣀⣀⡀⠀⢻⣿⣿⣿⣿⣿⠀⠀⢿⣿⣿⣿⣿⣿⡇⠀⠀⣿⣿⣿⣿⣿⣿⠀⠀⣿⣿⣿⣿⣿⣿⡄⠀⣿⣿⣿⣿⣿⣿⡇⠀⠀⣴⣿⣿⣿⣦⠀⢸⣿⡀\n" +
        "⠀⠀⢸⣿⡇⢠⣾⣿⣿⣿⣧⣼⣿⣿⣿⣿⣿⡇⠀⠸⣿⣿⣿⣿⣿⡇⠀⠀⣿⣿⣿⣿⣿⠇⠀⠀⢻⣿⣿⣿⣿⣿⡇⢀⣿⣿⣿⣿⣿⡿⠀⠀⠘⣿⣿⣿⣿⣧⠀⢼⣗⠀\n" +
        "⠀⠀⠘⣿⡇⢸⣿⣿⣿⣿⡿⢸⣿⣿⣿⣿⣿⠃⠀⠀⠘⢿⣿⣿⣿⣧⣀⣠⣿⣿⣿⡿⠋⠀⠀⣴⣿⣿⣿⣿⣿⣿⣿⣿⣿⠿⠿⠿⠿⠋⠀⠀⠀⠀⠙⠿⠿⠟⢃⢀⢸⡇⠀\n" +
        "⠀⠀⠀⣿⡇⠈⠻⣿⣿⣿⡤⣾⣿⣿⣿⡿⠋⠀⠀⠀⠀⠀⠉⠛⠻⠿⠿⠛⠟⠛⠉⠀⠀⢀⣀⣈⣉⣉⣉⣡⣠⣀⣀⣀⣴⣦⣦⣦⣶⣶⣶⣶⣿⣿⣿⣿⣿⣿⠾⠾⠇⠀\n" +
        "⠀⠀⠀⣿⣿⣀⣀⣀⣉⣉⣅⣍⣉⣁⣠⣤⣠⣤⣶⣦⣦⣴⣶⣶⣿⣿⣿⣿⣿⠿⠿⠿⠿⠿⠛⠛⠛⠛⠛⠛⠛⠛⠋⠉⠉⠉⠉⠉⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" +
        "⠀⠀⠀⢹⣿⠿⠿⠿⠿⠿⠛⠛⠛⠛⠛⠋⠉⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀";
    }
    

    
    private static String GameOver() {
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
    
    // private static void resetGame() {
    //     Arrivals.ClearwPositions();

    //     for (PlayerInfo player : players.values()) {
    //         player.frogX = WIDTH / 2;
    //         player.frogY = HEIGHT - 1;
    //     }
    //     initGame();
    // }
    
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
                
                PlayerInfo player = new PlayerInfo(nextPlayerId++);
                players.put(clientSocket, player);
                
                //  gestion client
                ClientHandler clientHandler = new ClientHandler(clientSocket, player);
                clients.put(clientSocket, clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void initGame() {
        
        
        obstacles = new Obstacle[5];
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle(i * 4, HEIGHT / 2 - 2);
            obstacles[i].start();

        }
    }
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


    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
           
        }
    }
    
   
}