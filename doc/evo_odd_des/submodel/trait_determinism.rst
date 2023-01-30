.. _trait_deter:

Traits’ genetic determinism and expression 
######################################################

The two alleles :math:`A_{Z, l, k1}(i)` and :math:`A_{Z, l, k2}(i)`  at a functional locus :math:`l` (:math:`l \in [1, 2, ... l_Z]`)  coding for trait :math:`Z(i)`
of diploid individual :math:`i`  can each take one allelic value among the :math:`n_{Z, l}`  versions possible in the population. Alleles act additively at and between loci. 
Since allelic values describe deviations around the mean genotypic value of trait :math:`Z`, the genotype value :math:`G_Z(i)` for trait :math:`Z(i)`  in school :math:`i`
is thus the sum of the initial genotypic mean :math:`\overbar{G_Z}(0)`  of the trait for the population and of the two allelic values :math:`A{Z, l, k}`
at each locus :math:`l`  coding for the trait of interest:

.. math::

    G_Z(i) = \overbar{G_Z}(0) + \sum_{l=1}^{l=l_Z} (A_{Z, l, k1} + A_{Z, l, k2})


Given the normal distribution additive property and that the initial distributions  :math:`N\left(0, \dfrac{\sigma^2_{A, Z}(0)}{2 l_Z}\right)` of allelic values in the population are independent between loci, the initial distribution of genotypic values :math:`G_Z(i)`  in the population thus follows a normal 
distribution :math:`N(\overbar{G_Z}(0), \sigma^2_{A,Z}(0))` . At later time steps :math:`t`, the processes of selection, drift and inheritance will modify this distribution in terms of its mean :math:`\overbar{G_Z}(t)`  and its variance :math:`\sigma^2_{A,Z}(t)`  but also potentially in terms of its shape as it is not constrained to stay normally distributed.

In EV-OSMOSE, part of the phenotypic expression of emerging variables (e.g. somatic mass :math:`w(i,t)` , gonadic mass :math:`g(i,t)` , length  at maturation :math:`L_m(i)`) is emerging from the bioenergetic responses to conditions faced by an individual: the available food, the temperature and the oxygen concentration in the environment during the entire individual life cycle. In contrast, the four evolving traits (maximum mass-specific ingestion rate :math:`I_{max}` , gonado-somatic index :math:`r`, intercept  and slope  of linear maturation reaction norm, :math:`m_0` and :math:`m_1`) describe underlying individual characteristics whose phenotypic expression does not depend on these “macro-environmental” conditions. Yet, the phenotypic expression of evolving traits will also be affected by dominance and recessivity of alleles at the same locus and epistasis between loci, which are not modeled explicitly in the above genetic model, as well as by “micro-environmental” variations capturing the potentially unaccounted effects of individuals’ internal environment or external micro-environment :cite:`lynch1998genetics`. These sources of phenotypic variability for evolving trait  are implicitly represented by an expression noise :math:`e_Z(i)`  randomly drawn from a normal distribution :math:`N(0, \sigma^2_{e, Z})`  at the individual’s birth and added to the genotypic value of its trait :math:`Z`. 
The phenotypic value of evolving trait :math:`z`  for individual :math:`i`  is then:

.. math::

    Z(i) = G_Z(i) + e_Z(i)
