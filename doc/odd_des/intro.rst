Introduction
===========================

The present document contains all the necessary information and guidelines to help understand the principles and assumptions of the OSMOSE model, 
apply it to a specific case study and run simulations for addressing specific issues. The OSMOSE model aims at exploring fish community dynamics and the 
ecosystem effects of fishing and climate change. It is an Individual-based model (IBM) which focuses on fish species and their trophic interactions (:cite:`Shin2001,Shin2004`). 
The model description follows the ODD ("Overview", "Design concept" and "Details") protocol for describing individual- or agent-based models (Grimm et al. 2006, Grimm et al. 2010).

The Osmose model assumes opportunistic predation based on spatial co-occurrence and size adequacy between a predator and its prey (size-based opportunistic predation). It represents fish individuals grouped in schools, which are characterized by their size, weight, age, taxonomy and geographical location (2D model), and which undergo different processes of fish life cycle (growth, explicit predation, natural and starvation mortalities, reproduction and migration) and a fishing mortality distinct for each species and structured by age/size, space and season. The model needs basic biological parameters for growth and reproduction processes, that are often available for a wide range of species, and which can be found in FishBase for instance. It also needs to be forced by spatial distribution maps for each species, by age/size/stage and by season depending on data availability. In output, a variety of size-based and species-based ecological indicators can be produced and compared to in situ data (surveys and catch data) at different levels of aggregation: at the species level (e.g. mean size, mean size-at-age, maximum size, mean trophic level, within-species distribution of TL), and at the community level (e.g. slope and intercept of size spectrum, Shannon diversity index, mean TL of catch). The model can be fitted to observed biomass and catch data, using a dedicated evolutionary algorithm. Recent developments have focused on the coupling of OSMOSE to various hydrodynamic and biogeochemical models, allowing to build end-to-end models of marine ecosystems that explicit combined effects of climate and fishing on fish dynamics. 

.. .. include:: overview/purpose.txt
.. .. include:: overview/statevar.txt
.. .. include:: overview/input.txt
.. .. include:: overview/proc_sche.txt
.. .. include:: overview/design_concepts.txt
.. .. include:: overview/init.txt
.. .. include:: overview/submodel.txt
