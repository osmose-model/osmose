Design concepts
==========================

Basic principles
++++++++++++++++++++++++++


EV-OSMOSE relies on a number of well-established concepts and theories and combines them in an original way to describe marine fish biodiversity and its dynamics from the intra-specific, genetic and phenotypic variability, to the inter-specific, taxonomic and trait-based level. Previous multi-species models of fish communities have been devised to project interspecific biodiversity trajectories under various scenarios considering only ecological dynamics. However, fish populations may also adapt to natural and anthropogenic pressures via phenotypic plasticity and/or evolutionary changes, leading to modifications in their physiology and life-history that could either mitigate or worsen the consequences of these pressures. EV-OSMOSE has been precisely developed to account for plastic and evolutionary dynamics in fish biodiversity projections by introducing the following elements to the existing OSMOSE model.

EV-OSMOSE describes explicitly mendelian inheritance of quantitative traits determined by polygenic genotypes according to quantitative genetics principles. These genotypes are composed of a finite number of loci and alleles per locus with effects of heterogeneous amplitude :cite:`soularue2012assortative`, which allows accounting realistically for both adaptive and neutral (genetic drift) evolutionary changes induced by natural and anthropogenic selective pressures. Genetically determined quantitative traits affect individuals’ bioenergetics and sexual maturation processes, which are described with a bioenergetic submodel.

Emergence
+++++++++++++++++++++++++++

Phenotypic values of schools’ evolving traits- maximum ingestion rate :math:`I_{max}` , gonado-somatic index :math:`r` , intercept
and slope  of the maturation reaction norm
:math:`m_0` and :math:`m_1`- are entirely determined by their genotype and randomly drawn expression noise. In contrast,
other individual variables or traits at higher integrative levels of organization (hereafter named "emerging variables":
somatic mass :math:`w(i,t)` , length :math:`L(i,t)` , gonadic mass :math:`g(i,t)`  and thus fecundity :math:`N_{eggs}(i,t)` ,
maturation age :math:`a_m(i)`
and somatic mass or length at maturation :math:`w_m(i)` and :math:`L_m(i)`) as well as starvation mortality
emerge from the combination of evolving traits’ values, energy intake from size-based opportunistic predation and physiological or plastic responses of bioenergetics to experienced sea water temperature and dissolved oxygen concentration (Figure 2).

.. mermaid:: _static/mermaid/trait_variations.md
    :caption: Ev-OSMOSE processes describing trait variations from loci to population level (pink) and the causes impacting trait values (blue).
    :align: center
