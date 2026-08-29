package CatanAnalysis.testing;

import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Road;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;
import java.util.Arrays;

// A single, deliberately harder scenario: a map where the immediate neighborhood of the
// best town is mediocre, PLUS a couple of enemy-occupied towns placed near the strongest
// nearby alternatives - forcing Road.java's real Danger(v)-weighted Dijkstra to actually
// route around them instead of landing on an immediate neighbor. Not a new mechanic - just
// exercising findSafeSpace()/Danger(v) exactly as already implemented, with real opponent
// data (which nothing else currently provides).
public class ComplexScenarioWithEnemies {

    private static final String[] WIN_COND_NAMES = {
        "4 Cities + LR",
        "2 Cities + 1 Settlement + LR & LA (Extra Brick & Wood)",
        "3 cities + 2 settlements + LR",
        "2 Cities + 3 Settlements + LR",
        "1 City + 3 Settlements + LR & LA"
    };

    private static final double[][] RESOURCES_AVAILABILITY = {
        { 3, 15.8 }, { 3, 15.8 }, { 4, 21.1 }, { 4, 21.1 }, { 4, 21.1 }, { 1, 5.3 }
    };

    private static final double[][] WIN_CONDITIONS_RESOURCES = {
        { 11, 12, 4, 12, 11, 0 },
        { 9, 11, 8, 12, 9, 0 },
        { 14, 9, 5, 11, 14, 0 },
        { 13, 11, 10, 14, 13, 0 },
        { 14, 8, 10, 12, 14, 0 }
    };

    public static void main(String[] args) {

        // reuse the Map 4 board (desert-centered reshuffle, seed 42)
        int[] mapResources = { 3,3,1,2,1,2,4,2,3,5,4,0,3,0,4,4,2,0,1 };
        int[] mapDiceNumbers = { 9,10,8,6,11,3,9,5,11,0,3,2,4,8,6,4,10,5,12 };

        WinConditions winConditions = new WinConditions(WIN_CONDITIONS_RESOURCES.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, WIN_CONDITIONS_RESOURCES.length);
        catanMap.HexagonAnalyse();
        int topWinStrategyRank = 0;
        int originalStrategyIndex = (int) catanMap.winConditionPerTileScores[topWinStrategyRank][19];

        Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES, topWinStrategyRank);
        catanTowns.connectHexToTowns();
        catanTowns.connectTownsToHexagons();
        catanTowns.setTownHexagonCounts();
        catanTowns.rateTownLocation();

        int bestTownIndex = catanTowns.getBestTownIndex();

        // enemy settlements placed on/near the strongest nearby alternatives to the
        // immediate neighbor that would otherwise be picked (see visualize_dijkstra_path.py's
        // scenario 4 result: without enemies, the path is just one hop to town 39)
        // 3 enemy settlements = 3 opponents, each having already placed their FIRST
        // settlement (this models picking our SECOND settlement/road, after seeing
        // where the other players already settled - not the very first placement of
        // the game, when no one has settled anywhere yet).
        // IMPORTANT: Catan's distance rule requires every settlement to be at least 2
        // roads from every other settlement - so none of these can be a direct (1-hop)
        // neighbor of our own town 34. All three below are exactly 2 hops from us (the
        // legal minimum) and at least 2 hops from each other.
        // legal (>=2 hops from our town 34, and >=2 hops from each other) enemy placements:
        // the 3 objectively strongest remaining spots on the board after our own town
        ArrayList<Integer> enemyOccupiedNodes = new ArrayList<>(Arrays.asList(40, 13, 41));

        Road road = new Road(catanTowns.mapTilesData[topWinStrategyRank], catanTowns.townOptimalLocationValues, catanTowns);
        int roadDirection = road.calculateOptimalRoad(bestTownIndex, enemyOccupiedNodes);
        int[] fullPath = road.getFullDijkstraPath();

        System.out.println("STRATEGY: " + WIN_COND_NAMES[originalStrategyIndex]);
        System.out.print("RESOURCES:");
        for (int r : mapResources) System.out.print(" " + r);
        System.out.println();
        System.out.print("DICE:");
        for (int d : mapDiceNumbers) System.out.print(" " + d);
        System.out.println();
        System.out.println("BEST_TOWN: " + bestTownIndex);
        System.out.println("ROAD: " + bestTownIndex + " -> " + roadDirection);
        System.out.print("ENEMY_TOWNS:");
        for (int e : enemyOccupiedNodes) System.out.print(" " + e);
        System.out.println();
        System.out.print("FULL_PATH:");
        for (int t : fullPath) System.out.print(" " + t);
        System.out.println();
        if (fullPath.length > 0) {
            System.out.println("PATH_TARGET_COST: " + road.getDijkstraDistanceTo(fullPath[fullPath.length - 1]));
        }
        System.out.print("OPT_VALUES:");
        for (double v : catanTowns.townOptimalLocationValues[topWinStrategyRank]) {
            System.out.print(" " + v);
        }
        System.out.println();
        System.out.print("TILE_VALUES:");
        double[] tileScores = catanMap.winConditionPerTileScores[topWinStrategyRank];
        for (int i = 0; i < 19; i++) {
            System.out.print(" " + tileScores[i]);
        }
        System.out.println();
    }
}
