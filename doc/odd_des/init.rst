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

Using a NetCDF file
++++++++++++++++++++++++++++++++++++++++

Another possibility to initialize the Osmose model is by using a NetCDF file.
This method is used if the ``simulation.restart.file`` or the
``population.initialization.file`` are set.

These parameters point to the NetCDF file that will be used to restart the simulation. If ``simulation.restart.file``
is used, then one file per replicate is expected (sufix ends with ``.nc.#``, with ``#`` the index of the simulation).

.. index::
    single: population.initialization.file
    single: simulation.restart.file

Using relative biomass
++++++++++++++++++++++++++++++++++++++++

Another method to initialize Osmose is to use relative biomass. This method is called if the ``population.initialization.relativebiomass.enabled``
is enabled. With this method, the user specifies, for each species:

- ``population.initialization.biomass.sp#``: the initialization biomass for each species (:math:`B_s`, `float`)
- ``population.initialization.size.sp#``: the size-boundaries (`float[]`). Size is :math:`N_{class} + 1`
- ``population.initialization.tl.sp#``: the trophic levels for each size-class (`float[]`, size :math:`N_{class}`)
- ``population.initialization.relativebiomass.sp#``: the proportion of total biomass to attribute to each size-class (:math:`P_{s, k}`, `float[]`, size :math:`N_{class}`, :math:`\in[0, 1]`)
- ``population.initialization.age.sp#``: the age to attribute to each size-class (`float[]`, size :math:`N_{class}`)
- ``population.initialization.nschool.sp#``: the number of schools to create for each size-class (:math:`N_{s, k}``int[]`)

Using all these parameters, schools for a given species are initialized as follows.

- For each size-class, computes the biomass to release by multiplying input biomass and relative proportion: :math:`B_{s, k} = B_s \times P_{s, k}`.
- Loop over the number of schools to create
- Randomly select a length between the lower and upper size-bonds of the given class. If age is 0, force length to be equal to species ``eggSize``.
- Using length, computes the weight using allometric relationship. If age is 0, force weight to be equal to species ``eggWeight``.
- Compute the number of individuals to put in school :math:`i`:  :math:`A_{s, k, i} = \dfrac{B_{s, k} \times 10^6}{N_{s, k} \times W_{s, k, i}}`


The steps are summarized in :numref:`init_biomass`

.. _init_biomass:

.. mermaid:: _static/mermaid/init_biomass.md
    :align: center
    :caption: Initialization steps using the relative biomass method.

.. index::
    single: population.initialization.relativebiomass.enabled
    single: population.initialization.seed
    single: species.linf.sp#
    single: population.initialization.biomass.sp#
    single: population.initialization.size.sp#
    single: population.initialization.tl.sp#
    single: population.initialization.relativebiomass.sp#
    single: population.initialization.age.sp#
    single: population.initialization.nschool.sp#