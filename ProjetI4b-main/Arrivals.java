
import java.util.*;
public class Arrivals {
    private static int totalArrivals = 3;
    private static ArrayList<int[]> wPositions = new ArrayList<>();

    public static void addWPosition(int x, int y) {
        if (!isWPosition(x, y)) {
            wPositions.add(new int[] { x, y });
        }
    }

    public static boolean GlobalWin() {
        return wPositions.size() >= totalArrivals;
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

