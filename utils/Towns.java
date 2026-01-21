package CatanAnalysis.utils;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.entities.Tile;
import CatanAnalysis.entities.TownConnectionGraph;
import java.util.ArrayList;

// class to analyse the optimal town locations based on the map analysis results
public class Towns {
    
    // attributes:
    public final TownConnectionGraph[] townArray = new TownConnectionGraph[this.TOTAL_TOWNS]; // array to hold all 54 towns
    public double[][] townOptimalLocationValues; // 2D array to hold the optimal location values for each town and win condition, i = win condition, j = town index 
    public final ArrayList<ArrayList<ConnectedComponentTile>>[] mapTilesData; // array to hold all the 19 map tiles

    private final int TOTAL_TOWNS = 54; // total number possible of town locations in the game
    private final int TOTAL_TILES = 19; // total number of tiles in the game
    private final int NUM_OF_RESOURCES = 5; // total resource num
    private final int NUM_OF_WIN_CONDITIONS; // number of win conditions from the map analysis
    private final Map mapAnalysis;
    private final double[][] resImportanceTable;
    double CLUSTER_SIZE_EXPONENT = 0.5; // exponent for cluster size factor calculation, bigger value = more relaince on longer roads
    
    // each rows represents a hexagon, from top to down, each hexagon contains the 6 town IDs that touches it
    private final int[][] hexToTownsMap = {
        // (Hexes 0, 1, 2)
        {0, 3, 4, 7, 8, 12}, // Hex 0
        {1, 4, 5, 8, 9, 13}, // Hex 1
        {2, 5, 6, 9, 10, 14}, // Hex 2

        // (Hexes 3, 4, 5, 6)
        {7, 11, 12, 16, 17, 22}, // Hex 3
        {8, 12, 13, 17, 18, 23}, // Hex 4
        {9, 13, 14, 18, 19, 24}, // Hex 5
        {10, 14, 15, 19, 20, 25}, // Hex 6

        // (Hexes 7, 8, 9, 10, 11)
        {16, 21, 22, 27, 28, 33}, // Hex 7
        {17, 22, 23, 28, 29, 34}, // Hex 8
        {18, 23, 24, 29, 30, 35}, // Hex 9 
        {19, 24, 25, 30, 31, 36}, // Hex 10
        {20, 25, 26, 31, 32, 37}, // Hex 11

        // (Hexes 12, 13, 14, 15)
        {28, 33, 34, 38, 39, 43}, // Hex 12
        {29, 34, 35, 39, 40, 44}, // Hex 13
        {30, 35, 36, 40, 41, 45}, // Hex 14
        {31, 36, 37, 41, 42, 46}, // Hex 15

        // (Hexes 16, 17, 18)
        {39, 43, 44, 47, 48, 51}, // Hex 16
        {40, 44, 45, 48, 49, 52}, // Hex 17
        {41, 45, 46, 49, 50, 53}  // Hex 18
    };

    
    /* constructor:
    @param mapAnalysis Map - Map object from the map analysis
    @param resourcesAvailability double[][] - 2D array of resource availability
    @param winConditionsResources double[][] - 2D array of win conditions resources */
    public Towns(Map mapAnalysis, double[][] resourcesAvailability, double[][] winConditionsResources) {

        this.mapTilesData = mapAnalysis.mapGroupData;
        this.mapAnalysis = mapAnalysis;
        this.NUM_OF_WIN_CONDITIONS = mapAnalysis.getWinConitionsNumber();
        this.townOptimalLocationValues = new double[this.NUM_OF_WIN_CONDITIONS][this.TOTAL_TOWNS]; // initialize the 2D array

        // load resource importance table
        WinConditions resImportance = new WinConditions(this.NUM_OF_WIN_CONDITIONS);
        this.resImportanceTable = resImportance.importantIndex(winConditionsResources, resourcesAvailability);
        createTownsArray(); // load info for the townArray
    }


