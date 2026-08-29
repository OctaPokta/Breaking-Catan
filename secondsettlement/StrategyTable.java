package CatanAnalysis.secondsettlement;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

    /** Loads from the default path (secondsettlement/strategies.csv, relative to the working directory). */
    public static StrategyTable loadDefault() throws IOException {
        return load(DEFAULT_PATH);
    }

    public int size() {
        return this.names.length;
    }
}
