package nu.mine.mosher.genealogy;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.sql.*;
import java.util.*;

@Slf4j
public final class Individual {
    private static final GedcomUidGenerator idgen = new GedcomUidGenerator();

    final String gedid;
    final String refn;
    final Sex sex;
    final String gedname;
    final int yearBirth;
    final Place placeBirth;
    final int yearDeath;
    final Place placeDeath;
    final List<Family> rFams = new ArrayList<>();
    final List<Family> rFamc = new ArrayList<>();

    public Individual(final ResultSet rs) throws SQLException {
        this.gedid = idgen.generateId();
        this.refn = refnOrElseGuid(rs);
        this.sex = Sex.valueOf(rs.getString("sex"));
        this.gedname = rs.getString("gedname");
        this.yearBirth = rs.getInt("yearBirth");
        this.placeBirth = Place.fromFtmPlace(rs.getString("placeBirth"));
        this.yearDeath = rs.getInt("yearDeath");
        this.placeDeath = Place.fromFtmPlace(rs.getString("placeDeath"));
    }

    private static String refnOrElseGuid(final ResultSet rs) throws SQLException {
        val refn = rs.getString("refn");
        if (Objects.nonNull(refn) && !refn.isBlank()) {
            return refn;
        }
        return rs.getString("guidPerson");
    }

    @Override
    public String toString() {
        return "Individual{" +
            "gedid='" + gedid + '\'' +
            ", refn='" + refn + '\'' +
            ", sex=" + sex +
            ", gedname='" + gedname + '\'' +
            ", yearBirth=" + yearBirth +
            ", placeBirth=" + placeBirth +
            ", yearDeath=" + yearDeath +
            ", placeDeath=" + placeDeath +
            '}';
    }

    @Override
    public boolean equals(final Object object) {
        return
            object instanceof Individual that &&
            this.refn.equals(that.refn);
    }

    public void addFams(final Family family) {
        if (this.rFams.contains(family)) {
            log.warn("Adding same family multiple times as spouse of: {}", this.refn);
        }
        this.rFams.add(family);
    }

    public void addFamc(final Family family) {
        if (this.rFamc.contains(family)) {
            log.warn("Adding same family multiple times as child of: {}", this.refn);
        }
        this.rFamc.add(family);
    }
}
