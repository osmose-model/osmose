
    graph TD;

    id0(Loop over species)
    id1(Loop over size-classes)
    id2("Compute weight<br><i>W = species.computeWeight(L)</i>")
    id3(Compute abundance<br><i>A = B * 1e6 / W</i>)
    id4{<i>A < nSchool?</i>}

    id5(Create <i>1</i> school<br>Abundance <i>A</i>)
    id6(Create <i>nSchool</i> school<br>Abundance <i>A/nSchool</i>)

    id0 --> id1
    id1 --> id2
    id2 --> id3
    id3 --> id4
    id4 -->|yes| id5
    id4 -->|no| id6
