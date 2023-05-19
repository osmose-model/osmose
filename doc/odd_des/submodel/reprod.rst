.. _reprod:

Reproduction
+++++++++++++++++++++++++++++++++++++++++++++

.. In previous versions of the model, the main way of initialising the system consisted in building age-structured populations from target biomass specified in input. Osmose would distribute fish biomass across age classes and schools according to a simple exponential decay of fish populations and applying the total annual mortality from one age to the next (sum of fishing and natural mortality parameters in input). Fish numbers were then calculated by using the length-weight parameters in input of the model and distributed evenly among the schools of a given age cohort. This initialisation method shows several drawbacks:
..
.. - it provides a fully structured population though, ideally, no assumption should be made about this structure which the model needs ot build up given some basic laws at individual level.
.. - it often initializes the system with an unstable state which can lead to premature and artificial species collapses or explosion in the first year of the simulation.
.. - it slows down the simulation because the initial population contains a big number of schools: though mortality rates are applied to build the age structure of the populations, fish schools do not disappear from the system but only their fish numbers are decreased.

.. We must first acknowledge that there is no ideal solution for initialising OSMOSE but it should be done by making as little assumptions as possible, keeping the spin-up time as short as possible and individual rules as much as possible. Therefore, OSMOSE 3 Update 2 proposes a new "seeding" mechanism for initialising the population.

The system starts from a pristine state, with no schools in the domain. For a few years (user-defined), Osmose will
release some eggs for every species. The eggs enter the different steps of the life cycle, and once the
fish reach sexual maturity, the reproduction process takes over. Osmose stops the seeding, unless the
spawning stock biomass gets depleted. In that case Osmose resumes the seeding by releasing some
eggs until there are again mature individuals in the system for carrying on the reproduction process. Osmose
completely ceases the seeding when the simulation reaches the maximal number of years for seeding (user-defined).

For a given species:

.. math::

    B_{mat} = \sum_{mature\ sch.}B

.. math::

    N_{eggs} = FRAC_{fem} \times \alpha \times season \times B_{mat}\ \ if\ SSB > 0

.. math::

    N_{eggs} = FRAC_{fem} \times \alpha \times season \times B_{seeding}\ \ if\ SSB=0

By following this approach:

- No assumption is made about the structure of the populations but it emerges from individual interactions
- it reduces computing requirements for the spin-up as the first years are the fastest to run
- it reduces the number of time steps for the spin-up
- it minimizes the amplitude of population oscillations

Finally, the seeding biomass is then used to add new schools to the system, depending on the value of :math:`N_{eggs}`.

If :math:`N_{eggs} < N_s`:

.. math::

    N_{sch} = 1

.. math::

    A_{sch} = N_{eggs}

else:

.. math::

    N_{sch} = N_s

.. math::

    A_{sch} = \frac{N_{eggs}}{N_s}

.. table:: Reproduction paramters
    :align: center

    .. csv-table::
        :delim: ;

        simulation.nschool.sp# ; Number of schools of species # to create during reproduction (:math:`N_s`)
        simulation.nschool ; Number of schools to create during reproduction (:math:`N_s`). Used if no species specific value provided
        population.seeding.biomass.sp# ; Seeding biomass (:math:`B_{seeding}`, tons)
        reproduction.season.file.sp# ; File providing the seeding distribution within a year
        species.sexratio.sp# ; Fraction of females (:math:`FRAC_{fem}`)
        species.relativefecundity.sp# ; Number of eggs per gram of mature female (relative fecundity)
        population.seeding.year.max ; Number of years when the artificial seeding is activated
