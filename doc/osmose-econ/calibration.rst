Calibration
---------------------------------------------

We estimate parameters of the cost function given catch, accessible biomass and profitability data. Species and size class preferences are estimated using the inverse demand function and assuming market equilibrium.

Cost parameter estimation
############################################



Rearranging the definition of the profit margin we get 

.. math::
 \ln(1-\pi_{i,t}) \, \ln \left(\frac{\sum_s \, p_{i,s,t} h_{i,s,t}}{H_{i,t}} \right)= \ln(c_{i0}) -\chi_i \, \ln(B_{it}) + \tau_i \, t,

and we can estimate baseline costs :math:`c_{i,0}`, stock elasticity :math:`\chi_{i}` and the time trend on fishing costs :math:`\tau_{i}`.

Demand parameter estimation
############################################

Rearranging the inverse demand function we get

.. math::
    p_{i,s, t} \, h_{i,s,t}^{\frac{1}{\mu_i}} =\beta_{i,s} \,  \sum_j p_{i,j,t} \, h_{i,j,t}^ {\frac{1}{\mu_i}} 

and

.. math::
    \mathcal{P}_{i,t} \, \mathcal{H}_{i,t}^{\frac 1 \sigma} = \alpha_i \, \sum_j \mathcal{P}_{j,t} \,  \mathcal{H}_{j,t}^{\frac 1 \sigma}


with :math:`\mathcal{P}_{i,t}:=\sum_k \frac{p_{i,k,t} \, h_{i,k,t}^{\frac 1 \mu_i}}{\beta_{i,k}}` and :math:`\mathcal{H}_{i,t} :=\left(\sum_k \beta_{i,k} \, h_{i,k,t}^{\frac{\mu_i -1}{\mu_i}} \right)^{\frac{\mu_i-\sigma}{\mu_i-1}}` we can first estimate size preferences :math:`\beta_{i,s}` for each species and then species preferences :math:`\alpha_i`.

The elasticities of substitution between size classes :math:`\mu_{i}` and between species :math:`\sigma` are either taken from the literature or assumed.

Furthermore, given :math:`\nu_t` as defined before and using

.. math::
    \ln \left( \sum_i \sum_s p_{i,s,t} \, h_{i,s,t} \right) = \ln(\gamma) + \frac{\eta-1}{\eta} \ln(\nu_t) 

we estimate :math:`\gamma` and :math:`\eta`.

Data requirements
######################

For the parameterisation we need 

- profit margins :math:`\pi_{it}`. If this is not available and the fishery has been operating at or close to open access conditions the profit margin is zero.
- prices per kg :math:`p_{i,s,t}`
- the weight of individuals :math:`w_{i,s}` in kg
- the catchability :math:`q_{i,s,t}`, the proportion of individuals in a size class that are retained in the net. Needs to be between 0 and 1. Alternatively one can fit the catchability to fishing mortalities :math:`F_{i,s,t}=F^{max}_{i,t} \, q_{i,s,t}`.
- population numbers :math:`N_{i,s,t}`
- instead of catchability and numbers it is possible to provide instead the harvest :math:`h_{i,s,t}` and the accessible biomass :math:`B_{i,t}`

for species (i), size class (s) and year (t) as indicated.
