package CatanAnalysis.utils;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.entities.Tile;
import CatanAnalysis.entities.TownsAndHexagons;
import java.util.ArrayList;
;

public class AnalyseTownLocation {
    
    // attributes
    private final TownsAndHexagons[] townArray = new TownsAndHexagons[54]; // array to hold all 54 towns
    private ArrayList<ArrayList<ConnectedComponentTile>>[] mapTilesData; // array to hold all the 19 map tiles
    private final MapAnalysis mapAnalysis;
    private final int TOTAL_TOWNS = townArray.length;
    private final int TOTAL_TILES = 19; // total tiles in a catan map

    // constructor
    public AnalyseTownLocation(ArrayList<ArrayList<ConnectedComponentTile>>[] mapGroupsData, MapAnalysis mapAnalysis) {

        this.mapTilesData = mapGroupsData;
        this.mapAnalysis = mapAnalysis;
        createTownsArray(); // load info for the townArray
        bipartiteGraph(); // create bipartite graph between hexagons and towns

        // printTiles(); // helper function for debugging
    }


    // function to create the 54 towns array
    // we match the data of adusted hexagons to the adjacency matrix in the "BipartiteTownsAndHexagons" class.
    private void createTownsArray() {

        // loading data into array
        for (int i = 0; i < this.TOTAL_TOWNS; i++) {

            int townIndex = i;
            int townAdjustedHexagons;

            // handle 1:1 ratio
            if (i <= 17) {
                townAdjustedHexagons = 1;
            }
            // handle 1:2 ratio
            else if (i > 17 && i <= 35) {
                townAdjustedHexagons = 2;
            }
            // handle 1:3 ratio
            else {
                townAdjustedHexagons = 3;
            }

            this.townArray[i] = new TownsAndHexagons(townIndex, townAdjustedHexagons);
        }
    }


    // create the bipartite graph of - town:hexagons
    // for each town index, this function connects the town to all the close hexagons that touches it 
    private void bipartiteGraph() {

        // each rows represents a hexagon, from top to down, each hexagon contains the 6 town IDs that touches it
        int[][] hexToTownsMap = {
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

        // load the hexagon data into each town using the array
        for (int hexIndex = 0; hexIndex < hexToTownsMap.length; hexIndex++) {
            
            int[] townIndices = hexToTownsMap[hexIndex]; // get list of all town indices for this hexagon

            // loop through those towns (6 total) and add the connection
            for (int townIndex : townIndices) {
                this.townArray[townIndex].connectedHexagons.addLast(getMapTilesIndex(hexIndex)); // load the hexagon to each town
            }
        }
    }









    // others:


    // given an index, it searches the tile linearly for the given index, in the connectedcomponenet array, then it returns the tile object
    private ConnectedComponentTile getMapTilesIndex(int index) {

        int WIN_CONDITION_NUMBER = 4;
        int WIN_RATE_NUMBER = 4;

        // win condition
        for (int i = 0; i < WIN_CONDITION_NUMBER; i++) {
            
            // win rate
            for (int j = 0; j < WIN_RATE_NUMBER; j++) {
                
                int COMPONENT_NUM = this.mapTilesData[i].get(j).size();
                // component
                for (int k = 0; k < COMPONENT_NUM; k++) {
                    
                    // tiles
                    int currIndex = this.mapTilesData[i].get(j).get(k).getTileIndex();

                    if (index == currIndex) {
                        return this.mapTilesData[i].get(j).get(k);
                    }
                        
                    

                }
            }
        }
        // not found
        return null;
    }


    // helper function used for debugging the connection between indices in ConnectedComponentTile class and Tile Class
    private void printTiles() {

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
