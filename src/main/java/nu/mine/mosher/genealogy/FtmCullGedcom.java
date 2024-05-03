package nu.mine.mosher.genealogy;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;
import nu.mine.mosher.gnopt.Gnopt;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;
import static nu.mine.mosher.logging.Jul.log;

@Slf4j
public final class FtmCullGedcom {
    private static final Instant TIMESTAMP = Instant.now();
    private static final String timestamp =
        DateTimeFormatter.
        ofPattern("yyyyMMdd'T'HHmmssSSSX").
        withZone(ZoneOffset.UTC).
        format(TIMESTAMP);



    @SneakyThrows
    public static void main(final String... args) {
        log.trace("processing command line arguments...");
        val opts = Gnopt.process(FtmCullGedcomOptions.class, args);

        if (opts.help) {
            System.out.println("Usage: ftm-cull-gedcom input.ftm [...]");
            if (!opts.pathIns.isEmpty()) {
                log.info("paths specified:");
                for (var pathIn : opts.pathIns) {
                    String msg;
                    try {
                        pathIn = pathIn.toRealPath();
                        msg = "found: "+pathIn;
                    } catch (final Exception e) {
                        msg = e.toString();
                    }
                    log.info("    %s\n", msg);
                }
            }
        } else {
            if (opts.pathIns.isEmpty()) {
                log.warn("No input files specified. Use --help for usage information.");
            } else {
                val pathOut = Path.of(timestamp+".ged");
                log.info("Will cull to output file: "+pathOut);
                log.info("Will cull from these files:");
                for (val pathIn : opts.pathIns) {
                    log.info("    "+pathIn);
                }
                cull(opts.pathIns, pathOut);
            }
        }

        System.out.flush();
        System.err.flush();
    }

    private static void cull(final Collection<Path> pathIns, final Path pathOut) throws SQLException, IOException {
        val mapRefnIndividual = new HashMap<String, Individual>();
        val rFamily = new ArrayList<Family>();
        for (val pathIn : pathIns) {
            val mapDbpkFamily = new HashMap<Integer, Family>();
            try (val conn = DriverManager.getConnection("jdbc:sqlite:"+pathIn.toString())) {
                readIndividuals(conn, mapRefnIndividual);
                readFamilyParents(conn, mapRefnIndividual, mapDbpkFamily, rFamily);
                readFamilyChildren(conn, mapRefnIndividual, mapDbpkFamily);
            }
        }
        deduplicateFamilies(rFamily);
        writeGedcom(mapRefnIndividual, rFamily);
    }

    private static void readIndividuals(Connection conn, HashMap<String, Individual> mapRefnIndividual) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(sql("individual"));
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

