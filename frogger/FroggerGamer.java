import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FroggerGamer {
    public static final String FINISH_LINE_CHAR = "🏁";
    public static final String WALL_CHAR = "🧱";
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String ROAD_CHAR = ".";
    private static final String WAVE_CHAR = "🌊";
    private static final String TERRE_PLEIN_CHAR = "🌱";
    private static final String FROG_WIN = "🤴";
    private static final String DEPART_CHAR = "⬆️";
    private static volatile int currentPlayers = 0; 
    public static int NbrPlayer =3; 
    private static Map<Integer, SalleJeu> sallesJeu = new ConcurrentHashMap<>();
    private static int nextRoomId = 1;


    private static synchronized SalleJeu createSalleJeu(String roomName) {
        SalleJeu salle = new SalleJeu(nextRoomId++, roomName);
        sallesJeu.put(salle.getRoomId(), salle);
        return salle;
    }

    private static SalleJeu getSalleJeuById(int roomId) {
        return sallesJeu.get(roomId);
    }



    private static Obstacle[] obstaclesA;
    private static Obstacle[] obstaclesB;
    private static final int LIVES_MAX = 3;
    public static int nbVieActuel;
    public static Arrivals A = new Arrivals();
    private static final int DIFFICULTE = 500; 
    private static volatile boolean PartieStarted;
    
    // multijoueur
    private static Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<Socket, PlayerInfo> players = new ConcurrentHashMap<>();
    private static Map<Socket, Equipe> equipes = new ConcurrentHashMap<>();
    private static int nextPlayerId = 1;
    

   
    
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
                        Welcome(this);
                    } else if (message.equals("QUIT")) {
                        break;
                    } else if (player.isPlaying) {
              
                            // try {
                            //     int choice = Integer.parseInt(message);
                            //     processMenuChoice(this, choice);
                            // } catch (NumberFormatException e) {
                            //     switch(message){

                            //         case "y": afficherMenuPrincipalClient(this); break;
                            //         case "n": break;

                            //     }
                                
                                updatePlayer(player, message);
                                checkCollisionForPlayer(player);
                                    
                                
                            // }
                        
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
                NbrPlayer = 1;
                
                startWaitingThread();
                break;
            case 2:
                
                try {
                    
                    collaboCompet(client);

                    
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
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
    
    private static void startWaitingThread() {
        new Thread(() -> {
            while (!PartieStarted) {
                int remainingPlayers = NbrPlayer - players.size();
                if (remainingPlayers > 0) {
                    sendAllMessage(" \n Veuillez patienter, il reste " + remainingPlayers + " joueur(s) avant de lancer la partie.");
                } else {
                    sendAllMessage("Tous les joueurs sont connectés. La partie commence maintenant !");
                    startGameForAllClients();
                    break;
                }
                pause(5000);
            }
        }).start();
    }
    private static void collaboCompet(ClientHandler client) {
        try {
            client.sendMessage("Choisissez un mode de jeu :");
            client.sendMessage("1. ✊😈 Mode Collaboratif 😈✊");
            client.sendMessage("2. 🥇 Mode Competition 🥇");
            client.requestInput("Entrez votre choix (1 ou 2) : ");
    
            String message = client.input.readLine();
            
            switch (message) {
                case "1":
                    client.requestInput("Quel sera le nom de votre équipe ?: ");
                    try {
                        String nomEquipe = client.input.readLine();
                        client.player.getEquipe().setNomEquipe(nomEquipe);
                    } catch (IOException e) {
                        e.printStackTrace();
                        client.sendMessage("Erreur lors de la lecture du nom de l'équipe.");
                    }
                    client.requestInput("Insérez le nombre de joueurs pour le mode collaboratif : ");
                    try {
                        NbrPlayer = Integer.parseInt(client.input.readLine());
                        
                        startWaitingThread();
                        choisirJoueurMechant(); 
                    } catch (NumberFormatException e) {
                        client.sendMessage("Nombre de joueurs invalide. Utilisation de la valeur par défaut.");
                        NbrPlayer = 3; 
                    }
                    
                    break;
                
                case "2":
                   
                    client.requestInput("Insérez le nombre de joueurs pour le mode compétition : ");
                    try {
                        NbrPlayer = Integer.parseInt(client.input.readLine());
                       
                        
                        startWaitingThread();
                    } catch (NumberFormatException e) {
                        client.sendMessage("Nombre de joueurs invalide. Utilisation de la valeur par défaut.");
                        NbrPlayer = 3; 
                    }
                    break;
                
                default:
                    client.sendMessage("Choix invalide. Retour au menu principal.");
                    afficherMenuPrincipalClient(client);
            }
        } catch (IOException e) {
            client.sendMessage("Erreur lors de la lecture de l'entrée : " + e.getMessage());
            e.printStackTrace();
        }}

    private static void afficherMenuPrincipalClient(ClientHandler client) {
        client.sendMessage("\n=== Menu Principal ===");
        client.sendMessage("1. Mode Solo");
        client.sendMessage("2. Mode Multijoueur");
        client.sendMessage("3. Paramètres");
        client.sendMessage("4. Quitter");
        client.requestInput("Veuillez choisir une option : ");
        try {

            int choix =Integer.parseInt(client.input.readLine());
            processMenuChoice(client, choix);
            
        } catch (Exception e) {
        }
        
    }
    
    private static void parametrerJeuClient(ClientHandler client) {
        client.sendMessage("\n=== Paramètres du Jeu ===");
        client.sendMessage("1. Modifier le nombre de vies (Actuel : " + LIVES_MAX + ")");
        client.sendMessage("4. Retour au menu principal");
        client.requestInput("Votre choix : ");
    }
    
    private static void startGameForClient(ClientHandler client) {
        
        PartieStarted=true;
        client.player.running=true;
        PlayerInfo player = players.get(client.socket);
        player.cpt=0;
        if (player != null) {
            player.isPlaying = true;
            player.lives = LIVES_MAX;
  
            player.actuEmoji(client.player);
            
            
            
            Thread gameThread = new Thread(() -> {
                while (player.isPlaying && player.running) {
                    renderForClient(client, player);
                    client.requestMove();
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
                } else if (getPlayerAt(x, y) != null) {
                    sb.append(getPlayerAt(x, y).getEmojiNiveau());
                }else if (y == HEIGHT / 2) {
                    sb.append(TERRE_PLEIN_CHAR);
                }  else if (isObstacleAt(x, y)) {
                    boolean isObstacleA = false;
                    for (Obstacle obs : obstaclesA) {
                        if (obs.getX() == x && obs.getY() == y) {
                            sb.append(obs.getCharA());  
                            isObstacleA = true;
                            break;
                        }
                    }
                    
                    if (!isObstacleA) {
                        for (Obstacle obs : obstaclesB) {
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
    
    private static boolean isPlayerAt(int x, int y, PlayerInfo currentPlayer) {
        for (PlayerInfo player : players.values()) {
            if (player != currentPlayer && player.frogX == x && player.frogY == y) {
                return true;
            }
        }
        return false;
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
       
        for (PlayerInfo otherPlayer : players.values()) {
            if (player != otherPlayer) { 
                MangerMiam(player, otherPlayer);
            }
        }
    
        if (isObstacleAt(player.frogX, player.frogY)) {
            player.lives--;
    
            client.sendMessage("\033[H\033[2J");
            System.out.flush();
            
            if (client != null) {
                client.sendMessage("💀 Un obstacle vous a écrasé ! Il vous reste " + player.lives + " vies. 💀");
            }
            
            if (player.lives <= 0) {
                if (client != null) {
                    client.sendMessage(GameOver());
                    client.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                    player.isPlaying = false;
                    askreplay(client);
                    return;
                }
            } else {
                resetFrog(player, 0 ,HEIGHT - 1);
            }
        }
        
    }
    

    private static void askreplay(ClientHandler client) {
        new Thread(() -> {
            client.requestInput("Voulez-vous rejouer ? (y/n) : ");
            try {
                String choix = client.input.readLine();
                if(choix != null){
                    switch (choix) {
                        case "y":
                            PlayerInfo player = players.get(client.socket);
                            if (player != null) {
                                player.running = false;
                                player.isPlaying = false;
                                player.lives = LIVES_MAX;
                                player.cpt = 0;
                                resetFrog(player, 0 ,HEIGHT - 1);
                            }
        
                            boolean allPlayersInactive = true;
                            for (PlayerInfo p : players.values()) {
                                if (p.isPlaying && p.running) {
                                    allPlayersInactive = false;
                                    break;
                                }
                            }
        
                            if (allPlayersInactive) {
                                PartieStarted = false;
                                Arrivals.ClearwPositions();
                            }
        
                            afficherMenuPrincipalClient(client);
                            break;
        
                        case "n":
                            client.sendMessage("Au revoir !");
                            try {
                                removeClient(client.socket);
                                client.socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
        
                        default:
                            askreplay(client); 
                    }

                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
            Arrivals.addWPosition(player.frogX, player.frogY);
            ClientHandler client = getClientForPlayer(player);
            if (client != null) {
                player.cpt++;
                
                sendAllMessage("🎉 Félicitations ! Un prince est apparu !");
                resetFrog(player, 0 ,HEIGHT - 1);
            }
            
            
            if (Arrivals.GlobalWin()) {
                System.out.println("DEBUG: Victoire détectée !");
                sendAllMessage("\033[H\033[2J");
                System.out.flush();
                player.isPlaying = false;
                
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
                
                if (W!= null) {
                    sendAllMessage("🎖️ L'équipe \""+ W.getEquipe().getNomEquipe() +"\" remporte la partie avec " + W.cpt + " arrivées ! 🎖️");
                }
                player.niveau+=player.cpt;
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
        

        private static void startGameForAllClients() {
            
            PartieStarted = true;
            Arrivals.ClearwPositions();
            
            for (PlayerInfo player : players.values()) {
                player.isPlaying = false;
                player.running = false;
                player.lives = LIVES_MAX;
                player.cpt = 0;
            }
            
            sendAllMessage("La partie commence ! Bonne chance à tous !");
            int i = 0;
            
            for (ClientHandler client : clients.values()) {
                client.player.frogX = i;
                client.player.frogY = HEIGHT - 1;
                startGameForClient(client);
                i += 2;
            }
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
            client.sendMessage("Êtes-vous prêt à jouer ? (o/n) : ");
            try {
                String message = client.input.readLine();
                switch (message) {
                    case "o":
                        offerRoomOptions(client);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static void offerRoomOptions(ClientHandler client) {
            client.sendMessage("\n=== Gestion des Salles ===");
            client.sendMessage("1. Créer une nouvelle salle de jeu");
            client.sendMessage("2. Rejoindre une salle existante");
            client.sendMessage("3. Retour au menu principal");
            client.requestInput("Veuillez choisir une option : ");
            
            try {
                String choice = client.input.readLine();
                switch (choice) {
                    case "1":
                        createNewRoom(client);
                        break;
                    case "2":
                        joinExistingRoom(client);
                        break;
                    case "3":
                        afficherMenuPrincipalClient(client);
                        break;
                    default:
                        client.sendMessage("Choix invalide. Veuillez réessayer.");
                        offerRoomOptions(client);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        
        
    }
    private static void createNewRoom(ClientHandler client) {
        client.requestInput("Entrez un nom pour votre salle de jeu : ");
        try {
            String roomName = client.input.readLine();
            SalleJeu newRoom = createSalleJeu(roomName);
            
            // Associate player with this room
            PlayerInfo player = players.get(client.socket);
            if (player != null) {
                player.setCurrentRoomId(newRoom.getRoomId());
                
                client.sendMessage("Salle créée avec succès ! ID de la salle : " + newRoom.getRoomId());
                client.sendMessage("Vous êtes maintenant dans la salle : " + roomName);
                
                // Setup room waiting
                roomSetupOptions(client, newRoom);
            }
        } catch (IOException e) {
            e.printStackTrace();
            client.sendMessage("Erreur lors de la création de la salle.");
            offerRoomOptions(client);
        }
    }
    
    private static void joinExistingRoom(ClientHandler client) {
        // List available rooms
        if (sallesJeu.isEmpty()) {
            client.sendMessage("Aucune salle disponible. Veuillez créer une nouvelle salle.");
            offerRoomOptions(client);
            return;
        }
        
        client.sendMessage("\n=== Salles Disponibles ===");
        for (SalleJeu room : sallesJeu.values()) {
            if (!room.isGameStarted() && room.getCurrentPlayers() < room.getMaxPlayers()) {
                client.sendMessage(room.getRoomId() + ". " + room.getRoomName() + 
                        " (" + room.getCurrentPlayers() + "/" + room.getMaxPlayers() + " joueurs)");
            }
        }
        
        client.requestInput("Entrez l'ID de la salle que vous souhaitez rejoindre (ou 0 pour revenir) : ");
        try {
            int roomId = Integer.parseInt(client.input.readLine());
            if (roomId == 0) {
                offerRoomOptions(client);
                return;
            }
            
            SalleJeu room = getSalleJeuById(roomId);
            if (room == null) {
                client.sendMessage("ID de salle invalide. Veuillez réessayer.");
                joinExistingRoom(client);
                return;
            }
            
            if (room.isGameStarted()) {
                client.sendMessage("Cette partie a déjà commencé. Veuillez choisir une autre salle.");
                joinExistingRoom(client);
                return;
            }
            
            if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
                client.sendMessage("Cette salle est pleine. Veuillez choisir une autre salle.");
                joinExistingRoom(client);
                return;
            }
            
            // Add player to the roo
            PlayerInfo player = players.get(client.socket);
            if (player != null) {
                player.setCurrentRoomId(room.getRoomId());
                room.addPlayer(player);
                
                client.sendMessage("Vous avez rejoint la salle : " + room.getRoomName());
                
                notifyRoomPlayers(room, "Joueur " + player.id + " a rejoint la salle !");
                
                if (room.getCurrentPlayers() >= room.getMaxPlayers()) {
                    notifyRoomPlayers(room, "La salle est pleine. La partie va commencer !");
                    startGameForRoom(room);
                } else {
                    client.sendMessage("En attente d'autres joueurs... (" + 
                            room.getCurrentPlayers() + "/" + room.getMaxPlayers() + ")");
                }
            }
        } catch (NumberFormatException e) {
            client.sendMessage("ID de salle invalide. Veuillez entrer un nombre.");
            joinExistingRoom(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void roomSetupOptions(ClientHandler client, SalleJeu room) {
        client.sendMessage("\n=== Configuration de la Salle ===");
        client.sendMessage("1. Définir le nombre maximum de joueurs");
        client.sendMessage("2. Choisir le mode de jeu (Collaboratif/Compétition)");
        client.sendMessage("3. Démarrer la partie");
        client.sendMessage("4. Retour au menu des salles");
        client.requestInput("Veuillez choisir une option : ");
        
        try {
            String choice = client.input.readLine();
            switch (choice) {
                case "1":
                    client.requestInput("Entrez le nombre maximum de joueurs (2-10) : ");
                    try {
                        int maxPlayers = Integer.parseInt(client.input.readLine());
                        if (maxPlayers >= 2 && maxPlayers <= 10) {
                            room.setMaxPlayers(maxPlayers);
                            client.sendMessage("Nombre maximum de joueurs défini à " + maxPlayers);
                        } else {
                            client.sendMessage("Nombre invalide. Veuillez entrer un nombre entre 2 et 10.");
                        }
                    } catch (NumberFormatException e) {
                        client.sendMessage("Entrée invalide. Veuillez entrer un nombre.");
                    }
                    roomSetupOptions(client, room);
                    break;
                    
                case "2":
                    client.sendMessage("Choisissez un mode de jeu :");
                    client.sendMessage("1. Mode Collaboratif");
                    client.sendMessage("2. Mode Competition");
                    client.requestInput("Entrez votre choix (1 ou 2) : ");
                    
                    try {
                        int modeChoice = Integer.parseInt(client.input.readLine());
                        room.setGameMode(modeChoice == 1 ? "Collaboratif" : "Competition");
                        client.sendMessage("Mode de jeu défini à : " + room.getGameMode());
                    } catch (NumberFormatException e) {
                        client.sendMessage("Choix invalide. Mode par défaut (Competition) utilisé.");
                        room.setGameMode("Competition");
                    }
                    roomSetupOptions(client, room);
                    break;
                    
                case "3":
                    if (room.getCurrentPlayers() < 2) {
                        client.sendMessage("Il faut au moins 2 joueurs pour démarrer la partie.");
                        client.sendMessage("En attente d'autres joueurs... (" + 
                                room.getCurrentPlayers() + "/" + room.getMaxPlayers() + ")");
                                
                        startRoomWaitingThread(room);
                    } else {
                        startGameForRoom(room);
                    }
                    break;
                    
                case "4":
                    offerRoomOptions(client);
                    break;
                    
                default:
                    client.sendMessage("Choix invalide. Veuillez réessayer.");
                    roomSetupOptions(client, room);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void startRoomWaitingThread(SalleJeu room) {
        new Thread(() -> {
            while (!room.isGameStarted() && room.getCurrentPlayers() < room.getMaxPlayers()) {
                notifyRoomPlayers(room, "En attente de joueurs... (" + 
                        room.getCurrentPlayers() + "/" + room.getMaxPlayers() + ")");
                pause(5000);
            }
            
            if (!room.isGameStarted() && room.getCurrentPlayers() >= room.getMaxPlayers()) {
                notifyRoomPlayers(room, "Nombre de joueurs atteint ! La partie va commencer.");
                startGameForRoom(room);
            }
        }).start();
    }
    
    private static void notifyRoomPlayers(SalleJeu room, String message) {
        for (PlayerInfo player : players.values()) {
            if (player.getCurrentRoomId() == room.getRoomId()) {
                ClientHandler client = getClientForPlayer(player);
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        }
    }
    
    private static void startGameForRoom(SalleJeu room) {
        room.setGameStarted(true);
        
        room.initializeGame();
        
        notifyRoomPlayers(room, "La partie commence dans la salle : " + room.getRoomName() + " !");
        int i = 0;
        for (PlayerInfo player : players.values()) {
            if (player.getCurrentRoomId() == room.getRoomId()) {
                ClientHandler client = getClientForPlayer(player);
                if (client != null) {
                    player.isPlaying = true;
                    player.running = true;
                    player.lives = LIVES_MAX;
                    player.cpt = 0;
                    resetFrog(player, i , HEIGHT - 1);
                    i += 2;
                    
                    Thread gameThread = new Thread(() -> {
                        while (player.isPlaying && player.running && room.isGameStarted()) {
                            renderForClient(client, player);
                            client.requestMove();
                            pause(10);
                        }
                    });
                    gameThread.start();
                }
            }
        }
        
        if (room.getGameMode().equals("Collaboratif")) {
            chooseEvilPlayerForRoom(room);
        }
    }
    
    private static void chooseEvilPlayerForRoom(SalleJeu room) {
        List<PlayerInfo> roomPlayers = new ArrayList<>();
        
        for (PlayerInfo player : players.values()) {
            if (player.getCurrentRoomId() == room.getRoomId()) {
                roomPlayers.add(player);
            }
        }
        
        if (!roomPlayers.isEmpty()) {
            Random random = new Random();
            PlayerInfo mechant = roomPlayers.get(random.nextInt(roomPlayers.size()));
            
            mechant.isCarnivore = true;
            ClientHandler clientMechant = getClientForPlayer(mechant);
            
            if (clientMechant != null) {
                clientMechant.sendMessage("😈 Vous avez été choisi comme le joueur méchant !");
            }
            
            notifyRoomPlayers(room, "Un joueur méchant a été désigné ! Faites attention !");
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
                        cP2.sendMessage(GameOver());
                        cP2.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                        p2.isPlaying = false;
                        askreplay(cP2);
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
                        cP1.sendMessage(GameOver());
                        cP1.sendMessage("Game Over ! Vous avez perdu toutes vos vies.");
                        p1.isPlaying = false;
                        askreplay(cP1);
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
    private static void choisirJoueurMechant() {
        System.out.print("je suis mechant");
        while (players.size() < NbrPlayer) {
            sendAllMessage("En attente de joueurs pour choisir un méchant...");
            pause(5000);

        }
    
        List<PlayerInfo> joueurs = new ArrayList<>(players.values());
        Random random = new Random();
        PlayerInfo mechant = joueurs.get(random.nextInt(joueurs.size()));
    
        mechant.isCarnivore = true; 
        ClientHandler clientMechant = getClientForPlayer(mechant);
    
        if (clientMechant != null) {
            clientMechant.sendMessage("😈 Vous avez été choisi comme le joueur méchant !");
        }
        
        sendAllMessage("Un joueur méchant a été désigné ! Faites attention !");
    }
    
    private static boolean isObstacleAt(int x, int y) {
        for (Obstacle obs : obstaclesA) {
            if (obs.getX() == x && obs.getY() == y) {
                return true;
            }
        }
        for (Obstacle obs : obstaclesB) {
            if (obs.getX() == x && obs.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    // private static void stopAllObstacles() {
    //     if (obstacles != null) {
    //         for (Obstacle obs : obstacles) {
    //             if (obs != null) {
    //                 obs.stopObstacle();
    //             }
    //         }
    //     }
    // }


    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
           
        }
    }
    
   
}