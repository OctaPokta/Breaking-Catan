package CatanAnalysis.secondsettlement.gui;

import CatanAnalysis.secondsettlement.BoardGeometry;
import CatanAnalysis.secondsettlement.SecondSettlementSolver;
import CatanAnalysis.secondsettlement.StrategyTable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

/**
 * Local desktop app for the second-settlement algorithm: an interactive Catan board where
 * you set up each hex's resource + dice number, mark existing settlements/roads (yours and
 * the opponents'), then run the algorithm to get the recommended next settlement + road.
 * Replaces the old System.out/Scanner debug console (CatanAnalysisEngine.runInteractiveConsole()).
 */
public class CatanBoardApp extends JFrame {

    private enum Tool { EDIT_HEX, OUR_SETTLEMENT, ENEMY_SETTLEMENT, OUR_ROAD, ENEMY_ROAD, ERASE }

    private final BoardPanel boardPanel = new BoardPanel();
    private final JTextArea resultsArea = new JTextArea();
    private final JTextArea strategyArea = new JTextArea();
    private Tool currentTool = Tool.EDIT_HEX;
    private StrategyTable strategyTable;
    private String strategyTablePath; // where loadStrategyTable() actually found strategies.csv, for display
    private JLabel strategyLabel; // kept as a field so loadStrategyTable() can update it after resolving the path

    // when checked, "Randomize Board" always keeps the desert on the single center hex
    // (index 9 - the middle tile of the 3-4-5-4-3 layout) instead of placing it anywhere
    private final javax.swing.JCheckBox forceDesertCenterCheckbox = new javax.swing.JCheckBox("Force desert at center", false);
    private static final int CENTER_HEX_INDEX = 9;

    // standard Catan tile distribution (3 brick, 3 ore, 4 sheep, 4 wheat, 4 wood, 1 desert)
    // and the standard dice-number set, desert always kept at hex 9 (board center)
    private static final int[] DEFAULT_RESOURCES = { 2,4,2,0,0,3,4,3,1,5,2,1,4,2,1,3,4,0,3 };
    private static final int[] DEFAULT_DICE = { 5,11,12,9,8,4,5,8,3,0,10,9,3,6,4,10,11,2,6 };

    public CatanBoardApp() {
        super("Catan Second-Settlement Planner");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 820);
        setLocationRelativeTo(null);

