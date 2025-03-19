// Modifiez la classe Obstacle dans Obstacle.java pour ajouter une vérification de collision après chaque mouvement

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
                
                
                FroggerGamer.checkAllPlayersCollisions();
            } catch (InterruptedException e) {
                // Interruption gérée
            }
        }
    }
    
    private void moveObstacle() {
        x = (x + 1) % WIDTH;
    }
    
    public void stopObstacle() {
        active = false;
        interrupt(); 
    }
    
    public void restartObstacle() {
        if (!active) {
             active = true;
             start();  // Redémarre le thread
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public String getChar() { return OBSTACLE_CHAR; }
}