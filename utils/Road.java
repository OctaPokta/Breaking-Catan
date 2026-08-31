// this class is used to performe calculation for the road placement

package CatanAnalysis.utils;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.entities.RoadConnectionGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

public class Road {

    // attributes
    
    // existing data
    private ArrayList<ArrayList<ConnectedComponentTile>> mapTilesData; // list of all the hexagons of the map, each hexagon is represented by a "ConnectedComponentTile" class, and they are grouped by the town they are connected to (54 possible town locations)
    private double[][] townOptimalLocationValues; // 54x5 matrix, each row is a town, each column is the value of placing a road in that town for a specific win condition
    private int[][] roadConnectionGraph = new int[54][54]; // graph representing the connection of each town to another, and the hexagons they are connected to, this is used to calculate the area of control of each town and the best road placement based on that
    private final Towns catanTowns;
    // new data
    private int topWinStrategy; // index of the top win strategy from the map analysis, used to pick the best town based on that strategy
    private double[][] roadWeights = new double[54][54]; // used to run dikstra's algorithm
    private ArrayList<Integer> safeNodes; // list of nodes indices that we are closer to than the opponents, this is the area of control that we currently own
    private double dMax; // max distance of roads we wanna place from a starting town position, based on strategy and map state
    private double maxOptLocation; // max opt town location on the map
    private double mu; // average opt location of the nodes in the safe space
    private int startingTownIndex; // the town index we already placed (0-53) - kept for backward compatibility; equals startingTownIndices.get(0) when there's only one
    private ArrayList<Integer> startingTownIndices; // ALL of our currently-owned town indices (settlements/cities) - Dijkstra runs multi-source from all of them at once, for mid-game road choices where we may own several
    private ArrayList<Integer> enemyOccupiedNodes; // saved here so other methods can reuse it
    private int[] playerDistances; // BFS distance (in roads) from our town to every node
    private int[] opponentDistances; // BFS distance (in roads) from nearest opponent to every node
    private int maxOptLocationIndex = -1; // index of the "MaxOpt" town (best free spot on the map)
    private double dRef; // distance between nearest opponent and the MaxOpt town
    private double lambda; // risk-aversion coefficient (derived via the indifference point)
    private double gamma; // distance decay factor
    private double[] adjustedOptLocation = new double[54]; // AdjustedOpt(v) for every node

    private static final double BASE_COST = 1.0; // BaseCost from the formula
    private static final double EPSILON = 0.1; // ε, keeps Dijkstra weights positive

    // used to reconstruct the shortest path after Dijkstra runs
    private double[] dijkstraDistances;
    private int[] dijkstraPrevious;


    // constructor
    public Road(ArrayList<ArrayList<ConnectedComponentTile>> mapTilesData, double[][] townOptimalLocationValues, Towns catanTowns) {
        this.mapTilesData = new ArrayList<>(mapTilesData);
        this.townOptimalLocationValues = townOptimalLocationValues;
        this.roadConnectionGraph = new RoadConnectionGraph(0, 0).getAdjacencyMatrix(); // initialize the road connection graph from the class
        this.catanTowns = catanTowns;
        this.topWinStrategy = catanTowns.topWinStrategy; // get the top win strategy from the town class

        // full pipeline implemented in calculateOptimalRoad():
        // findSafeSpace -> setMaxOptLocation -> calculateMu -> calculateDref -> calculateGamma
        // -> calculateLambda -> setAdjustedOptLocationValues -> buildRoadWeights -> runDijkstra
        // -> findOptimalRoadDirection
    }


