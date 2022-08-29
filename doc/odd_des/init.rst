Initialization
=========================

.. In previous versions of the model, the main way of initialising the system consisted in building age-structured populations from target biomass specified in input. Osmose would distribute fish biomass across age classes and schools according to a simple exponential decay of fish populations and applying the total annual mortality from one age to the next (sum of fishing and natural mortality parameters in input). Fish numbers were then calculated by using the length-weight parameters in input of the model and distributed evenly among the schools of a given age cohort. This initialisation method shows several drawbacks:

.. - it provides a fully structured population though, ideally, no assumption should be made about this structure which the model needs ot build up given some basic laws at individual level.
.. - it often initializes the system with an unstable state which can lead to premature and artificial species collapses or explosion in the first year of the simulation.
.. - it slows down the simulation because the initial population contains a big number of schools: though mortality rates are applied to build the age structure of the populations, fish schools do not disappear from the system but only their fish numbers are decreased.

.. We must first acknowledge that there is no ideal solution for initialising OSMOSE but it should be done by making as
.. little assumptions as possible, keeping the spin-up time as short as possible and individual rules as much as possible.

In Osmose, there are several initialisation methods.

Seeding method
++++++++++++++++++++++++++++++++++++++++

In the seeding method, the system starts from a pristine state, with no schools in the domain.
For a few years (user-defined), Osmose will release some eggs for every species.
The eggs enter the different steps of the life cycle, and once the fish reach sexual maturity, the reproduction process takes over and
Osmose stops the seeding, unless the spawning stock biomass gets depleted.
In that case Osmose resumes the seeding by releasing some eggs until there are again mature individuals in the system
for carrying on the reproduction process. Osmose completely ceases the seeding when the simulation reaches the maximal
number of years for seeding (user-defined).

By following this approach:

- No assumption is made about the structure of the populations but it emerges from individual interactions
- it reduces computing requirements for the spin-up as the first years are the fastest to run
- it reduces the number of time steps for the spin-up
- it minimizes the amplitude of population oscillations

.. index::
    single: population.seeding.biomass.sp#
    single: population.seeding.year.max

The initialisation process is controlled by two parameters:

- :samp:`population.seeding.year.max` defined the number of years for running the seeding process (from year 0 to :samp:`population.seeding.year.max`), and during which Osmose ensures that some eggs will be released even though there are no mature individuals in the system. Then, the seeding ceases completely until the end of the simulation. If the parameter is not specified, Osmose will set it by default to the lifespan of the longest lived species of the system.
- :samp:`population.seeding.biomass.sp#` defines the spawning stock biomass (SSB) that Osmose considers during the seeding period when there are no mature adults to ensure the reproduction process. SSB is then used to compute the number of eggs to be released in the system (cf. :numref:`reprod`).
