/**
 * Rescores the latest Final listings without calling any APIs, so scoring weights and
 * thresholds can be tuned in seconds. Writes a new timestamped Final JSON and CSV.
 *
 * @author Andrew Peirce
 */
package main;

import models.Apartment;
import scoring.DealScorer;
import scoring.ScoringConfig;
import utils.CsvUtils;
import utils.JsonUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public final class ScoreCommand {

    private final OutputPaths paths;
    private final PrintStream out;

    public ScoreCommand(OutputPaths paths) {
        this(paths, System.out);
    }

    public ScoreCommand(OutputPaths paths, PrintStream out) {
        this.paths = paths;
        this.out = out;
    }

    /**
     * @return the new Final JSON path
     */
    public Path run() throws IOException {
        Path latest = paths.findLatest("listings_final", "json", paths.finalDir());
        List<Apartment> apartments = JsonUtils.fromJson(latest.toString());
        out.println("[Score] Loaded " + apartments.size() + " listings from: " + latest);

        new DealScorer(ScoringConfig.load()).score(apartments);

        Path json = paths.finalJson();
        Path csv = paths.finalCsv();
        JsonUtils.toJson(apartments, json.toString());
        CsvUtils.toCsv(apartments, csv.toString());
        DealScorer.printSummary(apartments, out);
        out.println("[Score] Final JSON saved: " + json);
        out.println("[Score] Final CSV saved: " + csv);
        return json;
    }
}
