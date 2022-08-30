.. _movements:

Spatial distribution
+++++++++++++++++++++++++++++++++++++++++++++

At each time-step, the spatial distribution of fished is randomly changed based either based on distribution maps or based on random walk processes.

The displacement mode is defined through the :samp:`movement.distribution.method.sp#`, whose values are either :samp:`random` or :samp:`maps`.


Random distribution
#########################

.. ipython:: python

    import os
    import subprocess
    cwd = os.getcwd()

    fpath = "odd_des/submodel/_static/plot_random_dis.py"
    subprocess.call(["python", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

For random distribution, two parameters need to be defined:

.. table:: Random distribution parameters
    :align: center

    .. csv-table::
        :delim: =
        :header: Parameter, Description

        movement.distribution.ncell.sp# = Number of cells in which the species is allowed to move. If undefined, the whole domain is used.
        movement.randomwalk.range.sp# = Number of adjacent cells a species can use during random walk (foraging)

At the beginning of the simulation, during the initialization process, the areas where the schools
can live is initialized:

- If ``ncell`` is not set or is equal to the total number of ocean cells, this areas is set equal to all the ocean cells of the domain.
- Else, one cell is first randomly picked up in the whole domain, and the domain is extended from neighbours to neighbours until the number of cells reaches ``ncell``.

Then, at each time-step, schools are moved following a random walk method within this domain, with the range defined in the parameters.
Unlocated schools are randomly put in one of the cell of the defined domain.

Map definition
#####################

Another way to define species distribution is to use distribution maps, which can generally be obtained from niche modelling.

A single map is defined as follows:

.. table:: Random distribution parameters
    :align: center

    .. csv-table::
        :delim: =
        :header: Parameter, Description

        movement.randomwalk.range.sp# = Number of adjacent cells a species can use during random walk (foraging)
        movement.map#.file = Name of the CSV file that contains the distribution map
        movement.map#.species = Name of the species associated with the map.
        movement.map#.age.min = Minimum age (in years) when to use the map.
        movement.map#.age.max = Maximum age (in years) when to use the map.
        movement.map#.year.min = Minimum simulation time (in years) when to use the map.
        movement.map#.year.max = Maximum simulation time (in years) when to use the map.
        movement.map#.years = Array of years during when to use the map. (instead of setting initial and final year)
        movement.map#.season = Array of time-steps during when to use the map

One map is associated to a unique species for a given age span, year span and season. The full spatial distribution of a species can be represented using as many maps as necessary to cover different age spans and/or year spans and/or seasons. Let's now have a look at each parameter in detail.

Note that the CSV file has the same number of lines and columns as the OSMOSE grid. The distribution area can be defined using either a presence/absence map (1 for presence, 0 for absence) or a map of probability of presence (containing values ranging from 0 to values <1).

The same CSV file can be used to define different maps.

If the file path is set to ``null`` it means that the schools concerned by this map are out of the simulated domain (e.g., migrating species).

.. See the parameter mortality.out.rate.sp for mortality rate of species momentarily out of the simulated area.

When a school comes back to the simulated area, it will be randomly located on a new map (the one corresponding to the species and age class of the school at the current time step of the simulation).

Several maps can be defined for representing the spatial distribution of a single species. For example:

::

    #Map 0
    movement.map0.species = euphausiids
    movement.map0.file = maps/mymap_euphau1.csv
    movement.map0.age.min = 0
    movement.map0.age.max = 0.2
    movement.map0.year.min = 0
    movement.map0.year.max = 40
    movement.map0.season = 0;1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23

    #Map 1
    movement.map1.species = euphausiids
    movement.map1.file = maps/mymap_euphau2.csv
    movement.map1.age.min = 0.2
    movement.map1.age.max = 1
    movement.map1.year.min = 0
    movement.map1.year.max = 40
    movement.map1.season = 0;1;2;3;4;5;6;7;8;9

    #Map 2
    movement.map2.species = euphausiids
    movement.map2.file = maps/mymap_euphau3.csv
    movement.map2.age.min = 0.2
    movement.map2.age.max = 1
    movement.map2.year.min = 0
    movement.map2.year.max = 40
    movement.map2.season = 10;11;12;13;14;15;16;17;18;19;20;21;22;23

By increasing the number of maps, the description of the spatial distribution can be as
detailed and refined as you want, as long as you have such information. It will allow
for instance to create some maps for eggs (an egg in Osmose is a new school of age zero that
is created during the reproduction process), some maps for the juveniles and some maps for the
adults, as many as necessary to describe ontogenetic migrations.

From one time step to an other, the movement manager checks whether a given school remains in the
same map or should "jump" to an other map (e.g. eggs map to juvenile map or adults in summer to adults in winter).
In the latter case (change of map), the schools are relocated randomly in the new map. In the former case (same map), the movement
manager mimics foraging movement with a random-walk that moves schools to immediately adjacent cells within their distribution area.
