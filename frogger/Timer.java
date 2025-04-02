import frogger.*;

class Timer extends Thread {
    private int compteur = 0;

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                synchronized(this) {
                    compteur++;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized int getCompteur() {
        return compteur;
    }

    public synchronized void resetCompteur() {
        compteur = 0;
    }
}