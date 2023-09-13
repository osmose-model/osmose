Migration mortality
--------------------------

When a school is out of the domain, none of the standards processes (predation, growth, fishing, natural mortality, growth, starvation) apply. 

The migration mortality is used to simulate all sources of mortality outside the simulated area.
It applies as long as the school is located out of the simulated
area

.. math:: 

    N_{out} = N \times \left(1 - e^{-M_{out}}\right)

.. index:: mortality.out.rate.sp#

.. table:: Parameters for migration mortality

    .. csv-table::
        :delim: ;

        mortality.out.rate.sp# ; Annual mortality rate for species that move out of the simulated area (:math:`M_{out}`)
