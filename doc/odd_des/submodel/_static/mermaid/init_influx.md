    graph TD;

    id00("Loop over species")

    id0{"Parameter<br>
        <i>flux.incoming.byDt.byAge.file.sp#</i> exists?"}

    id1(Read input file)
    id2(Compute centerred age)
    id3("Compute centerred length<br>Growth.ageToLength(age)")

    id1bis(Read input file)
    id2bis(Compute centerred length)
    id3bis("Compute centerred length<br>Growth.lengthToAge(length)")

    id0 -->|yes| id1
    id1 --> id2
    id2 --> id3

    id0 -->|no| id1bis
    id1bis --> id2bis
    id2bis --> id3bis

    id00 --> id0