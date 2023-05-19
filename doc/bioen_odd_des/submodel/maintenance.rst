.. _maintenance:

Maintenance
++++++++++++++++++++++++++++++++++++++++++++++++

The mobilized energy :math:`E_M` fuels all metabolic processes starting in priority with the costs of maintenance of existing tissues :math:`E_m`, 
which is often referred to as the standard metabolic rate in ecophysiology literature. Here, we include in 
maintenance costs also routine activities of individuals including foraging and digestion, so that 
they might be compared to routine metabolic rate in the ecophysiology literature. The maintenance costs are explicitly modeled 
to describe the share of mobilized energy between maintenance and the production of new tissues 
:cite:`charnov2001reproductive, mollet2010multiple, holt2014climate`, with precedence of the former over the latter, 
as well as to link mechanistically starvation mortality to energetic starvation when neither mobilized energy nor gonad energy reserves 
can cover the costs of maintenance (see next section New tissue production for more details). 

The maintenance 
energy rate :math:`E_m` scales with individual’s somatic mass :math:`w(i,t)` with the same exponent as maximum ingestion rate. Thus, for a given 
temperature, the production of new tissues, and notably somatic growth, is not limited by disproportionately increasing maintenance 
costs relative to ingestion rate as somatic mass increases :cite:`lefevre2017models, lefevre2018modelling`. In addition, the maintenance rate 
also increases with the temperature :math:`T(i,t)` experienced by individuals :cite:`gillooly2002effects` and can be described as

.. math::

   E_m(i,t) = C_m w(i,t)^{\beta} \varphi_m(T(i,t))

with :math:`C_m` the mass-specific maintenance rate and :math:`varphi_m`  the Arrhenius function describing the effect of temperature on :math:`E_m`, defined as:

.. math::

   \varphi_m(T(i,t)) = e^{\dfrac{-\varepsilon{}_m}{k_B T}}

with :math:`\varepsilon{}_m` the activation energy for maintenance rate increase with temperature.
