import java.util.ArrayList;

class Equipe {
    private int idEquipe;
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


    public void removeJoueur(PlayerInfo joueur) {
        this.joueurs.remove(joueur);
    }
    public int getIdEquipe() {
        return idEquipe;
    }


    public void setNomEquipe(String nomEquipe) {
        this.nomEquipe = nomEquipe;
    }
    public String getNomEquipe() {
        return nomEquipe;
    }


    public int getScore() {
        return score;
    }
    public synchronized void setScore(int score) {
        this.score = score;
    }


    public ArrayList<PlayerInfo> getJoueurs() {
        return joueurs;
    }
    public int getNbJoueurs(){
        return joueurs.size();
    }
}
