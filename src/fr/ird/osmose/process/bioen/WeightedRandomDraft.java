/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.ird.osmose.process.bioen;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;


public class WeightedRandomDraft<E> {
    
  private final NavigableMap<Double, E> map = new TreeMap<>();
  private double total = 0;

  public void add(double weight, E result) {
    if (weight <= 0 || map.containsValue(result))
      return;
    total += weight;
    map.put(total, result);
  }

  public E next() {
    double value = ThreadLocalRandom.current().nextDouble() * total;
    return map.ceilingEntry(value).getValue();
  }
}

