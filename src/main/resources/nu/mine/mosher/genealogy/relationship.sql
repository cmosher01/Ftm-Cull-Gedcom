SELECT
    R.ID AS dbpkRelationship,
    Refn1.Text refn1,
    Refn2.Text refn2
FROM
    Relationship R LEFT OUTER JOIN
    Fact AS Refn1 ON (
        Refn1.LinkTableID = 5 AND
        Refn1.LinkID = R.Person1ID AND
        Refn1.Preferred > 0 AND
        Refn1.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Abbreviation = '_ID' LIMIT 1
        )
    ) LEFT OUTER JOIN
    Fact AS Refn2 ON (
        Refn2.LinkTableID = 5 AND
        Refn2.LinkID = R.Person2ID AND
        Refn2.Preferred > 0 AND
        Refn2.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Abbreviation = '_ID' LIMIT 1
        )
    )
