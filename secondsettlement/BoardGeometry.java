package CatanAnalysis.secondsettlement;

/**
 * Static pixel-free layout data for a standard Catan board: hex centers, town corner
 * positions, and road edges, all in "model units" (a hex has size 1.0) - the GUI scales
 * and translates these into screen pixels. Town positions and edges were derived once via
 * the same corner-elimination solver used by testing/visualize_road_scenarios.py (pure
 * geometry from Towns.hexToTownsMap - see that script's _solve_town_positions()/
 * _solve_physical_edges() for the derivation), then copied here as fixed constants so the
 * GUI doesn't need to redo that elimination at runtime.
 */
public class BoardGeometry {

    public static final int TOTAL_HEXES = 19;
    public static final int TOTAL_TOWNS = 54;

    // hex index -> model (x, y) center, pointy-top layout, rows of 3,4,5,4,3 (hex 9 = desert, dead center)
    public static final double[] HEX_X = new double[TOTAL_HEXES];
    public static final double[] HEX_Y = new double[TOTAL_HEXES];

    static {
        int[][] rows = { {0, 1, 2}, {3, 4, 5, 6}, {7, 8, 9, 10, 11}, {12, 13, 14, 15}, {16, 17, 18} };
        double horiz = Math.sqrt(3);
        double vert = 1.5;
        for (int r = 0; r < rows.length; r++) {
            int[] row = rows[r];
            int len = row.length;
            for (int i = 0; i < len; i++) {
                int hex = row[i];
                HEX_X[hex] = (i - (len - 1) / 2.0) * horiz;
                HEX_Y[hex] = -r * vert;
            }
        }
    }

    // town index (0-53) -> model (x, y) position, solved via corner elimination (see class doc)
    public static final double[] TOWN_X = {
        -1.7321, 0.0000, 2.5981, -2.5981, -0.8660, 0.8660, 1.7321, -2.5981, -0.8660, 0.8660,
        2.5981, -3.4641, -1.7321, 0.0000, 1.7321, 3.4641, -3.4641, -1.7321, 0.0000, 1.7321,
        3.4641, -4.3301, -2.5981, -0.8660, 0.8660, 2.5981, 4.3301, -4.3301, -2.5981, -0.8660,
        0.8660, 2.5981, 4.3301, -3.4641, -1.7321, 0.0000, 1.7321, 3.4641, -3.4641, -1.7321,
        0.0000, 1.7321, 3.4641, -2.5981, -0.8660, 0.8660, 2.5981, -2.5981, -0.8660, 0.8660,
        1.7321, -1.7321, -0.0000, 2.5981
    };

    public static final double[] TOWN_Y = {
        1.0000, 1.0000, 0.5000, 0.5000, 0.5000, 0.5000, 1.0000, -0.5000, -0.5000, -0.5000,
        -0.5000, -1.0000, -1.0000, -1.0000, -1.0000, -1.0000, -2.0000, -2.0000, -2.0000, -2.0000,
        -2.0000, -3.5000, -2.5000, -2.5000, -2.5000, -2.5000, -3.5000, -2.5000, -3.5000, -3.5000,
        -3.5000, -3.5000, -2.5000, -4.0000, -4.0000, -4.0000, -4.0000, -4.0000, -5.0000, -5.0000,
        -5.0000, -5.0000, -5.0000, -5.5000, -5.5000, -5.5000, -5.5000, -6.5000, -6.5000, -6.5000,
        -7.0000, -7.0000, -7.0000, -6.5000
    };

    // 72 road edges {townA, townB} - agrees with RoadConnectionGraph.adjacencyMatrix4Towns
    public static final int[][] EDGES = {
        {0,3}, {0,4}, {1,4}, {1,5}, {2,6}, {2,10}, {3,7}, {4,8}, {5,6}, {5,9},
        {7,11}, {7,12}, {8,12}, {8,13}, {9,13}, {9,14}, {10,14}, {10,15}, {11,16}, {12,17},
        {13,18}, {14,19}, {15,20}, {16,22}, {16,27}, {17,22}, {17,23}, {18,23}, {18,24}, {19,24},
        {19,25}, {20,25}, {20,32}, {21,27}, {21,33}, {22,28}, {23,29}, {24,30}, {25,31}, {26,32},
        {26,37}, {28,33}, {28,34}, {29,34}, {29,35}, {30,35}, {30,36}, {31,36}, {31,37}, {33,38},
        {34,39}, {35,40}, {36,41}, {37,42}, {38,43}, {39,43}, {39,44}, {40,44}, {40,45}, {41,45},
        {41,46}, {42,46}, {43,47}, {44,48}, {45,49}, {46,53}, {47,51}, {48,51}, {48,52}, {49,50},
        {49,52}, {50,53}
    };

    // each row = the 6 (or fewer, on the edge of the board) town indices touching that hex,
    // copied from Towns.hexToTownsMap so the GUI's resource/dice editor can highlight them
    public static final int[][] HEX_TO_TOWNS = {
        {0, 3, 4, 7, 8, 12}, {1, 4, 5, 8, 9, 13}, {2, 5, 6, 9, 10, 14},
        {7, 11, 12, 16, 17, 22}, {8, 12, 13, 17, 18, 23}, {9, 13, 14, 18, 19, 24}, {10, 14, 15, 19, 20, 25},
        {16, 21, 22, 27, 28, 33}, {17, 22, 23, 28, 29, 34}, {18, 23, 24, 29, 30, 35}, {19, 24, 25, 30, 31, 36}, {20, 25, 26, 31, 32, 37},
        {28, 33, 34, 38, 39, 43}, {29, 34, 35, 39, 40, 44}, {30, 35, 36, 40, 41, 45}, {31, 36, 37, 41, 42, 46},
        {39, 43, 44, 47, 48, 51}, {40, 44, 45, 48, 49, 52}, {41, 45, 46, 49, 50, 53}
    };

    public static final String[] RESOURCE_NAMES = { "Brick", "Ore", "Sheep", "Wheat", "Wood", "Desert" };

    private BoardGeometry() {} // static-only class
}
