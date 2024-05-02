SELECT
    Person.ID AS dbpkPerson,
    LOWER(
        SUBSTR(HEX(PersonGUID), 7,2)||
        SUBSTR(HEX(PersonGUID), 5,2)||
        SUBSTR(HEX(PersonGUID), 3,2)||
        SUBSTR(HEX(PersonGUID), 1,2)||'-'||
        SUBSTR(HEX(PersonGUID),11,2)||
        SUBSTR(HEX(PersonGUID), 9,2)||'-'||
        SUBSTR(HEX(PersonGUID),15,2)||
        SUBSTR(HEX(PersonGUID),13,2)||'-'||
        SUBSTR(HEX(PersonGUID),17,4)||'-'||
        SUBSTR(HEX(PersonGUID),21)) AS guidPerson,
    Refn.Text AS refn,
    CASE Person.Sex WHEN 0 THEN 'MALE' WHEN 1 THEN 'FEMALE' ELSE 'UNKNOWN' END AS sex,
    GedName.Text AS gedname,
    STRFTIME('%Y', Birth.Date/512) AS yearBirth,
    BirthPlace.Name AS placeBirth,
    STRFTIME('%Y', Death.Date/512) AS yearDeath,
    DeathPlace.Name AS placeDeath
FROM
    Person LEFT OUTER JOIN
    Fact AS Refn ON (
        Refn.LinkTableID = 5 AND
        Refn.LinkID = Person.ID AND
        Refn.Preferred > 0 AND
        Refn.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Abbreviation = '_ID' LIMIT 1
        )
    ) LEFT OUTER JOIN
    Fact AS Birth ON (
        Birth.LinkTableID = 5 AND
        Birth.LinkID = Person.ID AND
        Birth.Preferred > 0 AND
        Birth.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Name = 'Birth' LIMIT 1
        )
    ) LEFT OUTER JOIN
    Fact AS Death ON (
        Death.LinkTableID = 5 AND
        Death.LinkID = Person.ID AND
        Death.Preferred > 0 AND
        Death.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Name = 'Death' LIMIT 1
        )
    ) LEFT OUTER JOIN
    Fact AS GedName ON (
        GedName.LinkTableID = 5 AND
        GedName.LinkID = Person.ID AND
        GedName.Preferred > 0 AND
        GedName.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Name = 'Name' LIMIT 1
        )
    ) LEFT OUTER JOIN
    Place AS BirthPlace ON (
        BirthPlace.ID = Birth.PlaceID
    ) LEFT OUTER JOIN
    Place AS DeathPlace ON (
        DeathPlace.ID = Death.PlaceID
    )
