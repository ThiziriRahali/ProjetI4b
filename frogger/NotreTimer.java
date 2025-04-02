class NotreTimer extends Thread {
    private int tempsRestant; 
    private boolean running = true;
    private final Runnable onTimeUp; 
    private final SalleJeu salle; 

    public NotreTimer(int durationInSeconds, Runnable onTimeUp, SalleJeu salle) {
        this.tempsRestant = durationInSeconds;
        this.onTimeUp = onTimeUp;
        this.salle = salle;
    }

    @Override
    public void run() {
        while (running && tempsRestant > 0) {
            try {
                Thread.sleep(1000); 
                tempsRestant--;

                
                notifysallePlayers(salle, "⏳ Temps restant : " + tempsRestant + " secondes.");
                System.out.println("Temps restant pour la salle " + salle.getsalleName() + ": " + tempsRestant + " secondes.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (running && tempsRestant == 0) {
            onTimeUp.run(); // Exécuter l'action lorsque le temps est écoulé
        }
    }

    public void stopTimer() {
        running = false;
        this.interrupt(); // Interrompre le thread si nécessaire
    }

    public int gettempsRestant() {
        return tempsRestant;
    }

    private void notifysallePlayers(SalleJeu salle, String message) {
        for (PlayerInfo player : salle.players) {
            FroggerGamer.ClientHandler client = FroggerGamer.getClientForPlayer(player);
            if (client != null) {
                
                client.sendMessage(message);
            }
        }
    }
}