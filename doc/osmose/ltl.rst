Biotic ressources
---------------------------------

Biotic resources such as phytoplankton and zooplankton are not explicitly modelled in Osmose but are essential to take into account are they constitute the base of the trophic chain. They are considered as an input of the model, spatially explicit and varying with time.

In this section, the way LTL biotic resources are defined is described

Osmose <= 4.2.0
#################

In the Osmose versions prior to 4.2.0, the biotic resources were defined as follows:

.. index:: 
    single: plankton.name.plk#
    single: plankton.TL.plk#
    single: plankton.size.min.plk#
    single: plankton.size.max.plk#
    single: plankton.accessibility2fish.plk#

.. _ltl:
.. table:: List of parameters to define biotic resources (<= 4.2.0)

    .. csv-table:: 
        :delim: = 

        plankton.name.plk# = Name of the plankton group.
        plankton.TL.plk# = Trophic level of the plankton group.
        plankton.size.min.plk# = Minimum size of the organisms in the plankton group (centimeters).
        plankton.size.max.plk# = Maximun size of the organisms in the plankton group (centimeters).
        plankton.accessibility2fish.plk# =  Fraction of the plankton biomass that is accessible to the fish, ranging from zero to one. 


The :samp:`plankton.accessibility2fish.plk#` parameter accounts for many biological processes that are not explicitly represented in Osmose and basically says that only a small fraction of the plankton in the water column is effectively available to the fish for preying upon. Plankton accessibility is generally completely unknown and, just like larval mortality, it should be estimated in the calibration process.

.. The samp:`plankton.multiplier.plk#` is used for multiplying plankton biomass, in order to run different scenarios of plankton forcing in relation to a reference state (plankton.multiplier.plk0 = 1 by default for the reference state). For instance :samp:`plankton.multiplier.plk0=2` means that Osmose will multiply by two the biomass of the plankton group zero of the LTL dataset.
.. :samp:`plankton.conversion2tons.plk#` is the factor for converting biomass from plankton unit (as provided in the LTL input file) to wet weight in :math:`ton/km^2` (e.g. :math:`mmolN/m^2` to :math:`tonne/km^2`)

Osmose >= 4.3.0
#############################

Since Osmose 4.3.0, plankton groups are defined using the same parameters, except that they have different names.

.. index::
    single: species.name.sp#
    single: species.TL.sp#
    single: species.size.min.sp#
    single: species.size.max.sp#
    single: species.accessibility2fish.sp#
    single: species.type.sp#

.. _ltlnew:
.. table:: List of parameters to define biotic resources (>= 4.3.0)

    .. csv-table:: 
        :delim: = 

        species.name.sp# = Name of the plankton group.
        species.TL.sp# = Trophic level of the plankton group.
        species.size.min.sp# = Minimum size of the organisms in the plankton group (centimeters).
        species.size.max.sp# = Maximum size of the organisms in the plankton group (centimeters).
        species.accessibility2fish.sp# =  Fraction of the plankton biomass that is accessible to the fish, ranging from zero to one. 
        **species.type.sp#** = **Type of the species. Must be** :samp:`resource` **for biotic resources**

An additional argument, :samp:`species.type.sp#`, must be defined and set to :samp:`resource` for biotic resources.
