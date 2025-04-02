
import java.util.*;

class Obstacle extends Thread {
    private int x;
    private int y;
    private boolean active = true;
    public static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String OBSTACLE_CHARA = "🚣";
    private static final String OBSTACLE_CHARB = "🦅";
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
                e.printStackTrace();
                
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
             start();  
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public static String getCharA() { return OBSTACLE_CHARA; }
    public static String getCharB() { return OBSTACLE_CHARB; }

}