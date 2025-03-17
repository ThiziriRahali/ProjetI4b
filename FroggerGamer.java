import java.util.*;

public class FroggerGamer {
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
    private static boolean running;
    private static Obstacle[] obstacles;
    private static int LIVES_MAX = 3;
    public static int nbVieActuel;
    private static final String MESSAGE = "Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ";    
    private static boolean paused =false;
    private static boolean gagne = false;
    public static Arrivals A = new Arrivals();
    private static final int DIFFICULTE = 500; 

    public static void main(String[] args) {
        choix();
    }

    public static void choix() {
        boolean quitter = false;

        while (!quitter) {
            afficherMenuPrincipal();
            int choix = Lire.i();

            switch (choix) {
                case 1:
                    modeSolo();
                    break;
                case 2:
                    //modeMultijoueur();
                    break;
                case 3:
                    parametrerJeu();
                    break;
                case 4:
                    quitter = true;
                    break;
                default:
                    System.out.println("Choix invalide. Veuillez réessayer.");
            }
        }

        System.out.println("Merci d'avoir joué !");
        System.exit(0);
    }

    private static void modeSolo() {
        System.out.println("\n--- Mode Solo ---");
        // Logique du mode solo
        System.out.println("Lancement du mode solo avec " + LIVES_MAX + " vies");
        miseValeur();
    }

    private static void afficherMenuPrincipal() {
        System.out.println("\n=== Menu Principal ===");
        System.out.println("1. Mode Solo");
        System.out.println("2. FUTUR Mode Multijoueur");
        System.out.println("3. Paramètres");
        System.out.println("4. Quitter");
        System.out.print("Veuillez choisir une option : ");
    }

    private static void parametrerJeu() {
        boolean retourMenu = false;
    
        while (!retourMenu) {
            System.out.println("\n=== Paramètres du Jeu ===");
            System.out.println("1. Modifier le nombre de vies (Actuel : " + LIVES_MAX + ")");
            /*System.out.println("2. Modifier le nombre d'obstacles (Actuel : " + obstacles.length + ")");
            System.out.println("3. Modifier le nombre d'arrivées (Actuel : " + A.getNbArrives() + ")");*/
            System.out.println("4. Retour au menu principal");
            System.out.print("Votre choix : ");
    
            int choix = Lire.i();
    
            switch (choix) {
                case 1:
                    System.out.print("Entrez le nouveau nombre de vies : ");
                    LIVES_MAX = Lire.i();
                    break;
                /*case 2 :, " +
                obstacles.length + " obstacles, et " + A.getNbArrives() + " arrivée(s)."
                    System.out.print("Entrez le nouveau nombre d'obstacles : ");
                    nombreDObstacles = lireEntier(scanner, nombreDObstacles);
                    break;
                case 3:
                    System.out.print("Entrez le nouveau nombre d'arrivées : ");
                    nombreDArrivees = lireEntier(scanner, nombreDArrivees);
                    break;*/
                case 4:
                    retourMenu = true;
                    break;
                default:
                    System.out.println("❌ Choix invalide. Veuillez réessayer.");
            }
        }
    }

    public static void miseValeur(){
        nbVieActuel = LIVES_MAX;
        startGame();
    }
    
    private static void startGame() {
        gagne = false;
        paused = false;
        FROGACT = FROG_CHAR;
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
        running = true;
        obstacles = new Obstacle[5];
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle(i * 4, HEIGHT / 2 - 2); 
            obstacles[i].start();
        }
        
        Thread renderThread = new Thread(() -> {
            while (running) {
                if (!paused && !gagne) {
                    render();
                    checkCollision(); // Vérifier si un obstacle passe sur la grenouille
                    System.out.print(MESSAGE);
                }
                try {
                    Thread.sleep(DIFFICULTE);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        renderThread.start();

        while (running) {
            if (!paused) {
                char move = Lire.c();
                update(move);
            }
        }
        System.out.println("Merci d'avoir joué !");

    }
    private static void resetGame() {
        FROGACT = FROG_CHAR;
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
        running = true;
        nbVieActuel = LIVES_MAX;

        
        Arrivals.setTotalArrivals(2); 
        Arrivals.ClearwPositions();

        obstacles = new Obstacle[5];
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle(i * 4, HEIGHT / 2 - 2); 
            obstacles[i].start();  
        }
    }
    
    private static void render() {
        clearScreen();
        
        System.out.println("Vies restantes : " + nbVieActuel);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if (Arrivals.isWPosition(x, y)) {
                    System.out.print(FROG_WIN);
                } else if (y == 0) {
                    if(x%5 == 0){
                        System.out.print(FINISH_LINE_CHAR);
                    }else{
                        System.out.print(WALL_CHAR);
                    }
                } else if (x == frogX && y == frogY) {
                    System.out.print(FROGACT);
                } else if (y == HEIGHT / 2) {
                    System.out.print(TERRE_PLEIN_CHAR);
                } else if (isObstacleAt(x, y)) {
                    System.out.print(Obstacle.OBSTACLE_CHAR);
                } else {
                    System.out.print(ROAD_CHAR);
                }
                System.out.print("\t");
            }
            System.out.println();
        }}
    
    private static boolean isObstacleAt(int x, int y) {
        for (Obstacle obs : obstacles) {
            if (obs.getX() == x && obs.getY() == y) {
                return true;
            }
        }
        return false;
    }
    