    // function to create the 54 towns array
    private void createTownsArray() {

        // loading data into array
        for (int i = 0; i < this.TOTAL_TOWNS; i++) {
            this.townArray[i] = new TownConnectionGraph(i, 0);
        }
    }


    // create the bipartite graph of - town:hexagons
    // for each town index, this function connects the town object to all the close hexagons that touches it 
    public void connectHexToTowns() {

        // loop through each hexagon
        for (int hexIndex = 0; hexIndex < hexToTownsMap.length; hexIndex++) {

            // get indices for each hexagon
            int[] townIndices = this.hexToTownsMap[hexIndex]; 
            // find and load the heaxgon tile object by the index
            ConnectedComponentTile hexagon = getMapTilesIndex(hexIndex);

            // add the tile 
            for (int townIndex : townIndices) {
                this.townArray[townIndex].connectedHexagons.add(hexagon);
            }
        }
    
    }


    // connect Hexagon to Towns
    public void connectTownsToHexagons() {
        // loop through every win condition
        for (int winCon = 0; winCon < this.NUM_OF_WIN_CONDITIONS; winCon++) {
            
            // loop through each win rate group (1.0, 0.75, etc.) within that win condition
            for (ArrayList<ConnectedComponentTile> componentList : this.mapTilesData[winCon]) {
                if (componentList == null) continue;

                // loop through every individual tile in this specific component/rate group
                for (ConnectedComponentTile tile : componentList) {
                    
                    // retrieve the pre-defined town indices for this specific hexagon index
                    int hexIndex = tile.getTileIndex();
                    int[] townIndices = this.hexToTownsMap[hexIndex];

                    // link the town objects to this specific tile instance
                    for (int townIdx : townIndices) {
                        TownConnectionGraph town = this.townArray[townIdx];
                        
                        // Add the town to the tile's list  if not already present
                        if (!tile.connectedTowns.contains(town)) {
                            tile.connectedTowns.add(town);
                        }
                    }
                }
            }
        }
    }
    
    
    // rates each town location based on the hexagons that touches it
    public void rateTownLocation() {

        double winIndex; // holds the win rate index value
        double clusterSizeFactor;

        // loop through each win condition
        for (int winConditionIndex = 0; winConditionIndex < this.NUM_OF_WIN_CONDITIONS; winConditionIndex++) {
            
            // loop through each win rate group within that win condition
            for (ArrayList<ConnectedComponentTile> component : this.mapTilesData[winConditionIndex]) {

                // calculate cluster size factor
                clusterSizeFactor = (double) component.size() / this.TOTAL_TILES; 

                // loop through every individual tile in this specific component group
                for (ConnectedComponentTile tile : component) {
                    winIndex = tile.TILE_VALUE; 

                    // loop through each town connected to this tile
                    for (TownConnectionGraph town : tile.connectedTowns) {
                        // calculate the optimal location value for this town and tile
                        double optLocationValue = calculateOptimalLocation(winConditionIndex, this.resImportanceTable, winIndex, clusterSizeFactor);
                        this.townOptimalLocationValues[winConditionIndex][town.TOWN_INDEX] += optLocationValue; // accumulate the optimal location value
                        
                    }
                }
            }
        }
    }


    // others:

        
    // calculate the optimal location value for a given town and hexagon tile
    private double calculateOptimalLocation(int winConditionIndex, double[][] resImportanceTable, double winIndex, double clusterSizeFactor) {

        double finalOptLocation = 0.0;
        double[] townOptLocation = new double[this.NUM_OF_RESOURCES];
        int optLocationLength = townOptLocation.length;

        // loop through each resource type
        for (int i = 0; i < optLocationLength; i++) {

            // define alpha, beta, gamma values
            double beta = 1.5;
            double gamma = this.CLUSTER_SIZE_EXPONENT;

            // calculate the optimal location value
            townOptLocation[i] += resImportanceTable[winConditionIndex][i] * Math.pow(winIndex, beta) * Math.pow(clusterSizeFactor, gamma); 
        }

        // square root normalization for final optimal location value
        for (int i = 0; i < optLocationLength; i++) {
            finalOptLocation += Math.sqrt(townOptLocation[i]);
        }

        return finalOptLocation;
    }
    

