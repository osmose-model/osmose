Fishing parameters (Osmose <= 4.0.0)
---------------------------------------------

On Osmose versions **previous** to 4.0.0, fishing mortality was species-specific. It was parameterized either by providing fishing rates or catches.

.. index:: mortality.fishing.type, mortality.fishing.recruitment.age.sp#, mortality.fishing.recruitment.size.sp#

.. table:: Fishing parameters (< 4.0.0)
    :align: center

    .. csv-table:: 
        :delim: ;

        mortality.fishing.type ; Whether fishing mortality is provided as :samp:`rate` or :samp:`catches``
        mortality.fishing.recruitment.age.sp# ; Age at which a species can be fished (years)
        mortality.fishing.recruitment.size.sp# ; Size at which a species can be fished (cm)

.. warning::

    Osmose does not accept fishing mortality rates for some species and catches for other species.

By rate
@@@@@@@@@@@@@@@@@@@@@@@@ 

If mortality rate is provided, the number of dead fishes in a school is computed as follows.

.. math:: 

    N_{fishing} = N \times \left(1 - exp^{-F}\right)

Osmose offers several degrees of refinement for inputting the fishing mortality: constant, seasonal, interannual and interannual with age or size class. Depending on the available information for each species of the configuration, one must choose the best way to input it. Each species can be parameterized independently from an other. For instance fishing mortality for species zero is a constant annual rate and fishing mortality for species three is provided as a time series per size class.

.. index:: mortality.fishing.rate.byDt.byAge.file.sp#, mortality.fishing.rate.byDt.bySize.file.sp#, mortality.fishing.rate.byYear.file.sp#, mortality.fishing.rate.sp#, mortality.fishing.season.distrib.file.sp#

.. table:: Parameters for fishing rate.
    :align: center

    .. csv-table:: 
        :delim: ;

        mortality.fishing.rate.byDt.byAge.file.sp# ; CSV file containing the fishing rates by age and by time-step
        mortality.fishing.rate.byDt.bySize.file.sp# ; CSV file containing the fishing rates by size and by time-step
        mortality.fishing.rate.byYear.file.sp# ; File containing the fishing rates by year
        mortality.fishing.rate.sp# ; Annual fishing rate
        mortality.fishing.season.distrib.file.sp# ; File containing the seasonal distribution of fishing mortality. If not provided, assumes constant fishing rate

Osmose will first look for any of the first two parameters. If not found, it will look for the third one. If not found, Osmose will finally look for
the fourth one. If the third or fourth parameter are found, it will also looks for the fifth one. 


By catches
@@@@@@@@@@@@@@@

If mortality is provided by catches, the number dead individuals is computed as follows:

.. math:: 

    N_{fish} = min\left(N, C \times \frac{B_{fish}} {B_{fishable} \times W}\right)\ if\ B_{fishable} > 0

with :math:`C` the catches, :math:`B_{fish}` the fish biomass, :math:`W` its weight and :math:`B_{fishable}` the biomass
that can be fished.

.. index:: mortality.fishing.catches.byDt.byAge.file.sp#, mortality.fishing.catches.byDt.bySize.file.sp#, mortality.fishing.catches.byYear.file.sp#, mortality.fishing.catches.sp#, mortality.fishing.season.distrib.file.sp#

.. table:: Parameters for fishing catches.
    :align: center

    .. csv-table:: 
        :delim: ;

        mortality.fishing.catches.byDt.byAge.file.sp# ; CSV file containing the fishing rates by age and by time-step
        mortality.fishing.catches.byDt.bySize.file.sp# ; CSV file containing the fishing rates by size and by time-step
        mortality.fishing.catches.byYear.file.sp# ; File containing the fishing rates by year
        mortality.fishing.catches.sp# ; Annual fishing rate
        mortality.fishing.season.distrib.file.sp# ; File containing the seasonal distribution of fishing mortality

.. note:: 
    
    Catches are assumed to be in tons.

Osmose will first look for any of the first two parameters. If not found, it will look for the third one. If not found, Osmose will finally look for
the fourth one. If the third or fourth parameter are found, it will also looks for the fifth one. 

Marine Protected Areas (MPAs)
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@

The user can defined as many MPA as he wishes.

.. index:: mpa.file.mpa#, mpa.start.year.mpa#, mpa.end.year.mpa#

.. table:: Parameters for setting MPA

    .. csv-table:: 
        :delim: ;

        mpa.file.mpa# ; File containing the MPA definition
        mpa.start.year.mpa# ; First year when this MPA is active
        mpa.end.year.mpa# ; Last year when this MPA is active

The map is a CSV file similar to the movement maps. The CSV file has the same number of lines and columns as the OSMOSE grid. The MPA file
must contain 1 where the MPA is defined, 0 elsewhere. 

Start year and end year parameters define the time span when the MPA is enabled.

The MPA are handled within the fishing process. Every time there is 
a new MPA to be activated or deactivated, Osmose updates the correction 
factor that will be applied to the fishing mortality rates in order to take 
into account the uniform redistribution of the fishing effort outside the MPAs.

