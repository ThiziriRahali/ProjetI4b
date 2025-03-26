 class PlayerInfo {
    public static final int WIDTH = 10;
    private static final int HEIGHT = 10;
    private static final String FROG_TET = "🦎";
    private static final String FROG_CHAR = "🐸";
    private static final String FROG_DRAG = "🐲";
    private static final int LIVES_MAX = 3;
    private static final String FROG_PRINCESS = "👸";
    private static final String FROG_MEAN = "😈";

    public boolean running= true;
    int id;
    int frogX;
    int frogY;
    int lives;
    String frogChar;
    boolean isPlaying;
    int cpt ;
    int niveau;
    String rang;
    boolean isCarnivore;
    
    public PlayerInfo(int id) {
        this.id = id;
        initPlayer();
    }

    public PlayerInfo(int id, boolean isCarnivore) {
        this.id = id;
        this.isCarnivore = isCarnivore;
        initPlayer();
    }

    private void initPlayer() {
        this.frogX = WIDTH / 2;
        this.frogY = HEIGHT - 1;
        this.lives = LIVES_MAX;
        this.isPlaying = true;
        this.niveau = 0;
        this.frogChar = FROG_TET + id;
    }

    public void isMechant(){
        if (this.isCarnivore){
            this.frogChar=FROG_MEAN;
        }
    }

    public String getEmojiNiveau() {
        actuEmoji(this);
        return frogChar;
    }

    public void actuEmoji(PlayerInfo player){
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
}