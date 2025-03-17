import java.util.*;


class Obstacle extends Thread {
    private int x;
    private int y;
    private boolean active = true;
    public static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    public static final String OBSTACLE_CHAR = "🚣";
    public static final String FINISH_LINE_CHAR = "🏁";
    
    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void run() {
        Random rand = new Random();
        while (active) {
            try {
                Thread.sleep(rand.nextInt(1000) + 500);
                moveObstacle();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void moveObstacle() {
        x = (x + 1) % WIDTH;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    public String getChar() { return OBSTACLE_CHAR; }
}

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
    private static int lives;
<<<<<<< HEAD
    private static final String MESSAGE = "Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer : ";    
    private static boolean paused = false;
    private static boolean gagne = false;
=======
    public static Arrivals A = new Arrivals();
>>>>>>> 7ce1b5b (les arrivées)
    
    public static void main(String[] args) {
        startGame();
        Arrivals A = new Arrivals();
    }
    
    private static void startGame() {
        gagne = false;
        paused = false;
        FROGACT = FROG_CHAR;
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
        running = true;
        lives = 3;
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
                    Thread.sleep(500);
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
        lives = 3;

        
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
        
        System.out.println("Vies restantes : " + lives);
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
<<<<<<< HEAD
            gagne = true;
            System.out.println("🎉 Félicitations ! Vous avez gagné ! La grenouille est devenue un prince ! Quel incroyable comte de fée !");
=======
            System.out.println("🎉 Félicitations ! Un prince est apparu à cette place !");
            continuePlay();
>>>>>>> 7ce1b5b (les arrivées)
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
            lives--;
            paused = true; // Bloquer le jeu temporairement
            clearScreen();
            System.out.println("💀 Un obstacle vous a écrasé ! Il vous reste " + lives + " vies. 💀");
            pause(1000); // Cooldown avant réaffichage
            paused = false; // Réactiver le jeu après la pause
            if (lives <= 0) {
                System.out.println("Game Over !");
                askReplay();
                return;
            }
            resetFrog();
        }
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
            resetGame();
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
        System.out.println("\033[0m\033c"); 
        System.exit(0); 
    }
    
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
    
}