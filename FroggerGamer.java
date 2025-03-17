import java.util.Random;

// Ceci est un test

class Obstacle extends Thread {
    private int x;
    private int y;
    private boolean isTest = False;
    private boolean active = true;
    private static final int WIDTH = 10;
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
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String FROG_CHAR = "🐸";
    private static final String FROGDEAD_CHAR = "⚰️";
    private static String FROGACT = FROG_CHAR;
    private static final String ROAD_CHAR = ".";
    private static final String TERRE_PLEIN_CHAR = "🌱";
    private static final String FROG_WIN = "🤴";
    private static int frogX;
    private static int frogY;
    private static boolean running;
    private static Obstacle[] obstacles;
    private static int lives;
    
    public static void main(String[] args) {
        startGame();
    }
    
    private static void startGame() {
        FROGACT = FROG_CHAR;
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
        running = true;
        lives = 3;
        obstacles = new Obstacle[5];
        for (int i = 0; i < obstacles.length; i++) {
            obstacles[i] = new Obstacle(i * 4, HEIGHT / 2 - 2); // Éviter le terre-plein
            obstacles[i].start();
        }
        
        while (running) {
            render();
            System.out.print("Déplacez la grenouille (z/q/s/d) ou appuyez sur 'x' pour arrêter de jouer: ");
            char move = Lire.c();  
            update(move);
        }
        System.out.println("Merci d'avoir joué !");

    }
    
    private static void render() {
        clearScreen();
        
        System.out.println("Vies restantes : " + lives);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                if(frogY == 0 && frogY == y && frogX%5==0 && frogX == x){
                    System.out.print(FROG_WIN);
                } else if (y == 0 && x%5==0) {
                    System.out.print(FINISH_LINE_CHAR);
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
    
    private static void update(char move) {
        switch (move) {
            case 'z': if (frogY > 0) frogY--; break;
            case 's': if (frogY < HEIGHT - 1) frogY++; break;
            case 'q': if (frogX > 0) frogX--; break;
            case 'd': if (frogX < WIDTH - 1) frogX++; break;
            case 'x':  exitGame(); break;
        }
        if (frogY == 0 && frogX%5==0) {
            render(); 
            System.out.println("🎉 Félicitations ! Vous avez gagné ! La grenouille est devenue un prince ! Quel incroyable comte de fée !");
            pause(1000);
            askReplay();
            return;
        }
        
        
        if (isObstacleAt(frogX, frogY)) {
            FROGACT = FROGDEAD_CHAR;
            render();
            lives--;
            System.out.println("Vous avez touché un obstacle ! Il vous reste " + lives + " vies.");
            if (lives <= 0) {
                System.out.println("Game Over !");
                askReplay();
                return;
            }
            FROGACT = FROG_CHAR;
            pause(1000);
            resetFrog();
        }
        
    }
    
    private static void resetFrog() {
        frogX = WIDTH / 2;
        frogY = HEIGHT - 1;
    }
    
    private static void askReplay() {
        System.out.print("Voulez-vous rejouer ? (y/n) : ");
        char choice = Lire.c();
        if (choice == 'y') {
            startGame();
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
        System.out.println("\033[0m\033c"); // Réinitialiser l'affichage ANSI
        System.exit(0);  // Quitter le programme
    }
    
    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}