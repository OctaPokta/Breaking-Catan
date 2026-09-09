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

    /** Everything computed from the board+strategy alone, before any settlement/road choice - shared by every solve method below so the strategy stays locked to whatever ranked best at the very start of the game (see class-level note in OurRegularPlayStrategy). */
    private static class TownRatings {
        final Towns catanTowns;
        final Map catanMap;
        final int topWinStrategyRank;
        final int originalStrategyIndex;
        final String strategyName;

        TownRatings(Towns catanTowns, Map catanMap, int topWinStrategyRank, int originalStrategyIndex, String strategyName) {
            this.catanTowns = catanTowns;
            this.catanMap = catanMap;
            this.topWinStrategyRank = topWinStrategyRank;
            this.originalStrategyIndex = originalStrategyIndex;
            this.strategyName = strategyName;
        }
    }

    private TownRatings computeTownRatings() {
        // ONE shared fresh copy, deliberately reused for both calls below (not two separate
        // copies) - winConditionsCalculation() mutates it in place (via importantIndex()) into
        // weighted-need form, and Towns' constructor is meant to receive exactly that already-
        // weighted array, not the original raw resource counts - this matches the original
        // solve()/RoadTestScenarios/CatanAnalysisEngine data flow exactly. Starting from a
        // fresh copy of the untouched this.winConditionsResources each call is what makes this
        // method safe to call repeatedly (mid-game), unlike the original single-shot solve().
        double[][] freshWinConditionsResources = deepCopy(winConditionsResources);

        WinConditions winConditions = new WinConditions(freshWinConditionsResources.length);
        winConditions.winConditionsCalculation(RESOURCES_AVAILABILITY, freshWinConditionsResources);

        Map catanMap = new Map(mapResources, mapDiceNumbers, winConditions.relativeTokenScoresResults, freshWinConditionsResources.length);
        catanMap.HexagonAnalyse();

        // rank 0 = the best-scoring strategy for THIS map (mapGroupData/townOptimalLocationValues
        // are sorted by rank, not by original strategy index). Since this depends only on the
        // fixed board (hex resources/dice), calling this again later in the game with the same
        // board always returns the same strategy - the "locked at the start" requirement holds
        // automatically, no extra bookkeeping needed.
        int topWinStrategyRank = 0;
        int originalStrategyIndex = (int) catanMap.winConditionPerTileScores[topWinStrategyRank][19];

        Towns catanTowns = new Towns(catanMap, RESOURCES_AVAILABILITY, freshWinConditionsResources, topWinStrategyRank, strategyNames);
        catanTowns.connectHexToTowns();
        catanTowns.connectTownsToHexagons();
        catanTowns.setTownHexagonCounts();
        catanTowns.rateTownLocation();

        String strategyName = (originalStrategyIndex >= 0 && originalStrategyIndex < strategyNames.length)
            ? strategyNames[originalStrategyIndex] : "Unknown";

        return new TownRatings(catanTowns, catanMap, topWinStrategyRank, originalStrategyIndex, strategyName);
    }

    private static double[] trimTileHeatmap(Map catanMap, int rank) {
        // winConditionPerTileScores has a 20th column tacked on (the original strategy index) -
        // trim it off so callers just get the 19 per-tile heatmap values.
        return java.util.Arrays.copyOf(catanMap.winConditionPerTileScores[rank], BoardGeometry.TOTAL_HEXES);
    }

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

        TownRatings ratings = computeTownRatings();
        Towns catanTowns = ratings.catanTowns;
        Map catanMap = ratings.catanMap;
        int topWinStrategyRank = ratings.topWinStrategyRank;

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

        return new SolveResult(
            bestTownIndex,
            roadDirection,
            fullPath,
            ratings.strategyName,
            ratings.originalStrategyIndex,
            catanTowns.townOptimalLocationValues[topWinStrategyRank],
            trimTileHeatmap(catanMap, topWinStrategyRank)
        );
    }

    /**
     * Chooses the best settlement to build NEXT, mid-game - not among "every unblocked town"
     * (that free-for-all is only valid during the two initial placements), but strictly among
     * the nodes JSettlers itself already knows are legal right now (adjacent to one of our
     * built roads, and respecting the distance rule) - see the {@code SOCPlayer.getPotentialSettlements()}
     * note in the article: "להתייחס ל-OptLocation שוב... לבחור את היישובים עם הדירוג הכי גבוה
     * שנמצאים במקומות החוקיים". Uses the exact same locked strategy as every other call on this
     * board (see {@link #computeTownRatings()}).
     * @param legalCandidateTowns  our town indices that are legally buildable right now
     * @return the highest-OptLocation-ranked one of those, or -1 if the list is empty
     */
    public SolveResult chooseNextSettlement(ArrayList<Integer> legalCandidateTowns) {
        TownRatings ratings = computeTownRatings();
        double[] optLocationRow = ratings.catanTowns.townOptimalLocationValues[ratings.topWinStrategyRank];

        int bestTown = -1;
        double bestValue = -1;
        for (int candidate : legalCandidateTowns) {
            if (optLocationRow[candidate] > bestValue) {
                bestValue = optLocationRow[candidate];
                bestTown = candidate;
            }
        }

        return new SolveResult(
            bestTown,
            -1,             // road choice is a separate call (chooseNextRoad) - not tied 1:1 to a new settlement
            new int[0],
            ratings.strategyName,
            ratings.originalStrategyIndex,
            optLocationRow,
            trimTileHeatmap(ratings.catanMap, ratings.topWinStrategyRank)
        );
    }

    public static class RoadResult {
        public final int originTown;    // one of ourTownIndices - the road to build is (originTown, direction)
        public final int direction;     // the neighboring town that defines the recommended road (-1 if none)
        public final int[] fullDijkstraPath;

        public RoadResult(int originTown, int direction, int[] fullDijkstraPath) {
            this.originTown = originTown;
            this.direction = direction;
            this.fullDijkstraPath = fullDijkstraPath;
        }
    }

    /**
     * Chooses the best road to build NEXT, mid-game, multi-source across every town we
     * currently own at once (see {@link Road#calculateOptimalRoad(ArrayList, ArrayList)}) -
     * not tied to a specific new settlement, just "which single edge best extends our network
     * toward the strategy's best remaining opportunities right now".
     * @param ourTownIndices    every town we currently own (settlements and cities)
     * @param enemySettlements  opposing settlements - the Danger/opportunity BFS "opponent"
     *                          source nodes, same role as in {@link #solve}
     * @param enemyRoadEdges    edges already built by an opponent - can't be reused
     * @param ourOwnRoadEdges   edges we've already built ourselves - also can't be built again
     */
    public RoadResult chooseNextRoad(ArrayList<Integer> ourTownIndices, ArrayList<Integer> enemySettlements,
                                      ArrayList<int[]> enemyRoadEdges, ArrayList<int[]> ourOwnRoadEdges) {
        TownRatings ratings = computeTownRatings();
        Towns catanTowns = ratings.catanTowns;

        Road road = new Road(catanTowns.mapTilesData[ratings.topWinStrategyRank], catanTowns.townOptimalLocationValues, catanTowns);
        road.removeRoadEdges(enemyRoadEdges);
        road.removeRoadEdges(ourOwnRoadEdges);
        int direction = road.calculateOptimalRoad(ourTownIndices, enemySettlements);
        int origin = road.getOptimalRoadOrigin();

        return new RoadResult(origin, direction, road.getFullDijkstraPath());
    }
}
