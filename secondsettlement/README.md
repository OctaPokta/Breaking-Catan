# Second-Settlement Planner

A self-contained local desktop app (Java Swing) that wraps the existing road/settlement
algorithm to answer: **given a board where opponents may already have settlements and
roads, where should our next settlement and road go?**

Everything for this feature lives in this folder, separate from the core algorithm classes
in `utils/`/`entities/` (which only received small, additive, backward-compatible methods -
see below).

## Running it

From the `CatanAnalysis` project root:

```bash
javac -encoding UTF-8 -d _out $(find . -name "*.java")
java -cp _out CatanAnalysis.secondsettlement.gui.CatanBoardApp
```

(On Windows PowerShell, replace the `find` command with a loop, or just compile the whole
`CatanAnalysis` tree with your usual `javac` invocation and run the same main class.)

## How it works

1. Pick a tool from the toolbar: edit a hex's resource/dice number, mark our own or an
   enemy's settlement, mark our own or an enemy's road, or erase a mark.
2. Click hexes/town nodes/road edges on the board to set up the current game state.
3. Click **"Find Best Settlement + Road"**. The board is recolored with the heatmap for the
   winning strategy, the recommended settlement is highlighted gold, and the recommended
   road is drawn as a dashed gold line - the text panel spells it out too.

## Files

- `strategies.csv` - the win-condition/resource-requirement table, in plain editable CSV.
  Edit it by hand (or via "Reload strategy table from disk" after editing) - no need to
  touch any Java code to try a different strategy.
- `StrategyTable.java` - loads that CSV.
- `BoardGeometry.java` - fixed pixel-free layout data (hex centers, town positions, the 72
  road edges) used to draw and hit-test the board.
- `SecondSettlementSolver.java` - orchestrates the existing `WinConditions -> Map -> Towns ->
  Road` pipeline, pruning enemy settlements/roads before picking the next settlement + road.
- `gui/BoardPanel.java`, `gui/CatanBoardApp.java` - the Swing UI.

## What "pruning" actually means

Only two things are removed when opponents are marked on the board:

1. **Town candidates**: an opponent's own settlements, and every town exactly one road away
   from one (Catan's distance rule - any two settlements must be at least two roads apart).
2. **Road edges**: the exact edges an opponent has already built (an edge can only be built
   on once).

A road is otherwise free to pass near or through an opponent's settlement node - only
*settling* there is illegal, not routing a road past it - so no other exclusion is applied.
