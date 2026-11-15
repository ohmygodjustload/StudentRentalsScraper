/**
 * Runs the bed/bath audit on the latest raw scrape, writes a CSV report,
 * and prints summary counts. Read-only with respect to listing data.
 *
 * @author Andrew Peirce
 */
package main;

import clean.BedBathAudit;
import clean.BedBathAudit.DescriptionMatch;
import clean.BedBathAudit.Row;
import clean.BedBathAudit.TitleVsField;
import clean.LandlordAddressAudit;
import clean.LandlordAddressAudit.Check;
import models.Apartment;
import utils.JsonUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;

public final class AuditCommand {

    private static final String[] HEADER = {
            "id", "url", "title", "bedBathField",
            "titleBeds", "fieldBeds", "descriptionBeds", "bedTitleVsField", "bedDescription",
            "titleBaths", "fieldBaths", "descriptionBaths", "bathTitleVsField", "bathDescription",
            "descriptionPhrases", "rawDescription",
            "titleLandlord", "fieldLandlord", "landlordCheck",
            "titleAddress", "breadcrumbAddress", "addressCheck"
    };

    private final OutputPaths paths;
    private final PrintStream out;

    public AuditCommand(OutputPaths paths) {
        this(paths, System.out);
    }

    public AuditCommand(OutputPaths paths, PrintStream out) {
        this.paths = paths;
        this.out = out;
    }

    /**
     * @return the CSV report path
     */
    public Path run() throws IOException {
        Path raw = paths.findLatest("listings_scraped", "json", paths.rawDir());
        List<Apartment> apartments = JsonUtils.fromJson(raw.toString());
        out.println("[Audit] Loaded " + apartments.size() + " listings from: " + raw);

        long withTitle = apartments.stream().filter(a -> a.getRawTitle() != null).count();
        long withDescription = apartments.stream().filter(a -> a.getRawDescription() != null).count();
        if (withDescription == 0) {
            out.println("[Audit] Warning: no listings have rawDescription. Re-scrape with the current version first.");
        }

        List<Row> rows = new BedBathAudit().audit(apartments);
        List<LandlordAddressAudit.Result> consistency = apartments.stream()
                .map(LandlordAddressAudit::check)
                .toList();
        Files.createDirectories(paths.auditDir());
        Path csv = paths.bedBathAuditCsv();
        writeCsv(rows, consistency, csv);

        out.println("[Audit] Listings with title: " + withTitle + ", with description: " + withDescription);
        printSummary("Bedrooms", rows, true);
        printSummary("Bathrooms", rows, false);
        printLandlordAddressSummary(apartments, consistency);
        out.println("[Audit] Report saved: " + csv);
        return csv;
    }

    private void printLandlordAddressSummary(List<Apartment> apartments, List<LandlordAddressAudit.Result> results) {
        Map<Check, Integer> landlord = new EnumMap<>(Check.class);
        Map<Check, Integer> address = new EnumMap<>(Check.class);
        List<String> landlordConflicts = new ArrayList<>();
        List<String> addressConflicts = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            LandlordAddressAudit.Result r = results.get(i);
            String id = apartments.get(i).getId();
            landlord.merge(r.landlord(), 1, Integer::sum);
            address.merge(r.address(), 1, Integer::sum);
            if (r.landlord() == Check.CONFLICT) {
                landlordConflicts.add(id);
            }
            if (r.address() == Check.CONFLICT) {
                addressConflicts.add(id);
            }
        }

        out.println();
        out.println("Landlord - title vs Landlord field:");
        landlord.forEach((k, v) -> out.printf("  %-16s %d%n", k.name().toLowerCase(), v));
        if (!landlordConflicts.isEmpty()) {
            out.println("  conflict ids: " + String.join(", ", landlordConflicts));
        }

        out.println();
        out.println("Address - title vs breadcrumb address:");
        address.forEach((k, v) -> out.printf("  %-16s %d%n", k.name().toLowerCase(), v));
        if (!addressConflicts.isEmpty()) {
            out.println("  conflict ids: " + String.join(", ", addressConflicts));
        }
    }

    private void printSummary(String label, List<Row> rows, boolean beds) {
        Map<TitleVsField, Integer> titleVsField = new EnumMap<>(TitleVsField.class);
        Map<DescriptionMatch, Integer> descOnConflict = new EnumMap<>(DescriptionMatch.class);
        Map<DescriptionMatch, Integer> descOnAgree = new EnumMap<>(DescriptionMatch.class);

        for (Row row : rows) {
            TitleVsField tvf = beds ? row.bedTitleVsField() : row.bathTitleVsField();
            DescriptionMatch desc = beds ? row.bedDescription() : row.bathDescription();
            titleVsField.merge(tvf, 1, Integer::sum);
            if (tvf == TitleVsField.CONFLICT) {
                descOnConflict.merge(desc, 1, Integer::sum);
            } else if (tvf == TitleVsField.AGREE) {
                descOnAgree.merge(desc, 1, Integer::sum);
            }
        }

        out.println();
        out.println(label + " - title vs Bed/Bath field:");
        titleVsField.forEach((k, v) -> out.printf("  %-16s %d%n", k.name().toLowerCase(), v));
        if (!descOnConflict.isEmpty()) {
            out.println("  When title and field conflict, the description:");
            descOnConflict.forEach((k, v) -> out.printf("    %-20s %d%n", k.name().toLowerCase(), v));
        }
        if (!descOnAgree.isEmpty()) {
            out.println("  When title and field agree, the description:");
            descOnAgree.forEach((k, v) -> out.printf("    %-20s %d%n", k.name().toLowerCase(), v));
        }
    }

    static void writeCsv(List<Row> rows, List<LandlordAddressAudit.Result> consistency, Path file) throws IOException {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
             CSVWriter csv = new CSVWriter(writer)) {
            csv.writeNext(HEADER);
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                LandlordAddressAudit.Result check = consistency.get(i);
                Apartment apt = row.apartment();
                csv.writeNext(new String[] {
                        apt.getId(),
                        nullToEmpty(apt.getUrl()),
                        nullToEmpty(row.titleText()),
                        nullToEmpty(row.fieldText()),
                        format(row.title().beds()),
                        format(row.field().beds()),
                        formatAll(row.description().beds()),
                        row.bedTitleVsField().name().toLowerCase(),
                        row.bedDescription().name().toLowerCase(),
                        format(row.title().baths()),
                        format(row.field().baths()),
                        formatAll(row.description().baths()),
                        row.bathTitleVsField().name().toLowerCase(),
                        row.bathDescription().name().toLowerCase(),
                        String.join("; ", row.description().phrases()),
                        nullToEmpty(apt.getRawDescription()),
                        nullToEmpty(check.titleLandlord()),
                        nullToEmpty(check.fieldLandlord()),
                        check.landlord().name().toLowerCase(),
                        nullToEmpty(check.titleAddress()),
                        nullToEmpty(check.fieldAddress()),
                        check.address().name().toLowerCase()
                });
            }
        }
    }

    private static String format(Double value) {
        if (value == null) {
            return "";
        }
        return value == Math.rint(value) ? String.valueOf(value.longValue()) : value.toString();
    }

    private static String formatAll(Collection<Double> values) {
        return values.stream().map(AuditCommand::format).collect(Collectors.joining("|"));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
