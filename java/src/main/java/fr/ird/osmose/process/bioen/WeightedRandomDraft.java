/* 
 * 
 * OSMOSE (Object-oriented Simulator of Marine Ecosystems)
 * http://www.osmose-model.org
 * 
 * Copyright (C) IRD (Institut de Recherche pour le Développement) 2009-2020
 * 
 * Osmose is a computer program whose purpose is to simulate fish
 * populations and their interactions with their biotic and abiotic environment.
 * OSMOSE is a spatial, multispecies and individual-based model which assumes
 * size-based opportunistic predation based on spatio-temporal co-occurrence
 * and size adequacy between a predator and its prey. It represents fish
 * individuals grouped into schools, which are characterized by their size,
 * weight, age, taxonomy and geographical location, and which undergo major
 * processes of fish life cycle (growth, explicit predation, additional and
 * starvation mortalities, reproduction and migration) and fishing mortalities
 * (Shin and Cury 2001, 2004).
 * 
 * Contributor(s):
 * Yunne SHIN (yunne.shin@ird.fr),
 * Morgane TRAVERS (morgane.travers@ifremer.fr)
 * Ricardo OLIVEROS RAMOS (ricardo.oliveros@gmail.com)
 * Philippe VERLEY (philippe.verley@ird.fr)
 * Laure VELEZ (laure.velez@ird.fr)
 * Nicolas Barrier (nicolas.barrier@ird.fr)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License). Full description
 * is provided on the LICENSE file.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 */

package fr.ird.osmose.process.bioen;

import java.util.NavigableMap;
import java.util.TreeMap;
import fr.ird.osmose.util.OsmoseLinker;
import java.util.Random;


public class WeightedRandomDraft<E> extends OsmoseLinker {
  
  private NavigableMap<Double, E> map = new TreeMap<>();
  private double total = 0;
  
  /** Random generator */
  private Random rdDraft;
  
  public void init() {
    
    boolean fixedSeed = false;
    String key = "reproduction.randomseed.fixed";
    
    if(!getConfiguration().isNull(key)) {
      fixedSeed =  getConfiguration().getBoolean(key);
    }
    
    // Init random number generator
    if(fixedSeed) { 
      int nSpecies = getConfiguration().getNSpecies();
      rdDraft = new Random(13L * nSpecies); 
    } else {
      rdDraft = new Random();  
    }
     
  }

  public void add(double weight, E result) {
    if (weight <= 0 || map.containsValue(result))
      return;
    total += weight;
    map.put(total, result);
  }
  
  // Reinitialize the weights
  public void reset() {
    map = new TreeMap<>();
    total = 0;
  }

  public E next() {
    double value = rdDraft.nextDouble() * total;
    return map.ceilingEntry(value).getValue();
  }
}
