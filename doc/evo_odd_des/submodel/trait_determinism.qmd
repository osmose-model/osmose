(trait-deter)=

# Traits’ genetic determinism and expression

The two alleles $A_{Z, l, k1}(i)$ and $A_{Z, l, k2}(i)$  at a functional locus $l$ ($l \in [1, 2, ... l_Z]$)  coding
for trait $Z(i)$
of diploid individual $i$  can each take one allelic value among the $n_{Z, l}$  versions possible in
the population. Alleles act additively at and between loci.
Since allelic values describe deviations around the mean genotypic value of trait $Z$, the genotype value $G_Z(i)$
for trait $Z(i)$  in school $i$
is thus the sum of the initial genotypic mean $\overline{G_Z}(0)$  of the trait for the population and of the
two allelic values $A{Z, l, k}$
at each locus $l$  coding for the trait of interest:

$$
G_Z(i) = \overline{G_Z}(0) + \sum_{l=1}^{l=l_Z} (A_{Z, l, k1} + A_{Z, l, k2})
$$

Given the normal distribution additive property and that the initial distributions  $N\left(0, \dfrac{\sigma^2_{A, Z}(0)}{2 l_Z}\right)$ of allelic values in the population are independent between loci, the initial distribution of genotypic values $G_Z(i)$  in the population thus follows a normal
distribution $N(\overline{G_Z}(0), \sigma^2_{A,Z}(0))$ . At later time steps $t$, the processes of selection, drift and inheritance will modify this distribution in terms of its mean $\overline{G_Z}(t)$  and its variance $\sigma^2_{A,Z}(t)$  but also potentially in terms of its shape as it is not constrained to stay normally distributed.

In EV-OSMOSE, part of the phenotypic expression of emerging variables (e.g. somatic mass $w(i,t)$ , gonadic
mass $g(i,t)$ , length  at maturation $L_m(i)$) is emerging from the bioenergetic responses to conditions
faced by an individual: the available food, the temperature and the oxygen concentration in the environment during the
entire individual life cycle. In contrast, the four evolving traits (maximum mass-specific ingestion rate $I_{max}$, gonado-somatic
index $r$, intercept  and slope  of linear maturation reaction norm, $m_0$ and $m_1$) describe underlying
individual characteristics whose phenotypic expression does not depend on these “macro-environmental” conditions. Yet, the phenotypic
expression of evolving traits will also be affected by dominance and recessivity of alleles at the same locus and epistasis between
loci, which are not modeled explicitly in the above genetic model, as well as by “micro-environmental” variations capturing the
potentially unaccounted effects of individuals’ internal environment or external micro-environment {cite}`lynch1998genetics`.
These sources of phenotypic variability for evolving trait  are implicitly represented by an expression noise $e_Z(i)$
randomly drawn from a normal distribution $N(0, \sigma^2_{e, Z})$  at the individual’s birth and added to the genotypic
value of its trait $Z$. The phenotypic value of evolving trait $z$  for individual $i$  is then:

$$
Z(i) = G_Z(i) + e_Z(i)
$$
