(reproduction)=

# Reproduction

Mature individuals spawn during the breeding season, which occurs when successive time steps $t$
have a spawning seasonality value $sp(t)$  (expressed as a fraction of spawning activity per time step and summing to 1 over one year) above 0.
At the first time step  of the breeding season, all the energy contained in the gonadic compartment $g(i, t)$  of individuals in school $i$  is used to produce eggs. The sex-ratio is assumed to be 1 for 1 for all species and the number of eggs produced by school $i$  for the whole breeding season is defined as follows:

$$
N_{eggs} (i, t) = N(i, t) \dfrac{g(i,t)}{ 2 w_{egg}}
$$

with $w_{egg}$  the weight of an egg. The pool $g(i,t)$  is then set to 0 and can start growing again (equation {eq}`eq_gonad_growth`) at the next time steps in view of the next spawning season. Then, the total number of eggs produced by the species $s(i)$  for the breeding season is obtained as

$$
N_{eggs_{tot}} = \sum_{j \lor s(j) = s(i)} N_{eggs}(j, t)
$$

At each time step $t$  of the breeding season, $N_s$ new schools are produced by species $s(i)$, with the number of eggs, and thus individuals, per new school $i'$  calculated as follows:

$$
N(i', t) = sp(t) \times \dfrac{N_{eggs_{tot}}}{N_{s}}
$$

with age of offspring set to 0 ($a(i', t) = 0$), their somatic weight set to the weight of an egg ($w(i', t) = w_{egg}$) and their gonadic weight set to 0 ($g(i,t) = 0$).
The new schools are released randomly depending on the specific larvae habitat map.
