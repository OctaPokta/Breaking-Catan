package CatanAnalysis.testing;

import CatanAnalysis.secondsettlement.SecondSettlementSolver;
import CatanAnalysis.secondsettlement.StrategyTable;
import java.util.ArrayList;
import java.util.Arrays;

// Verifies the new mid-game methods (chooseNextSettlement/chooseNextRoad):
//   1. Repeated calls on the SAME solver instance give IDENTICAL results each time (this is
//      exactly the kind of data-mutation bug that was just caught and fixed in
//      computeTownRatings() - solve()/chooseNextSettlement()/chooseNextRoad() must all be safe
//      to call many times in a row on one instance, since a real game calls them once per turn).
//   2. chooseNextSettlement() only ever returns one of the given legal candidates.
//   3. chooseNextRoad(), given two owned towns, returns a road starting from one of them.
public class MidGameSolverTest {

    public static void main(String[] args) throws Exception {
        int[] mapResources = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
        int[] mapDiceNumbers = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };
        StrategyTable strategyTable = StrategyTable.load("secondsettlement/strategies.csv");

        SecondSettlementSolver solver = new SecondSettlementSolver(mapResources, mapDiceNumbers, strategyTable);

        // --- repeated solve() calls on the same instance must be identical ---
        SecondSettlementSolver.SolveResult r1 = solver.solve(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        SecondSettlementSolver.SolveResult r2 = solver.solve(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        boolean solveRepeatable = (r1.bestTownIndex == r2.bestTownIndex) && (r1.roadDirection == r2.roadDirection)
            && r1.strategyName.equals(r2.strategyName);
        System.out.println("solve() repeatable on same instance: " + solveRepeatable
            + " (town=" + r1.bestTownIndex + " both times, strategy=" + r1.strategyName + ")");

        // --- chooseNextSettlement: only ever picks from the given legal candidates ---
        ArrayList<Integer> legalCandidates = new ArrayList<>(Arrays.asList(5, 12, 30, 41));
        SecondSettlementSolver.SolveResult s1 = solver.chooseNextSettlement(legalCandidates);
        SecondSettlementSolver.SolveResult s2 = solver.chooseNextSettlement(legalCandidates);
        boolean pickedFromLegalSet = legalCandidates.contains(s1.bestTownIndex);
        boolean settlementRepeatable = (s1.bestTownIndex == s2.bestTownIndex);
        System.out.println("chooseNextSettlement picked from legal set: " + pickedFromLegalSet
            + " (chose town " + s1.bestTownIndex + " from " + legalCandidates + ")");
        System.out.println("chooseNextSettlement repeatable: " + settlementRepeatable);

        // --- chooseNextRoad: multi-source from two owned towns ---
        ArrayList<Integer> ourTowns = new ArrayList<>(Arrays.asList(12, 41));
        SecondSettlementSolver.RoadResult road1 = solver.chooseNextRoad(ourTowns, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        SecondSettlementSolver.RoadResult road2 = solver.chooseNextRoad(ourTowns, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        boolean originIsOurs = ourTowns.contains(road1.originTown);
        boolean roadRepeatable = (road1.originTown == road2.originTown) && (road1.direction == road2.direction);
        System.out.println("chooseNextRoad origin is one of our towns: " + originIsOurs
            + " (road " + road1.originTown + " -> " + road1.direction + ")");
        System.out.println("chooseNextRoad repeatable: " + roadRepeatable);

        boolean allPass = solveRepeatable && pickedFromLegalSet && settlementRepeatable && originIsOurs && roadRepeatable
            && road1.direction != -1;
        System.out.println();
        System.out.println(allPass ? "PASS" : "FAIL");
        if (!allPass) System.exit(1);
    }
}
