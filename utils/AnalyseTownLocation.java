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
    private final int TOTAL_TOWNS = townArray.length;
    private final MapAnalysis mapAnalysis;

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
        for (int i = 0; i < TOTAL_TOWNS; i++) {

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
    private void bipartiteGraph() {

        // for each tile


        // connect towns
        return;
    }









    // others:

    // helper function used for debugging the connection between indices in ConnectedComponentTile class and Tile Class
    private void printTiles() {

        int WIN_NUMBER = 4;
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
        for (int i = 0; i < WIN_NUMBER; i++) {
            
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
