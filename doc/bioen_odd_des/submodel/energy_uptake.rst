.. _energy_uptake:

Energy uptake, assimilation and mobilization
++++++++++++++++++++++++++++++++++++++++++++++++

For an individual in school :math:`i`, the energy uptake  from food at time step :math:`t`
is described by a Holling’s type 1 functional response :math:`f` (:cite:t:`holling1959components`) that depends on its 
somatic mass :math:`w(i, t)` (:cite:t:`christensen2004ecopath, Shin2004, holt2014climate`) in two ways. 

First, it determines prey food biomass :math:`P(i,t)` available to an individual of school :math:`i`  from the other schools and lower trophic levels present in the same grid cell :math:`c(i,t)`  according to a minimum :math:`R_{min}(s(i))` and a maximum :math:`R_{max}(s(i))` predator to prey size ratio (:cite:t:`Shin2004, Travers2009`):

.. math::

   P(i,t) = \dfrac{\sum_j{   \gamma(s(i), s(j)) B(j, t)}}{N(i, t)}


with 

.. math::

   j \in \left( j \lor (c(j,t)=c(i,t)) \left( \dfrac{L(i,t)}{R_{max}(i)} \leq L(j, t) \leq \dfrac{L(i,t)}{R_{min}(i)} \right) \right)


where :math:`\gamma(s(i), s(j))` is the accessibility coefficient essentially determined by 
the position of potential prey schools :math:`j`  in the water column relative to that of school :math:`i`  
and :math:`B(j,t) = N(j,t) w(j, t)`  is the biomass of prey school :math:`j` . Second, it sets the 
maximum possible ingestion rate according to an allometric function with a scaling exponent :math:`\beta`. The energy uptake can then be written as:

.. math::

   I(i,t) = f(P(i,t)) = min(P(i,t); I_{max}(i) \psi(i, t) w(i,t) ^{\beta}


with :math:`I_{max}(i)`  the maximum ingestion rate per mass unit at exponent :math:`\beta` (or mass-specific maximum ingestion rate) of individuals in school :math:`i`  and :math:`\psi(i,t)`
a multiplicative factor that depends of their life stage according to:

.. math::

   \psi(i, t) = 
   \Biggl \{
   {  
    \theta \text{ if } a(i,t) < a_l
    \atop
    1\text{ otherwise }
   }

where :math:`a_l`  is age at the end of a fast growth period (e.g larval period or the larval and post-larval period) and :math:`\theta`  a factor accounting 
for higher mass-specific maximum ingestion rate at this stage. A portion :math:`\xi`  of the energy uptake :math:`I(i,t)` is assimilated, 
a fraction :math:`1 - \xi` being lost due to excretion and faeces egestion.