    /* create adjusted optimal location values
       NOTE: normalized by maxOptLocation (mirroring the same MaxOpt-normalization already
       applied to Danger(v) in the article) so AdjustedOpt(v) stays in the same [0, BaseCost]
       currency as BaseCost, instead of an unbounded raw OptLocation value. Without this, a
       decent-OptLocation neighbor immediately overwhelms BaseCost=1 and every reasonable edge
       collapses onto the epsilon floor in buildRoadWeights(), erasing real differences between
       neighbors (verified empirically - see the road-placement visualizations). */
    public void setAdjustedOptLocationValues() {

        this.adjustedOptLocation = new double[54];
        double[] optLocationRow = catanTowns.townOptimalLocationValues[this.topWinStrategy];

        for (int v = 0; v < 54; v++) {
            if (this.playerDistances[v] == Integer.MAX_VALUE) {
                this.adjustedOptLocation[v] = 0; // unreachable from our town - can't be a road target
                continue;
            }
            double normalizedOpt = this.maxOptLocation > 0 ? optLocationRow[v] / this.maxOptLocation : 0;
            this.adjustedOptLocation[v] = BASE_COST * normalizedOpt * Math.pow(this.gamma, this.playerDistances[v]);
        }
    }


    /* Calculates 'mu' (u), the average heuristic value of all safe nodes. */
    public void calculateMu() {

        if (this.safeNodes.isEmpty()) {
            this.mu = 0; // no safe nodes found (edge case) - avoid dividing by zero
            return;
        }

        double totalOptValue = 0;

        // sum up the OptLocation values for every node in our Safe Space
        for (int nodeIndex : this.safeNodes) {
            // Access the specific town score from the 54x5 matrix
            totalOptValue += this.catanTowns.townOptimalLocationValues[this.topWinStrategy][nodeIndex];
        }

        // calculate the average
        this.mu = totalOptValue / this.safeNodes.size();
    }


    // used to find the max opt location on the map, with the current best strategy
    // (only counts FREE towns - not our own starting town and not enemy-occupied ones,
    // since MaxOpt is defined in the document as "the score of the best AVAILABLE town")
    public void setMaxOptLocation() {

        this.maxOptLocation = 0;
        this.maxOptLocationIndex = -1;
        double[] optLocationRow = catanTowns.townOptimalLocationValues[this.topWinStrategy];

        for (int i = 0; i < optLocationRow.length; i++) {

            if (i == this.startingTownIndex || this.enemyOccupiedNodes.contains(i)) {
                continue; // not a free/available spot
            }

            if (optLocationRow[i] > this.maxOptLocation) {
                this.maxOptLocation = optLocationRow[i];
                this.maxOptLocationIndex = i;
            }
        }
    }


    /* Calculates d_ref - the distance (in roads) between the nearest opponent
       and the best available town on the map (the MaxOpt node). */
    public void calculateDref() {

        if (this.maxOptLocationIndex == -1) {
            this.dRef = 0; // no free town found on the map (edge case)
            return;
        }

        int distance = this.opponentDistances[this.maxOptLocationIndex];
        // if no opponents exist yet, treat it as "infinitely safe" -> 0
        this.dRef = (distance == Integer.MAX_VALUE) ? 0 : distance;
    }


    /* Calculates gamma (γ) - the distance decay factor, based on Dmax
       and the ratio between mu (average safe-node score) and MaxOpt. */
    public void calculateGamma() {

        if (this.dMax <= 0 || this.maxOptLocation <= 0) {
            this.gamma = 1.0; // no safe space / no free towns -> no decay
            return;
        }

        double ratio = this.mu / this.maxOptLocation;
        this.gamma = Math.pow(ratio, 1.0 / this.dMax); // Dmax-th root of (mu / MaxOpt)
    }


    /* Calculates lambda (λ) - the risk-aversion coefficient, derived from the
       "indifference point" between a risky-but-excellent road and a safe-but-average one. */
    public void calculateLambda() {

        if (this.maxOptLocation <= 0) {
            this.lambda = 0;
            return;
        }

        this.lambda = Math.pow(this.dRef, 2) * (1 - (this.mu / this.maxOptLocation));
    }


