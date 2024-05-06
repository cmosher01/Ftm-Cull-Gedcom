package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Slf4j
public class FamiliesManager {
    private final List<Family> rFamily = new ArrayList<>();

    public void read(final Connection conn, final IndividualsManager indis, final String nameTree) throws SQLException, IOException {
        val mapDbpkFamily = new HashMap<Integer, Family>();
        readFamilyParents(conn, indis, mapDbpkFamily, nameTree);
        readFamilyChildren(conn, indis, mapDbpkFamily);
    }

    public void postProcess() {
        removeEmptyFamilies();
        deduplicateFamilies();
        warnFamilyWithSameParents();
        addFamcFamsPointers();
    }

    public final List<Family> all() {
        return List.copyOf(new ArrayList<>(this.rFamily));
    }



    private void readFamilyParents(final Connection conn, final IndividualsManager indis, final Map<Integer, Family> mapDbpkFamily, final String nameTree) throws SQLException, IOException {
        try (
                val stmt = conn.prepareStatement(Util.sql("relationship"));
                val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val indi1 = indis.withRefn(rs.getString("refn1"));
                val indi2 = indis.withRefn(rs.getString("refn2"));
                val fami = new Family(indi1, indi2, nameTree);
                this.rFamily.add(fami);
                mapDbpkFamily.put(rs.getInt("dbpkRelationship"), fami);
            }
        }
    }

    private void readFamilyChildren(final Connection conn, final IndividualsManager indis, final Map<Integer, Family> mapDbpkFamily) throws SQLException, IOException {
        try (
                val stmt = conn.prepareStatement(Util.sql("child"));
                val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val fami = mapDbpkFamily.get(rs.getInt("dbfkFamily"));
                val indi = indis.withRefn(rs.getString("refn"));
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