    private static void readFamilyParents(Connection conn, HashMap<String, Individual> mapRefnIndividual, HashMap<Integer, Family> mapDbpkFamily, ArrayList<Family> rFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(sql("relationship"));
            val rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                val refn1 = rs.getString("refn1");
                val indi1 = Optional.ofNullable(mapRefnIndividual.get(refn1));
                val refn2 = rs.getString("refn2");
                val indi2 = Optional.ofNullable(mapRefnIndividual.get(refn2));
                val fami = new Family(indi1, indi2);
                rFamily.add(fami);
                val dbpkRelationship = rs.getInt("dbpkRelationship");
                mapDbpkFamily.put(dbpkRelationship, fami);
            }
        }
    }

    private static void readFamilyChildren(Connection conn, HashMap<String, Individual> mapRefnIndividual, HashMap<Integer, Family> mapDbpkFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(sql("child"));
            val rs = stmt.executeQuery();
        ) {
            while (rs.next()) {
                val dbfkFamily = rs.getInt("dbfkFamily");
                val fami = mapDbpkFamily.get(dbfkFamily);
                val refn = rs.getString("refn");
                val indi = Optional.ofNullable(mapRefnIndividual.get(refn));
                if (indi.isPresent()) {
                    fami.addChild(indi.get());
                } else {
                    log.error("Invalid REFN: {}", refn);
                }
            }
        }
    }


    private static void deduplicateFamilies(ArrayList<Family> rFamily) {
        // TODO
    }




    private static void writeGedcom(HashMap<String, Individual> mapRefnIndividual, ArrayList<Family> rFamily) throws IOException {
        final GedcomTree tree = new GedcomTree();

        final TreeNode<GedcomLine> head = new TreeNode<>(GedcomLine.createHeader());
        tree.getRoot().addChild(head);

        head.addChild(new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.CHAR)));
        tree.setCharset(StandardCharsets.UTF_8);

        final TreeNode<GedcomLine> gedc = new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.GEDC));
        head.addChild(gedc);
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.VERS, "5.5.1")));
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.FORM, "LINEAGE-LINKED")));

        head.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.SOUR, "github.com/cmosher01/ftm-cull-gedcom")));
        head.addChild(new TreeNode<>(GedcomLine.createPointer(1, GedcomTag.SUBM, "M0")));

        final TreeNode<GedcomLine> subm = new TreeNode<>(GedcomLine.createEmptyId("M0", GedcomTag.SUBM));
        tree.getRoot().addChild(subm);

        subm.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.NAME, System.getProperty("user.name"))));



        for (val indi : mapRefnIndividual.values()) {
            val lnIndi = GedcomLine.createEmptyId(indi.gedid, GedcomTag.INDI);
            val ndIndi = new TreeNode<>(lnIndi);
            tree.getRoot().addChild(ndIndi);

            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.REFN, indi.refn)));
            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.SEX, gedsex(indi.sex))));
            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.NAME, indi.gedname)));

            if (indi.yearBirth != 0 || !indi.placeBirth.description().isBlank()) {
                val lnBirth = lnIndi.createChild(GedcomTag.BIRT, "");
                val ndBirth = new TreeNode<>(lnBirth);
                ndIndi.addChild(ndBirth);
                if (indi.yearBirth != 0) {
                    ndBirth.addChild(new TreeNode<>(lnBirth.createChild(GedcomTag.DATE, "" + indi.yearBirth)));
                }
                if (!indi.placeBirth.description().isBlank()) {
                    ndBirth.addChild(new TreeNode<>(lnBirth.createChild(GedcomTag.PLAC, indi.placeBirth.description())));
                }
            }

            if (indi.yearDeath != 0 || !indi.placeDeath.description().isBlank()) {
                val lnDeath = lnIndi.createChild(GedcomTag.DEAT, "");
                val ndDeath = new TreeNode<>(lnDeath);
                ndIndi.addChild(ndDeath);
                if (indi.yearDeath != 0) {
                    ndDeath.addChild(new TreeNode<>(lnDeath.createChild(GedcomTag.DATE, "" + indi.yearDeath)));
                }
                if (!indi.placeDeath.description().isBlank()) {
                    ndDeath.addChild(new TreeNode<>(lnDeath.createChild(GedcomTag.PLAC, indi.placeDeath.description())));
                }
            }
            // TODO FAMC, FAMS
        }



        for (val fami : rFamily) {
            val lnFami = GedcomLine.createEmptyId(fami.gedid, GedcomTag.FAM);
            val ndFami = new TreeNode<>(lnFami);
            tree.getRoot().addChild(ndFami);

            if (fami.husband.isPresent()) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.HUSB, "").replacePointer(fami.husband.get().gedid)));
            }
            if (fami.wife.isPresent()) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.WIFE, "").replacePointer(fami.wife.get().gedid)));
            }
            for (val child : fami.children) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.CHIL, "").replacePointer(child.gedid)));
            }
        }



        tree.getRoot().addChild(new TreeNode<>(GedcomLine.createTrailer()));

        try (final BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(Paths.get(ts()+".ged"), WRITE, CREATE_NEW))) {
            Gedcom.writeFile(tree, out);
        }
    }

    private static String gedsex(final Sex sex) {
        return switch (sex) {
            case MALE -> "M";
            case FEMALE -> "F";
            case UNKNOWN -> "U";
        };
    }

    private static final Instant NOW = Instant.now();
    private static String ts() {
        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSSX").withZone(ZoneOffset.UTC);
        return fmt.format(NOW);
    }

    private static String sql(final String name) throws IOException {
        try (val in = FtmCullGedcom.class.getResourceAsStream(name+".sql")) {
            return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    private FtmCullGedcom() {
        throw new UnsupportedOperationException();
    }
}
