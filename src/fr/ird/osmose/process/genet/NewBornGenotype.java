/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.genet;

import fr.ird.osmose.School;
import fr.ird.osmose.process.AbstractProcess;

/**
 *
 * @author amorell
 */
/**
 * This class is used to simulate meiosis and the formation of a new offspring
 * and the random drawn of two parents. It is applied to a new egg.
 *
 */
public class NewBornGenotype extends AbstractProcess {

    private int Lc;
    private int nEvolvingParam;
    private int iSpec;

    private School school;

    public NewBornGenotype(int rank, School school) {
        super(rank);
    }

    @Override
    public void init() {
        String key;

        key = "genet.Lc";
        Lc = getConfiguration().getInt(key);

        iSpec = school.getSpeciesIndex();
    }

    @Override
    public void run() {

        int[][][] Genotype_parent1 = ParentSelection(this.iSpec).getGenotype();
        int[][][] Genotype_parent2 = ParentSelection(this.iSpec).getGenotype();
        meiosis(Genotype_parent1, Genotype_parent2);
    }

    private int[][][] meiosis(int[][][] Genotype_parent1, int[][][] Genotype_parent2) {
        int[][][] Genotype_NewBorn;
        Genotype_NewBorn = new int[nEvolvingParam][Lc][2];
        for (int i = 0; i < this.nEvolvingParam; i++) {
            for (int j = 0; j < Lc; j++) {
                Genotype_NewBorn[i][j][0] = Genotype_parent1[i][j][(int) (Math.random() * 2)];
                Genotype_NewBorn[i][j][1] = Genotype_parent2[i][j][(int) (Math.random() * 2)];
            }
        }
        return Genotype_NewBorn;
    }

    private double getGonadWeight_species(int iSpec) {
        // loop over all the schools of the species iSpec to compute the 
        // specific g 
        double g_species = 0;
        for (School school : getSimulation().getSchoolSet().getSchools(getSpecies(iSpec))) {
            g_species = +school.getGonadWeight();
        }
        return g_species;
    }

    private School ParentSelection(int iSpec) {
        // loop to calcul the probabilities to be parent of each school 
        double[] g_species;
        int i = 0;
        g_species = new double[iSpec];
        double g_tot = getGonadWeight_species(iSpec);
        for (School school : getSimulation().getSchoolSet().getSchools(getSpecies(iSpec))) {
            if (i == 0) {
                g_species[i] = school.getGonadWeight() / g_tot;
            } else {
                g_species[i] = (school.getGonadWeight() + g_species[i - 1]) / g_tot;
            }
            i = +1;
        }

        // Random drawn of a parent
        double parent = Math.random();
        int j = 0;
        while (parent > g_species[j]) {
            j = +1;
        }

        return getSimulation().getSchoolSet().getSchools(getSpecies(iSpec)).get(j);
    }
}