    // adds data to each town about the number of hexagon that touches it
    public void setTownHexagonCounts() {
        
        for (int townIndex = 0; townIndex < this.TOTAL_TOWNS; townIndex++) {
            int numOfTotalHexagons = this.townArray[townIndex].connectedHexagons.size();
            this.townArray[townIndex].setAdjustedHexagons(numOfTotalHexagons);
        }
    }


    // given an index, it searches the tile linearly for the given index, in the connectedcomponenet array, then it returns the tile object
    private ConnectedComponentTile getMapTilesIndex(int index) {

        // win condition
        for (ArrayList<ArrayList<ConnectedComponentTile>> rateList : this.mapTilesData) {
            
            // heatmap value
            for (int j = 0; j < rateList.size(); j++) {
                
                ArrayList<ConnectedComponentTile> componentList = rateList.get(j);
                if (componentList == null) continue;

                // component
                for (ConnectedComponentTile tile : componentList) {
                    
                    if (tile.getTileIndex() == index) {
                        return tile; // found the tile
                    }
                }
            }
        }
        // not found
        return null;
    }


    // debug methods:


    // method to see the finalOptLocation value for each town
    public void printFinalOptLocation(int winConditionIndex) {

        System.out.println("--- Starting Final Optimal Location Verification ---");

        System.err.println("Win Condition Index: " + winConditionIndex);

        // loop through each town
        for (int k = 0; k < this.TOTAL_TOWNS; k++) { 
            System.out.println(" Town [" + k + "]" + "\tFinal Opt Location Value: " + this.townOptimalLocationValues[winConditionIndex][k]);
        }
        
    }


    // method to see the connection between each town to its adjusted hex
    public void printTownHex() {

        System.out.println("--- Starting Town Verification ---");

        for (int i = 0; i < this.TOTAL_TOWNS; i++) {
            
            int totalHexagons = this.townArray[i].connectedHexagons.size();
            
            for (int j = 0; j < totalHexagons; j++) {

                ConnectedComponentTile tile = this.townArray[i].connectedHexagons.get(j);
                int attachedHexagons = this.townArray[i].TOWN_ADJUSTED_HEXAGONS;
                System.out.println(" Town [" + i + "] => Hexagon [" + tile.getTileIndex() + "]" + "\tTotal of [" + attachedHexagons + "] hexagons attached");
            }
        }
    }


    // helper function used for debugging the connection between indices in ConnectedComponentTile class and Tile Class
    public void printTiles() {

        int WIN_CONDITION_NUMBER = 4;
        int WIN_RATE_NUMBER = 4;

        // tiles:

        Tile[] t = new Tile[19];
        t = this.mapAnalysis.getMapTiles();
        
        for (int i = 0; i < 19; i++) {

            int tileIndex = t[i].getTileIndex();
            int diceNumber = t[i].getDiceNumber();
            int resourceNumber = t[i].getResourceType();
            
            System.out.println(tileIndex + " Dice: " + diceNumber + " ResourceType: " + resourceNumber + "\t" + "......");
        }

        System.err.println("");
        // Connected component

        // win condition
        for (int i = 0; i < WIN_CONDITION_NUMBER; i++) {
            
            // win rate
            for (int j = 0; j < WIN_RATE_NUMBER; j++) {
                
                int COMPONENT_NUM = this.mapTilesData[i].get(j).size();
                // component
                for (int k = 0; k < COMPONENT_NUM; k++) {
                    
                    // tiles
                    int index = this.mapTilesData[i].get(j).get(k).getTileIndex();
                    t = this.mapAnalysis.getMapTiles();
                    
                    int diceNumber = t[index].getDiceNumber();
                    int resourceNumber = t[index].getResourceType();

                    System.err.println(index + " Dice: " + diceNumber + " ResourceType: " + resourceNumber);
                }
            }
        }
    }






// end of class    
}
