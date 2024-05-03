SELECT
    R.RelationshipID AS dbfkFamily,
    Refn.Text AS refn
FROM
    ChildRelationship R INNER JOIN
    Fact AS Refn ON (
        Refn.LinkTableID = 5 AND
        Refn.LinkID = R.PersonID AND
        Refn.Preferred > 0 AND
        Refn.FactTypeID IN (
            SELECT ID FROM FactType WHERE FactType.Abbreviation = '_ID' LIMIT 1
        )
    )
