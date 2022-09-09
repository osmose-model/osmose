Mortality
-----------------

Within each time step, the total mortality of a given school is comprised of predation mortality caused by other schools, starvation mortality, fishing mortality, and diverse other natural mortality rate. The four different mortalities are computed so as to represent quasi simultaneous processes, and we consider that there is competition and stochasticity in the predation process.

Within each time step, OSMOSE considers each pair of school/source of mortality in turn in a random order. To ensure that the random order of the mortality sources and of the schools does not bias the resulting instantaneous mortality rates applied and effectively correspond to the mortality rates specified in input (for fishing and diverse natural mortality), all the mortality events are iterated within a time step over a fixed number of sub-time step.

.. index::
    single: stochastic.mortality.seed
    single: mortality.subdt

.. table:: Mortality parameters

    .. csv-table::
        :delim: ;

        stochastic.mortality.seed ; Integer to fix the random number generator.
        mortality.subdt ; Number of mortality sub time-steps.

Mortality processes are detailed below.

.. toctree::
    :maxdepth: 1

    mort/pred.rst
    mort/starv.rst
    mort/migrat.rst
    mort/fish.rst
