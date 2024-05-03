package nu.mine.mosher.genealogy;

import lombok.*;

import java.util.random.RandomGenerator;

@RequiredArgsConstructor
public class GedcomUidGenerator {
    public static final int DEFAULT_LENGTH = 10;
    public static final String DEFAULT_ALPHABET = "0123456789bcdfghjkmnpqrstvwxz";

    private final int length;
    private final String alphabet;
    private final RandomGenerator rnd = RandomGenerator.of("SecureRandom");

    public GedcomUidGenerator() {
        this(DEFAULT_LENGTH, DEFAULT_ALPHABET);
    }

    public String generateIdWithAts() {
        return "@" + generateId() + "@";
    }

    public String generateId() {
        val sb = new StringBuilder(this.length);
        for (var i = 0; i < this.length; ++i) {
            val r = this.rnd.nextInt(this.alphabet.length());
            sb.appendCodePoint(this.alphabet.codePointAt(r));
        }
        return sb.toString();
    }
}
