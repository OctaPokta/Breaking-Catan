package CatanAnalysis.testing;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;

// For the article's "Finding the Second Settlement" section: shows, for the SAME map,
// how the tile heatmap splits into connected components (clusters) under 3 different
// strategies (the 3 top-ranked ones for this map) - illustrating that whichever
// strategy is chosen, some cluster structure (one big cluster, or several smaller ones)
// always exists, leaving room for a second settlement even if part of the map gets
// blocked by opponents.
public class SecondSettlementClustersScenario {

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

        // the default board from Main.java
        int[] mapResources = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
        int[] mapDiceNumbers = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };

        WinConditions winConditions = new WinConditions(WIN_CONDITIONS_RESOURCES.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, WIN_CONDITIONS_RESOURCES.length);
        catanMap.HexagonAnalyse();

        System.out.print("RESOURCES:");
        for (int r : mapResources) System.out.print(" " + r);
        System.out.println();
        System.out.print("DICE:");
        for (int d : mapDiceNumbers) System.out.print(" " + d);
        System.out.println();
        System.out.println();

        // the 3 top-ranked strategies for this map
        for (int rank = 0; rank < 3; rank++) {

            int originalIndex = (int) catanMap.winConditionPerTileScores[rank][19];

            Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, WIN_CONDITIONS_RESOURCES, rank);
            catanTowns.connectHexToTowns();
            catanTowns.connectTownsToHexagons();
            catanTowns.setTownHexagonCounts();
            catanTowns.rateTownLocation();

            System.out.println("=== STRATEGY RANK " + rank + " (original index " + originalIndex + ") ===");
            System.out.println("STRATEGY: " + WIN_COND_NAMES[originalIndex]);

            System.out.print("TILE_VALUES:");
            double[] tileScores = catanMap.winConditionPerTileScores[rank];
            for (int i = 0; i < 19; i++) System.out.print(" " + tileScores[i]);
            System.out.println();

            // assign each tile a component id (index into mapGroupData[rank]'s list of
            // connected components), so tiles sharing a component id belong to the same
            // cluster and can be drawn with a matching outline color
            int[] componentId = new int[19];
            java.util.Arrays.fill(componentId, -1);
            ArrayList<ArrayList<ConnectedComponentTile>> components = catanMap.mapGroupData[rank];
            for (int c = 0; c < components.size(); c++) {
                for (ConnectedComponentTile tile : components.get(c)) {
                    componentId[tile.getTileIndex()] = c;
                }
            }
            System.out.print("COMPONENT_IDS:");
            for (int i = 0; i < 19; i++) System.out.print(" " + componentId[i]);
            System.out.println();

            System.out.print("OPT_VALUES:");
            for (double v : catanTowns.townOptimalLocationValues[rank]) System.out.print(" " + v);
            System.out.println();

            System.out.println("BEST_TOWN: " + catanTowns.getBestTownIndex());
            System.out.println();
        }
    }
}
