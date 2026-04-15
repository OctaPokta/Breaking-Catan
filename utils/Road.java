// this class is used to performe calculation for the road placement

package CatanAnalysis.utils;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.entities.RoadConnectionGraph;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class Road {

    // attributes
    
    // existing data
    private ArrayList<ArrayList<ConnectedComponentTile>> mapTilesData; // list of all the hexagons of the map, each hexagon is represented by a "ConnectedComponentTile" class, and they are grouped by the town they are connected to (54 possible town locations)
    private double[][] townOptimalLocationValues; // 54x5 matrix, each row is a town, each column is the value of placing a road in that town for a specific win condition
    private int[][] roadConnectionGraph = new int[54][54]; // graph representing the connection of each town to another, and the hexagons they are connected to, this is used to calculate the area of control of each town and the best road placement based on that

    // new data
    double[][] roadWeights = new double[54][54]; // used to run dikstra's algorithm
    private ArrayList<Integer> safeNodes; // list of nodes indices that we are closer to than the opponents, this is the area of control that we currently own
    private double dMax; // max distance of roads we wanna place from a starting town position, based on strategy and map state 




    // constructor
    public Road(ArrayList<ArrayList<ConnectedComponentTile>> mapTilesData, double[][] townOptimalLocationValues) {
        this.mapTilesData = new ArrayList<>(mapTilesData);
        this.townOptimalLocationValues = townOptimalLocationValues;
        this.roadConnectionGraph = new RoadConnectionGraph(0, 0).getAdjacencyMatrix(); // initialize the road connection graph from the class

        /*
        TODO:
        1. findsafespace
        2. find maxoptlocation on the map
        3. finding "u"
        4. create adjusted opt val and add it to "roadweights"
        5. finding Dref and Dopp
        6. adjusting "roadweights" based on step 3 on the pdf file
        7. Finally, run Dijkstra's algorithm from the starting town on roadweights
        8. the shortest path is the directiopn of the optiomal road placement
         */
    }

    
    /**
     * Calculates the "Safe Space" (Voronoi region equivalent) & Dmax for a given starting town position and current enemy occupied nodes.
     * * @param townStartingIndex The index (0-53) of your current starting position.
     * @param enemyOccupiedNodes A list of all town indices (0-53) where enemies currently have 
     * settlements, cities, or the tips of their built roads.
     */
    public void findSafeSpace(int townStartingIndex, ArrayList<Integer> enemyOccupiedNodes) {

        this.safeNodes = new ArrayList<>(); // initialize the safe nodes list
        int numNodes = 54; // total number of nodes (town locations)
        
        // STEP 1: Calculate Player Distances
        int[] playerDistances = new int[numNodes];
        Arrays.fill(playerDistances, Integer.MAX_VALUE);
        Queue<Integer> playerQueue = new LinkedList<>();
        
        playerQueue.add(townStartingIndex);
        playerDistances[townStartingIndex] = 0;
        
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
    }





}
