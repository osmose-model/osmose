Mortality
-----------------

Within each time step, the total mortality of a given school is comprised of predation mortality caused by other schools, starvation mortality, fishing mortality, and diverse other natural mortality rate. The four different mortalities are computed so as to represent quasi simultaneous processes, and we consider that there is competition and stochasticity in the predation process.

Within each time step, OSMOSE considers each pair of school/source of mortality in turn in a random order. To ensure that the random order of the mortality sources and of the schools does not bias the resulting instantaneous mortality rates applied and effectively correspond to the mortality rates specified in input (for fishing and diverse natural mortality), all the mortality events are iterated within a time step over a fixed number of sub-time step.

.. index:: 
    single: stochastic.mortality.seed
    single: mortality.subdt
    single: mortality.algorithm

.. table:: Mortality parameters

    .. csv-table:: 
        :delim: ;

        stochastic.mortality.seed ; Integer to fix the random number generator. 
        mortality.subdt ; Number of mortality sub time-steps.
        mortality.algorithm ; Mortality algorithm (iterative or stochastic) (Osmose <= 4.2.0)


.. note:: 

    In Osmose >= 4.3.0, the :samp:`mortality.algorithm` parameter is no more used. Stochastic mortality algorithm is used.

Mortality processes are detailed below.

.. toctree::

    mort/pred.rst
    mort/starv.rst
    mort/migrat.rst
    mort/fish.rst
    mort/fish-4.0.0.rst
    mort/fish-4.3.rst

