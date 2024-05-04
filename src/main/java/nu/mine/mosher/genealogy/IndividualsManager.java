package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@Slf4j
public class IndividualsManager {
    private final Map<String, Individual> mapRefnIndividual = new HashMap<>();

    public void read(final Connection conn) throws SQLException, IOException {
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

    public Optional<Individual> withRefn(final String refn) {
        return Optional.ofNullable(this.mapRefnIndividual.get(refn));
    }

    public List<Individual> all() {
        return List.copyOf(new ArrayList<>(this.mapRefnIndividual.values()));
    }
}
