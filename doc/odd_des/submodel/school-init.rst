School initialisation
+++++++++++++++++++++++++

The school initialisation process allows to reset some variables at the beginning of the time steps.

These variales and their value after the initialization are listed below:

.. table:: List of reinitialized state variables
    :align: center

    .. csv-table::
        :delim: =
        :header: Variable, Value

        out = false
        abundance = instantaneous abundance at previous time-step
        biomass = abundance x weight
        lengthi = length at previous time step
        preys = empty listed
        preyedBiomass = 0
        predSuccessRate = 0
        nEggs = 0
        ingestion = 0
        nDead = array of 0
        ageDeath = array of 0
        fishedBiomass = array of 0
        this.discardedBiomass = array of 0