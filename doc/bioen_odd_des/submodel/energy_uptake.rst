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

No reserves are modeled in EV-OSMOSE, hence all the assimilated energy 
can be directly mobilized. Mobilized energy :math:`E_M` , referred to as active metabolic rate in 
the ecophysiology literature, fuels all metabolic processes such as maintenance, digestion, 
foraging, somatic growth, gonadic growth, etc… The mobilization of energy relies on the 
use of oxygen to transform the energy held in the chemical bonds of nutrients into a usable 
form, namely ATP :cite:`clarke2019energy`. In consequence, the maximum possible energy mobilized at a given 
temperature depends directly on dissolved oxygen saturation and, as temperature increases, on the 
capacity of individuals to sustain oxygen uptake and delivery for ATP 
production (see section 1.2.1 Basic principles for more details). The mobilized energy rate :math:`E_M`  is thus described by 

.. math::

   E_M(i,t) = \xi I(i,t) \lambda \left([O_2](i, t)\right) \varphi_M(T(i, t))

with :math:`\lambda \left([O_2](i, t)\right)` and :math:`\phi_M(T(i, t))`  being the mobilization responses to dissolved oxygen saturation
and temperature encountered by school :math:`i`  in the grid cell it occupies at time :math:`t` . 
These are scaled between 0 and 1 such that, in optimal conditions, all assimilated energy :math:`E_M(i,t) = \xi I(i, t)`  
can be mobilized and, in suboptimal conditions, only a fraction of assimilated energy 
can be mobilized (:math:`E_M(i,t) < \xi I(i, t)`). More precisely, the effect of dissolved oxygen is described by a dose-response 
function :math:`\lambda` :cite:`thomas2019effects` which increases with the saturation of dissolved oxygen:

.. math::

    \lambda([O_2]) = C_{O,1} \dfrac{[O_2]}{[O_2] + C_{O, 2}}


with :math:`C_{O,1}` and :math:`C_{O, 2}` the asymptote and the slope of the dose-response function.

The effect of temperature  is described according to the :cite:t:`Johnson1946` model :cite:`pawar2015metabolic`:

.. math::

   \varphi_{M}(T) = \Phi \dfrac
   {e^{\dfrac{-\varepsilon{}_M}{k_BT}}}
   {1 + 
    \dfrac{\varepsilon_M}{\varepsilon{}_D - \varepsilon{}_M}
    e^{\dfrac{\varepsilon{}_D}{k_B}\left(\dfrac{1}{T_P} - \dfrac{1}{T}\right)}
   }


with :math:`k_B`  the Boltzmann constant, :math:`\varepsilon_M` the activation energy for the Arrhenius-like increase 
in mobilized energy with temperature :math:`T` before reaching its peak value at :math:`T_P` , :math:`\varepsilon_D`  the activation energy for the energy mobilization decline with :math:`T`  after :math:`T_P` , and 

.. math::

   \Phi = \left(1 +  \dfrac{\varepsilon_M}{\varepsilon{}_D - \varepsilon{}_M}\right) {e^{\dfrac{\varepsilon{}_M}{k_BT}}}

a standardizing constant insuring that :math:`\psi_M(T_P) = 1`.

