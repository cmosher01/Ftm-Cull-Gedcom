package nu.mine.mosher.genealogy;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Sex {
    MALE("M"),
    FEMALE("F"),
    UNKNOWN("U");

    private final String ged;

    public String ged() {
        return this.ged;
    }
}
