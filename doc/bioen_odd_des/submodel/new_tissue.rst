.. _new_tissue_prod:

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

is the average net energy available per time step to individuals of school :math:`i`  since their birth, with :math:`\Delta t`  being the duration of a time step. Equation :eq:`eq_rho` differs from a deterministic continuous time version of the same model :cite:`boukal2014life, lester2004interpreting, quince2008biphasic` where the current net energy :math:`E_P(i,t)`  would be used instead of the average:math:`\overline{E_P}(i)`. The averaging in a stochastic discrete time individual-based model such as EV-OSMOSE insures a smooth increase of proportion :math:`\rho`  as individuals grow by dampening strong variations in :math:`E_P(i,t)`  and thus in :math:`\rho(i,t)`  due to the stochasticity of prey encounter and hence of ingested energy :math:`I(i, t)`.
