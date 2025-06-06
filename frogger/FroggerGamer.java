import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FroggerGamer {
    public static final String FINISH_LINE_CHAR = "🏁";
    public static final String WALL_CHAR = "🧱";
    public static final int WIDTH = 10;
    public static final int HEIGHT = 10;
    private static final String ROAD_CHAR = ".";
    private static final String WAVE_CHAR = "🌊";
    private static final String TERRE_PLEIN_CHAR = "🌱";
    private static final String FROG_WIN = "🤴";
    private static final String DEPART_CHAR = "⬆️";
    private static final Map<Integer, SalleJeu> sallesJeu = new ConcurrentHashMap<>();
    private static int nextsalleId = 1;
    private static  NotreTimer timer;
    private static boolean PartieStarted;

    private static Obstacle[] obstaclesA;
    private static Obstacle[] obstaclesB;

    public static final int LIVES_MAX = 3;

    public static Arrivals A = new Arrivals();

    public static final int DIFFICULTE = 2; // 1 = facile, 2 = moyen, 3 = difficile;

    private static final Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final Map<Socket, PlayerInfo> players = new ConcurrentHashMap<>();
    private static final Map<Socket, Equipe> equipes = new ConcurrentHashMap<>();
    private static int nextPlayerId = 1;
    

   
    
    private static synchronized SalleJeu createSalleJeu(String salleName) {
        SalleJeu salle = new SalleJeu(nextsalleId++, salleName);
        sallesJeu.put(salle.getsalleId(), salle);
        return salle;
    }

    private static SalleJeu getSalleJeuById(int salleId) {
        return sallesJeu.get(salleId);
    }

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
                        Welcome(this);
                    } else if (message.equals("QUIT")) {
                        break;
                    } else if (!player.isPlaying && !player.running) {
                        if (player.lives <= 0) {
                            if (message.equals("y")) {
                                player.lives = LIVES_MAX;
                                player.cpt = 0;
                                player.isPlaying = true;
                                player.running = true;
                                OptionsSalle(this);
                            } else if (message.equals("n")) {
                                player.isPlaying = false;
                                player.running = false;
                                this.sendMessage("Au revoir !");
                                OptionsSalle(this);
                            }
                        } 
                    }
                    else {
                        updatePlayer(player, message);
                        checkCollisionForPlayer(player);
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
    
    private static void renderForClient(ClientHandler client, PlayerInfo player) {
        SalleJeu salle = getSalleJeuById(player.getCurrentsalleId());
        if (salle == null) return;
        
        client.sendMessage("\033[H\033[2J");
        System.out.flush();
        StringBuilder sb = new StringBuilder();
        sb.append("Vies restantes : ").append(player.lives).append(" | Équipe: ").append(player.getEquipe().getNomEquipe()).append("\n");
        sb.append("Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ").append("\n");
        
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
                } else if (getPlayerAtInSalle(x, y, salle) != null) {
                    sb.append(getPlayerAtInSalle(x, y, salle).getEmojiNiveau());
                } else if (y == HEIGHT / 2) {
                    sb.append(TERRE_PLEIN_CHAR);
                } else if (salle.isObstacleAt(x, y, salle)) {
                    boolean isObstacleA = false;
                    for (Obstacle obs : salle.getObstaclesA()) {
                        if (obs.getX() == x && obs.getY() == y) {
                            sb.append(obs.getCharA());  
                            isObstacleA = true;
                            break;
                        }
                    }
                    
                    if (!isObstacleA) {
                        for (Obstacle obs : salle.getObstaclesB()) {
                            if (obs.getX() == x && obs.getY() == y) {
                                sb.append(obs.getCharB());  
                                break;
                            }
                        }
                    }
                }
                else if (y <=4 && y != 0 && y != HEIGHT-1 )  {
                    sb.append(WAVE_CHAR);
                } 
                else if ( y == HEIGHT-1 )  {
                    sb.append(DEPART_CHAR);
                }else {
                    sb.append(ROAD_CHAR);
                }
                sb.append("\t");
            }
            sb.append("\n");
        }
        client.sendMessage(sb.toString());
    }
    
    
    public static PlayerInfo getPlayerAtInSalle(int x, int y, SalleJeu salle) {
        for (PlayerInfo player : salle.players) {
            if (player.frogX == x && player.frogY == y) {
                return player;
            }
        }
        return null;
    }

    public static PlayerInfo getPlayerAt(int x, int y) {
        for (PlayerInfo player : players.values()) {
            if (player.frogX == x && player.frogY == y) {
                return player;
            }
        }
        return null;
    }

    public static synchronized void checkAllPlayersCollisions() {
        for (PlayerInfo player : players.values()) {
            if (player.isPlaying && player.lives > 0) {
                checkCollisionForPlayer(player);
            }
        }
    }
    private static void checkCollisionForPlayer(PlayerInfo player) {
        ClientHandler client = getClientForPlayer(player);
        SalleJeu salle = getSalleJeuById(player.getCurrentsalleId());
        if (salle == null) return;
       
        
        for (PlayerInfo otherPlayer : salle.players) {
            if (player != otherPlayer) { 
                MangerMiam(player, otherPlayer);
            }
        }
        if (player.lives <= 0) {
            return;
        }
        
        if (salle.isObstacleAt(player.frogX, player.frogY, salle)) {
            player.lives--;
    
            client.sendMessage("\033[H\033[2J");
            System.out.flush();
            
            client.sendMessage("💀 Un obstacle vous a écrasé ! Il vous reste " + player.lives + " vies. 💀");
            
            if (player.lives <= 0) {
                GameOver(client);
                client.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                player.isPlaying = false;
                player.running = false;
                salle.removePlayer(player);               
            } else {
                resetFrog(player, 0, HEIGHT - 1);
            }
        }
    }

    private static void askreplay(ClientHandler client) {
        try {
            client.player.running = false;
            client.sendMessage("Vous avez perdu! Voulez-vous rejouer ? (y/n) : ");
            client.requestInput("Répondez par 'y' pour rejouer ou 'n' pour quitter : ");
            String message = "";
            try {
                message = client.input.readLine();
                switch (message) {
                    case "y":
                            client.player.lives = LIVES_MAX;
                            client.player.cpt = 0;
                            client.player.isPlaying = true;
                            client.player.running = true;
                            resetFrog(client.player, 0, HEIGHT - 1);
                            OptionsSalle(client);
                        break;
                    case "n":
                             client.sendMessage("QUIT");
                
                    default:
                        askreplay(client);
                        break;
                }
            } catch (Exception e) {
                System.out.println("Erreur de lecture de l'entrée : " + e.getMessage());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updatePlayer(PlayerInfo player, String move) {
       
        switch (move) {
            case "z": 
                if (player.frogY > 1 || (player.frogY == 1 && player.frogX % 4 == 0))
                    player.frogY--;
                break;
            case "s":
                if (player.frogY < HEIGHT - 1)
                    player.frogY++;
                break;
            case "q": 
                if (player.frogX > 0)
                    player.frogX--;
                break;
            case "d":
                if (player.frogX < WIDTH - 1)
                    player.frogX++;
                break;
            case "x":
                player.isPlaying = false;
                break;
        }
    
        
        checkCollisionForPlayer(player);
        if (player.frogY == 0 && player.frogX % 4 == 0) {
            SalleJeu salle = getSalleJeuById(player.getCurrentsalleId());
            if (salle == null) return;
            salle.getArrivals().addWPosition(player.frogX, player.frogY);
            ClientHandler client = getClientForPlayer(player);
            if (client != null) {
                player.cpt++;
                player.niveau++;
                
                notifysallePlayers(salle, "🎉 Félicitations ! Un prince est apparu !");
                resetFrog(player, 0 ,HEIGHT - 1);
            }
            
            
            if (salle.getArrivals().GlobalWin()) {
                
                sendAllMessage("\033[H\033[2J");
                System.out.flush();
                notifysallePlayers(salle, "🏆 TOUS les emplacements sont remplis ! LE JEU EST TERMINÉ ! 🏆");
                PlayerInfo W = null;
                int gagnant = -1;
                
                for (PlayerInfo p : players.values()) {
                    if (p.cpt > gagnant) {
                        gagnant = p.cpt;
                        W = p;
                    }
                }
                
                
                if (W!= null) {
                    goodJob(getClientForPlayer(W));
                    salle.removePlayer(player);
                    notifysallePlayers(salle, "🎖️ L'équipe \""+ W.getEquipe().getNomEquipe() +"\" remporte la partie avec " + W.cpt + " arrivées ! 🎖️");
                }
                
                
            }
        }
    }

    public static ClientHandler getClientForPlayer(PlayerInfo player) {
        for (Map.Entry<Socket, PlayerInfo> entry : players.entrySet()) {
            if (entry.getValue() == player) {
                return clients.get(entry.getKey());
            }
        }
        return null;
    }

    public static void startTimer(int durationInSeconds, Runnable onTimeUp) {
        
        timer = new NotreTimer(durationInSeconds, onTimeUp);
        timer.start();
    }
    
   
    private static void resetFrog(PlayerInfo player, int x, int y) {
        player.frogX = x ;
        player.frogY = y ;
    }

    private static void demarServeur() {
        try {
            ServerSocket serverSocket = new ServerSocket(12345);
            System.out.println("Serveur démarré, en attente de connexions...");
    
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Nouvelle connexion entrante !");
    
                PlayerInfo player = new PlayerInfo(nextPlayerId++, new Equipe(nextPlayerId, "Equipe "+nextPlayerId, null));
                player.getEquipe().addJoueur(player);
                player.isCarnivore = false;
                
                equipes.put(clientSocket, player.getEquipe());
                players.put(clientSocket, player);
    
                ClientHandler clientHandler = new ClientHandler(clientSocket, player);
                clients.put(clientSocket, clientHandler);
                clientHandler.start();
            
               
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
        


    private static void goodJob(ClientHandler client) {
        client.sendMessage( "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀                    ⠀⣀⣀⣀⣀⣀⣀⣤⣤⣤⣤⣤⣤⣤⣤⣤⣶⣶⣦⠤⡤⠶\n" +
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
        "⠀⠀⠀⢹⣿⠿⠿⠿⠿⠿⠛⠛⠛⠛⠛⠋⠉⠉⠉⠉⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
        PlayerInfo player = players.get(client.socket);
        SalleJeu salle = getSalleJeuById(player.getCurrentsalleId());
        
        if (salle != null) {
            salle.removePlayer(player); 
        }

        if (salle != null) {
            salle.removePlayer(player); 
        }

        
        player.isPlaying = false;
        player.running = false;
        askreplay(client);
        if (salle != null) {
            Verif(salle);
        }
    }
    

    
    private static void GameOver(ClientHandler client) {
        client.sendMessage("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣠⡀⠀\n" +
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
        "⠀⠀⠀⠀⠈⠙⠛⠛⠛⠋⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
        PlayerInfo player = players.get(client.socket);
        SalleJeu salle = getSalleJeuById(player.getCurrentsalleId());
        
        if (salle != null) {
            salle.removePlayer(player);
        }

        player.isPlaying = false;
        player.running = false;
        askreplay(client);
        if (salle != null) {
            Verif(salle);
        }

    }
    private static void Verif(SalleJeu salle) {
        boolean toutp = true;
    
        for (PlayerInfo player : salle.players) {
            if (player.isPlaying && player.lives > 0) {
                toutp = false;
                break;
            }
        }
    
        if (toutp) {
            notifysallePlayers(salle, "😢 Tous les joueurs ont perdu ! La partie est terminée.");
            salle.setGameStarted(false);
            salle.stopTimer();
        }
    }
    private static void Welcome(ClientHandler client) { 
        client.sendMessage( """
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓████████▓▒░▒▓█▓▒░      ░▒▓██████▓▒░ ░▒▓██████▓▒░░▒▓██████████████▓▒░░▒▓████████▓▒░ 
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░      ░▒▓█▓▒░     ░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░        
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░      ░▒▓█▓▒░     ░▒▓█▓▒░      ░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░        
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓██████▓▒░ ░▒▓█▓▒░     ░▒▓█▓▒░      ░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓██████▓▒░   
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░      ░▒▓█▓▒░     ░▒▓█▓▒░      ░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░        
            ░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░      ░▒▓█▓▒░     ░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓█▓▒░        
             ░▒▓█████████████▓▒░░▒▓████████▓▒░▒▓████████▓▒░▒▓██████▓▒░ ░▒▓██████▓▒░░▒▓█▓▒░░▒▓█▓▒░░▒▓█▓▒░▒▓████████▓▒░ 
            """);
            client.sendMessage("😊 Coucou je suis Anne TiVirus, ton assitante sur le jeu ! Je remplace mon mari Jean TiVirus qui est malade, je crois qu'il a un virus ");
            client.sendMessage("😊 Es-tu prêt à jouer ? (o/n) : ");
            try {
                String message = client.input.readLine();
                switch (message) {
                    case "o":
                        OptionsSalle(client);
                        break;
                    case "n":
                        client.sendMessage("😪 Dommage, mais je comprends. Prends soin de toi et reviens quand tu es prêt !");
                        client.sendMessage("QUIT");
                        client.socket.close();
                        break;
                    
                    default:
                    Welcome(client);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static void OptionsSalle(ClientHandler client) {
            client.sendMessage("\n😊 LES OPTIONS DONT TU DISPOSES 🏠");
            client.sendMessage("1. Créer une nouvelle salle de jeu 🏠✨");
            client.sendMessage("2. Rejoindre une salle existante 🏘️");
            client.sendMessage("3.  🤐 Mode secret");
            client.requestInput("😊  Choisis une option : ");
            
            try {
                String choice = client.input.readLine();
                switch (choice) {
                    case "1":
                        createNewsalle(client);
                        break;
                    case "2":
                        joinExistingsalle(client);
                        break;
                    case "3":
                        createNewsalleSOLO(client);
                        break;
                    default:
                        client.sendMessage("🫤 le choix est invalide.Réessaie.");
                        OptionsSalle(client);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        
    }
    private static void createNewsalle(ClientHandler client) {
        client.requestInput(" 😊 Entre un nom pour ta salle de jeu : 😊");
        try {
            String salleName = client.input.readLine();
            SalleJeu newsalle = createSalleJeu(salleName);
            
            PlayerInfo player = players.get(client.socket);
            if (player != null) {
                player.setCurrentsalleId(newsalle.getsalleId());
                newsalle.addPlayer(player);
                
                client.sendMessage("😊 "+salleName + "?  J'aime bien le nom ! Voici l'ID de votre salle : " + newsalle.getsalleId());
                client.sendMessage("Vous êtes maintenant dans votre salle");
                
                salleSetupOptions(client, newsalle);
            }
        } catch (IOException e) {
            e.printStackTrace();
            client.sendMessage(" 🫤 Erreur lors de la création de la salle. 🫤");
            OptionsSalle(client);
        }
    }

    private static void createNewsalleSOLO(ClientHandler client) {
        client.requestInput(" 😊 Entre un nom pour ta salle de jeu : 😊");
        try {
            String salleName = client.input.readLine();
            SalleJeu newsalle = createSalleJeu(salleName);
            
            PlayerInfo player = players.get(client.socket);
            if (player != null) {
                player.setCurrentsalleId(newsalle.getsalleId());
                newsalle.addPlayer(player);
                
                client.sendMessage("😊 "+salleName + "?  J'aime bien le nom ! Voici l'ID de votre salle : " + newsalle.getsalleId());
                client.sendMessage("Vous êtes maintenant dans votre salle");
                
                newsalle.setMaxPlayers(1);
                newsalle.setGameStarted(true);

                startGameForsalle(newsalle);
            }
        } catch (IOException e) {
            e.printStackTrace();
            client.sendMessage(" 🫤 Erreur lors de la création de la salle. 🫤");
            OptionsSalle(client);
        }
    }
    
    private static void joinExistingsalle(ClientHandler client) {
       
        if (sallesJeu.isEmpty()) {
            client.sendMessage("😊 Aucune salle n'est disponible. Veux tu créer une nouvelle salle.");
            OptionsSalle(client);
            return;
        }
        
        client.sendMessage("\n🏘️  Salles Disponibles 🏘️");
        for (SalleJeu salle : sallesJeu.values()) {
            if (!salle.isGameStarted() && salle.getCurrentPlayers() < salle.getMaxPlayers()) 
            {
                client.sendMessage(salle.getsalleId() + ". " + salle.getsalleName() +  "\t 😒 ne juge pas le nom il est horrible j'ai essayé de le lui dire... (" + salle.getCurrentPlayers() + "/" + salle.getMaxPlayers() + " joueurs)");
            }
        }
        
        client.requestInput("😊 Entre l'ID de la salle que tu souhaite rejoindre (ou 0 pour revenir en arrière) : ");
        try {
            int salleId = Integer.parseInt(client.input.readLine());
            if (salleId == 0) {
                OptionsSalle(client);
                return;
            }
            
            SalleJeu salle = getSalleJeuById(salleId);
            if (salle == null) {
                client.sendMessage("🫤 l'ID de salle est invalide. réessaye.");
                joinExistingsalle(client);
                return;
            }
            
            if (salle.isGameStarted()) {
                client.sendMessage("🫤 Cette partie a déjà commencé. Choisis une autre salle.");
                joinExistingsalle(client);
                return;
            }
            
            if (salle.getCurrentPlayers() >= salle.getMaxPlayers()) {
                client.sendMessage(" 🫤 Cette salle est pleine. Choisis une autre salle.");
                joinExistingsalle(client);
                return;
            }
            
            
            PlayerInfo player = players.get(client.socket);
            if (player != null) {
                player.setCurrentsalleId(salle.getsalleId());
                salle.addPlayer(player);
                
                client.sendMessage("😊 Tu as rejoint la salle : " + salle.getsalleName());
                
                notifysallePlayers(salle, "Joueur " + player.id + " a rejoint la salle! Soyez sympas avec lui");
                
                if (salle.getCurrentPlayers() >= salle.getMaxPlayers()) {
                    notifysallePlayers(salle, "😁 A vos marques pret? La partie va commencer !!!");
                    startGameForsalle(salle);
                } else {
                    client.sendMessage("😪⌚ On attend les autres... (" + 
                            salle.getCurrentPlayers() + "/" + salle.getMaxPlayers() + ")");
                }
            }
        } catch (NumberFormatException e) {
            client.sendMessage("🫤 l'ID de salle est invalide.Réessaie.");
            joinExistingsalle(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void salleSetupOptions(ClientHandler client, SalleJeu salle) {
        client.sendMessage("\n🏚️ Configuration de la Salle 🏚️");
        client.sendMessage("1.🤼 Définir le nombre maximum de joueurs 🤼");
        client.sendMessage("2.🕹️ Choisir le mode de jeu (Collaboratif/Compétition)🕹️");
        client.sendMessage("3. 🚩 Démarrer la partie 🚩");
        client.sendMessage("4. 🔙 Retour au menu des salles");
        client.requestInput("😊 Veuillez choisir une option : ");
        
        try {
            String choice = client.input.readLine();
            switch (choice) {
                case "1":
                    client.requestInput("😊 Entrez le nombre maximum de joueurs (2-10) : ");
                    try {
                        int maxPlayers = Integer.parseInt(client.input.readLine());
                        if (maxPlayers >= 1 && maxPlayers <= 10) {
                            salle.setMaxPlayers(maxPlayers);
                            client.sendMessage(" 😊 Nombre maximum de joueurs est défini à " + maxPlayers);
                        } else {
                            client.sendMessage("🫤 Nombre invalide. ");
                        }
                    } catch (NumberFormatException e) {
                        client.sendMessage("🫤 Concentre toi choisis un nombre!.");
                    }
                    salleSetupOptions(client, salle);
                    break;
                    
                case "2":
                    client.sendMessage("😊 Choisissez un mode de jeu :");
                    client.sendMessage(" 👬 1. Mode Collaboratif 👬");
                    client.sendMessage("🤼 2. Mode Competition 🤼 ");
                    client.requestInput("😊 Entrez votre choix (1 ou 2) : ");
                    
                    try {
                        int modeChoice = Integer.parseInt(client.input.readLine());
                        salle.setGameMode(modeChoice == 1 ? "Collaboratif" : "Competition");
                        client.sendMessage("Le mode de jeu est  défini à : " + salle.getGameMode());
                    } catch (NumberFormatException e) {
                        client.sendMessage("🫤 Choix invalide. On choisi pour vous, Competition se sera.");
                        salle.setGameMode("Competition");
                    }
                    salleSetupOptions(client, salle);
                    break;
                    
                case "3":
                    if (salle.getCurrentPlayers() < 2) {
                        client.sendMessage("🫤  Il faut au moins 2 joueurs pour démarrer la partie.");
                                
                        startsalleWaitingThread(salle);
                        
                    } else {
                        
                        startGameForsalle(salle);
                    }
                    break;
                    
                case "4":
                    OptionsSalle(client);
                    break;
                    
                default:
                    client.sendMessage("🫤  Choix invalide. Réessaie.");
                    salleSetupOptions(client, salle);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void startsalleWaitingThread(SalleJeu salle) {
        new Thread(() -> {
            while (!salle.isGameStarted() && salle.getCurrentPlayers() < salle.getMaxPlayers()) {
                notifysallePlayers(salle, "😪⌚ En attente de joueurs... (" +   salle.getCurrentPlayers() + "/" + salle.getMaxPlayers() + ")");
                pause(5000);
                if(salle.getGameMode().equals("Collaboratif")) {
                    salle.equipes.clear();
                    salle.JoueursPourEquipe();
                }
            }
            
            if (!salle.isGameStarted() && salle.getCurrentPlayers() >= salle.getMaxPlayers()) {
                
                notifysallePlayers(salle, " 😊 A vos marques pret? La partie va commencer !!!");
                startGameForsalle(salle);
            }
        }).start();
    }
    
    private static void notifysallePlayers(SalleJeu salle, String message) {
        for (PlayerInfo player : players.values()) {
            if (player.getCurrentsalleId() == salle.getsalleId()) {
                ClientHandler client = getClientForPlayer(player);
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        }
    }
    

    private static void startGameForsalle(SalleJeu salle) {
        salle.setGameStarted(true);
        salle.initializeGame();
        notifysallePlayers(salle, "😊 La partie commence dans la salle : " + salle.getsalleName() + " !");
        
        int i = 0;
        for (PlayerInfo player : salle.players) {
            if (player.getCurrentsalleId() == salle.getsalleId()) {
                
                ClientHandler client = getClientForPlayer(player);
                if (client != null) {
                    player.isPlaying = true;
                    player.running = true;
                    player.lives = LIVES_MAX;
                    player.cpt = 0;
                    resetFrog(player, i, HEIGHT - 1);
                    i += 2;
    
                    Thread gameThread = new Thread(() -> {
                        while (player.isPlaying && player.running && salle.isGameStarted()) {
                            renderForClient(client, player);
                            
                            client.requestMove();
                            pause(10);
                        }
                    });
                    gameThread.start();
                }
            }
        }
        if (salle.getGameMode().equals("Competition")) {
                    
            salle.startTimer(90, () -> { 
                notifysallePlayers(salle, "⏰ Temps écoulé ! La partie est terminée !");
                endGameForCompetitiveMode(salle);
                if(!PartieStarted){
                    timer.stopTimer();
                }

            });
        }
      
        if (salle.getGameMode().equals("Collaboratif")) {
                            chooseMechantPlayerForsalle(salle);
                        }
    
      
    }
    private static void endGameForCompetitiveMode(SalleJeu salle) {
        salle.setGameStarted(false);
    
        PlayerInfo winner = null;
        int maxScore = -1;
        for (PlayerInfo player : salle.players) {
            if (player.cpt > maxScore) {
                maxScore = player.cpt;
                winner = player;
            }
        }
    
        if (winner != null) {
            notifysallePlayers(salle, "🏆 Le joueur " + winner.id + " de l'équipe \"" + winner.getEquipe().getNomEquipe() + "\" a gagné avec " + maxScore + " points !");
        } else {
            notifysallePlayers(salle, "😐 Aucun gagnant. La partie est terminée.");
        }
        salle.stopTimer();
    }
    
    private static void chooseMechantPlayerForsalle(SalleJeu salle) {
        List<PlayerInfo> sallePlayers = new ArrayList<>();
        
        for (PlayerInfo player : players.values()) {
            if (player.getCurrentsalleId() == salle.getsalleId()) {
                sallePlayers.add(player);
            }
        }
        
        if (!sallePlayers.isEmpty()) {
            Random random = new Random();
            PlayerInfo mechant = sallePlayers.get(random.nextInt(sallePlayers.size()));
            
            mechant.isCarnivore = true;
            ClientHandler clientMechant = getClientForPlayer(mechant);
            
            if (clientMechant != null) {
                clientMechant.sendMessage("😈 Vous avez été choisi comme le joueur méchant !");
            }
            
            notifysallePlayers(salle, "Un joueur méchant a été désigné ! Faites attention !");
        }
    }

    
    public static void main(String[] args) {
        
            initGame();
            demarServeur();
            
           
    }

    private static void MangerMiam(PlayerInfo p1, PlayerInfo p2)
    {
        ClientHandler cP1 = getClientForPlayer(p1);
        ClientHandler cP2 = getClientForPlayer(p2);

        if(p1.getCarni()==true && p2.getCarni()==false){
            if ((p1.frogX==p2.frogX) && (p1.frogY==p2.frogY))
            {
                p2.lives--;
                
                cP2.sendMessage("Vous vous êtes fait manger");

                if (p2.lives <= 0) {
                    if (cP2 != null) {
                        GameOver(cP2);
                        cP2.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                        p2.isPlaying = false;
                        return;
                    }
                }
                resetFrog(p2 , 0 , HEIGHT - 1);
                

            }
        }
        if(p2.getCarni()==true && p1.getCarni()==false){
            if ((p2.frogX==p1.frogX) && (p2.frogY==p1.frogY))
            {
                p1.lives--;
                
                cP1.sendMessage("Vous vous êtes fait manger");
                if (p1.lives <= 0) {
                    if (cP1 != null) {
                        GameOver(cP1);
                        cP1.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                        p1.isPlaying = false;
                        return;
                    }
                }
                resetFrog(p1, 0 , HEIGHT - 1);

            }
        }
    }
    
    private static void initGame() {
        
        
        obstaclesA = new Obstacle[5];
        obstaclesB = new Obstacle[5];
        for (int i = 0; i < obstaclesA.length; i++) {
            obstaclesA[i] = new Obstacle(i * 4, HEIGHT / 2 - 2);
            obstaclesB[i] = new Obstacle(i * 4, HEIGHT / 2 + 2);
            obstaclesA[i].start();
            obstaclesB[i].start();

        }
    }


    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
           
        }
    }
    
   
}