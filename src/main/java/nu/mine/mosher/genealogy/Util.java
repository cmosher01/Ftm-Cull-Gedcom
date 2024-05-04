package nu.mine.mosher.genealogy;

import lombok.val;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class Util {
    private static final Instant TIMESTAMP = Instant.now();
    static final String timestamp =
        DateTimeFormatter.
        ofPattern("yyyyMMdd'T'HHmmssSSSX").
        withZone(ZoneOffset.UTC).
        format(TIMESTAMP);

    static Connection conn(final Path path) throws SQLException {
            val config = new SQLiteConfig();
            config.setReadOnly(true);
            return DriverManager.getConnection("jdbc:sqlite:"+path, config.toProperties());
        }

    static String sql(final String name) throws IOException {
            try (val in = FtmCullGedcom.class.getResourceAsStream(name+".sql")) {
                return new String(in.readAllBytes(), StandardCharsets.US_ASCII);
            }
        }
}
