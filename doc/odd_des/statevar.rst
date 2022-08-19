Entities, state variables, and scales
---------------------------------------------------

Entities
++++++++++++++++++

In the current section, the different types of entities of the Osmose model are described.

- The ``Cell`` entity describes the individual cells of the Osmose computation 2D grid (longitude, latitude,
  cell index, etc.)
- The ``Species`` entity represents the species whose life-cycle is fully represented.
  This entity contain **fixed** parameters, which are not modified during the simulation.
- The ``BackgroundSpecies`` entity represents the species whose life-cycle is not represented. These species can feed on and be eaten by ``Species``, and can be fished.
  This entity contain **fixed** parameters, which are not modified during the simulation.
- The ``ResourceSpecies`` entity represents the low-trophic level species. These species cannot predatate ``Species`` or ``BackgroundSpecies`` but can be eaten by them.
  This entity contain **fixed** parameters, which are not modified during the simulation.
- The ``School`` entity describes a group of ``Species`` individuals, whose life cycle is fully considered and which all share the
  same characteristics (age, weight, species, location, etc, trophic level).
- The ``BackgroundSchool`` entity describes a group of ``BackgroundSpecies`` individuals, whose biomass is provided as input and whose life-cycle is not simulated and which all share the
  same characteristics (age, weight, species, location, etc, trophic level).
- The ``Resource`` entity describes a swarm of ``ResourceSpecies``, whose biomass is provided as input.


State variables
++++++++++++++++++

Cell
###################

The ``Cell`` entity contains the following state variables:

.. csv-table:: ``Cell`` state variables
   :file: _static/grid.csv
   :delim: ;
   :header-rows: 1
   :class: tight-table

Species
###########################

The ``Species`` entities contain the following state variables:

.. csv-table:: ``Species`` state variables
   :file: _static/species.csv
   :delim: ;
   :header-rows: 1
   :class: tight-table

The ``BackgroundSpecies`` entities contain the following state variables:

.. csv-table:: ``BackgroundSpecies`` state variables
   :file: _static/bkgspecies.csv
   :delim: ;
   :header-rows: 1
   :class: tight-table

.. csv-table:: ``ResourceSpecies`` state variables
   :file: _static/rscspecies.csv
   :delim: ;
   :header-rows: 1
   :class: tight-table

School
###########################

The ``School`` entity contains the following state variables:

.. csv-table:: ``School`` state variables
   :file: _static/school.csv
   :delim: ;
   :header-rows: 1
   :widths: 20, 60, 20
   :class: tight-table

.. csv-table:: ``BackgroundSchool`` state variables
   :file: _static/bkgschool.csv
   :delim: ;
   :header-rows: 1
   :widths: 20, 60, 20
   :class: tight-table


Scales
++++++++++++++++++

The basic units of OSMOSE are fish schools, which are composed of individuals that belong to the same species, and that have the same age, size (length, weight), food requirements and, at a given time step, the same geographical coordinates. From the school states (hereafter called individual states), biomass and abundance can be tracked at the population or community levels along with the size, age, and spatial dimensions (:numref:`table_stvar`).

Other variables can be reported such as the trophic level, the diets, the different sources of mortality, the catches from fishing operations. Because each school simulated in OSMOSE is represented from the egg stage to the terminal age, which necessitates high calculation and memory capacities, and because comprehensive information on entire life cycles needs to be parameterized, the selection of focus species is made parsimoniously, and usually between 10 and 20 high-trophic level species or functional groups are explicitly considered in OSMOSE applications.

The model operates on a weekly to monthly time step, and runs up to 100 years or more depending on applications and simulations.

For eggs (age 0), weight and sizes are provided as parameters. For the others, conversion from size to weight (and conversely) is obtained by using allometric relationships:

.. math::

    W = c \times L^b

    L = \left(\frac{W}{c}\right)^{\frac{1}{3}}

where the :math:`c` parameter is a 'condition.factor', and :math:`b` the 'allometric.power'.

Biomass to abundance conversion for a school is made by using the mean weight of the school:

.. math::

    B = N \times W

    N = \frac{B}{W}

.. table:: Allometric parameters

    .. csv-table::
        :delim: ;

        species.length2weight.condition.factor.sp# ; Allometric factor (:math:`c`)
        species.length2weight.allometric.power.sp# ; Allometric power (:math:`b`)
        species.egg.size.sp# ; Egg size (cm)
        species.egg.weight.sp# ; Egg weight (g)

.. _table_stvar:

.. table:: List of state variables
    :widths: 1,3,1

    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | Individual State variables| Description                    | Auxiliary state variables / indicators                                                  |
    +===========================+================================+=========================================================================================+
    | abundance                 | Number of fish (N) in the      |                                                                                         |
    |                           | school at the beginning        |                                                                                         |
    |                           | of the time step	         |   	                                                                                   |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | biomass                   | Biomass (B) of the school at   |                                                                                         |
    |                           | the beginning                  |	                                                                                   |
    |                           | of the time step (tons)	 |                                                                                         |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | age                       | Age of the fish (year)         | species N or B per age class                                                            |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | length                    | Size of the fish (cm)          | fish N or B per size                                                                    |
    |                           |                                | class (size spectrum),                                                                  |
    |                           |                                | mean size of fish, large                                                                |
    |                           |                                | fish indicator                                                                          |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | weight                    | Weight of the fish (g)	 |                                                                                         |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | trophicLevel              | Trophic level (TL) of the fish |  fish N or B per TL (trophic                                                            |
    |                           |                                |  spectrum), TL of species,                                                              |
    |                           |                                |  TL of catches                                                                          |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | nDead[]                   | Number of dead fish in the     |                                                                                         |
    |                           | current time step              |                                                                                         |
    |                           | for each mortality cause       |                                                                                         |
    |                           | (predation, fishing,           |                                                                                         |
    |                           | natural mortality, starvation) | Catches per species, size                                                               |
    |                           |                                | class, age class                                                                        |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | predSuccessRate           | Ingested biomass at current    |                                                                                         |
    |                           | time step/maximum              |                                                                                         |
    |			        | ingestion rate                 |                                                                                         |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | preyedBiomass             | Biomass of prey ingested by    |                                                                                         |
    |                           | the school 	                 |                                                                                         |
    |                           | at current time step (tons)    |                                                                                         |
    |                           |                                | fish diets per species,                                                                 |
    |                           |                                | per size class, per age class                                                           |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
    | lat, lon                  | location of the fish school    |                                                                                         |
    |                           | in latitude and                |                                                                                         |
    |			        | longitude coordinates	         |                                                                                         |
    +---------------------------+--------------------------------+-----------------------------------------------------------------------------------------+
