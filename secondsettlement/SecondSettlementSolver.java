package CatanAnalysis.secondsettlement;

import CatanAnalysis.entities.RoadConnectionGraph;
import CatanAnalysis.utils.Map;
import CatanAnalysis.utils.Road;
import CatanAnalysis.utils.Towns;
import CatanAnalysis.utils.WinConditions;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Orchestrates the full settlement + road pipeline (WinConditions -> Map -> Towns -> Road)
 * for a board that may already have opponent settlements and roads on it, e.g. when picking
 * our SECOND settlement after the other players have taken their first turn.
 *
 * The only two things opponents actually remove from consideration are:
 *   1. Town candidates: their own settlements, AND every town exactly one road away from one
 *      of their settlements (Catan's distance rule - two settlements must be >= 2 roads apart).
 *   2. Road edges: the exact edges they've already built (an edge can only be built on once).
 * A road is otherwise free to pass near/through an opponent's settlement node - only settling
 * there is illegal, not routing a road past it. Passing zero enemy settlements/roads (the
 * normal first-settlement case) makes this behave exactly like the plain, unrestricted algorithm.
 */
public class SecondSettlementSolver {

    // fixed board constants (standard Catan tile distribution) - same table CatanAnalysisEngine uses
    private static final double[][] RESOURCES_AVAILABILITY = {
        { 3, 15.8 }, // brick
        { 3, 15.8 }, // ore
        { 4, 21.1 }, // sheep
        { 4, 21.1 }, // wheat
        { 4, 21.1 }, // wood
        { 1, 5.3 }   // desert
    };

    public static class SolveResult {
        public final int bestTownIndex;      // recommended new settlement location (-1 if none available)
        public final int roadDirection;      // the neighboring town that defines the recommended road (-1 if none)
        public final int[] fullDijkstraPath; // full path the road-finder is aiming towards
        public final String strategyName;    // name of the top-ranked strategy for this map
        public final int strategyIndex;      // its original index in the strategy table
        public final double[] townOptLocationValues; // per-town OptLocation score (54), for heatmap display
        public final double[] tileHeatmapValues;     // per-tile heatmap value (19), for heatmap display

        public SolveResult(int bestTownIndex, int roadDirection, int[] fullDijkstraPath, String strategyName,
                            int strategyIndex, double[] townOptLocationValues, double[] tileHeatmapValues) {
            this.bestTownIndex = bestTownIndex;
            this.roadDirection = roadDirection;
            this.fullDijkstraPath = fullDijkstraPath;
            this.strategyName = strategyName;
            this.strategyIndex = strategyIndex;
            this.townOptLocationValues = townOptLocationValues;
            this.tileHeatmapValues = tileHeatmapValues;
        }
    }

    private final int[] mapResources;
    private final int[] mapDiceNumbers;
    private final double[][] winConditionsResources;
    private final String[] strategyNames;

    public SecondSettlementSolver(int[] mapResources, int[] mapDiceNumbers, StrategyTable strategyTable) {
        this.mapResources = mapResources;
        this.mapDiceNumbers = mapDiceNumbers;
        // WinConditions.importantIndex() mutates the rows it's given (fills in the "total"
        // column) - copy so re-running solve() repeatedly on the same StrategyTable is safe.
        this.winConditionsResources = deepCopy(strategyTable.winConditionsResources);
        this.strategyNames = strategyTable.names;
    }

    private static double[][] deepCopy(double[][] source) {
        double[][] copy = new double[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    /**
     * Runs the full pipeline and returns the recommended next settlement + road.
     * @param ourSettlements    towns we already own - never a candidate for the new settlement.
     * @param enemySettlements  opposing settlements - blocked, and their direct neighbors are
     *                          blocked too (Catan's distance rule); also the Danger/opportunity
     *                          BFS "opponent" source nodes for the road-finding stage.
     * @param enemyRoadEdges    road edges already built by an opponent - removed from the graph
     *                          before running Dijkstra (that exact edge can't be built on again).
     */
    public SolveResult solve(ArrayList<Integer> ourSettlements, ArrayList<Integer> enemySettlements,
                              ArrayList<int[]> enemyRoadEdges) {

        WinConditions winConditions = new WinConditions(winConditionsResources.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, winConditionsResources);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, winConditionsResources.length);
        catanMap.HexagonAnalyse();

        // rank 0 = the best-scoring strategy for THIS map (mapGroupData/townOptimalLocationValues
        // are sorted by rank, not by original strategy index)
        int topWinStrategyRank = 0;
        int originalStrategyIndex = (int) catanMap.winConditionPerTileScores[topWinStrategyRank][19];

        Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, winConditionsResources, topWinStrategyRank);
        catanTowns.connectHexToTowns();
        catanTowns.connectTownsToHexagons();
        catanTowns.setTownHexagonCounts();
        catanTowns.rateTownLocation();

        // --- blocked town candidates: every existing settlement (ours AND enemy) plus their
        // direct neighbors (distance rule: two settlements must be >= 2 roads apart - this
        // applies universally, not just to enemies; our own settlements block their neighbors
        // exactly the same way). ---
        LinkedHashSet<Integer> blockedTowns = new LinkedHashSet<>();
        blockedTowns.addAll(ourSettlements);
        blockedTowns.addAll(enemySettlements);
        int[][] townAdjacency = new RoadConnectionGraph(0, 0).getAdjacencyMatrix();
        LinkedHashSet<Integer> allExistingSettlements = new LinkedHashSet<>();
        allExistingSettlements.addAll(ourSettlements);
        allExistingSettlements.addAll(enemySettlements);
        for (int occupiedTown : allExistingSettlements) {
            for (int neighbor = 0; neighbor < BoardGeometry.TOTAL_TOWNS; neighbor++) {
                if (townAdjacency[occupiedTown][neighbor] == 1) {
                    blockedTowns.add(neighbor);
                }
            }
        }

        int bestTownIndex = catanTowns.getBestTownIndex(new ArrayList<>(blockedTowns));

        int roadDirection = -1;
        int[] fullPath = new int[0];

        if (bestTownIndex != -1) {
            Road road = new Road(catanTowns.mapTilesData[topWinStrategyRank], catanTowns.townOptimalLocationValues, catanTowns);
            road.removeRoadEdges(enemyRoadEdges); // opponent-built edges can't be reused
            roadDirection = road.calculateOptimalRoad(bestTownIndex, enemySettlements);
            fullPath = road.getFullDijkstraPath();
        }

        String strategyName = (originalStrategyIndex >= 0 && originalStrategyIndex < strategyNames.length)
            ? strategyNames[originalStrategyIndex] : "Unknown";

        // winConditionPerTileScores has a 20th column tacked on (the original strategy index) -
        // trim it off so callers just get the 19 per-tile heatmap values.
        double[] tileHeatmapValues = java.util.Arrays.copyOf(
            catanMap.winConditionPerTileScores[topWinStrategyRank], BoardGeometry.TOTAL_HEXES);

        return new SolveResult(
            bestTownIndex,
            roadDirection,
            fullPath,
            strategyName,
            originalStrategyIndex,
            catanTowns.townOptimalLocationValues[topWinStrategyRank],
            tileHeatmapValues
        );
    }
}
