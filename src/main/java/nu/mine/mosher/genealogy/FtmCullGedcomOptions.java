package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.*;
import java.util.*;

@Slf4j
public final class FtmCullGedcomOptions {
    public boolean help = false;
    public final List<Path> pathIns = new ArrayList<>();

    public void help(final Optional<String> s) {
        log.trace("opt: help");
        this.help = true;
    }

    public void __(final Optional<String> s) {
        log.trace("arg: {}", s.isEmpty() ? "<NONE>" : s.get().isBlank() ? "<BLANK>" : s.get());
        if (s.isPresent() && !s.get().isBlank()) {
            this.pathIns.add(Paths.get(s.get()).toAbsolutePath().normalize());
        }
    }
}
