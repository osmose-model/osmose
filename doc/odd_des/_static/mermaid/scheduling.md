graph TD;

    id1(Incoming Flux) --> id2(School initialization)
    id2 --> id3(LTL update)
    id3 --> id4(Spatial Distribution)
    id4 --> id5(Mortality)
    id5 --> id6(Growth)
    id6 --> id7(Reproduction)
    id7 --> id8(Removing of dead schools)
    id8 -->id1

    classDef className fill:steelblue,stroke:black,stroke-width:3px,color:white
    class id1,id2,id3,id4,id5,id6,id7,id8 className;