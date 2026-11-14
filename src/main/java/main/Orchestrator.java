/**
 * Entry point for the scrape → clean → geocode → enrich pipeline.
 *
 * @author Andrew Peirce
 */
package main;

import api.ApiConfig;
import api.ApiException;
import api.CensusGeocoderAPI;
import api.CrimeDataAPI;
import api.GeocodioAPI;
import api.TravelTimeAPI;
import api.WalkScoreAPI;
import clean.ApartmentPostProcessor;
import clean.DataCleaner;
import geocoding.DataMerger;
import geocoding.GeoUtils;
import models.Apartment;
import scraper.StudentRentalsScraper;
import utils.CsvUtils;
import utils.JsonUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Scanner;

public class Orchestrator {

    public static void main(String[] args) {
        String[] effectiveArgs = args;
        if (args == null || args.length == 0) {
            try {
                PipelineOptions defaults = PipelineOptions.parse(new String[0]);
                boolean interactive = System.console() != null;
                InteractiveMenu menu = new InteractiveMenu(
                        new InputStreamReader(System.in, StandardCharsets.UTF_8),
                        System.out,
                        interactive);
                effectiveArgs = menu.run(defaults);
                if (effectiveArgs == null) {
                    return;
                }
            } catch (IOException e) {
                System.err.println("[ERROR] Menu failed: " + e.getMessage());
                System.exit(1);
                return;
            }
        }

        PipelineOptions options;
        try {
            options = PipelineOptions.parse(effectiveArgs);
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] " + e.getMessage());
            System.err.println();
            System.err.println(CliHelp.overview());
            System.exit(1);
            return;
        }

        if (options.isNoArgs()) {
            // Should have been handled above; keep as safety net.
            System.out.println(CliHelp.overview());
            return;
        }

        if (options.getMode() == PipelineOptions.Mode.HELP || options.isHelpRequested()) {
            System.out.print(CliHelp.forCommand(options.getHelpTopic()));
            return;
        }

        if (options.getMode() == PipelineOptions.Mode.STATUS) {
            OutputPaths paths = new OutputPaths(options.getOutputDir());
            new StatusCommand(paths).run();
            return;
        }

        Instant start = Instant.now();
        OutputPaths paths = new OutputPaths(options.getOutputDir());

