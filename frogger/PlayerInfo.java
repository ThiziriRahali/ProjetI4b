
class PlayerInfo {
    private static final String FROG_TET = "🦎";
    private static final String FROG_CHAR = "🐸";
    private static final String FROG_DRAG = "🐲";
    private static final String FROG_DEAD = "⚰️";
    private static final int LIVES_MAX = FroggerGamer.LIVES_MAX;
    private static final String FROG_PRINCESS = "👸";
    private static final String FROG_MEAN = "😈";
    private int currentsalleId;

    public boolean running= true;
    int id;
    int frogX;
    int frogY;
    int lives;
    String frogChar;
    boolean isPlaying;
    int cpt ;
    int niveau;
    boolean isCarnivore;
    private Equipe equipe;
    
    public PlayerInfo(int id, Equipe equipe) {
        this.equipe = equipe;
        this.id = id;
        initPlayer();
    }

    public PlayerInfo(int id, Equipe equipe,boolean isCarnivore) {
        this.equipe = equipe;
        this.id = id;
        this.isCarnivore = isCarnivore;
        this.currentsalleId = -1;
        initPlayer();
    }

    private void initPlayer() {
        this.frogX = FroggerGamer.WIDTH / 2;
        this.frogY = FroggerGamer.HEIGHT - 1;
        this.lives = LIVES_MAX;
        this.isPlaying = true;
        this.niveau = 0;
        this.frogChar = FROG_TET + id;
    }

    public boolean getCarni(){
        return this.isCarnivore;
    }

    public void isMechant(){
        if (this.isCarnivore){
            this.frogChar=FROG_MEAN;
        }
    }
     
    public int getCurrentsalleId() {
        return currentsalleId;
    }
    
    public void setCurrentsalleId(int salleId) {
        this.currentsalleId = salleId;
    }

    public String getEmojiNiveau() {
        if(this.lives > 0) {
            actuEmoji(this);
            return frogChar;
        }
        else if (this.lives == 0 && this.isCarnivore){
            return FROG_MEAN+id;
        }
        else if (this.lives == 0 && !this.isCarnivore){
            return FROG_DEAD+id;
        }
        else if (this.lives < 0){
            return FROG_DEAD+id;
        }
        return "";
    }

    public void actuEmoji(PlayerInfo player){
        if( player.isCarnivore){
            player.frogChar=FROG_MEAN;
        }
        if (player.niveau > 0 && player.niveau<=2){
            player.frogChar = FROG_TET + id;
        }
        else  if (player.niveau > 2 && player.niveau <= 5){
            player.frogChar = FROG_CHAR+id; 
        }
        else  if (player.niveau > 5 && player.niveau <= 8){
            player.frogChar = FROG_DRAG+id; 
        }
        else  if (player.niveau > 8){
            player.frogChar = FROG_PRINCESS+id; 
        }
    }
    public void setEquipe(Equipe equipe) {
        this.equipe = equipe;
    }
    public Equipe getEquipe() {
        return equipe;
    }
}