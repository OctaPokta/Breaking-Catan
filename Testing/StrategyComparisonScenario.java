package CatanAnalysis.testing;

import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Road;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;

// Runs ALL 5 strategies against the SAME map (rather than letting the engine auto-pick just
// the top one), so the results can be compared side by side. This matches how
// Map.winConditionPerTileScores actually works: after WinConditionTileScores() sorts it,
// row r (0..4) is the r-th BEST strategy for THIS map, and column [19] on that row holds the
// ORIGINAL strategy index (into WIN_COND_NAMES/winConditionsResources) it came from - the
// table is deliberately sorted from best to worst per the current map, not listed in
// original-strategy order.
public class StrategyComparisonScenario {

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

        // the default board from Main.java - ONE fixed map used for every strategy below
        int[] mapResources = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
        int[] mapDiceNumbers = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };

        WinConditions winConditions = new WinConditions(WIN_CONDITIONS_RESOURCES.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, WIN_CONDITIONS_RESOURCES.length);
        catanMap.HexagonAnalyse(); // sorts winConditionPerTileScores best -> worst for THIS map

        System.out.print("RESOURCES:");
        for (int r : mapResources) System.out.print(" " + r);
        System.out.println();

        System.out.print("DICE:");
        for (int d : mapDiceNumbers) System.out.print(" " + d);
        System.out.println();
        System.out.println();

        // rank 0 = best strategy for this map, rank 4 = worst
        for (int rank = 0; rank < WIN_CONDITIONS_RESOURCES.length; rank++) {

            int originalIndex = (int) catanMap.winConditionPerTileScores[rank][19];

            // topWinStrategy is passed as `rank` on purpose: townOptimalLocationValues and
            // mapTilesData/mapGroupData are RANK-indexed (0 = best), not original-index-indexed,
            // so this is what makes getBestTownIndex()/Road read the correct row for this rank.
            Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES, rank);
            catanTowns.connectHexToTowns();
            catanTowns.connectTownsToHexagons();
            catanTowns.setTownHexagonCounts();
            catanTowns.rateTownLocation();

            int bestTownIndex = catanTowns.getBestTownIndex();

            ArrayList<Integer> enemyOccupiedNodes = new ArrayList<>();
            Road road = new Road(catanTowns.mapTilesData[rank], catanTowns.townOptimalLocationValues, catanTowns);
            int roadDirection = road.calculateOptimalRoad(bestTownIndex, enemyOccupiedNodes);

            System.out.println("=== RANK " + rank + " (original strategy index " + originalIndex + ") ===");
            System.out.println("STRATEGY: " + WIN_COND_NAMES[originalIndex]);
            System.out.println("BEST_TOWN: " + bestTownIndex);
            System.out.println("ROAD: " + bestTownIndex + " -> " + roadDirection);

            System.out.print("OPT_VALUES:");
            for (double v : catanTowns.townOptimalLocationValues[rank]) {
                System.out.print(" " + v);
            }
            System.out.println();

            System.out.print("TILE_VALUES:");
            double[] tileScores = catanMap.winConditionPerTileScores[rank];
            for (int i = 0; i < 19; i++) {
                System.out.print(" " + tileScores[i]);
            }
            System.out.println();
            System.out.println();
        }
    }
}
