
import java.util.*;
public class Arrivals {
    private static int totalArrivals = 4; // Nombre total de places à remplir
    private static ArrayList<int[]> wPositions = new ArrayList<>();

    public static void addWPosition(int x, int y) {
        // Vérifier si la position n'est pas déjà enregistrée
        if (!isWPosition(x, y)) {
            wPositions.add(new int[] { x, y });
        }
    }

    public static boolean GlobalWin() {
        return wPositions.size() >= totalArrivals;
    }

    public static int getNbArrives(){
        return totalArrivals;
    } 

    public static void setTotalArrivals(int total) {
        totalArrivals = total;
    }

    public static boolean isWPosition(int x, int y) {
        for (int[] pos : wPositions) {
            if (pos[0] == x && pos[1] == y) {
                return true;
            }
        }
        return false;
    }
    public static void ClearwPositions(){
        wPositions.clear();
    }
}

