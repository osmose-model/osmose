Background species
---------------------

Background species have first been introduced in Osmose by :cite:`Fu17`. They can be viewed as an intermediary between focal species (i.e. species of interest, whose full life cycle is simulated) and lower trophic levels (plankton for instance). They differ from lower trophic levels since they can feed on focal species and be targeted by fisheries (since version 4.3)

They are available on Osmose since version 4.1.0, although their parameterization has changed in version 4.3.0.

Osmose <= 4.1.0
===========================

Background parameters in Osmose version 4.1.0 are paremeterized as follows:

.. index:: 
    single: biomass.byDt.bySize.file.bkg#
    single: species.trophiclevel.bkg#
    single: species.name.bkg#
    single: species.length2weight.allometric.power.bkg#
    single: species.length2weight.condition.factor.bkg#
    single: predation.accessibility.stage.threshold.bkg#
    single: predation.efficiency.critical.bkg#
    single: predation.ingestion.rate.max.bkg#
    single: predation.predPrey.stage.threshold.bkg#
    single: predation.predPrey.sizeRatio.max.bkg#
    single: predation.predPrey.sizeRatio.min.bkg#

.. _bkg:
.. table:: Parameters for background species.

    .. csv-table::
        :delim: = 
        
            biomass.byDt.bySize.file.bkg# = CSV file containg the background species biomass by size class and by time step.
            species.trophiclevel.bkg# = Array of trophic level (one for each size class)
            
            species.name.bkg# = Name of the background species
            species.length2weight.allometric.power.bkg# = Allometric factor for weight to length conversion
            species.length2weight.condition.factor.bkg# = Allometrioc power for weight to length conversion
            predation.accessibility.stage.threshold.bkg# = Threshold for accessibility matrix
            predation.efficiency.critical.bkg# = Critical predation success (:math:`C_{S_R}`)
            predation.ingestion.rate.max.bkg# = :math:`I_{max}` (grams of food per gram of fish and per year)
            predation.predPrey.stage.threshold.bkg# = Age or size thresholds for predation/prey size ratios
            predation.predPrey.sizeRatio.max.bkg# = Array of :math:`R_{max}` values
            predation.predPrey.sizeRatio.min.bkg# = Array of :math:`R_{min}` values
            

The biomass provided in the CSV file will then be distributed over the domain using distribution maps (similar to the ones defined for focal species).

.. index:: movement.bkgspecies.species.map#, movement.bkgspecies.class.map#, movement.bkgspecies.season.map#, movement.bkgspecies.year.min.map#, movement.bkgspecies.year.max.map#

.. table:: Parameters for background species.

    .. csv-table::
        :delim: = 

            movement.bkgspecies.species.map#  = Name of the background species to which the map is associated.
            movement.bkgspecies.class.map# = Size class for which the map is associated.
            movement.bkgspecies.season.map# = Time steps during which the map will be used
            movement.bkgspecies.year.min.map# = Initial year when the map will be used
            movement.bkgspecies.year.max.map# = Final year when the map will be used

.. warning:: 

    For background species, the size classes are **fixed**!

Accessibility matrix
++++++++++++++++++++++++++++

When using background species, the accessibility matrix must be changed accordingly. It must always have the following form:

+-------------------+--------------+------------------------------+
|                   | Focal (pred) | Background (pred)            |
+-------------------+--------------+------------------------------+
| Focal (prey)      |              |                              |
+-------------------+--------------+------------------------------+
| Background (prey) |              |                              |
+-------------------+--------------+------------------------------+
| LTL (prey)        |              |                              |
+-------------------+--------------+------------------------------+


.. _bkg_biomass:

Background biomass
++++++++++++++++++++++++++++

Background species biomass is defined from a biomass time series (one per species and per size class) and by distribution maps. The distribution maps contain defines the distribution of the background species over space. They contain float values, which are normalize, so that the integral over space equals one:

.. math::

    \sum_{k=0}^{N_{cell}-1} D_k = 1

Before each time step, the background species biomass is reset by multiplying the biomass time series by the map distribution factor. Because of the normalisation, the spatially integrated biomass is equal to the biomass provided in the time-series.


.. Mortality
.. ============================
.. 
.. Background species come into play in the predation mortality process.
.. 
.. .. warning:: 
.. 
..     Background species have only be implemented in the stochastic mortality algorithm (:samp:`StochasticMortalityProcess.java`).
.. 
.. For each cell:
.. 
.. - The list of background species schools is recovered. As for LTL, it is assumed that each cell contain one unique school for each background species and for each size class. This list is added to the list of potential preys (i.e. school of focal species, swarms of ltl that belong to the cell). 
.. - For all the background schools, the accessibility to all the preys is estimated.
.. - The biomass of all the schools is initialized (see :numref:`bkg_biomass`).
.. - A random draft of predators (both focal and background species) is performed. And for each predator, the predation mortality toward each prey (focal, background and ltl species) is computed.
.. - The number of dead individuals are then removed.


Osmose >= 4.3.0
============================

In Osmose version >= 4.3, background species are parameterized as follows:

.. index::
    single: species.name.sp#
    single: species.length2weight.allometric.power.sp#
    single: species.length2weight.condition.factor.sp#
    single: predation.efficiency.critical.sp#
    single: predation.ingestion.rate.max.sp#
    single: predation.predPrey.stage.threshold.sp#
    single: predation.predPrey.sizeRatio.max.sp#
    single: predation.predPrey.sizeRatio.min.sp#
    single: species.biomass.total.sp#
    single: species.file.sp#
    single: species.file.caching.sp#
    single: species.nclass.sp#
    single: species.trophiclevel.sp#
    single: species.age.sp#
    single: species.length.sp#
    single: species.size.proportion.sp#


.. _bkg-new:
.. table:: Parameters for background species (>= 4.3.0)

    .. csv-table::
        :delim: = 

            **Species parameters**
            species.name.sp# = Name of the background species
            **species.type.sp#** = **Type of the background species. Must be** :samp:`background`
            species.length2weight.allometric.power.sp# = Allometric factor for weight to length conversion
            species.length2weight.condition.factor.sp# = Allometrioc power for weight to length conversion
            predation.efficiency.critical.sp# = Critical predation success (:math:`C_{S_R}`)
            predation.ingestion.rate.max.sp# = :math:`I_{max}` (grams of food per gram of fish and per year)
            predation.predPrey.stage.threshold.sp# = Age or size thresholds for predation/prey size ratios
            predation.predPrey.sizeRatio.max.sp# = Array of :math:`R_{max}` values
            predation.predPrey.sizeRatio.min.sp# = Array of :math:`R_{min}` values

            **Resource forcing parameters**
            species.biomass.total.sp# = Total biomass for the given ressource (will be distributed over the whole domain)
            species.file.sp# = Regular expression defining the input files. Can be a file name.
            species.file.caching.sp# = Resource caching method. Must be :samp:`none`, :samp:`incremental` or :samp:`all` (default).

            **Background species parameters**
            species.nclass.sp# = Number of size classes.
            species.trophiclevel.sp# = Array of trophic level (one value for each size class)
            species.age.sp# = Array of school ages (one value per each size class)
            species.length.sp# = Array of school lengths (one value for each size class)
            species.size.proportion.sp# = Array of size proportion (one value for each size class) 

The spatio-temporal distribution of background is now defined from a NetCDF file (see :numref:`rsc-forcing`).

The distribution among size is now controlled by the :samp:`species.nclass.sp#` and :samp:`species.size.proportion.sp#` parameters.

.. warning:: 

    At this time, the distribution among the size classes is constant over the entire simulation.