        loadStrategyTable();

        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            boardPanel.setHex(h, DEFAULT_RESOURCES[h], DEFAULT_DICE[h]);
        }

        setLayout(new BorderLayout());
        add(boardPanel, BorderLayout.CENTER);
        add(buildSidePanel(), BorderLayout.EAST);
        add(buildToolbar(), BorderLayout.NORTH);

        boardPanel.setListener(new BoardPanel.Listener() {
            @Override
            public void onHexClicked(int hexIndex) { handleHexClick(hexIndex); }
            @Override
            public void onTownClicked(int townIndex) { handleTownClick(townIndex); }
            @Override
            public void onEdgeClicked(int[] edge) { handleEdgeClick(edge); }
        });
    }

    private void loadStrategyTable() {
        try {
            String path = StrategyTable.resolveDefaultPath();
            strategyTable = StrategyTable.load(path);
            strategyTablePath = path;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Could not find secondsettlement/strategies.csv (" + e.getMessage() + ").\n" +
                "It's looked for next to the app's own install folder, and in the current\n" +
                "working directory. Using no strategies for now.",
                "Strategy table not found", JOptionPane.WARNING_MESSAGE);
            strategyTable = new StrategyTable(new String[0], new double[0][]);
            strategyTablePath = null;
        }
        refreshStrategyArea();
        refreshStrategyLabel();
    }

    /** Updates the "Strategy table (...)" side-panel label with wherever it actually loaded from - a no-op before the side panel exists yet (first call, from the constructor). */
    private void refreshStrategyLabel() {
        if (strategyLabel == null) return;
        strategyLabel.setText("Strategy table (" + (strategyTablePath != null ? strategyTablePath : "not found") + ")");
    }

    private void refreshStrategyArea() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-3s %-45s %6s %5s %6s %6s %5s%n", "#", "Strategy", "Brick", "Ore", "Sheep", "Wheat", "Wood"));
        for (int i = 0; i < strategyTable.size(); i++) {
            double[] row = strategyTable.winConditionsResources[i];
            sb.append(String.format("%-3d %-45s %6.0f %5.0f %6.0f %6.0f %5.0f%n",
                i, strategyTable.names[i], row[0], row[1], row[2], row[3], row[4]));
        }
        strategyArea.setText(sb.toString());
    }

    // ---- toolbar ----

    private JPanel buildToolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Tool"));

        ButtonGroup group = new ButtonGroup();
        panel.add(makeToolButton("Edit hex (resource/dice)", Tool.EDIT_HEX, group, true));
        panel.add(makeToolButton("Mark our settlement", Tool.OUR_SETTLEMENT, group, false));
        panel.add(makeToolButton("Mark enemy settlement", Tool.ENEMY_SETTLEMENT, group, false));
        panel.add(makeToolButton("Mark our road", Tool.OUR_ROAD, group, false));
        panel.add(makeToolButton("Mark enemy road", Tool.ENEMY_ROAD, group, false));
        panel.add(makeToolButton("Erase", Tool.ERASE, group, false));

        JButton reset = new JButton("Reset to Default Board");
        reset.addActionListener(e -> resetBoard());
        panel.add(reset);

        JButton randomize = new JButton("Randomize Board");
        randomize.addActionListener(e -> randomizeBoard());
        panel.add(randomize);

        panel.add(forceDesertCenterCheckbox);

        return panel;
    }

    private JRadioButton makeToolButton(String label, Tool tool, ButtonGroup group, boolean selected) {
        JRadioButton button = new JRadioButton(label, selected);
        button.addActionListener(e -> currentTool = tool);
        group.add(button);
        return button;
    }

    private void resetBoard() {
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            boardPanel.setHex(h, DEFAULT_RESOURCES[h], DEFAULT_DICE[h]);
        }
        for (int t = 0; t < BoardGeometry.TOTAL_TOWNS; t++) boardPanel.setTownMark(t, BoardPanel.TownMark.NONE);
        for (int[] edge : BoardGeometry.EDGES) boardPanel.setRoadMark(edge[0], edge[1], BoardPanel.RoadMark.NONE);
        boardPanel.clearResultOverlay();
        resultsArea.setText("");
    }

    // shuffles the tiles into a fresh, still-valid standard board: the same fixed resource
    // counts (3 brick, 3 ore, 4 sheep, 4 wheat, 4 wood, 1 desert) and the same fixed set of
    // 18 dice numbers, just placed on random hexes - resource type and dice number are
    // shuffled independently so a resource doesn't always end up with the same number.
    // If forceDesertCenterCheckbox is checked, the desert is pinned to the center hex
    // (index 9) instead of being placed anywhere, and only the other 18 hexes are shuffled.
    private void randomizeBoard() {
        boolean forceCenter = forceDesertCenterCheckbox.isSelected();

        List<Integer> nonDesertResources = new ArrayList<>();
        for (int i = 0; i < 3; i++) nonDesertResources.add(0); // brick
        for (int i = 0; i < 3; i++) nonDesertResources.add(1); // ore
        for (int i = 0; i < 4; i++) nonDesertResources.add(2); // sheep
        for (int i = 0; i < 4; i++) nonDesertResources.add(3); // wheat
        for (int i = 0; i < 4; i++) nonDesertResources.add(4); // wood

        List<Integer> resources;
        if (forceCenter) {
            Collections.shuffle(nonDesertResources); // 18 tiles, one per non-center hex
            resources = new ArrayList<>();
            int cursor = 0;
            for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
                resources.add(h == CENTER_HEX_INDEX ? 5 : nonDesertResources.get(cursor++));
            }
        } else {
            resources = new ArrayList<>(nonDesertResources);
            resources.add(5); // desert can land anywhere, including the center
            Collections.shuffle(resources);
        }

        List<Integer> diceNumbers = new ArrayList<>(List.of(2, 3, 3, 4, 4, 5, 5, 6, 6, 8, 8, 9, 9, 10, 10, 11, 11, 12));
        Collections.shuffle(diceNumbers);

        int diceCursor = 0;
        for (int h = 0; h < BoardGeometry.TOTAL_HEXES; h++) {
            int resource = resources.get(h);
            int dice = resource == 5 ? 0 : diceNumbers.get(diceCursor++); // desert has no dice number
            boardPanel.setHex(h, resource, dice);
        }

        boardPanel.clearResultOverlay();
        resultsArea.setText("");
    }

    // ---- side panel ----

    private JPanel buildSidePanel() {
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(420, 0));

        side.add(buildLegend());

        strategyLabel = new JLabel("Strategy table");
        strategyLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        refreshStrategyLabel();
        side.add(strategyLabel);

        strategyArea.setEditable(false);
        strategyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane strategyScroll = new JScrollPane(strategyArea);
        strategyScroll.setPreferredSize(new Dimension(400, 130));
        strategyScroll.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        side.add(strategyScroll);

        JButton reloadStrategy = new JButton("Reload strategy table from disk");
        reloadStrategy.setAlignmentX(JButton.LEFT_ALIGNMENT);
        reloadStrategy.addActionListener(e -> loadStrategyTable());
        JPanel reloadWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        reloadWrap.add(reloadStrategy);
        side.add(reloadWrap);

        JButton run = new JButton("Find Best Settlement + Road");
        run.setFont(run.getFont().deriveFont(Font.BOLD, 14f));
        run.addActionListener(e -> runAlgorithm());
        JPanel runWrap = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runWrap.add(run);
        side.add(runWrap);

        JLabel resultsLabel = new JLabel("Result");
        resultsLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        side.add(resultsLabel);

        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane resultsScroll = new JScrollPane(resultsArea);
        resultsScroll.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));
        side.add(resultsScroll);

        return side;
    }

    private JPanel buildLegend() {
        JPanel legend = new JPanel(new GridLayout(0, 1));
        legend.setBorder(BorderFactory.createTitledBorder("Legend"));
        legend.add(legendRow(new Color(0x2E8B57), "Our settlement / our road"));
        legend.add(legendRow(new Color(0xB03A2E), "Enemy settlement / enemy road"));
        legend.add(legendRow(new Color(0xFFD700), "Recommended settlement / road"));
        legend.add(new JLabel("  Tip: after a run, hex/town color shows the heatmap score for the chosen strategy."));
        return legend;
    }

    private JPanel legendRow(Color color, String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JLabel swatch = new JLabel("■");
        swatch.setForeground(color);
        swatch.setFont(swatch.getFont().deriveFont(20f));
        row.add(swatch);
        row.add(new JLabel(text));
        return row;
    }

    // ---- click handlers ----

    private void handleHexClick(int hexIndex) {
        if (currentTool != Tool.EDIT_HEX) return;

        JComboBox<String> resourceBox = new JComboBox<>(BoardGeometry.RESOURCE_NAMES);
        resourceBox.setSelectedIndex(boardPanel.getHexResource(hexIndex));
        int currentDice = boardPanel.getHexDice(hexIndex);
        JSpinner diceSpinner = new JSpinner(new SpinnerNumberModel(currentDice == 0 ? 6 : currentDice, 2, 12, 1));

        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Resource:"));
        form.add(resourceBox);
        form.add(new JLabel("Dice number (ignored for desert):"));
        form.add(diceSpinner);

        int choice = JOptionPane.showConfirmDialog(this, form, "Edit Hex " + hexIndex,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            int resource = resourceBox.getSelectedIndex();
            int dice = resource == 5 ? 0 : (Integer) diceSpinner.getValue(); // desert has no dice number
            boardPanel.setHex(hexIndex, resource, dice);
        }
    }

    private void handleTownClick(int townIndex) {
        switch (currentTool) {
            case OUR_SETTLEMENT -> boardPanel.setTownMark(townIndex, BoardPanel.TownMark.OURS);
            case ENEMY_SETTLEMENT -> boardPanel.setTownMark(townIndex, BoardPanel.TownMark.ENEMY);
            case ERASE -> boardPanel.setTownMark(townIndex, BoardPanel.TownMark.NONE);
            default -> { /* other tools don't act on towns */ }
        }
    }

    private void handleEdgeClick(int[] edge) {
        switch (currentTool) {
            case OUR_ROAD -> boardPanel.setRoadMark(edge[0], edge[1], BoardPanel.RoadMark.OURS);
            case ENEMY_ROAD -> boardPanel.setRoadMark(edge[0], edge[1], BoardPanel.RoadMark.ENEMY);
            case ERASE -> boardPanel.setRoadMark(edge[0], edge[1], BoardPanel.RoadMark.NONE);
            default -> { /* other tools don't act on roads */ }
        }
    }

    // ---- run algorithm ----

    private void runAlgorithm() {
        if (strategyTable.size() == 0) {
            JOptionPane.showMessageDialog(this, "No strategies loaded - fix secondsettlement/strategies.csv and reload.",
                "Cannot run", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SecondSettlementSolver solver = new SecondSettlementSolver(
            boardPanel.getAllResources(), boardPanel.getAllDice(), strategyTable);

        ArrayList<Integer> ourSettlements = new ArrayList<>(boardPanel.getTownsMarked(BoardPanel.TownMark.OURS));
        ArrayList<Integer> enemySettlements = new ArrayList<>(boardPanel.getTownsMarked(BoardPanel.TownMark.ENEMY));
        ArrayList<int[]> enemyRoadEdges = new ArrayList<>(boardPanel.getEnemyRoadEdges());

        SecondSettlementSolver.SolveResult result = solver.solve(ourSettlements, enemySettlements, enemyRoadEdges);

        boardPanel.setResultOverlay(result.townOptLocationValues, result.tileHeatmapValues,
            result.bestTownIndex, result.fullDijkstraPath);

        StringBuilder sb = new StringBuilder();
        sb.append("Top strategy: ").append(result.strategyName).append("\n\n");

        if (result.bestTownIndex == -1) {
            sb.append("No available settlement spot was found - every town on the board is blocked.");
        } else {
            sb.append("Place settlement at town ").append(result.bestTownIndex).append(".\n");
            if (result.roadDirection == -1) {
                sb.append("No road could be routed from that settlement (it may be fully cut off).");
            } else {
                sb.append("Place road: ").append(result.bestTownIndex).append(" -> ").append(result.roadDirection).append(".\n\n");
                sb.append("Full planned path: ");
                for (int i = 0; i < result.fullDijkstraPath.length; i++) {
                    if (i > 0) sb.append(" -> ");
                    sb.append(result.fullDijkstraPath[i]);
                }
            }
        }
        resultsArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CatanBoardApp().setVisible(true));
    }
}
