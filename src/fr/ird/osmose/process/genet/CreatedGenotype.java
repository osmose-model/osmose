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
 * This class is applied to a new school at the inialization. 
 * It creates genotype is a class to initialize the genetic diversity in the 
 * population. When the class is called, a complete genotype is simulated for 
 * an only individual. The trait value of the corresponding evolving trait is 
 * calculated.
 * 
 */


public class CreatedGenotype extends AbstractProcess {
    
    //Number of evolving traits
    private int nEvolvingParam;
    
    // String with the name of evolving traits
    private String[] EvolvingParam;
    
    // Two vectors of doubles with the minimum and the maximum for each 
    // evolving trait         
    private double[] xmin,xmax;

    
    //Number of alleles V and locus Lc for the trait x 
    // All the evolving traits have the same V and Lc
    
    private int V,Lc; 

    public CreatedGenotype(int rank) {

        super(rank);
    }

    @Override
    public void init() {
        
        String key;
        
        key = "genet.NbEvolvingParam";
        nEvolvingParam = getConfiguration().getInt(key);
        
        EvolvingParam = new String[nEvolvingParam];
        
        // Vector with the names of the evolving parameters
        for (int i = 0; i < nEvolvingParam; i++) {
            EvolvingParam[i]=getConfiguration().getString("genet.EvolvingParam" + i);
                                
        }
        
        // Lc and V 
        key = "genet.V";
        V = getConfiguration().getInt(key);
        
        key = "genet.Lc";
        Lc = getConfiguration().getInt(key);
        
        // For each evolving parameter, its min, max, V and Lc are called
        for (int i = 0; i < nEvolvingParam; i++) {

            String x = EvolvingParam[i];
        
            key = "genet.evolving.param."+x+"min";
            xmin[i] = getConfiguration().getDouble(key);
        
            key = "genet.evolving.param."+x+"max";
            xmax[i] = getConfiguration().getDouble(key);
                          
        }         
    }

    @Override
    public void run() {
        double[][][] Genotype= new double[nEvolvingParam][this.Lc][2];
        // Get the genotype and the value of all the evolving traits 
        for (int i = 0; i < nEvolvingParam; i++) {
            Genotype[i]=get_xLocus(); 
            get_xtrait(Genotype[i], this.xmax[i], this.xmin[i]);
        }
            
        
    }
    // Random drawn of an allele among the V possible
    public int get_Allele(){
        double i = Math.random()*this.V;
        int n = (int)i;
        return n;
    }
    // Random drawn of the two allele of a locus
    public double[] get_Locus(){
        double[] Locus = new double [2];
        Locus[1]=get_Allele();
        Locus[2]=get_Allele();
        return Locus;
    }
    // Calculate the unity value for the trait x
    public double get_UnityValue(double xmax, double xmin){
        double unity = (xmax-xmin)/(2*this.V*this.Lc);
        return(unity);
    }
    
    // Get the alleles of the Lc locus of the trait x
    public double[][] get_xLocus(){
        double[][] xLocus;
        xLocus = new double [this.Lc][2];
        for (int i = 0; i < this.Lc; i++) {
            xLocus[i]=get_Locus();
        }
        return(xLocus);
    }
    
    // Sum the effects of the Lc diploid loci to get the value of the trait x
    public double get_xtrait(double[][] xGenotype, double xmax, double xmin){
        double x_value=0;
        for (int i = 0; i < this.Lc; i++) {
            for (int j = 0; j < 2; i++) {
                x_value=+xGenotype[i][j];
        }
        }
        return x_value*get_UnityValue(xmax, xmin);
    }
    
    }
