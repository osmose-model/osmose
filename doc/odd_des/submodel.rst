Submodels
==========================

.. table paramètres: notation math, nom code, description, unité

Spatial distribution of schools
----------------------------------

The spatial distribution of schools at each time step is driven by input maps that depend on the focus species, the size/age of the fish, the year and the season. The maps in input can be derived directly from presence/absence or density survey data. They can also be produced by statistical climate niche models which determine the probability of fish presence in a given cell of the grid according to a set of predefined environmental variables (sea surface temperature, phytoplankton concentration, sea surface height, O2, etc). The climate niche models can be run using in situ data (as in Gruss et al. 2015) or using output from coupled hydrodynamic-BGC models (as in Oliveros-Ramos 2014). At each time step, if fish schools are assigned a new distribution map (due to aging, season, year, growth) or when new eggs are released, schools are distributed uniformly over the distribution area corresponding to their age/size, species, season, year of simulation. When the maps do not change from one time step to the next (for example within a season, or the same map is used for several age/size classes of a species), schools can move to adjacent cells within their distribution area following a random walk. Random walk movements are meant to represent small-scale foraging movements and diffusion.

Computation of mortalities
----------------------------------

Within each time step, the total mortality of a given school i is comprised of predation mortality caused by various schools j (Mpredation i, j), starvation mortality (Mstarvation i), fishing mortality (Fi), and diverse other natural mortality rate (Mdiverse i). The four different mortalities are computed so as to represent quasi simultaneous processes, and we consider that there is competition and stochasticity in the predation process.

Within each time step, OSMOSE considers each pair of {school-source of mortality} in turn in a random order. To ensure that the random order of the mortality sources and the schools does not bias the resulting instantaneous mortality rates applied and effectively correspond to the mortality rates specified in input (for fishing and diverse natural mortality), all the mortality events are iterated within a time step, over a fixed number of sub-time steps (user-defined as nsubdt, by default set to 10).

Predation
++++++++++++++++++
The central assumption in OSMOSE is that predation is an opportunistic process, which depends on: (1) the overlap between predators and potential prey items in the horizontal dimension; (2) size adequacy between the predators and the potential prey (determined by ‘predator/prey size ratios’); and when the information is available (3) the accessibility of prey items to predators, which depends on their vertical distribution (this being determined by means of ‘accessibility coefficients’). Thus, in OSMOSE, the food web structure emerges from local predation and competition interactions.

Starvation
++++++++++++++++++

Diverse natural mortality
+++++++++++++++++++++++++++++

An additional source of natural mortality other than predation and starvation is applied to all schools older than 1 month: Mdiverse, which is the mortality due to marine organisms (top predators) and events (e.g., red tide events, diseases) that are not explicitly considered in OSMOSE. Moreover, an additional source of natural mortality other than predation is applied to the first age class corresponding to eggs and larvae (0–1 month old individuals): Mdiverse0, which is due to different causes (e.g., non-fertilization of eggs, advection away from suitable habitat,
sinking, mortality of first-feeding larvae). For recruited stages, the Mdiverse parameter can be estimated from the predation mortality rate by marine organisms that are considered in Ecopath
model but not in OSMOSE. Mdiverse0 is unknown for almost all the HTL groups represented in OSMOSE. Therefore, this parameter is estimated during the calibration process of OSMOSE.

Growth
----------------------------------------

Individuals of a given school i are assumed to grow in size and weight at time t only when the amount of food they ingested fulfill maintenance requirements, i.e., only when their predation efficiency at t is greater than the predation efficiency ensuring body maintenance of school. In such a case, the growth in length of school i at time t ( ) varies between 0 and twice the mean length increase :math:`Delta L` calculated from a von Bertalanffy model (:cite:`Shin2001,Shin2004`):
 
.. math::

    \Delta L_{max} = L_{\infty} \times \exp^{-K\left(age - t_0\right)} \left(1 - \exp^{-K}\right)

.. math::

    \Delta L = 0 \ if\ \zeta_i<\zeta_{crit}

.. math::

    \Delta L = 2 \Delta L_{max} \frac{\zeta_i - \zeta_{crit}}{1-\zeta_{crit}} \ if\ \zeta_i>\zeta_{crit}


(B.1)
A von Bertalanffy model is used to calculate mean length increase above a threshold age Athres determined for each HTL group from the literature (Table B1). Below Athres, a simple linear model is used. The rationale behind this is that von Bertalanffy parameters are usually estimated from data excluding youngs of the year or including only very few of them. Assuming a linear growth between age 0 day and Athres ensures a more realistic calculation of mean length increases for early ages of HTL groups (:cite:`Travers2009`). The weight of school i at time t is evaluated from the allometric relationship: 

.. math::

    W = C \times L^b

where :math:`b` and :math:`C` are allometric parameters for the HTL group to which school i belongs (Table B1).

Reproduction
---------------------------

Any school whose length is greater than the length at sexual maturity Lmat reproduces at the end of each time step, allowing for the generation of new schools at the eggs stage for the next time step. At the scale of the HTL group, the number of eggs produced at time t ( ) is calculated as:
 
(B.3)
where SR is the female: male sex ratio of the HTL group;  the relative annual fecundity of the group (number of eggs spawned per gram of mature female per year);  the probability for the HTL group to spawn a given month relatively to the other months of the year (Table B1); and the spawning stock biomass of the group at time t. In the absence of information, we assumed no seasonality of reproduction for reef omnivores. The   parameters of all other HTL groups were estimated from the literature (Fig. B2 and Table B2). The eggs of all HTL groups are allocated a size of 1 mm, which appears to be a representative average estimate for marine fish species regardless of the body size of the adults (Cury and Pauly, 2000), and a weight of 0.0005386 g, considering eggs as spheres with water density.
It can be noted that, since the growth of schools is evaluated in relation to their predation efficiency, the number of eggs produced at each time step, which depends on biomass (Eq. B.3), also depends implicitly on the food intake of schools (Shin & Cury, 2001, 2004). 
