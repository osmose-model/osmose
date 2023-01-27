.. _mortality:


Mortality
##################################

At each time step, a school experiences several mortality sources. The total mortality of a school :math:`i` is the sum of predation 
mortality caused by other schools, foraging mortality, starvation mortality, fishing mortality and diverse 
other natural mortalities (i.e. senescence, diseases, and non-explicitly modeled predators). Within each time step , the different mortality sources impact a school :math:`i`
in a random order so as to simulate the simultaneous nature of these processes.

The mortality induced by predation emerges from the energy uptake process previously described (see :numref:`energy_uptake`) and thus is an 
explicit stochastic size-dependent process depending on the spatial co-occurrence between predators 
and preys. The predation mortality experienced by school :math:`i`  is simply the sum of biomass losses due to the ingestion 
of all predator schools :math:`j` present in the same grid cell :math:`c(i, t)` at time step :math:`t`  and with adequate minimum and maximum predator to prey size ratios (:math:`R_{min}(s(j))` 
and :math:`R_{max}(s(j))`):

.. math::

   \dfrac{dB}{dt} = \sum_j \dfrac{\gamma(s(j), s(i)) B(i,t)}{P(j, t)} I(j,t)


with

.. math::

    j \in \left( j \lor (c(j,t)=c(i,t)) \left( \dfrac{L(i,t)}{R_{max}(i)} \leq L(j, t) \leq \dfrac{L(i,t)}{R_{min}(i)} \right) \right)

where the ration :math:`\dfrac{\dfrac{\gamma(s(j), s(i)) B(i,t)}{P(j, t)}` represents the fact that predators prey on various prey schools according to their relative accessible abundance. Change in the number of individuals of school  due to predation during time step  is then given by:

.. math::

   N(i, t+\Delta t) = N(i, t) (1 - \sum_j \dfrac{\gamma(s(j), s(i))}{P(j,t)} I(j,t)

Organisms face a trade-off between mortality and foraging activity :cite:`mangel2003environment`
because more active foraging implies a higher exposure to predation, more unfavorable 
condition encounters (e.g. diseases) and/or increased oxidative stress. Assuming that 
variation in mass-specific maximum ingestion rate :math:`I_{max}` results from variation in foraging activity, this trade-off 
is modeled by including a foraging mortality that increases with :math:`I_{max}`  and thus when foraging 
activity is more intense. The instantaneous foraging mortality rate experienced by school  is defined as follows:

.. math::

   M_f(i) = \mu_f I_{max}(i)

with :math:`\mu_f` a conversion coefficient from foraging activity to mortality that measures the trade-off’s strength. Change in the number of individuals in school  due to foraging mortality during time step  is then obtained as:

.. math::

   N(i, t+\Delta t) = N(i, t) e^{-M_f(i) \Delta t}


Starvation occurs when an individual cannot cover its maintenance needs, i.e. when net energy is negative , even by drawing energy from its gonadic reserves, i.e. when  (see :numref:`new_tissue` for details). In this case, schools undergo a decrease in biomass equaling the energetic deficit after accounting for the energy reserves contained in the gonadic compartment:

.. math::

    B(i, t + \Delta t) = B(i, t) - N(i,t) \left( \|E_P(i,t)\| - \dfrac{g(i,t)}{\eta}\right) \text{ if $\eta E_P(i, t) < -g(i,t)$}

so that change in the number of individuals of school :math:`i` due to energetic starvation during time step :math:`t` is then given by:


.. math::

   N(i, t + \Delta t) = N(i, t) \times \left(1 - \dfrac{\|E_P(i,t)\| - \dfrac{g(i,t)}{\eta}}{w(i,t)} \right) \text{ if $\eta E_P(i, t) < -g(i,t)$}

Starvation mortality at the time step :math:`t+\Delta t` relies on the net energy amount of time step :math:`t`. During the first time step of a school, i.e. at eggs life stage, the school doesn’t feed to model the energy provision yolk reserve. Then, starvation does not occur at the first and second time step as starvation at the second step time relies on first time step. 
