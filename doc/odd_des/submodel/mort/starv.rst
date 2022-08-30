Starvation mortality
@@@@@@@@@@@@@@@@@@@@@@@@@@@

Starvation mortality is applied as follows:

.. math:: 

    N_{starv} = N \times (1 - e^{-M_{starv}})

with :math:`M_{starv}` the starvation mortality rate, which is computed as follows:

.. math:: 

    M_{starv} =  M_{max} \times \left(1 - \frac{S_R}{C_{S_R}}\right)\ if\ S_R \le C_{S_R}

.. note:: 

    Starvation mortality rate applied at time step :math:`t` is based on the predation success computed at time-step 
    :math:`t-1`

.. index:: mortality.starvation.rate.max.sp#, predation.efficiency.critical.sp#

.. table:: Starvation mortality parameters
    
    .. csv-table:: 
        :delim: ;

        mortality.starvation.rate.max.sp# ; Maximum rate of starvation mortality :math:`M_{max}`
        predation.efficiency.critical.sp# ; Critical predation success rate :math:`C_{S_R}`

