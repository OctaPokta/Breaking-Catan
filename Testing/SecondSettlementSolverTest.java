package CatanAnalysis.testing;

import CatanAnalysis.secondsettlement.SecondSettlementSolver;
import CatanAnalysis.secondsettlement.StrategyTable;
import java.util.ArrayList;
import java.util.Arrays;

// Quick smoke test for SecondSettlementSolver: runs it once with no opponents (should match
// the plain algorithm) and once with opponents placed (should route around them).
public class SecondSettlementSolverTest {

    public static void main(String[] args) throws Exception {
        int[] mapResources = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
        int[] mapDiceNumbers = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };

        StrategyTable strategyTable = StrategyTable.load("secondsettlement/strategies.csv");
        System.out.println("Loaded " + strategyTable.size() + " strategies.");

        SecondSettlementSolver solver = new SecondSettlementSolver(mapResources, mapDiceNumbers, strategyTable);

        // --- run 1: no opponents at all (should behave like the original algorithm) ---
        SecondSettlementSolver.SolveResult r1 = solver.solve(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        System.out.println("=== RUN 1 (no opponents) ===");
        System.out.println("Strategy: " + r1.strategyName);
        System.out.println("Best town: " + r1.bestTownIndex);
        System.out.println("Road: " + r1.bestTownIndex + " -> " + r1.roadDirection);
        System.out.println("Path: " + Arrays.toString(r1.fullDijkstraPath));

        // --- run 2: same map, but we already own r1's town, and 2 "enemies" sit far away ---
        ArrayList<Integer> ourSettlements = new ArrayList<>(Arrays.asList(r1.bestTownIndex));
        ArrayList<Integer> enemySettlements = new ArrayList<>(Arrays.asList(0, 53));
        ArrayList<int[]> enemyRoads = new ArrayList<>();
        enemyRoads.add(new int[]{0, 3});

        SecondSettlementSolver.SolveResult r2 = solver.solve(ourSettlements, enemySettlements, enemyRoads);
        System.out.println("\n=== RUN 2 (opponents at 0 and 53, our settlement at " + r1.bestTownIndex + ") ===");
        System.out.println("Best town: " + r2.bestTownIndex);
        System.out.println("Road: " + r2.bestTownIndex + " -> " + r2.roadDirection);
        System.out.println("Path: " + Arrays.toString(r2.fullDijkstraPath));

        boolean ok = r2.bestTownIndex != r1.bestTownIndex && r2.bestTownIndex != 0 && r2.bestTownIndex != 53;
        System.out.println("\nSanity check (2nd town differs from 1st, avoids enemies): " + (ok ? "PASS" : "FAIL"));
    }
}
