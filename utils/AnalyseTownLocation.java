package CatanAnalysis.utils;

import CatanAnalysis.entities.BipartiteTownsAndHexagons;;

public class AnalyseTownLocation {
    
    // attributes
    BipartiteTownsAndHexagons[] townArray = new BipartiteTownsAndHexagons[54]; // array to hold all 54 towns


    // constructor
    public AnalyseTownLocation() {

        // create array of all towns
        createTownsArray();

    }


    // function to create the 54 towns array
    // we match the data of adusted hexagons to the adjacency matrix in the "BipartiteTownsAndHexagons" class.
    private void createTownsArray() {

        // loading data into array
        for (int i = 0; i < 54; i++) {

            int townIndex = i;
            int townAdjustedHexagons = 0;

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

            this.townArray[i] = new BipartiteTownsAndHexagons(townIndex, townAdjustedHexagons);
        }



    }
}
