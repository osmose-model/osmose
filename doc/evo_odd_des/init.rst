.. _evo_init:

Initialization
+++++++++++++++++

For each species, the initial pool of alleles present in the population for each
functional or neutral locus is randomly drawn from a prescribed distribution (see :numref:`genetic_structure`). The system starts with no
schools in the domain and eggs are released
for every species during the specific reproductive season time step during a defined seeding period.
The eggs are grouped in super-individuals, representing schools that are distributed spatially according to
their habitat maps. For each school, a diploid genotype is randomly drawn from the functional and neutral pools
of alleles at each locus.

For a given species, the seeding process stops when there is at least
one mature individual in the population. If the mature part of the population collapses before
the end of the seeding period, the seeding process is resumed (see maturation description in :numref:`maturation`).
Seeding ceases definitely at the end of the defined period. If a simulation is stopped before the end of the seeding period,
the randomly drawn initial pools of functional and neutral alleles need to be saved as state variables in addition to those
described in :numref:`state_variables` in order to be able to
restart the ecosystem dynamics from the stopping time step.