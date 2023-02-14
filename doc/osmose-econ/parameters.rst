Parameters
---------------------------------------------

Short explanation of parameters

.. table:: Estimated parameters

    .. csv-table::
        :delim: ;

        :math:`c_{i,0}` ; baseline costs of harvesting in the year :math:`t_0` (usually 2020)
        :math:`\tau_{i}` ; exponential time trend (positive or negative) on costs of harvesting 
        :math:`\chi_{i}` ; stock elasticity. If >1 the stock is hypersensitive: fishing costs decline fast with accessible biomass. If <1 the stock is hyperstable and fishing costs remain relatively constant with the available biomass
        :math:`\beta_{i,s}` ; consumer preferences for different sizes (s) of each species (i). :math:`sum_s \beta_{i,s}=1`
        :math:`\alpha_{i}` ; consumer preferences for different species (i). :math:`sum_i \alpha_{i}=1`
        :math:`\eta` ; elasticity of demand for fish 
        :math:`\gamma` ; weight of fish consumption in total utility. When :math:`\eta=1` it is the total expenditure on fish

.. table:: Parameters from the literature

    .. csv-table::
        :delim: ;
        
        :math:`\mu_i` ; elasticity of substitution between different sizes of a species (i). Higher values indicate easier substitutability
        :math:`\sigma` ; elasticity of substitution between species of fish. Should be lower than :math:`\mu_i` since sizes are better substitutes than species.
