(genetic-structure)=

# Genetic structure

The genetic structure is described by a polygenic multi-allelic model with
finite numbers of loci and alleles for both the functional and neutral parts of the
genome. The value of trait $Z$ thus results from the expression of $l_Z$  functional loci, each
of which has a pool of $n_{Z, l}$ (with $l \in [1, 2, ..., l_Z]$) possible alleles in the initial population characterized by their
allelic value $A_{Z, l, k}$  (with  $k \in [1, 2, ... n_{Z, l}]$).

Following classical quantitative genetics {cite}`lynch1998genetics`, we assume that
the genotypic values $G_Z(i)$  of trait $Z$  in the population follow initially a normal distribution:

$$
N(\overline{G_Z}(0), \sigma^2_{A, Z}(0))
$$

with $\overline{G_Z}(0)$  the initial genotypic mean and $\sigma^2_{A, Z}(0)$ the initial additive genetic variance.
It follows (see justification in the next section) that the allelic
values $A_{Z, l, k}$  of the $n_{Z, l}$  alleles of locus $l$  initially present in the population are randomly drawn from a normal
distribution $N\left(0, \dfrac{\sigma^2_{A, Z}(0)}{2 l_Z}\right)$
{cite}`soularue2012assortative`. This allelic model defines allelic values as deviations
around the initial genotypvic mean $\overline{G_Z}(0)$ of the population and allows for heterogeneous allelic values
across loci coding for the same trait, many of them with minor effects and a few ones with major effects.

Similarly, the neutral part of the genome is described by $l_b$  neutral loci, each of which has a pool
of $n_{b, l}$  (with $l \in [1, 2, ..., l_b]$)
possible alleles in the initial population characterized by their allelic
identity $b_{l,k}$  (with $k \in [1, 2, ... n_{b, l}]$) with no effect of
evolving trait values. The allelic identities $b_{l,k}$  of the  alleles of
locus $l$  initially present in the population are randomly drawn from a discrete uniform distribution with probability
mass function $\dfrac{1}{n_{b,l}}$.
