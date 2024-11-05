package fr.ird.osmose.process.movement;

import fr.ird.osmose.School;

@FunctionalInterface
public interface GetMapDistribution {
    public void mapDistribution(School school, int istep);
}