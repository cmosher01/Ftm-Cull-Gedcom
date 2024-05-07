package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Slf4j
public class IndividualsManager {
    private final Map<String, Individual> mapRefnIndividual = new HashMap<>();
    private final Map<String, Individual> mapRefnDup = new HashMap<>();

    public void read(final Connection conn, final String nameTree) throws SQLException, IOException {
        try (
            val stmt = conn.prepareStatement(Util.sql("individual"));
            val rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                val indi = new Individual(rs);
                indi.addTree(nameTree);
                log.debug("{}", indi);

                if (this.mapRefnIndividual.containsKey(indi.refn)) {
                    val dup = this.mapRefnIndividual.get(indi.refn);
                    dup.addTree(nameTree);
                    if (!this.mapRefnDup.containsKey(dup.refn)) {
                        this.mapRefnDup.put(dup.refn, dup);
                    }
                } else {
                    this.mapRefnIndividual.put(indi.refn, indi);
                }
            }
        }
    }

    public Optional<Individual> withRefn(final String refn) {
        return Optional.ofNullable(this.mapRefnIndividual.get(refn));
    }

    public List<Individual> all() {
        return List.copyOf(new ArrayList<>(this.mapRefnIndividual.values()));
    }

    public void logDups() {
        val r = new ArrayList<>(this.mapRefnDup.values());
        r.sort((a,b) -> a.trees().compareToIgnoreCase(b.trees()));

        log.info("Duplicate individuals:");
        for (val dup : r) {
            log.info("{}", dup.display());
        }
    }
}
