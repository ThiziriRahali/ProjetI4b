import java.util.*;

class Equipe {
    private final int idEquipe;
    private String nomEquipe;
    private int score;
    private ArrayList<PlayerInfo> joueurs;

    public Equipe(int idEquipe, String nomEquipe, PlayerInfo joueur) {
        this.idEquipe = idEquipe;
        this.nomEquipe = nomEquipe;
        this.setScore(0);
        this.joueurs = new ArrayList<>();
        this.joueurs.add(joueur);
    }

    
    public void addJoueur(PlayerInfo joueur) {
        this.joueurs.add(joueur);
    }

    public String getNomEquipe() {
        return nomEquipe;
    }

    public synchronized void setScore(int score) {
        this.score = score;
    }

}
