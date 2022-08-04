Design concepts
==========================

Following the ODD framework and terminology, we briefly present here some design concepts characterizing OSMOSE:

- *Collectives*. As the total number of fish (from eggs to adult fish) to be taken into account in the simulated system can reach a value of the order of 10^12, the model was not brought down to the fish level but to an aggregated level consisting of a group of fish having similar ecological attributes. The unit of action and interaction, i.e., the “super-individual” as defined by Scheffer et al. (1995), is a group of fish having the same size, the same spatial coordinates, requiring similar food, and belonging to the same species (therefore having similar physiological and morphological characteristics). For convenience, this super-individual is also called a “fish school” in the following sections.

- *Interaction*. Super-individuals/schools interact locally through predation events.

- *Emergence.* From these local interactions, population and community dynamics emerge. In particular, the whole food web structure emerges from size-based local predation interactions.

- *Sensing.* Schools are assumed to know perfectly all the potential prey which are located in its vicinity, i.e. in the same cell of the grid. They also know the limits of their habitat.

- *Stochasticity.* There are different sources of stochasticity in the model. First, the order at which schools act and interact. There is a randomization of the precedence of schools crossed with a randomization of different sources of mortality (predation, fishing, other natural mortality). In addition, fish movements are also randomized within their habitats.

- *Observation.* For model testing and fitting, a variety of auxiliary state variables can be used in output of the model and compared with observations, data time series, and maps. Typically, species biomass and commercial catches, age or size distribution of abundance/biomass, diets can be confronted to observations.


