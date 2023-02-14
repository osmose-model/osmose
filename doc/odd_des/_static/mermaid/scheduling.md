%%{init: {'theme': 'dark', 'themeVariables': { 'fontSize': '13px'}}}%%
graph TD;

    id0(Simu. initialisation) --> id1
    id1(Incoming Flux) --> id2(School initialization)
    id2 --> id3(LTL update)
    id3 --> id4(Spatial Distribution)
    id4 --> id5(Mortality)
    id5 --> id6(Growth)
    id6 --> id7(Reproduction)
    id7 --> id8(Update indicators)
    id8 --> id9(Removing of dead schools)
    id9 --> id10(Merging new and old schoolsets)
    id10 -->id1

    classDef className fill:lightblue,stroke:black,stroke-width:3px,color:black
    class id0,id1,id2,id3,id4,id5,id6,id7,id8,id9,id10 className;

    classDef className2 fill:firebrick,stroke:black,stroke-width:3px,color:white
    class id5 className2;