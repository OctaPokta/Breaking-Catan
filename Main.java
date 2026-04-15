package CatanAnalysis;

import CatanAnalysis.handlers.CatanHandler;

public class Main {

    public static final String [] WIN_COND_NAMES = {
        "4 Cities + LR",
        "2 Cities + 1 Settlement + LR & LA (Extra Brick & Wood)",
        "3 cities + 2 settlements + LR",
        "2 Cities + 3 Settlements + LR",
        "1 City + 3 Settlements + LR & LA"
    };

    public static void main(String[] args) {

        // INPUTS

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

        // RUN

        CatanHandler catan = new CatanAnalysisEngine(winConditionsResources, mapResources, mapDiceNumbers);

        catan.runMapAnalysis(); // run map analysis
        catan.pickOptTown(); // pick optimal town placement
        catan.pickOptRoad();// pick best road placement
        // pick the second town
        
        catan.runInteractiveConsole(); // run interactive console for user to select debug options
        
        

    }

    
}
