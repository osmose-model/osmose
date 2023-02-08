.. _new_tissue:

New tissue production: somatic and gonadic growth
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

The net energy available for new tissues production :math:`E_P` is the difference between mobilized energy
:math:`E_M` and maintenance :math:`E_m` , defined as follows:

.. math::

   E_P(i,t) = E_M(i,t) - E_m(i, t)

The net energy :math:`E_P`  contributes to the production of new tissues with a proportion :math:`\rho`  being allocated to the gonadic compartment :math:`g(i,t)`  and a proportion 
:math:`1 - \rho` allocated to the somatic one :math:`w(i,t)`. This proportion depends on sexual maturity 
status :math:`m(i,t)`  of the schools’ individuals and their somatic mass :math:`w(i,t)`. Before sexual 
maturation, i.e., when :math:`m(i,t) = 0`,  is equal to 0 and, after maturation, i.e., when :math:`m(i,t) = 1`, it is defined such that the annual mean gonado-somatic 
index of individuals :math:`\dfrac{g(i,t)}{w(i,t)}`  is constant throughout their adult life-stage and equal to its genetically coded value :math:`r(i)`  :cite:`boukal2014life, lester2004interpreting, quince2008biphasic`:

.. math::
   :label: eq_rho

   \rho(i,t) = m(i,t) \dfrac{r(i)}{\eta \overline{E_P}(i)} w(i, t)

where

.. math:: 
    
    \overline{E_P}(i) = \dfrac{\Delta t}{a(i, t)} \sum_{t=0}^{t= a(i,t)/\Delta t} E_P(i, t')

is the average net energy available per time step to individuals of school :math:`i`  since their birth, with :math:`\Delta t`  being the duration of a time step. Equation :eq:`eq_rho` differs from a deterministic continuous time version of the same model :cite:`boukal2014life, lester2004interpreting, quince2008biphasic` where the current net energy :math:`E_P(i,t)`  would 
be used instead of the average :math:`\overline{E_P}(i)`. The averaging in a stochastic discrete time individual-based model such as EV-OSMOSE insures a smooth increase of proportion :math:`\rho`  as individuals grow by dampening strong variations in :math:`E_P(i,t)`  and thus in :math:`\rho(i,t)`  due to the stochasticity of prey encounter and hence of ingested energy :math:`I(i, t)`.

According to the definition of :math:`\rho`, the net energy :math:`E_P`  is thus fully allocated to somatic growth before maturation and it is shared between somatic and gonadic growth after, and the proportion :math:`\rho` allocated to gonads increases with mass :cite:`boukal2014life`, which limits somatic growth as individuals become bigger. However, in case mobilized energy :math:`E_M`  cannot fully cover maintenance :math:`E_m`, i.e. when :math:`E_P < 0` , new tissue production is not possible and the gonadic compartment :math:`g(i,t)`  can be resorbed to provide energy for sustaining maintenance. 

Somatic growth is then defined as follows:

.. math::
   :label: eq_somatic_growth

   \dfrac{dw}{dt}(i,t) = 
   \begin{cases}
    (1 - \rho(i, t)) E_P(i,t) & \text{ if $E_P \geq 0$}\\
    0 & \text{ otherwise }
    \end{cases}
    

and gonadic growth as:

.. math::
   :label: eq_gonad_growth

   \dfrac{dg}{dt}(i,t) = 
   \begin{cases}
    \eta \rho(i, t) E_P(i,t) & \text{ if $E_P \geq 0$} \\
    \eta E_P(i,t) &  \text{ if $-g(i,t) \leq \eta E_P(i,t) < 0$}\\
    -g(i,t) & \text{ if $\eta E_P(i,t) < -g(i, t)$}\\
    \end{cases}

where :math:`\eta` is the ratio of energy density between somatic and gonadic tissues, and the second and third expressions account for maintenance coverage by energy reserves contained in gonads. In the former case, gonads’ energy can fully cover maintenance costs but in the latter it cannot, so that individuals undergo energetic starvation and incur additional starvation mortality (see section 5 Mortality for more details).

Equation :eq:`eq_somatic_growth` mechanistically describes somatic mass increment at each time step. The length of an individual of school  at time  is then obtained from the length-mass allometric relationship:

.. math:: 

   L(i,t) = k w(i,t)^{\alpha}

where :math:`k` and :math:`\alpha` are allometric parameters.


