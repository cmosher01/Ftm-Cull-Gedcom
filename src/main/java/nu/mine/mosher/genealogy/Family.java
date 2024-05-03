package nu.mine.mosher.genealogy;

import java.util.*;

public class Family {
    private static final GedcomUidGenerator idgen = new GedcomUidGenerator();

    final String gedid;
    final Optional<Individual> husband;
    final Optional<Individual> wife;
    final List<Individual> children = new ArrayList<>();

    public Family(final Optional<Individual> indi1, final Optional<Individual> indi2) {
        this.gedid = idgen.generateId();
        /*
         *  1\2  M        F        U        empty
         *  M    x        H=1,W=2  H=1,W=2  H=1,W=2
         *  F    H=2,W=1  x        H=2,W=1  H=2,W=1
         *  U    H=2,W=1  H=1,W=2  x        x
         *  e    H=2,W=1  H=1,W=2  x        x
         */
        if (f(indi1) || m(indi2)) {
            this.husband = indi2;
            this.wife = indi1;
        } else {
            this.husband = indi1;
            this.wife = indi2;
        }
    }

    private static boolean f(final Optional<Individual> i) {
        return i.isPresent() && i.get().sex == Sex.FEMALE;
    }

    private static boolean m(final Optional<Individual> i) {
        return i.isPresent() && i.get().sex == Sex.MALE;
    }

    public void addChild(final Individual child) {
        this.children.add(child);
    }

    // cheat a little on checking if two families are the same:
    // just check references, regarless of wife/husb/child
    public boolean hasSameMembers(final Family that) {
        return this.all().equals(that.all());
    }

    private Set<Individual> all() {
        final Set<Individual> s = new HashSet<>(this.children);
        if (this.husband.isPresent()) {
            s.add(this.husband.get());
        }
        if (this.wife.isPresent()) {
            s.add(this.wife.get());
        }
        return Set.copyOf(s);
    }
}
