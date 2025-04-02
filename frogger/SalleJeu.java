import frogger.*;

import java.util.ArrayList;
import java.util.List;

public class SalleJeu {
    private int roomId;
    private String roomName;
    private int maxPlayers;
    private int currentPlayers;
    private boolean gameStarted;
    private String gameMode; // "Collaboratif" or "Competition"
    private List<PlayerInfo> players;
    private Obstacle[] obstaclesA;
    private Obstacle[] obstaclesB;
    private Arrivals arrivals;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;

    public SalleJeu(int roomId, String roomName) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.maxPlayers = 4; // Default max players
        this.currentPlayers = 0;
        this.gameStarted = false;
        this.gameMode = "Competition"; // Default game mode
        this.players = new ArrayList<>();
        this.arrivals = new Arrivals();
    }

    public int getRoomId() {
        return roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public String getGameMode() {
        return gameMode;
    }

    public void setGameMode(String gameMode) {
        this.gameMode = gameMode;
    }

    public void addPlayer(PlayerInfo player) {
        if (!players.contains(player)) {
            players.add(player);
            currentPlayers++;
        }
    }

    public void removePlayer(PlayerInfo player) {
        if (players.contains(player)) {
            players.remove(player);
            currentPlayers--;
        }
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void initializeGame() {
        obstaclesA = new Obstacle[5];
        obstaclesB = new Obstacle[5];
        
        for (int i = 0; i < obstaclesA.length; i++) {
            obstaclesA[i] = new Obstacle(i * 4, HEIGHT / 2 - 2);
            obstaclesB[i] = new Obstacle(i * 4, HEIGHT / 2 + 2);
            obstaclesA[i].start();
            obstaclesB[i].start();
        }
        
        arrivals.ClearwPositions();
    }

    public boolean isObstacleAt(int x, int y) {
        if (obstaclesA == null || obstaclesB == null) {
            return false;
        }
        
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

    public void stopGame() {
        gameStarted = false;
        
        // Stop all obstacles
        if (obstaclesA != null) {
            for (Obstacle obs : obstaclesA) {
                if (obs != null) {
                    obs.stopObstacle();
                }
            }
        }
        
        if (obstaclesB != null) {
            for (Obstacle obs : obstaclesB) {
                if (obs != null) {
                    obs.stopObstacle();
                }
            }
        }
    }

    public Arrivals getArrivals() {
        return arrivals;
    }
}