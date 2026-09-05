package CatanAnalysis.secondsettlement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Loads the human-editable strategy (win condition) table from a plain CSV file, so the
 * resource requirements for each strategy don't have to live hardcoded in Main.java.
 * File format (see strategies.csv): "name,brick,ore,sheep,wheat,wood" per row, '#' comments
 * and blank lines are skipped. A 6th "total" column is appended internally set to 0 - it is
 * only ever a scratch/output value that WinConditions.importantIndex() overwrites at runtime.
 */
public class StrategyTable {

    public static final String DEFAULT_PATH = "secondsettlement/strategies.csv";

    public final String[] names;
    public final double[][] winConditionsResources; // rows x 6 (brick, ore, sheep, wheat, wood, total)

    public StrategyTable(String[] names, double[][] winConditionsResources) {
        this.names = names;
        this.winConditionsResources = winConditionsResources;
    }

    /** Loads the strategy table from the given CSV path. */
    public static StrategyTable load(String path) throws IOException {
        ArrayList<String> loadedNames = new ArrayList<>();
        ArrayList<double[]> loadedRows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (trimmed.toLowerCase().startsWith("name,")) continue; // header row

                String[] parts = trimmed.split(",");
                if (parts.length < 6) {
                    throw new IOException("Malformed strategy row (expected name,brick,ore,sheep,wheat,wood): " + line);
                }

                loadedNames.add(parts[0].trim());
                double[] row = new double[6]; // last column ("total") is a scratch value, starts at 0
                for (int i = 0; i < 5; i++) {
                    row[i] = Double.parseDouble(parts[i + 1].trim());
                }
                loadedRows.add(row);
            }
        }

        if (loadedRows.isEmpty()) {
            throw new IOException("No strategy rows found in " + path);
        }

        String[] namesArray = loadedNames.toArray(new String[0]);
        double[][] rowsArray = loadedRows.toArray(new double[0][]);
        return new StrategyTable(namesArray, rowsArray);
    }

    /**
     * Loads from the default location, trying two places in order:
     *<OL>
     * <LI>{@link #DEFAULT_PATH} relative to the current working directory - the original
     *     behavior, for anyone running {@code java} from the CatanAnalysis project root (dev
     *     workflow, and the benchmark harness).
     * <LI>{@link #DEFAULT_PATH} relative to the directory this code is actually running
     *     from - the app's own install directory when packaged (see {@code build-installer}
     *     scripts), or its compiled-classes directory in dev - so a double-clicked packaged
     *     app finds its own bundled copy of the CSV regardless of the OS's notion of "current
     *     directory" at launch, exactly as if it had been run from the project root.
     *</OL>
     * Either way this is a plain file on disk, not a bundled classpath resource, so the
     * existing "reload strategy table from disk" workflow (edit the CSV as text, see the
     * effect without rebuilding) keeps working the same way for a packaged install too.
     * @see #resolveDefaultPath()
     */
    public static StrategyTable loadDefault() throws IOException {
        return load(resolveDefaultPath());
    }

    /**
     * Finds where {@link #loadDefault()} will actually read from - see there for the search
     * order. Exposed separately so callers (e.g. the GUI's error dialog) can tell the user
     * exactly which file to edit.
     * @throws IOException if the file isn't found in either location
     */
    public static String resolveDefaultPath() throws IOException {
        File cwdFile = new File(DEFAULT_PATH);
        if (cwdFile.isFile()) return cwdFile.getPath();

        try {
            File codeLocation = new File(StrategyTable.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
            // codeLocation is a .jar file when packaged/run from a jar, or the compiled
            // "classes" output directory when run directly from .class files in dev.
            File appDir = codeLocation.isFile() ? codeLocation.getParentFile() : codeLocation;
            File nearApp = new File(appDir, DEFAULT_PATH);
            if (nearApp.isFile()) return nearApp.getPath();
        } catch (URISyntaxException | NullPointerException | SecurityException ignored) {
            // fall through to the "not found" error below - resolving the code location is a
            // best-effort fallback, not something worth failing loudly over on its own.
        }

        throw new IOException("Could not find " + DEFAULT_PATH + " - looked relative to the "
            + "working directory and relative to the running application.");
    }

    public int size() {
        return this.names.length;
    }
}
