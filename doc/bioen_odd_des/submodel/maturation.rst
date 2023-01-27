.. _maturation:

Maturation
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

Age and size at maturation vary strongly between individuals due to phenotypic plasticity. This plasticity in maturation is modeled 
by a deterministic linear maturation reaction norm (LMRN) that represents all 
the age-length combinations at which an individual can become mature :cite:`stearns1986evolution, stearns1992evolution`. In this framework, 
individuals become sexually mature when their growth trajectory in term of body length intersects the LMRN, 
so that the maturity status  of individuals of school  at time step  is then described as:

.. math::

    m(i,t) =
    \begin{cases}
    0 & \text{if $L(i,t) < m_0(i) + m_1(i) a(i,t)$ (immature)} \\
    1 & \text{if $L(i,t) \geq m_0(i) + m_1(i) a(i,t)$ (mature)}
    \end{cases}

with :math:`m_0(i)` and :math:`m_1(i)`  the intercept and slope of the LMRN, respectively. 

