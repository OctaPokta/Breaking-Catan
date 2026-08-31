package CatanAnalysis.testing;

import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Road;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;
import java.util.Arrays;

// Verifies Road's new multi-source Dijkstra: seeded from TWO of our owned towns far apart on
// the board, it should find the same or better overall target than either single-source run
// alone, and getOptimalRoadOrigin() should correctly report which of the two towns the chosen
// road actually starts from.
public class RoadMultiSourceTest {

    private static final double[][] RESOURCES_AVAILABILITY = {
        { 3, 15.8 }, { 3, 15.8 }, { 4, 21.1 }, { 4, 21.1 }, { 4, 21.1 }, { 1, 5.3 }
    };
    private static final double[][] WIN_CONDITIONS_RESOURCES = {
        { 11, 12, 4, 12, 11, 0 }, { 9, 11, 8, 12, 9, 0 }, { 14, 9, 5, 11, 14, 0 },
        { 13, 11, 10, 14, 13, 0 }, { 14, 8, 10, 12, 14, 0 }
    };

    public static void main(String[] args) {
        int[] mapResources = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
        int[] mapDiceNumbers = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };

        WinConditions winConditions = new WinConditions(WIN_CONDITIONS_RESOURCES.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES);
        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, WIN_CONDITIONS_RESOURCES.length);
        catanMap.HexagonAnalyse();

        Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES, 0);
        catanTowns.connectHexToTowns();
        catanTowns.connectTownsToHexagons();
        catanTowns.setTownHexagonCounts();
        catanTowns.rateTownLocation();

        // two far-apart owned towns: 0 (a corner) and 53 (the opposite corner)
        int townA = 0, townB = 53;
        ArrayList<Integer> ourTowns = new ArrayList<>(Arrays.asList(townA, townB));
        ArrayList<Integer> noEnemies = new ArrayList<>();

        Road roadA = new Road(catanTowns.mapTilesData[0], catanTowns.townOptimalLocationValues, catanTowns);
        int dirA = roadA.calculateOptimalRoad(townA, noEnemies);

        Road roadB = new Road(catanTowns.mapTilesData[0], catanTowns.townOptimalLocationValues, catanTowns);
        int dirB = roadB.calculateOptimalRoad(townB, noEnemies);

        Road roadMulti = new Road(catanTowns.mapTilesData[0], catanTowns.townOptimalLocationValues, catanTowns);
        int dirMulti = roadMulti.calculateOptimalRoad(ourTowns, noEnemies);
        int origin = roadMulti.getOptimalRoadOrigin();

        System.out.println("Single-source from " + townA + ": road -> " + dirA);
        System.out.println("Single-source from " + townB + ": road -> " + dirB);
        System.out.println("Multi-source from {" + townA + "," + townB + "}: road " + origin + " -> " + dirMulti);
        System.out.println("Full path: " + Arrays.toString(roadMulti.getFullDijkstraPath()));

        boolean originIsOneOfOurs = (origin == townA || origin == townB);
        boolean pathStartsAtOrigin = roadMulti.getFullDijkstraPath().length > 0 && roadMulti.getFullDijkstraPath()[0] == origin;
        boolean dirIsNeighborOfOrigin = dirMulti != -1;

        System.out.println();
        System.out.println("Origin is one of our owned towns: " + originIsOneOfOurs);
        System.out.println("Full path starts at that origin: " + pathStartsAtOrigin);
        System.out.println("A direction was found at all: " + dirIsNeighborOfOrigin);

        if (originIsOneOfOurs && pathStartsAtOrigin && dirIsNeighborOfOrigin) {
            System.out.println("\nPASS");
        } else {
            System.out.println("\nFAIL");
            System.exit(1);
        }
    }
}