        try {
            paths.ensureDirectories();
            run(options, paths);
            System.out.println("[Orchestrator] Pipeline complete in "
                    + Duration.between(start, Instant.now()).toSeconds() + "s");
        } catch (Exception e) {
            System.err.println("[ERROR] Pipeline failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void run(PipelineOptions options, OutputPaths paths) throws IOException {
        List<Apartment> apartments = null;

        switch (options.getMode()) {
            case FULL_PIPELINE, SCRAPE_AND_CLEAN_ONLY -> {
                apartments = scrape(options);
                JsonUtils.toJson(apartments, paths.scrapedJson().toString());
                System.out.println("[Scraper] Raw data saved: " + paths.scrapedJson());

                apartments = postProcessAndClean(apartments);
                saveCleaned(apartments, paths);

                if (options.getMode() == PipelineOptions.Mode.SCRAPE_AND_CLEAN_ONLY) {
                    System.out.println("[Orchestrator] scrape-clean complete.");
                    return;
                }
            }
            case CLEAN_ONLY -> {
                Path scraped = paths.findLatest("listings_scraped", "json", paths.rawDir());
                apartments = JsonUtils.fromJson(scraped.toString());
                System.out.println("[Cleaner] Loaded scraped data from: " + scraped);
                apartments = postProcessAndClean(apartments);
                saveCleaned(apartments, paths);
                return;
            }
            case GEOCODE_ONLY, RESUME_FROM_CLEANED -> {
                Path cleaned = paths.findLatest("listings_cleaned", "json", paths.cleanedDir());
                apartments = JsonUtils.fromJson(cleaned.toString());
                System.out.println("[Orchestrator] Loaded cleaned data from: " + cleaned);
            }
            case APIS_ONLY -> {
                Path geocoded = paths.findLatest("listings_geocoded", "json", paths.geocodedDir());
                apartments = JsonUtils.fromJson(geocoded.toString());
                System.out.println("[Orchestrator] Loaded geocoded data from: " + geocoded);
            }
            default -> throw new IllegalStateException("Unsupported mode: " + options.getMode());
        }

        if (options.getMode() == PipelineOptions.Mode.FULL_PIPELINE
                || options.getMode() == PipelineOptions.Mode.RESUME_FROM_CLEANED
                || options.getMode() == PipelineOptions.Mode.GEOCODE_ONLY) {
            apartments = geocode(apartments, options, paths);
            JsonUtils.toJson(apartments, paths.geocodedJson().toString());
            System.out.println("[Geocoder] Geocoded data saved: " + paths.geocodedJson());

            if (options.getMode() == PipelineOptions.Mode.GEOCODE_ONLY) {
                System.out.println("[Orchestrator] geocode complete.");
                return;
            }
        }

        if (options.getMode() == PipelineOptions.Mode.FULL_PIPELINE
                || options.getMode() == PipelineOptions.Mode.RESUME_FROM_CLEANED
                || options.getMode() == PipelineOptions.Mode.APIS_ONLY) {
            apartments = enrich(apartments, options, paths);
            crimeReport(paths);
            JsonUtils.toJson(apartments, paths.finalJson().toString());
            CsvUtils.toCsv(apartments, paths.finalCsv().toString());
            System.out.println("[APIs] Final data saved: " + paths.finalJson());
            System.out.println("[APIs] Final CSV saved: " + paths.finalCsv());
        }
    }

    private static List<Apartment> scrape(PipelineOptions options) {
        StudentRentalsScraper scraper = new StudentRentalsScraper(
                options.getMinId(), options.getMaxId(), options.getDelayMs());
        List<Apartment> apartments = scraper.scrape();
        System.out.println("[Scraper] Apartments scraped: " + apartments.size());
        return apartments;
    }

    private static List<Apartment> postProcessAndClean(List<Apartment> apartments) {
        ApartmentPostProcessor postProcessor = new ApartmentPostProcessor();
        apartments = postProcessor.process(apartments);
        System.out.println("[PostProcessor] Title fallbacks applied");

        DataCleaner cleaner = new DataCleaner();
        apartments = cleaner.clean(apartments);
        System.out.println("[Cleaner] Data cleaning complete");
        return apartments;
    }

    private static void saveCleaned(List<Apartment> apartments, OutputPaths paths) {
        JsonUtils.toJson(apartments, paths.cleanedJson().toString());
        CsvUtils.toCsv(apartments, paths.cleanedCsv().toString());
        System.out.println("[Cleaner] Cleaned data saved: " + paths.cleanedJson());
        System.out.println("[Cleaner] Cleaned data saved: " + paths.cleanedCsv());
    }

    private static List<Apartment> geocode(
            List<Apartment> apartments, PipelineOptions options, OutputPaths paths) throws IOException {
        for (Apartment apt : apartments) {
            apt.clearGeocode();
        }

        PipelineOptions.GeocoderChoice choice = options.getGeocoder();
        ApiConfig geocodio = ApiConfig.load("Geocodio");
        boolean hasGeocodio = geocodio.has("api.key");

        if (choice == PipelineOptions.GeocoderChoice.AUTO) {
            choice = hasGeocodio
                    ? PipelineOptions.GeocoderChoice.GEOCODIO
                    : PipelineOptions.GeocoderChoice.CENSUS;
        }

        switch (choice) {
            case GEOCODIO -> {
                if (!hasGeocodio) {
                    throw new IOException("Geocodio selected but api.key is not configured in config/Geocodio.properties");
                }
                System.out.println("[Geocoder] Using Geocodio API");
                GeocodioAPI api = new GeocodioAPI(geocodio.get("api.key").orElseThrow());
                apartments = api.geocode(apartments);
            }
            case CENSUS -> {
                System.out.println("[Geocoder] Using US Census Geocoder (no key required)");
                CensusGeocoderAPI api = new CensusGeocoderAPI();
                apartments = api.geocode(apartments);
            }
            case CSV -> {
                System.out.println("[Geocoder] Manual CSV mode");
                System.out.println("[Geocoder] Place geocodio.csv in: " + paths.geocodedDir());
                System.out.println("[Geocoder] Press Enter to continue...");
                try (Scanner sc = new Scanner(System.in)) {
                    sc.nextLine();
                }
                Path csv = paths.geocodioCsv();
                if (!Files.isRegularFile(csv)) {
                    throw new IOException("Expected Geocodio CSV at " + csv);
                }
                DataMerger merger = new DataMerger();
                apartments = merger.mergeGeocodioData(apartments, csv.toString());
            }
            default -> throw new IllegalStateException("Unhandled geocoder choice: " + choice);
        }

        fillDistances(apartments, options.getCampusLat(), options.getCampusLng());
        long withCoords = apartments.stream().filter(Apartment::hasCoordinates).count();
        System.out.println("[Geocoder] Geocoding complete (" + withCoords + "/" + apartments.size()
                + " listings have coordinates)");
        return apartments;
    }

    static void fillDistances(List<Apartment> apartments, double campusLat, double campusLng) {
        for (Apartment apt : apartments) {
            if (apt.hasCoordinates()) {
                apt.setDistanceToCampus(GeoUtils.haversineMiles(
                        apt.getLatitude(), apt.getLongitude(), campusLat, campusLng));
            }
        }
    }

    private static List<Apartment> enrich(
            List<Apartment> apartments, PipelineOptions options, OutputPaths paths) {
        System.out.println("[APIs] Starting API enrichment...");
        for (Apartment apt : apartments) {
            apt.clearEnrichment();
        }

        ApiConfig travelConfig = ApiConfig.load("TravelTime");
        if (travelConfig.hasAll("api.key", "api.id")) {
            TravelTimeAPI travelTime = new TravelTimeAPI(
                    travelConfig.get("api.key").orElseThrow(),
                    travelConfig.get("api.id").orElseThrow(),
                    options.getCampusLat(),
                    options.getCampusLng(),
                    options.getTravelTimeMaxSeconds(),
                    options.getTravelTimeArrivalHour());
            try {
                apartments = travelTime.addTravelTimes(apartments);
                System.out.println("[APIs] Travel times added");
            } catch (ApiException e) {
                System.err.println("[APIs] TravelTime failed: " + e.getMessage());
            }
        } else {
            System.out.println("[APIs] TravelTime credentials missing; skipping");
        }
        JsonUtils.toJson(apartments, paths.afterTravelJson().toString());
        System.out.println("[APIs] Saved after travel API: " + paths.afterTravelJson());

        ApiConfig walkConfig = ApiConfig.load("WalkScore");
        if (walkConfig.has("api.key")) {
            WalkScoreAPI walkScore = new WalkScoreAPI(walkConfig.get("api.key").orElseThrow());
            try {
                apartments = walkScore.addWalkScores(apartments);
                System.out.println("[APIs] Walk scores added");
            } catch (ApiException e) {
                System.err.println("[APIs] WalkScore failed: " + e.getMessage());
            }
        } else {
            System.out.println("[APIs] WalkScore credentials missing; skipping");
        }
        JsonUtils.toJson(apartments, paths.afterWalkJson().toString());
        System.out.println("[APIs] Saved after walk API: " + paths.afterWalkJson());

        return apartments;
    }

    private static void crimeReport(OutputPaths paths) {
        ApiConfig crimeConfig = ApiConfig.load("CrimeData");
        if (!crimeConfig.hasAll("api.key", "api.ori")) {
            System.out.println("[APIs] CrimeData credentials missing; skipping crime report");
            return;
        }
        try {
            CrimeDataAPI crimeData = new CrimeDataAPI(
                    crimeConfig.get("api.key").orElseThrow(),
                    crimeConfig.get("api.ori").orElseThrow());
            crimeData.fetchAndWriteReport(paths.crimeReportJson());
            System.out.println("[APIs] Crime report saved: " + paths.crimeReportJson());
        } catch (ApiException e) {
            System.err.println("[APIs] CrimeData failed: " + e.getMessage());
        }
    }
}
