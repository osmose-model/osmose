graph TD;

    id0(Reading parameters) --> id1
    id1(Loop over species) --> id2(Loop over size-class)
    id2 --> id3(Compute relative biomass)
    id3 --> id4(Loop over number of schools)
    id4 --> id5(Random draft of lenght)
    id5 --> id6(Create school)
    id6 --> id4

    id6 --> id2
    id6 --> id1