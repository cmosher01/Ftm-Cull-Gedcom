package nu.mine.mosher.genealogy;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

@Slf4j
public class CullGedcom {
    private final GedcomTree tree = new GedcomTree();
    private final Map<String, Individual> mapRefnIndividual;
    private final List<Family> rFamily;

    public CullGedcom(final Map<String, Individual> mapRefnIndividual, final List<Family> rFamily) {
        this.mapRefnIndividual = Map.copyOf(mapRefnIndividual);
        this.rFamily = List.copyOf(rFamily);
    }

    public void write(final Path pathOut) throws IOException {
        buildHeader();
        buildwriteIndividuals();
        buildwriteFamilies();
        buildTrailer();

        writeGedcom(pathOut);
    }

    private void writeGedcom(final Path pathOut) throws IOException {
        try (val out = new BufferedOutputStream(Files.newOutputStream(pathOut, WRITE, CREATE_NEW))) {
            Gedcom.writeFile(this.tree, out);
        }
    }

    private void buildHeader() {
        val head = new TreeNode<>(GedcomLine.createHeader());
        this.tree.getRoot().addChild(head);

        head.addChild(new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.CHAR)));
        this.tree.setCharset(StandardCharsets.UTF_8);

        val gedc = new TreeNode<>(GedcomLine.createEmpty(1, GedcomTag.GEDC));
        head.addChild(gedc);
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.VERS, "5.5.1")));
        gedc.addChild(new TreeNode<>(GedcomLine.create(2, GedcomTag.FORM, "LINEAGE-LINKED")));

        head.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.SOUR, "github.com/cmosher01/ftm-cull-gedcom")));
        head.addChild(new TreeNode<>(GedcomLine.createPointer(1, GedcomTag.SUBM, "M0")));

        val subm = new TreeNode<>(GedcomLine.createEmptyId("M0", GedcomTag.SUBM));
        this.tree.getRoot().addChild(subm);

        subm.addChild(new TreeNode<>(GedcomLine.create(1, GedcomTag.NAME, System.getProperty("user.name"))));
    }

    private void buildwriteIndividuals() {
        for (val indi : this.mapRefnIndividual.values()) {
            val lnIndi = GedcomLine.createEmptyId(indi.gedid, GedcomTag.INDI);
            val ndIndi = new TreeNode<>(lnIndi);
            this.tree.getRoot().addChild(ndIndi);

            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.REFN, indi.refn)));
            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.SEX, indi.sex.ged())));
            ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.NAME, indi.gedname)));

            if (indi.yearBirth != 0 || !indi.placeBirth.description().isBlank()) {
                val lnBirth = lnIndi.createChild(GedcomTag.BIRT, "");
                val ndBirth = new TreeNode<>(lnBirth);
                ndIndi.addChild(ndBirth);
                if (indi.yearBirth != 0) {
                    ndBirth.addChild(new TreeNode<>(lnBirth.createChild(GedcomTag.DATE, "" + indi.yearBirth)));
                }
                if (!indi.placeBirth.description().isBlank()) {
                    ndBirth.addChild(new TreeNode<>(lnBirth.createChild(GedcomTag.PLAC, indi.placeBirth.description())));
                }
            }

            if (indi.yearDeath != 0 || !indi.placeDeath.description().isBlank()) {
                val lnDeath = lnIndi.createChild(GedcomTag.DEAT, "");
                val ndDeath = new TreeNode<>(lnDeath);
                ndIndi.addChild(ndDeath);
                if (indi.yearDeath != 0) {
                    ndDeath.addChild(new TreeNode<>(lnDeath.createChild(GedcomTag.DATE, "" + indi.yearDeath)));
                }
                if (!indi.placeDeath.description().isBlank()) {
                    ndDeath.addChild(new TreeNode<>(lnDeath.createChild(GedcomTag.PLAC, indi.placeDeath.description())));
                }
            }

            for (val fam : indi.rFamc) {
                ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.FAMC, "").replacePointer(fam.gedid)));
            }
            for (val fam : indi.rFams) {
                ndIndi.addChild(new TreeNode<>(lnIndi.createChild(GedcomTag.FAMS, "").replacePointer(fam.gedid)));
            }
        }
    }

    private void buildwriteFamilies() {
        for (val fami : this.rFamily) {
            val lnFami = GedcomLine.createEmptyId(fami.gedid, GedcomTag.FAM);
            val ndFami = new TreeNode<>(lnFami);
            this.tree.getRoot().addChild(ndFami);

            if (fami.husband.isPresent()) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.HUSB, "").replacePointer(fami.husband.get().gedid)));
            }
            if (fami.wife.isPresent()) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.WIFE, "").replacePointer(fami.wife.get().gedid)));
            }
            for (val child : fami.children) {
                ndFami.addChild(new TreeNode<>(lnFami.createChild(GedcomTag.CHIL, "").replacePointer(child.gedid)));
            }
        }
    }

    private void buildTrailer() {
        this.tree.getRoot().addChild(new TreeNode<>(GedcomLine.createTrailer()));
    }
}
