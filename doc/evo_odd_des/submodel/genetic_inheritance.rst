.. _genetic_inheritance:

Genetic inheritance
#############################

Both functional and neutral loci follow Mendelian inheritance under sexual reproduction. Reproduction is 
panmictic, which means that all sexually mature individuals can contribute to 
mating pairs of parents irrespective of their location and phenotype. When a new 
school is created during the breeding season (see section  Reproduction), its two parents are randomly drawn from a multinomial distribution  
:math:`M(2, p(t'))` for 2 trials with a probability vector :math:`p(t')` composed of as many elements :math:`p_i(t')` as there are schools in the population. The :math:`i^{th}` element of 
:math:`p_i(t')` is defined as the relative fecundity of school :math:`i`  in the population at the initial time step of the breeding season :math:`t`:

.. math::

    p_i(t') = \dfrac
    {N_{eggs}(i, t)}
    {\sum_{j \lor s(j) = s(i)} N_{eggs(j, t')}}

with :math:`N_{eggs}(i, t)` the fecundity of school :math:`i` and  :math:`\sum_{j \lor s(j) = s(i)} N_{eggs(j, t')}` the total fecundity
of the species :math:`s(i)` population at the initial time step of the breeding season :math:`t`. 

For each selected parental school, haploid gametes are assembled by randomly drawing one of the two alleles at each locus to represent allelic segregation during meiosis. This is done under the assumption of no linkage between loci, i.e. independence between loci, so that alleles recombine freely. New schools receive at each functional and neutral locus one allele from both chosen parents by randomly picking a haploid gamete for each of them.

