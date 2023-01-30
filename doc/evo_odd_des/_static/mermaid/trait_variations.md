graph TD;

    id1(Population dynamics<br>birth and mortality rates)
    id2(Emerging traits<br>Growth, maturation, reproduction)
    id3(Bioenergetics metabolic rates)
    id4(Process traits<br>Imax, r, m0, m1)
    id5(Multiple diploid loci)

    id5 --> |Expression| id4
    id4 --> |Phenotypic plasticity| id3
    id3 --> id2
    id2 --> id1

    id1 --> |<b>Evolution</b>| id5

    ida(Selective pressures<br>Predation, fishing, climate)
    idb(Macro-environment<br>food, temperature, oxygen)
    idc(Phenotypic expression<br>noise dominance, epistasis<br> micro-environment)

    ida --> id1
    idb --> id3
    idc --> id4

    classDef className fill:lightblue,stroke:black,stroke-width:3px,color:black
    class ida,idb,idc className;

    classDef className2 fill:lightred,stroke:black,stroke-width:3px,color:black
    class id1,id2,id3,id4,id5 className2;
