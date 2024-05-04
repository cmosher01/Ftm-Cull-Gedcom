package nu.mine.mosher.genealogy;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nu.mine.mosher.gnopt.Gnopt;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

@Slf4j
public final class FtmCullGedcom {
    private FtmCullGedcom() {
        throw new UnsupportedOperationException();
    }

    @SneakyThrows
    public static void main(final String... args) {
        log.trace("processing command line arguments...");
        val opts = Gnopt.process(FtmCullGedcomOptions.class, args);

        if (opts.help) {
            System.out.println("Usage: ftm-cull-gedcom input.ftm [...]");
            logPaths(opts.pathIns);
        } else {
            if (opts.pathIns.isEmpty()) {
                log.warn("No input files specified. Use --help for usage information.");
            } else {
                logPaths(opts.pathIns);
                val pathsInReal = new ArrayList<Path>();
                for (val pathIn : opts.pathIns) {
                    pathsInReal.add(pathIn.toRealPath()); // throws
                }

                val pathOut = Path.of(Util.timestamp+".ged").toAbsolutePath().normalize();
                log.info("Will cull to output file:");
                log.info("    {}", pathOut);

                cull(pathsInReal, pathOut);
            }
        }

        System.out.flush();
        System.err.flush();
    }

    private static void logPaths(final List<Path> pathIns) {
        if (!pathIns.isEmpty()) {
            log.info("Will cull from these files:");
            for (var pathIn : pathIns) {
                String msg;
                try {
                    pathIn = pathIn.toRealPath();
                    msg = "found: "+pathIn;
                } catch (final Exception e) {
                    msg = e.toString();
                }
                log.info("    {}", msg);
            }
        }
    }

    private static void cull(final Collection<Path> pathIns, final Path pathOut) throws SQLException, IOException {
        val mapRefnIndividual = new HashMap<String, Individual>();
        val rFamily = new ArrayList<Family>();
        for (val pathIn : pathIns) {
            try (val conn = Util.conn(pathIn)) {
                readIndividuals(conn, mapRefnIndividual);
                readFamilies(conn, mapRefnIndividual, rFamily);
            }
            removeEmptyFamilies(rFamily);
        }
        deduplicateFamilies(rFamily);
        warnFamilyWithSameParents(rFamily);
        addFamcFamsPointers(rFamily);
        new CullGedcom().writeGedcom(mapRefnIndividual, rFamily, pathOut);
    }

    private static void readIndividuals(final Connection conn, final Map<String, Individual> mapRefnIndividual) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("individual"));
            val rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                val indi = new Individual(rs);
                log.trace("{}", indi);

                if (mapRefnIndividual.containsKey(indi.refn)) {
                    log.info("Duplicate individual: {}", indi.refn);
                } else {
                    mapRefnIndividual.put(indi.refn, indi);
                }
            }
        }
    }

    private static void readFamilies(final Connection conn, final Map<String, Individual> mapRefnIndividual, final List<Family> rFamily) throws SQLException, IOException {
        val mapDbpkFamily = new HashMap<Integer, Family>();
        readFamilyParents(conn, mapRefnIndividual, mapDbpkFamily, rFamily);
        readFamilyChildren(conn, mapRefnIndividual, mapDbpkFamily);
    }

    private static void readFamilyParents(final Connection conn, final Map<String, Individual> mapRefnIndividual, final Map<Integer, Family> mapDbpkFamily, final List<Family> rFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("relationship"));
            val rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                val indi1 = Optional.ofNullable(mapRefnIndividual.get(rs.getString("refn1")));
                val indi2 = Optional.ofNullable(mapRefnIndividual.get(rs.getString("refn2")));
                val fami = new Family(indi1, indi2);
                rFamily.add(fami);
                mapDbpkFamily.put(rs.getInt("dbpkRelationship"), fami);
            }
        }
    }

    private static void readFamilyChildren(final Connection conn, final Map<String, Individual> mapRefnIndividual, final Map<Integer, Family> mapDbpkFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("child"));
            val rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                val fami = mapDbpkFamily.get(rs.getInt("dbfkFamily"));
                val indi = Optional.ofNullable(mapRefnIndividual.get(rs.getString("refn")));
                if (indi.isPresent()) {
                    fami.addChild(indi.get());
                } else {
                    log.error("Invalid REFN: {}", rs.getString("refn"));
                }
            }
        }
    }

    private static void removeEmptyFamilies(final List<Family> rFamily) {
        val rEmpty = rFamily.stream().filter(Family::isEmpty).toList();
        rFamily.removeAll(rEmpty);
    }

    private static void deduplicateFamilies(final List<Family> rFamily) {
        val dups = new TreeSet<Integer>();

        for (int i = 0; i < rFamily.size(); ++i) {
            for (int j = i+1; j < rFamily.size(); ++j) {
                val a = rFamily.get(i);
                val b = rFamily.get(j);
                if (a.hasSameMembers(b)) {
                    log.info("Duplicate family: {}", a.display());
                    dups.add(j);
                }
            }
        }

        val backwards = dups.descendingIterator();
        while (backwards.hasNext()) {
            final int x = backwards.next();
            log.info("removing family at index: {}", x);
            rFamily.remove(x);
        }
    }

    private static void warnFamilyWithSameParents(final List<Family> rFamily) {
        for (int i = 0; i < rFamily.size(); ++i) {
            for (int j = i + 1; j < rFamily.size(); ++j) {
                val a = rFamily.get(i);
                val b = rFamily.get(j);
                if (a.hasSameParents(b)) {
                    log.warn("Two families have same parents: {}", a.display());
                    log.warn("Two families have same parents: {}", b.display());
                }
            }
        }
    }

    private static void addFamcFamsPointers(final List<Family> rFamily) {
        rFamily.forEach(Family::addFamxPointers);
    }
}
