package CatanAnalysis.utils;

import CatanAnalysis.entities.ConnectedComponentTile;
import CatanAnalysis.entities.TownsAndHexagons;
import java.util.ArrayList;;

public class AnalyseTownLocation {
    
    // attributes
    private TownsAndHexagons[] townArray = new TownsAndHexagons[54]; // array to hold all 54 towns
    private ArrayList<ArrayList<ConnectedComponentTile>>[] mapGroupsData;

    // constructor
    public AnalyseTownLocation(ArrayList<ArrayList<ConnectedComponentTile>>[] mapGroupsData) {

        this.mapGroupsData = mapGroupsData;
        createTownsArray(); // load info for the townArray
    }


    // function to create the 54 towns array
    // we match the data of adusted hexagons to the adjacency matrix in the "BipartiteTownsAndHexagons" class.
    private void createTownsArray() {

        // loading data into array
        for (int i = 0; i < 54; i++) {

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



    // other functions......
    // create the bipartite graph of - town:hexagons





}
