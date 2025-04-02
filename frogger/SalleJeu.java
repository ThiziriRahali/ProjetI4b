import java.util.ArrayList;
import java.util.List;

public class SalleJeu {
    private final int salleId;
    private final String salleName;
    private int maxPlayers;

    private boolean gameStarted;
    private String gameMode;
    public final List<PlayerInfo> players;
    private Obstacle[] obstaclesA;
    private Obstacle[] obstaclesB;
    private Arrivals arrivals;
    private static final int HEIGHT = 10;
    private int currentPlayers;
    public List<Equipe> equipes;

    public SalleJeu(int salleId, String salleName) {
        this.salleId = salleId;
        this.salleName = salleName;
        this.maxPlayers = 4; 
        this.currentPlayers = 0;
        this.gameStarted = false;
        this.gameMode = "Competition"; 
        this.players = new ArrayList<>();
        this.arrivals = new Arrivals();
        this.equipes = new ArrayList<>();
    }

    public int getsalleId() {
        return salleId;
    }

    public String getsalleName() {
        return salleName;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Obstacle[] getObstaclesA() {
        return obstaclesA;
    }
    
    public Obstacle[] getObstaclesB() {
        return obstaclesB;
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

    void addPlayer(PlayerInfo player) {
        if (!players.contains(player)) {
            players.add(player);
            currentPlayers++;
        }
    }

    void removePlayer(PlayerInfo player) {
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

  public static boolean isObstacleAt(int x, int y, SalleJeu salle) {
    if (salle == null || salle.getObstaclesA() == null || salle.getObstaclesB() == null) {
        return false;
    }
    
    for (Obstacle obs : salle.getObstaclesA()) {
        if (obs.getX() == x && obs.getY() == y) {
            return true;
        }
    }
    for (Obstacle obs : salle.getObstaclesB()) {
        if (obs.getX() == x && obs.getY() == y) {
            return true;
        }
    }
    return false;
}

    public void stopGame() {
        gameStarted = false;
      
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

    public void JoueursPourEquipe() {
        if (equipes.isEmpty()) {
            Equipe equipe1 = new Equipe(1, "Les Crap'Ôs", null);
            Equipe equipe2 = new Equipe(2, "Les Croâssants", null);
            equipes.add(equipe1);
            equipes.add(equipe2);
        }
    
        Equipe equipe1 = equipes.get(0);
        Equipe equipe2 = equipes.get(1);
    
        boolean pasToi = true; 
        for (PlayerInfo player : players) {
            if (pasToi) {
                equipe1.addJoueur(player);
                player.setEquipe(equipe1);
            } else {
                equipe2.addJoueur(player);
                player.setEquipe(equipe2);
            }
            pasToi = !pasToi; 
        }
    
        System.out.println("Répartition terminée :");
        System.out.println("Équipe 1 : " + equipe1.getNomEquipe() + " (" + equipe1.getNbJoueurs() + " joueurs)");
        System.out.println("Équipe 2 : " + equipe2.getNomEquipe() + " (" + equipe2.getNbJoueurs() + " joueurs)");
    }

}
