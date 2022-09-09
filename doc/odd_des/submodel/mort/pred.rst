Predation mortality
@@@@@@@@@@@@@@@@@@@@@@@@@@@

The central assumption in OSMOSE is that predation is an opportunistic process, which depends on:

- the overlap between predators and potential prey items in the horizontal dimension
- size adequacy between the predators and the potential prey (determined by **predator/prey size ratios**); and when the information is available
- the accessibility of prey items to predators, which depends on their vertical distribution (this being determined by means of **accessibility coefficients**). Thus, in OSMOSE, the food web structure emerges from local predation and competition interactions.

During the predation mortality process, the predation success
rate (``predSuccessRate`` attribute) is updated.

Size predation
+++++++++++++++++++++++++++++++++++++++++

Size-predation matrix is controlled by two parameters. The predator school :math:`S_{pred}` can only feed on prey schools whose length belongs
to a given interval:

.. math::

    R_{max} < \frac{L_{pred}}{L_{prey}} \le R_{min}

with :math:`R_{min}` and :math:`R_{max}` the maximum and minimum predator/prey size ratios. Reorganizing this inequality, we obtain:

.. math::

    \frac{L_{pred}}{R_{min}} \le L_{prey} < \frac{L_{pred}}{R_{max}}

Therefore, the minimum and maximum sizes of a prey that a predator can eat is given by:

.. math::

    L_{max} = \frac{L_{pred}}{R_{max}}

    L_{min} = \frac{L_{pred}}{R_{min}}

.. index:: predation.predPrey.stage.structure, predation.predPrey.stage.threshold.sp#, predation.predPrey.sizeRatio.max.sp#, predation.predPrey.sizeRatio.min.sp#

.. table:: Size-predation parameters
    :class: tight-table

    .. csv-table::
        :delim: ;

        predation.predPrey.stage.structure ; Structure to determine thresholds for predator/prey size ratios (:samp:`age` or :samp:`size`)
        predation.predPrey.stage.threshold.sp# ; Array of age or size thresholds
        predation.predPrey.sizeRatio.max.sp# ; Array of :math:`R_{max}` values
        predation.predPrey.sizeRatio.min.sp# ; Array of :math:`R_{min}` values


.. danger::

    To make sure that  :math:`L_{max} < L_{min}`, the ``predation.predPrey.sizeRatio.max.sp#`` and ``predation.predPrey.sizeRatio.min.sp#``
    must verify :math:`R_{min} > R_{max}`

Since resource groups are defined by a range of sizes, and not by a single sizes, the predator will feed on a given percentage of the resource:

.. math::

    R_{rsc} = \frac{min(L_{max_{rsc}}, L_{max}) - max(L_{min_{rsc}}, L_{min})} {L_{max_{rsc}} - L_{min_{rsc}}}

which is the overlapping range of the predator accessible range and of the resource size range.

.. ipython:: python
    :suppress:

    import os
    import subprocess
    cwd = os.getcwd()
    fpath = "odd_des/submodel/mort/_static/plot_size_ratios.py"
    subprocess.call(["python", fpath], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)

.. figure::  _static/size_ratio.*
    :align: center

    Size-based predation in Osmose


Accessibility
+++++++++++++++++++++++++++++++++

First, the accessibility of all the preys to a given school is determined from an accessibility  matrix for every
species and stages. This matrix must not be used to define diet preferences but rather to take into account
for a difference of positions in the water column (meaning some schools might evolve around the same geographical
area but never meet because they do not occur at the same depth).

.. _table_paros_acessfile:
.. table:: Example of a CSV predation accessibility file.

    .. csv-table::
        :delim: ,
        :file: _static/predation-accessibility.csv

Each line of the matrix corresponds to a prey (including plankton groups), and each column to
a predator. The file must be understood as follow: ``lesserSpottedDogfish`` of age class less than 0.45 (line 1) are only accessible to
young ``lesserSpottedDogfish`` (5%) and old ``redMullet`` (5%).

The class thresholds (age or size, defined with the ``predation.accessibility.stage.structure`` parameter) that are used
to determine which row or column should be used are read directly from the CSV files by matching
the :samp:`<` character. It is assumed that if there is no match, no threshold is provided. However, when :samp:`<` is matched, it
is assumed that the number that follows is the upper bound of the class.

Furthermore, the column and row order is not important, since a match of the species name is performed.

Additionnally, accessibility matrix can vary over time. To do so, one set of parameters must be defined
for each accessibility matrix, as done for the parameterization of movements. The keys of these
parameters must end with :samp:`.acc#`, with :samp:`#` the number of the accessibility matrix.

.. index:: predation.accessibility.stage.structure, predation.accessibility.stage.threshold.sp#, predation.accessibility.file predation.accessibility.file.acc#, predation.accessibility.initialYear.acc#, predation.accessibility.finalYear.acc#, predation.accessibility.years.acc#, predation.accessibility.steps.acc#

.. table:: Parameters for accessibility matric
    :class: tight-table

    .. csv-table::
        :delim: ;

        predation.accessibility.stage.structure ; Threshold type. Must be :samp:`age` or :samp:`size`.
        predation.accessibility.file ; CSV file containing the accessibility matrix **if constant over time**

        predation.accessibility.file.acc# ; CSV file containing the accessibility matrix for the accessibility matrix ``#``
        predation.accessibility.initialYear.acc# ; Start year when to use the accessibility matrix ``#``
        predation.accessibility.finalYear.acc# ; Start year when to use the accessibility matrix ``#``
        predation.accessibility.years.acc# ; List of years when to use the accessibility matrix  ``#`` (instead of setting initial and final years)
        predation.accessibility.steps.acc# ; List of time steps when to use the accessibility matrix ``#``

.. danger::

    If the :samp:`predation.accessibility.file` (with no :samp:`.acc` suffix) is found, Osmose will assume constant
    predation accessibility matrix.

Predation rate
++++++++++++++++++

Finally, the predation rate is computed as follows. First, the total accessible biomass for the predator school is computed:

.. math::

    P_{tot} = \sum_{p=preys} A(pred, prey) \times B_{prey}

The total biomass that a predator can eat is also computed as follow:

.. math::

    P_{eatable} = \frac{B_{pred} \times I_{max}}{N_{mort}}

with :samp:`N_{mort}` the number of sub-step of mortality processes,  :math:`B_{pred}` the total biomass of predator and :math:`I_{max}` the maximum ingestion rate for each species, expressed in grams of food per gram of fish
and per year. It is assumed that predator eat as much as they can.

The effective biomass that will be eaten by the predator is

.. math::

    P_{eaten} = min(P_{tot}, P_{eatable})

Finally, for each prey, the biomass eaten by the predator is given by:

.. math::

    P_{lost} = P_{eaten} \times \frac{A(pred, prey) \times B_{prey}}{P_{tot}}

Finally, the success rate is computed as:

.. math::

    S_R = \frac{P_{eaten}} {P_{eatable}}

.. index:: predation.ingestion.rate.max.sp#

.. table:: Ingestion parameter

    .. csv-table::
        :delim: ;

        predation.ingestion.rate.max.sp# ; :math:`I_{max}` (grams of food per gram of fish and per year)
