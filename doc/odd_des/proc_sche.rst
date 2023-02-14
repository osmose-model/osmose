Process overview and scheduling
==================================

The life cycle of each focus species included in the OSMOSE model is modelled, starting with the egg stage. At the first time step, eggs are produced and split into a number of super-individuals called schools. At each time step, OSMOSE simulates the main life history processes for these schools, starting with the release of fish schools within their distribution area which is specified in input for each species and by age when presence/absence data are available. Then different sources of mortality are applied including predation, fishing, starvation and other natural mortality. In OSMOSE, predation is assumed to be opportunistic and based on predator and prey size adequation and spatio-temporal co-occurrence. Depending on the predation success, somatic growth is then implemented and mature individuals spawn at the end of the time step and produce new eggs for the next step.

.. mermaid:: _static/mermaid/scheduling.md
    :caption: Scheduling of the different Osmose processes
    :align: center