    /* Builds the "danger and opportunity" weighted graph, used to run Dijkstra
       and find the optimal road direction (final formula from the document). */
    public void buildRoadWeights() {

        for (int u = 0; u < 54; u++) {
            for (int v = 0; v < 54; v++) {

                if (this.roadConnectionGraph[u][v] == 0) {
                    this.roadWeights[u][v] = Double.POSITIVE_INFINITY; // no road exists between these two towns
                    continue;
                }

                double dOpp = (this.opponentDistances[v] == Integer.MAX_VALUE)
                        ? Double.POSITIVE_INFINITY
                        : Math.max(this.opponentDistances[v], 1); // avoid division by zero

                double danger = Double.isInfinite(dOpp) ? 0 : (this.maxOptLocation * this.lambda) / (dOpp * dOpp);

                double weight = BASE_COST + danger - this.adjustedOptLocation[v];
                this.roadWeights[u][v] = Math.max(EPSILON, weight);
            }
        }
    }


    /* Runs Dijkstra's algorithm from the starting town over roadWeights,
       storing cumulative distances + predecessors for path reconstruction. */
    public void runDijkstra() {

        int numNodes = 54;
        this.dijkstraDistances = new double[numNodes];
        this.dijkstraPrevious = new int[numNodes];
        boolean[] visited = new boolean[numNodes];

        Arrays.fill(this.dijkstraDistances, Double.POSITIVE_INFINITY);
        Arrays.fill(this.dijkstraPrevious, -1);

        PriorityQueue<Integer> queue = new PriorityQueue<>((a, b) -> Double.compare(this.dijkstraDistances[a], this.dijkstraDistances[b]));
        for (int source : this.startingTownIndices) { // multi-source: every town we currently own starts at distance 0
            this.dijkstraDistances[source] = 0;
            queue.add(source);
        }

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (visited[current]) continue;
            visited[current] = true;

            for (int neighbor = 0; neighbor < numNodes; neighbor++) {
                if (this.roadConnectionGraph[current][neighbor] == 0 || visited[neighbor]) continue;

                double newDist = this.dijkstraDistances[current] + this.roadWeights[current][neighbor];
                if (newDist < this.dijkstraDistances[neighbor]) {
                    this.dijkstraDistances[neighbor] = newDist;
                    this.dijkstraPrevious[neighbor] = current;
                    queue.add(neighbor);
                }
            }
        }
    }


    /* Picks the reachable free town with the lowest cumulative Dijkstra cost - the
       overall destination the whole path (and the road direction) is aimed at. */
    private int findBestTarget() {

        int bestTarget = -1;
        double bestCost = Double.POSITIVE_INFINITY;

        for (int v = 0; v < 54; v++) {
            if (this.startingTownIndices.contains(v) || this.enemyOccupiedNodes.contains(v)) continue;
            if (this.dijkstraDistances[v] < bestCost) {
                bestCost = this.dijkstraDistances[v];
                bestTarget = v;
            }
        }

        return bestTarget;
    }


    /* Picks the reachable free town with the lowest cumulative Dijkstra cost,
       then walks the path back to find its FIRST step - that step defines the
       road direction: (theOwnedTownItStartsFrom, returned index). With multiple
       starting towns, the path may emanate from any one of them. */
    public int findOptimalRoadDirection() {

        int bestTarget = findBestTarget();

        if (bestTarget == -1 || this.dijkstraPrevious[bestTarget] == -1) {
            return -1; // no reachable free town found
        }

        int step = bestTarget;
        while (! this.startingTownIndices.contains(this.dijkstraPrevious[step])) {
            step = this.dijkstraPrevious[step];
        }

        return step;
    }

    /**
     * Which of our owned starting towns the optimal road path (as returned by
     * {@link #findOptimalRoadDirection()}) actually starts from - the road to build is
     * {@code (getOptimalRoadOrigin(), findOptimalRoadDirection())}. Only meaningful after
     * calling {@link #calculateOptimalRoad}/{@link #findOptimalRoadDirection} first.
     * @return the owned town the chosen road starts from, or -1 if no path was found
     */
    public int getOptimalRoadOrigin() {
        int step = findOptimalRoadDirection();
        if (step == -1) return -1;
        return this.dijkstraPrevious[step];
    }


    /**
     * Returns the FULL path Dijkstra found from the starting town to its overall best
     * target - the same path findOptimalRoadDirection() only returns the first step of.
     * Exposed for debugging/visualization; not used by the core algorithm itself.
     * @return an ordered array of town indices from the starting town to the target
     *         (inclusive of both ends), or an empty array if no target was reachable.
     */
    public int[] getFullDijkstraPath() {

        int bestTarget = findBestTarget();

        if (bestTarget == -1 || this.dijkstraPrevious[bestTarget] == -1) {
            return new int[0];
        }

        ArrayList<Integer> path = new ArrayList<>();
        int step = bestTarget;
        path.add(step);
        while (! this.startingTownIndices.contains(step)) {
            step = this.dijkstraPrevious[step];
            path.add(step);
        }
        Collections.reverse(path);

        int[] result = new int[path.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = path.get(i);
        }
        return result;
    }


    /* Returns the cumulative Dijkstra cost to reach a given town - exposed for
       debugging/visualization alongside getFullDijkstraPath(). */
    public double getDijkstraDistanceTo(int town) {
        return this.dijkstraDistances[town];
    }


    /**
     * Removes specific edges from the internal road connection graph, marking them as
     * unavailable for a NEW road - used when an opponent has already built a road on that
     * exact edge (an edge can only be built on once, so we can't reuse it). This only
     * removes the exact edges given - a road may still legally pass NEAR/THROUGH an
     * opponent's settlement node (only settling there is illegal, not routing past it).
     * Call this AFTER construction and BEFORE calculateOptimalRoad()/findSafeSpace().
     * @param enemyRoadEdges list of {townA, townB} pairs already built by an opponent.
     */
    public void removeRoadEdges(ArrayList<int[]> enemyRoadEdges) {
        for (int[] edge : enemyRoadEdges) {
            int a = edge[0];
            int b = edge[1];
            this.roadConnectionGraph[a][b] = 0;
            this.roadConnectionGraph[b][a] = 0;
        }
    }


    /**
     * Runs the full road-placement algorithm end to end, from a single owned town.
     * @return the neighboring town index that defines the optimal road:
     *         the road to place is (townStartingIndex, returned index).
     */
    public int calculateOptimalRoad(int townStartingIndex, ArrayList<Integer> enemyOccupiedNodes) {
        ArrayList<Integer> single = new ArrayList<>();
        single.add(townStartingIndex);
        return calculateOptimalRoad(single, enemyOccupiedNodes);
    }


    /**
     * Runs the full road-placement algorithm end to end, multi-source from every town we
     * currently own (settlements and cities) - used mid-game, where we may own several towns
     * at once and want the single best road to build this turn across our whole network.
     * @return the neighboring town index that defines the optimal road: the road to place is
     *         ({@link #getOptimalRoadOrigin()}, returned index).
     */
    public int calculateOptimalRoad(ArrayList<Integer> ourTownIndices, ArrayList<Integer> enemyOccupiedNodes) {
        findSafeSpace(ourTownIndices, enemyOccupiedNodes);
        setMaxOptLocation();
        calculateMu();
        calculateDref();
        calculateGamma();
        calculateLambda();
        setAdjustedOptLocationValues();
        buildRoadWeights();
        runDijkstra();
        return findOptimalRoadDirection();
    }


    /**
     * Calculates the "Safe Space" (Voronoi region equivalent) & Dmax for a single starting
     * town position and current enemy occupied nodes. Convenience wrapper around the
     * multi-source {@link #findSafeSpace(ArrayList, ArrayList)}.
     * @param townStartingIndex The index (0-53) of your current starting position.
     * @param enemyOccupiedNodes A list of all road indices (0-53) where enemies currently have placed a road,
     */
    public void findSafeSpace(int townStartingIndex, ArrayList<Integer> enemyOccupiedNodes) {
        ArrayList<Integer> single = new ArrayList<>();
        single.add(townStartingIndex);
        findSafeSpace(single, enemyOccupiedNodes);
    }


    /**
     * Calculates the "Safe Space" (Voronoi region equivalent) & Dmax, multi-source from every
     * town we currently own at once (settlements and cities) and current enemy occupied nodes.
     * @param ourTownIndices The indices (0-53) of every town we currently own.
     * @param enemyOccupiedNodes A list of all road indices (0-53) where enemies currently have placed a road,
     */
    public void findSafeSpace(ArrayList<Integer> ourTownIndices, ArrayList<Integer> enemyOccupiedNodes) {

        this.safeNodes = new ArrayList<>(); // initialize the safe nodes list
        this.startingTownIndices = ourTownIndices;
        this.startingTownIndex = ourTownIndices.isEmpty() ? -1 : ourTownIndices.get(0); // kept for backward compatibility
        this.enemyOccupiedNodes = enemyOccupiedNodes;
        int numNodes = 54; // total number of nodes (town locations)

        // STEP 1: Calculate Player Distances (multi-source: every owned town starts at 0)
        int[] playerDistances = new int[numNodes];
        Arrays.fill(playerDistances, Integer.MAX_VALUE);
        Queue<Integer> playerQueue = new LinkedList<>();

        for (int ourTown : ourTownIndices) {
            playerQueue.add(ourTown);
            playerDistances[ourTown] = 0;
        }

        while (!playerQueue.isEmpty()) {
            int current = playerQueue.poll();
            
            for (int neighbor = 0; neighbor < numNodes; neighbor++) {
                // If a path exists (1) AND we haven't visited it yet
                if (this.roadConnectionGraph[current][neighbor] == 1 && playerDistances[neighbor] == Integer.MAX_VALUE) {
                    playerDistances[neighbor] = playerDistances[current] + 1;
                    playerQueue.add(neighbor);
                }
            }
        }

        // STEP 2: Calculate Enemy Distances (Multi-Source BFS)
        int[] opponentDistances = new int[numNodes];
        Arrays.fill(opponentDistances, Integer.MAX_VALUE);
        Queue<Integer> opponentQueue = new LinkedList<>();
        
        // Add ALL enemy starting points to the queue at once
        for (int enemyNode : enemyOccupiedNodes) {
            opponentQueue.add(enemyNode);
            opponentDistances[enemyNode] = 0;
        }
        
        while (!opponentQueue.isEmpty()) {
            int current = opponentQueue.poll();
            
            for (int neighbor = 0; neighbor < numNodes; neighbor++) {
                // If a path exists (1) AND the enemy BFS hasn't reached it faster yet
                if (this.roadConnectionGraph[current][neighbor] == 1 && opponentDistances[neighbor] == Integer.MAX_VALUE) {
                    opponentDistances[neighbor] = opponentDistances[current] + 1;
                    opponentQueue.add(neighbor);
                }
            }
        }

        // STEP 3: Determine Safe Nodes
        this.dMax = 0;
        for (int i = 0; i < numNodes; i++) {
            // A node is safe if we can actually reach it, AND our distance is strictly less than the enemy's
            if (playerDistances[i] != Integer.MAX_VALUE && playerDistances[i] < opponentDistances[i]) {
                this.safeNodes.add(i);

                // update Dmax if this safe node is the furthest one we've found so far
                if (playerDistances[i] > this.dMax) {
                    this.dMax = playerDistances[i];
                }
            }
        }

        // save both BFS results as fields - they're needed later for d_opp(v), d_ref and d_us(v)
        this.playerDistances = playerDistances;
        this.opponentDistances = opponentDistances;
    }





}
