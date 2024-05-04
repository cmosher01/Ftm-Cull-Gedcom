package nu.mine.mosher.genealogy;

import lombok.val;

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

    public boolean isEmpty() {
        return this.husband.isEmpty() && this.wife.isEmpty() && this.children.size() == 0;
    }

    // cheat a little on checking if two families are the same:
    // just check references, regarless of wife/husb/child
    public boolean hasSameMembers(final Family that) {
        return this.all().equals(that.all());
    }

    public boolean hasSameParents(final Family that) {
        return
            this.husband.isPresent() && that.husband.isPresent() &&
            this.husband.equals(that.husband) &&
            this.wife.isPresent() && that.wife.isPresent() &&
            this.wife.equals(that.wife);
    }

    private Set<Individual> all() {
        val s = new HashSet<>(this.children);
        if (this.husband.isPresent()) {
            s.add(this.husband.get());
        }
        if (this.wife.isPresent()) {
            s.add(this.wife.get());
        }
        return Set.copyOf(s);
    }

    public String display() {
        val sb = new StringBuilder();
        for (val i : all()) {
            sb.append(i.refn);
            sb.append(',');
        }
        sb.delete(sb.length()-1, sb.length());
        return sb.toString();
    }

    public void addFamxPointers() {
        if (this.husband.isPresent()) {
            this.husband.get().addFams(this);
        }
        if (this.wife.isPresent()) {
            this.wife.get().addFams(this);
        }
        for (val i : this.children) {
            i.addFamc(this);
        }
    }
}