    private static void update(char move) {
        switch (move) {
            case 'z': if (frogY > 1 ^ (frogY == 1 && frogX%5==0)) frogY--; break;
            case 's': if (frogY < HEIGHT - 1) frogY++; break;
            case 'q': if (frogX > 0) frogX--; break;
            case 'd': if (frogX < WIDTH - 1) frogX++; break;
            case 'x':  exitGame(); break;
        }
        if (frogY == 0 && frogX % 5 == 0) {
            Arrivals.addWPosition(frogX, frogY); 
            render(); 
            gagne = true;
            System.out.println("🎉 Félicitations ! Un prince est apparu à cette place !");
            continuePlay();
            pause(1000);
        
            if (Arrivals.GlobalWin()) {
                System.out.println("🏆 TOUS les emplacements sont remplis ! VOUS AVEZ GAGNÉ ! 🏆");
                pause(1000);
                askReplay(); // Demander si on veut rejouer
                return;
            }
        }
        checkCollision();
    }

    private static synchronized void checkCollision() {
        if (isObstacleAt(frogX, frogY)) {
            nbVieActuel--;
            paused = true;
            clearScreen();
            System.out.println("💀 Un obstacle vous a écrasé ! Il vous reste " + nbVieActuel + " vies. 💀");
            pause(1000); 
            paused = false; 
            if (nbVieActuel <= 0) {
                AfficherGameOver();
                askReplay();
                return;
            }
            resetFrog();
        }
    }
    public static void AfficherGameOver(){
        System.out.println("⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣠⡀⠀\n" + //
                        "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣤⣤⠀⠀⠀⢀⣴⣿⡶⠀⣾⣿⣿⡿⠟⠛⠁\n" + //
                        "⠀⠀⠀⠀⠀⠀⣀⣀⣄⣀⠀⠀⠀⠀⣶⣶⣦⠀⠀⠀⠀⣼⣿⣿⡇⠀⣠⣿⣿⣿⠇⣸⣿⣿⣧⣤⠀⠀⠀\n" + //
                        "⠀⠀⢀⣴⣾⣿⡿⠿⠿⠿⠇⠀⠀⣸⣿⣿⣿⡆⠀⠀⢰⣿⣿⣿⣷⣼⣿⣿⣿⡿⢀⣿⣿⡿⠟⠛⠁⠀⠀\n" + //
                        "⠀⣴⣿⡿⠋⠁⠀⠀⠀⠀⠀⠀⢠⣿⣿⣹⣿⣿⣿⣿⣿⣿⡏⢻⣿⣿⢿⣿⣿⠃⣼⣿⣯⣤⣴⣶⣿⡤⠀\n" + //
                        "⣼⣿⠏⠀⣀⣠⣤⣶⣾⣷⠄⣰⣿⣿⡿⠿⠻⣿⣯⣸⣿⡿⠀⠀⠀⠁⣾⣿⡏⢠⣿⣿⠿⠛⠋⠉⠀⠀⠀\n" + //
                        "⣿⣿⠲⢿⣿⣿⣿⣿⡿⠋⢰⣿⣿⠋⠀⠀⠀⢻⣿⣿⣿⠇⠀⠀⠀⠀⠙⠛⠀⠀⠉⠁⠀⠀⠀⠀⠀⠀⠀\n" + //
                        "⠹⢿⣷⣶⣿⣿⠿⠋⠀⠀⠈⠙⠃⠀⠀⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" + //
                        "⠀⠀⠈⠉⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⣤⣤⣴⣶⣦⣤⡀⠀\n" + //
                        "⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀⠀⠀⠀⠀⣠⡇⢰⣶⣶⣾⡿⠷⣿⣿⣿⡟⠛⣉⣿⣿⣿⠆\n" + //
                        "⠀⠀⠀⠀⠀⠀⢀⣤⣶⣿⣿⡎⣿⣿⣦⠀⠀⠀⢀⣤⣾⠟⢀⣿⣿⡟⣁⠀⠀⣸⣿⣿⣤⣾⣿⡿⠛⠁⠀\n" + //
                        "⠀⠀⠀⠀⣠⣾⣿⡿⠛⠉⢿⣦⠘⣿⣿⡆⠀⢠⣾⣿⠋⠀⣼⣿⣿⣿⠿⠷⢠⣿⣿⣿⠿⢻⣿⣧⠀⠀⠀\n" + //
                        "⠀⠀⠀⣴⣿⣿⠋⠀⠀⠀⢸⣿⣇⢹⣿⣷⣰⣿⣿⠃⠀⢠⣿⣿⢃⣀⣤⣤⣾⣿⡟⠀⠀⠀⢻⣿⣆⠀⠀\n" + //
                        "⠀⠀⠀⣿⣿⡇⠀⠀⢀⣴⣿⣿⡟⠀⣿⣿⣿⣿⠃⠀⠀⣾⣿⣿⡿⠿⠛⢛⣿⡟⠀⠀⠀⠀⠀⠻⠿⠀⠀\n" + //
                        "⠀⠀⠀⠹⣿⣿⣶⣾⣿⣿⣿⠟⠁⠀⠸⢿⣿⠇⠀⠀⠀⠛⠛⠁⠀⠀⠀⠀⠀⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀\n" + //
                        "⠀⠀⠀⠀⠈⠙⠛⠛⠛⠋⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀");
    }
    
    private static void resetFrog() {
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
    }
    private static void continuePlay(){
        if ( Arrivals.GlobalWin() )
        {
            System.out.println("Vous avez gagné, Génial!!");
            askReplay();

        }
        else{
            System.out.println("Remplissez toutes les arrivées!!!");
            startGame();
        }
       

    }
    
    private static void askReplay() {
        System.out.print("Voulez-vous rejouer ? (y/n) : ");
        char choice = Lire.c();
        if (choice == 'y') {
            choix();
        } else {
            exitGame();
        }
    }
    
    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private static void exitGame() {
        running = false;
        clearScreen();
        choix();
    }
    
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
}