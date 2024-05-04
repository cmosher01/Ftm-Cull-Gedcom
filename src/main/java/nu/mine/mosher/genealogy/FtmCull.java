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
            try (val conn = Util.conn(pathIn)) {
                this.indis.read(conn);
                this.fams.read(conn, this.indis);
            }
        }
        this.fams.postProcess();
    }
}
