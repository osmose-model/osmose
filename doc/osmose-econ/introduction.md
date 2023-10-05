# Introduction

The economic model takes into account supply and demand dynamics.
Supply depends mainly on fishing costst that are stock-dependent and technology-dependent.
Demand is driven by the price that consumers are willing to pay and consumer preferences for different species and different size classes.
Since evolutionary change impacts demographic changes within the fish population it is particularly interesting to study economic impacts given consumer preferences for size classes.

## Costs

Based on {cite}`Tahvonen2018`, {cite}`Quaas2018`, {cite}`Lancker2019`

Fishing costs depend on the availability of fish and the amount of fish harvested.
We assume perfect selectivity with respect to species, but imperfect selectivity with respect to the size of species.
Depending on the gear, larger or smaller fish of a species can be retained by the gear.
The part of the population that could potentially be harvested is the accessible biomass.
For example, with a trawl-like fishing net the probability of retainment increases with the size of the fish by a sigmoid pattern

$$
q_{i,s}(\sigma_{i,t})=\frac{1}{1+e^{-\omega_i \,(l_{i,s}-\sigma_{i,t})}}
$$

with length of fish $l_s$ in size class $s$, mesh sieze $\sigma_t$ in year $t$ and selectivity accuracy $\omega$ assumed to be constant.
The total available (or accessible) biomass of a species $i$ is therefore

$$
B_{i,t}=\sum_{s} q_{i,s}(\sigma_{i,t})\,w_{i,s}\,N_{i,s,t}
$$

where $w_{i,s}$ is the weight of fish species $i$ in size class $s$ and $N_{i,s,t}$ is the number of fish in that size class in a given year.

% When harvesting a total biomass of :math:`H_{i,t}`, this is distributed to the size classes according to the retainment, therefore
% h_{i,s,t}=q_{i,s}(\sigma_{i,t}) \, w_{i,s} \, N_{i,s,t}\frac{H_{i,t}}{B_{i,t}}.
% is the biomass of fish harvested from each size class in a given year.

The costs of harvesting fish increases with harvest and decreases when more fish are accessible.

$$
C_{i,t}=c_{i,t}\,\frac{H_{i,t}}{B_{i,t}^{\chi_i}},
$$

Here, $c_{i,t}$ are baseline harvesting costs that may increase over time (with increasing fuel prices) or decrease (with technological advancement), sucht that $c_{i,t}=c_{i,0} e^{\tau_i(t-t_0)}$ with trend $\tau_i$. The parameter $\chi_i$ is the stock elasticity. When $\chi_i<1$ the stock is hyperstable, meaning that at constant effort an increase in available biomass does not necessarily result in a higher harvest.

Fishermen's profit is the revenue from selling fish minus costs of harvesting.

$$
\Pi_{i,t} &=  \sum_{s} p_{i,s,t} \,h_{i,s,t} - C_{i,t} \\
$$

The profit margin is the fraction of profits with respect to revenues

$$
\pi_{i,t} = \frac{\sum_s p_{i,s,t} \, h_{is,t,} - C_{i,t}}{\sum_s p_{i,s,t} \, h_{is,t,}}
$$

## Demand

Based on {cite}`Quaas2013`, {cite}`Quaas2016`, {cite}`Quaas2018`, {cite}`Groeneveld2016`, {cite}`Tahvonen2018`, {cite}`Lancker2019`

Consumers have preferences for certain species but also size classes of fish.
Yet when preferred species are rare, and thus expensive, consumers can shift their consumption towards more abundant species, depending on the elasticity of demand.
The more elastic demand is, the easier it is to substitute.
The same holds for different size classes of fish, where demand is likely to be even more elastic - you would not pay an extremely high price for a bigger fish than for the same mass of small fishes.

The utility of fish consumption is

$$
\nu_t = \left( \sum_{i=1}^I \alpha_{i} \left( \sum_{s=1}^S \beta_{is} \, h_{is}^{\frac{\mu_i-1}{\mu_i}} \right) ^{\frac{\mu_i}{\mu_i-1} \frac{\sigma-1}{\sigma}} \right) ^{\frac{\sigma}{\sigma-1}}.
$$

Here $\mu_i$ is the elasticity of substitution for different size classes of fish species $i$, $\sigma$ is the elasticity of substitution between different species, $\beta_{i,s}$ are the constant consumer preferences for size classes and $\alpha_i$ are consumer preferences for species.

Consumers pay for fish consumption, therefore total utility is

$$
u(\nu_t) = \begin{cases}     \gamma \, \ln(\nu_t) - \sum_i \sum_s p_{i,s,t} \, h_{i,s,t}& \text{ if } \eta=1 \\     \gamma \, \frac{\eta}{\eta-1} \, \nu_t^{\frac{\eta-1}{\eta}} - \sum_i \sum_s p_{i,s,t} \, h_{i,s,t} & \text{ else} \end{cases}
$$

where $\gamma$ is the total expenditure on fish when $\eta=1$

Consomers maximise utility over the consumption of fish $h_{i,s,t}$ resulting in the inverse demand function

$$
p_{i,s,t}= \gamma \left( \beta_{i,s} \, h_{i,s,t}^{-\frac{1}{\mu_i}} \right) \, \alpha_i \left( \sum_k \beta_{i,k} h_{i,k,t} ^{\frac{\mu_i - 1}{\mu_i}}\right)^{\frac{\mu_i}{\mu_i-1}\frac{\sigma-1}{\sigma}-1} \nu^{\frac{1}{\sigma}-\frac{1}{\eta}}.
$$

## Social optimum

In total, the utility derived from fish consumption is the benefit obtained from fish consumption of different species and size classes minus the costs needed for their extraction.
Since individuals discount future utility (we prefer to have something today rather than tomorrow), the total net present value is

$$
NPV &= u(\nu_t) + \Pi_{i,t} \\ &= \sum_{t=t_0}^{t_0+T} \delta^t \left( \left(\gamma \, \frac{\eta}{\eta-1} \, \nu_t^{\frac{\eta-1}{\eta}} - \sum_i \sum_s p_{i,s,t} \, h_{i,s,t} \right) - \left( \sum_i \sum_s p_{i,s,t} \, h_{i,s,t}  - \sum_i C_{i,t}\right) \right) \\ &= \sum_{t=t_0}^{t_0+T} \delta^t \left(\gamma \, \frac{\eta}{\eta-1} \, \nu_t^{\frac{\eta-1}{\eta}} - \sum_i C_{i,t}\right)
$$

with discount factor $\delta$. The fist term is the consumers' utility and the last term is fishermen's profit.

A social planner, that accounts for consumer and producer surplus, would maximise this net present value over future control variables in the fishery, here future harvests and mesh sizes.
