package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

@Slf4j
public class FtmCull {
    private final Map<String, Individual> mapRefnIndividual = new HashMap<>();
    private final List<Family> rFamily = new ArrayList<>();

    public void read(final List<Path> pathIns) throws SQLException, IOException {
        for (val pathIn : pathIns) {
            try (val conn = Util.conn(pathIn)) {
                readIndividuals(conn);
                readFamilies(conn);
            }
            removeEmptyFamilies();
        }
        deduplicateFamilies();
        warnFamilyWithSameParents();
        addFamcFamsPointers();
    }

    public List<Family> families() {
        return List.copyOf(this.rFamily);
    }

    public Map<String, Individual> mapRefnIndividual() {
        return Map.copyOf(this.mapRefnIndividual);
    }



    private void readIndividuals(final Connection conn) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("individual"));
            val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val indi = new Individual(rs);
                log.debug("{}", indi);

                if (this.mapRefnIndividual.containsKey(indi.refn)) {
                    log.info("Duplicate individual: {}", indi.refn);
                } else {
                    this.mapRefnIndividual.put(indi.refn, indi);
                }
            }
        }
    }

    private void readFamilies(final Connection conn) throws SQLException, IOException {
        val mapDbpkFamily = new HashMap<Integer, Family>();
        readFamilyParents(conn, mapDbpkFamily);
        readFamilyChildren(conn, mapDbpkFamily);
    }

    private void readFamilyParents(final Connection conn, final Map<Integer, Family> mapDbpkFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("relationship"));
            val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val indi1 = Optional.ofNullable(this.mapRefnIndividual.get(rs.getString("refn1")));
                val indi2 = Optional.ofNullable(this.mapRefnIndividual.get(rs.getString("refn2")));
                val fami = new Family(indi1, indi2);
                this.rFamily.add(fami);
                mapDbpkFamily.put(rs.getInt("dbpkRelationship"), fami);
            }
        }
    }

    private void readFamilyChildren(final Connection conn, final Map<Integer, Family> mapDbpkFamily) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("child"));
            val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val fami = mapDbpkFamily.get(rs.getInt("dbfkFamily"));
                val indi = Optional.ofNullable(this.mapRefnIndividual.get(rs.getString("refn")));
                if (indi.isPresent()) {
                    fami.addChild(indi.get());
                } else {
                    log.error("Invalid REFN: {}", rs.getString("refn"));
                }
            }
        }
    }

    private void removeEmptyFamilies() {
        val rEmpty = this.rFamily.stream().filter(Family::isEmpty).toList();
        this.rFamily.removeAll(rEmpty);
    }

    private void deduplicateFamilies() {
        val dups = new TreeSet<Integer>();

        for (int i = 0; i < this.rFamily.size(); ++i) {
            for (int j = i+1; j < this.rFamily.size(); ++j) {
                val a = this.rFamily.get(i);
                val b = this.rFamily.get(j);
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
            this.rFamily.remove(x);
        }
    }

    private void warnFamilyWithSameParents() {
        for (int i = 0; i < this.rFamily.size(); ++i) {
            for (int j = i + 1; j < this.rFamily.size(); ++j) {
                val a = this.rFamily.get(i);
                val b = this.rFamily.get(j);
                if (a.hasSameParents(b)) {
                    log.warn("Two families have same parents: {}", a.display());
                    log.warn("Two families have same parents: {}", b.display());
                }
            }
        }
    }

    private void addFamcFamsPointers() {
        this.rFamily.forEach(Family::addFamxPointers);
    }
}
