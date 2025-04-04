
import java.util.*;

class Obstacle extends Thread {

   
    private int x;
    private final int y;
    private boolean active = true;
    private static final String OBSTACLE_CHARA = "🚣";
    private static final String OBSTACLE_CHARB = "🦅";
    
    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
    }
    @Override
    public void run() {
        Random rand = new Random();
        while (active) {
            try {
                Thread.sleep((3-FroggerGamer.DIFFICULTE)*500 + 500);
                moveObstacle();
                 
                
                FroggerGamer.checkAllPlayersCollisions();
            } catch (InterruptedException e) {
                e.printStackTrace();
                
            }
        }
    }
    public static int getWIDTH() {
        return FroggerGamer.WIDTH;
    }
    
    private void moveObstacle() {
        x = (x + 1) % FroggerGamer.WIDTH;
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
    public String getCharA() { return OBSTACLE_CHARA; }
    public String getCharB() { return OBSTACLE_CHARB; }

}