Purpose and patterns
===========================

Purpose
+++++++++++++++++++++++++++++++

The OSMOSE model represents the dynamics of fish communities. It is a multispecies and spatial model
which lies on size-based predation, traits-based life history, and individual-based processes.
The model aims to explore the functioning of marine ecosystems, the ecosystem effects of
fishing and climate changes, the impacts of management measures (changes in fishing pressure
and fishing strategies, implementation of marine protected areas).

The OSMOSE model represents the ecosystem dynamics of fish communities in
marine ecosystems. It is an individual-based, spatially-explicit multispecies model
accounting for explicit trophic interactions. The main characteristics of the model are
opportunistic predation based on size and spatial co-occurrence of predators and preys

.. and the spatial distribution of individuals is constrained by their physiological niche as
.. they avoid physiologically unfavorable environment.

The aim of the model is to explore the
functioning of marine trophic webs, notably in response
to perturbations such as fishing or climate change.

.. The Osmose model assumes opportunistic predation based on spatial co-occurrence and size
.. adequacy between a predator and its prey (size-based opportunistic predation).
.. It represents fish individuals grouped in schools, which are characterized by their
.. size, weight, age, taxonomy and geographical location (2D model), and which undergo different
.. processes of fish life cycle (growth, explicit predation, natural and starvation mortalities,
.. reproduction and migration) and a fishing mortality distinct for each species and structured by age/size,
.. space and season. The model needs basic biological parameters for growth and
.. reproduction processes, that are often available for a wide range of species,
.. and which can be found in FishBase for instance. It also needs to be forced by
.. spatial distribution maps for each species, by age/size/stage and by season depending on data availability.
.. In output, a variety of size-based and species-based ecological indicators can be produced and compared
.. to in situ data (surveys and catch data) at different levels of aggregation: at the species
.. level (e.g. mean size, mean size-at-age, maximum size, mean trophic level, within-species
.. distribution of TL), and at the community level (e.g. slope and intercept of size spectrum, Shannon
.. diversity index, mean TL of catch). The model can be fitted to observed biomass and catch data,
.. using a dedicated evolutionary algorithm. Recent developments have focused on the coupling of
.. OSMOSE to various hydrodynamic and biogeochemical models, allowing to build end-to-end models
.. of marine ecosystems that explicit combined effects of climate and fishing on fish dynamics.

Patterns
++++++++++++++++++++++++++++

The model is evaluated by comparing the model outputs with observations
of the marine ecosystem. These observations generally include catches, catches at length, biomass,
abundance for different species.