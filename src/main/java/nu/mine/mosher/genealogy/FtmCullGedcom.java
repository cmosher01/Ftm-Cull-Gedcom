package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import nu.mine.mosher.gnopt.Gnopt;

import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
public final class FtmCullGedcom {
    private static final Instant TIMESTAMP = Instant.now();
    private static final String timestamp =
        DateTimeFormatter.
        ofPattern("yyyyMMdd'T'HHmmssSSSX").
        withZone(ZoneOffset.UTC).
        format(TIMESTAMP);



    public static void main(final String... args) throws Gnopt.InvalidOption {
        log.trace("processing command line arguments...");
        val opts = Gnopt.process(FtmCullGedcomOptions.class, args);

        if (opts.help) {
            System.out.println("Usage: ftm-cull-gedcom input.ftm [...]");
            if (!opts.pathIns.isEmpty()) {
                System.out.println("paths specified:");
                for (var pathIn : opts.pathIns) {
                    String msg;
                    try {
                        pathIn = pathIn.toRealPath();
                        msg = "found: "+pathIn;
                    } catch (final Exception e) {
                        msg = e.toString();
                    }
                    System.out.printf("    %s\n", msg);
                }
            }
        } else {
            if (opts.pathIns.isEmpty()) {
                System.err.println("No input files specified. Use --help for usage information.");
            } else {
                val pathOut = Path.of(timestamp+".ged");
                System.out.println("Will cull to output file: "+pathOut);
                System.out.println("Will cull from these files:");
                for (val pathIn : opts.pathIns) {
                    System.out.println("    "+pathIn);
                }
                cull(opts.pathIns, pathOut);
            }
        }
        System.out.flush();
        System.err.flush();
    }

    private static void cull(final Collection<Path> pathIns, final Path pathOut) {
        for (val pathIn : pathIns) {

        }
    }



    private FtmCullGedcom() {
        throw new UnsupportedOperationException();
    }
}
