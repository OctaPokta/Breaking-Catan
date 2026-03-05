package CatanAnalysis;

import CatanAnalysis.handlers.CatanHandler;

public class Main {

    public static final int WIN_COND = 1; // 1
    public static final int TOWN_HEX = 1 << 1; // 2
    public static final int FINAL_OPT = 1 << 2; // 4
    public static final int TILES_DEBUG = 1 << 3; // 8

    public static void main(String[] args) {

        // INPUTS

        /*
         * Resources Avalibality:
         * brick | ore | sheep | wheat | wood | desert
         * j corrosponds to: Number of Tiles | Area Coverage %
         * i corrosponds to: Resource index
         */
        double[][] resourcesAvailability = {
                { 3, 15.8 }, // brick
                { 3, 15.8 }, // ore
                { 4, 21.1 }, // sheep
                { 4, 21.1 }, // wheat
                { 4, 21.1 }, // wood
                { 1, 5.3 } // desert
        };

        /*
         * win conditions table:
         * each row determines the resource importance for a specific win condition, the
         * last column is the total sum (later used for weighted need calculation)
         * j corrosponds to: Brick | Ore | Sheep | Wheat | Wood | Total
         * i corrosponds to: Condition index
         */
        double[][] winConditionsResources = {
                { 11, 12, 4, 12, 11, 0 }, // 4 Cities + LR
                { 9, 11, 8, 12, 9, 0 }, // 2 cities + 1 Settlement + LR & LA (Extra Brick & Wood)
                { 14, 9, 5, 11, 14, 0 }, // 3 cities + 2 settlements + LR
                { 13, 11, 10, 14, 13, 0 }, // 2 Cities + 3 Settlements + LR
                { 14, 8, 10, 12, 14, 0 } // 1 City + 3 Settlements + LR & LA
        };

        int[] mapResources = { 2, 4, 2, 0, 0, 3, 4, 3, 1, 5, 2, 1, 4, 2, 1, 3, 4, 0, 3 }; // example input
        int[] mapDiceNumbers = { 5, 11, 12, 9, 8, 4, 5, 8, 3, 0, 10, 9, 3, 6, 4, 10, 11, 2, 6 }; // example input

        CatanHandler catan = new CatanAnalysisEngine(resourcesAvailability, winConditionsResources, mapResources, mapDiceNumbers);

        
        catan.runMapAnalysis(); // run map analysis
        catan.pickOptTown(); // pick optimal town placement
        
        catan.runInteractiveConsole(); // run interactive console for user to select debug options
        
        

    }

    
}
