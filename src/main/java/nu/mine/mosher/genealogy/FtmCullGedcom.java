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
        val opts = Gnopt.process(FtmCullGedcomOptions.class, args);

        if (opts.help) {
            System.out.println("Usage: ftm-cull-gedcom input.ftm [...]");
            logPaths(opts.pathIns);
        } else if (opts.pathIns.isEmpty()) {
            log.warn("No input files specified. Use --help for usage information.");
        } else {
            logPaths(opts.pathIns);
            val pathsInReal = opts.pathIns.stream().map(Util::real).toList(); // throws if file not found

            val pathOut = Path.of(Util.timestamp+".ged").toAbsolutePath().normalize();
            log.info("Will cull to output file:");
            log.info("    {}", pathOut);

            cull(pathsInReal, pathOut);
        }

        System.out.flush();
        System.err.flush();
    }

    private static void logPaths(final List<Path> paths) {
        if (!paths.isEmpty()) {
            log.info("Will cull from these files:");
            for (var path : paths) {
                String msg;
                try {
                    msg = "found: "+path.toRealPath();
                } catch (final Exception e) {
                    msg = e.toString();
                }
                log.info("    {}", msg);
            }
        }
    }

    private static void cull(final List<Path> pathIns, final Path pathOut) throws IOException, SQLException {
        val indis = new IndividualsManager();
        val fams = new FamiliesManager();

        val input = new FtmCull(indis, fams);
        input.read(pathIns);

        val output = new CullGedcom(indis.all(), fams.all());
        output.write(pathOut);
    }
}
