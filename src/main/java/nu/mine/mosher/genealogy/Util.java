package nu.mine.mosher.genealogy;

import lombok.*;
import org.sqlite.SQLiteConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Util {
    private static final Instant TIMESTAMP = Instant.now();
    public static final String timestamp =
        DateTimeFormatter.
        ofPattern("yyyyMMdd'T'HHmmssSSSX").
        withZone(ZoneOffset.UTC).
        format(TIMESTAMP);

    public static Connection conn(final Path path) throws SQLException {
        val config = new SQLiteConfig();
        config.setReadOnly(true);
        return DriverManager.getConnection("jdbc:sqlite:"+path, config.toProperties());
    }

    public static String sql(final String name) throws IOException {
        try (val in = FtmCullGedcom.class.getResourceAsStream(name+".sql")) {
            return new String(Objects.requireNonNull(in).readAllBytes(), StandardCharsets.US_ASCII);
        }
    }

    @SneakyThrows
    public static Path real(final Path path) {
        return path.toRealPath();
    }
}
