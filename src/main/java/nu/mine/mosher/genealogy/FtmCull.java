package nu.mine.mosher.genealogy;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class FtmCull {
    final private IndividualsManager indis;
    final private FamiliesManager fams;

    public void read(final List<Path> pathIns) throws SQLException, IOException {
        for (val pathIn : pathIns) {
            val tree = pathIn.getFileName().toString();
            try (val conn = Util.conn(pathIn)) {
                this.indis.read(conn, tree);
                this.fams.read(conn, this.indis, tree);
            }
        }
        this.indis.logDups();
        this.fams.postProcess();
    }
}
