SELECT
    Person.ID AS pkid,
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
        SUBSTR(HEX(PersonGUID),21)) AS id,
    Refn.Text AS refn,
    Name.Text AS name,
    STRFTIME('%Y', Birth.Date/512) AS dateBirth,
    STRFTIME('%Y', Death.Date/512) AS dateDeath
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
    Fact AS Name ON (
        Name.LinkTableID = 5 AND
        Name.LinkID = Person.ID AND
        Name.Preferred > 0 AND
        Name.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Name = 'Name' LIMIT 1
        )
    )
