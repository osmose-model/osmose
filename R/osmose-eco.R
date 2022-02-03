#### CALIBRATION
#
## data requirements:
## data for pi (profit margin, usually 0) in [0, 1]
## data for prices (per species, size class, time step) in Eur/kg
#
## osmose output requirements:
## osmose output of harvest (per species, size class, time step) in 1000t
## osmose output of number of fish (per species, size class, time step)
#
## i is the species index
## s is the size class index
## t is the time index
#
#
## choose elasticity of substitution between different fish species
#sigma <- 8
## choose elasticity of substitution between different size classes. for each species
#mu[i] <- 4 # for all i
#
## define catchability
#for (i in 1:length(species)){
#    # get total harvest of a species
#    total_harvest[i, t] <- sum(harvest[i, s, t], axis=2)
#    for (s in 1:length(size_classes[i])){
#        # calculate distibution of harvest over size classes
#        catchability[i, s, t] <-  harvest[i, s, t] / total_harvest[i, t]
#    }
#    # normalise
#    catchability[i, s, t] <- catchability[i, s, t] / max(catchability[i, s, t], axis=2)
#}
#
## calculate catchable biomass
#for (i in 1:length(species)){
#    Biomass[i] <- sum(catchability[i] * numberOfFish[i], axis=1)  # sum over size classes, not time. should be 1dim vector over time
#}
#
## cost parameter estimation:
#for (i in 1:length(species)){
#    Y <- log(1 - pi[i]) * log(sum(prices[i] * weight[i] * catchability[i] * numberOfFish[i], axis=1))  # sum over size classes, not time. should be 1dim vector over time
#    X0 <- ones(length(time))
#    X1 <- log(Biomass[i]) # total catchable biomass
#    X2 <- time
#    X <- [X0, X1, X2]
#    solution <- lm(Y ~ X)
#    # user should now check whether regression is ok
#    print(summary(solution)) 
#    # coefficients and standard errors are not yet the parameters we need.
#    # transform and store coefficients
#    c[species_index] <- exp(coefficient[1])  # intercept --> baseline costs
#    chi[species_index] <- 1 - coefficient[2]  # first parameter --> stock elasticity
#    tau[species_index] <- coefficient[3]  # second parameter for time trend in costs
#    # transform and store standard errors
#    std_c[species_index] <- abs(c[species_index]) * stdEr[1]
#    std_chi[species_index] <- stdEr[2]
#    std_tau[species_index] <- stdEr[3]
#}
#
## demand parameters estimation - size preferences:
#for (i in 1:length(species)){
#    Y = (prices[i] * (harvest[i]) ** (1 / mu[i]))  # 2dim array: size classes x time
#    X = sum(Y, axis=1)  # sum over size classes. 1dim array over time
#    solution <- lm(Y ~ X)
#    # user should now check whether regression is ok
#    print(summary(solution)) 
#    betas <- coefficients  # preferences for different size classes. 1dim vector: size classes
#}
#
#
## demand parameters estimation - species preferences:
#P = sum( prices * harvest^(1/mu) / beta, axis=2)  # sum over size classes. 2dim vector species x time
#H = (sum(beta * harvest^((mu-1)/mu), axis=2))^((mu-sigma)/(mu-1))  # sum over size classes. 2dim vector species x time
#Y = P * H^(1/sigma)
#X = sum(Y, axis=1)  # sum over species. 1dim vector over time
#solution <- lm(Y ~ X)
#print(summary(solution)) 
#alphas <- coefficients  # preferences for different species. 1dim vector: species
#
## demand parameters estimation
#v = sum(sum(betas * harvest^((mu-1)/mu), axis=2)^(mu/(mu-1)*(sigma-1)/sigma), axis=1)^(sigma/(sigma-1))  # sum over species and size classes
#Y = log(sum(prices * harvest, axis=1,2))  # sum over species and sizes. 1dim vector over time
#X = log(v)
#solution <- lm(Y ~ X)
#print(summary(solution)) 
#gamma <- exp(coefficients[1])
#eta <- 1/(1-coefficients[2])
#std_gamma <- abs(gamma) * stdEr[1]
#std_eta <- eta^2 * stdEr[2]
#
