package CatanAnalysis.testing;

import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Road;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;

// Standalone harness that runs several test map scenarios end to end (win conditions ->
// hex analysis -> town optimal location -> road placement) and prints a structured,
// easy-to-parse summary for each one, so a Python script can turn it into visuals.
// This file only USES the existing public classes - it doesn't change any of them.
public class RoadTestScenarios {

    private static final String[] WIN_COND_NAMES = {
        "4 Cities + LR",
        "2 Cities + 1 Settlement + LR & LA (Extra Brick & Wood)",
        "3 cities + 2 settlements + LR",
        "2 Cities + 3 Settlements + LR",
        "1 City + 3 Settlements + LR & LA"
    };

    // Brick | Ore | Sheep | Wheat | Wood | Desert
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

        // each scenario is a different (but valid) reshuffle of the standard 19-tile
        // Catan board: 3 brick, 3 ore, 4 sheep, 4 wheat, 4 wood, 1 desert,
        // with the standard dice-number distribution (2,3,3,4,4,5,5,6,6,8,8,9,9,10,10,11,11,12).
        // The desert is always kept at index 9 (the single center hex of the 3-4-5-4-3 layout);
        // only the other 18 tiles/numbers are shuffled around it, so every map looks centered.
        int[][][] scenarios = {
            {
                { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 }, // Scenario 1 - the default board from Main.java
                { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 }
            },
            {
                { 3,1,4,4,2,0,4,4,1,5,2,3,3,3,2,0,2,0,1 }, // Scenario 2 - random reshuffle (seed 1)
                { 4,11,3,5,10,9,4,12,11,0,8,9,2,5,3,8,6,6,10 }
            },
            {
                { 4,0,2,4,2,2,3,1,3,5,1,4,2,0,0,4,3,1,3 }, // Scenario 3 - random reshuffle (seed 7)
                { 4,12,9,5,6,9,6,5,2,0,10,8,4,11,8,11,3,10,3 }
            },
            {
                { 3,3,1,2,1,2,4,2,3,5,4,0,3,0,4,4,2,0,1 }, // Scenario 4 - random reshuffle (seed 42)
                { 9,10,8,6,11,3,9,5,11,0,3,2,4,8,6,4,10,5,12 }
            }
        };

        for (int i = 0; i < scenarios.length; i++) {
            runScenario(i + 1, scenarios[i][0], scenarios[i][1]);
        }
    }

    private static void runScenario(int scenarioNumber, int[] mapResources, int[] mapDiceNumbers) {

        WinConditions winConditions = new WinConditions(WIN_CONDITIONS_RESOURCES.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, WIN_CONDITIONS_RESOURCES.length);
        catanMap.HexagonAnalyse();

        // rank 0 = the best-scoring strategy for THIS map (mapGroupData/townOptimalLocationValues
        // are sorted by rank, not by original strategy index - see CatanAnalysisEngine's note)
        int topWinStrategyRank = 0;
        int originalStrategyIndex = (int) catanMap.winConditionPerTileScores[topWinStrategyRank][19];

        Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES, topWinStrategyRank);
        catanTowns.connectHexToTowns();
        catanTowns.connectTownsToHexagons();
        catanTowns.setTownHexagonCounts();
        catanTowns.rateTownLocation();

        int bestTownIndex = catanTowns.getBestTownIndex();

        ArrayList<Integer> enemyOccupiedNodes = new ArrayList<>(); // no opponents placed in these test scenarios

        Road road = new Road(catanTowns.mapTilesData[topWinStrategyRank], catanTowns.townOptimalLocationValues, catanTowns);
        int roadDirection = road.calculateOptimalRoad(bestTownIndex, enemyOccupiedNodes);

        // ---- structured, parseable output ----
        System.out.println("=== SCENARIO " + scenarioNumber + " ===");
        System.out.println("STRATEGY: " + WIN_COND_NAMES[originalStrategyIndex]);

        System.out.print("RESOURCES:");
        for (int r : mapResources) System.out.print(" " + r);
        System.out.println();

        System.out.print("DICE:");
        for (int d : mapDiceNumbers) System.out.print(" " + d);
        System.out.println();

        System.out.println("BEST_TOWN: " + bestTownIndex);
        System.out.println("ROAD: " + bestTownIndex + " -> " + roadDirection);

        System.out.print("OPT_VALUES:");
        for (double v : catanTowns.townOptimalLocationValues[topWinStrategyRank]) {
            System.out.print(" " + v);
        }
        System.out.println();

        // per-tile heatmap value (0/0.25/0.5/0.75/1.0) used to rank hexes by suitability
        // for the chosen strategy, BEFORE town selection - see ConnectedComponentTile.TILE_VALUE
        System.out.print("TILE_VALUES:");
        double[] tileScores = catanMap.winConditionPerTileScores[topWinStrategyRank];
        for (int i = 0; i < 19; i++) {
            System.out.print(" " + tileScores[i]);
        }
        System.out.println();
        System.out.println();
    }
}
