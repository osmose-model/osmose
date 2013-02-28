/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.ird.osmose.util.filter;

/**
 *
 * @author pverley
 */
public interface IFilter<T> {

    boolean accept(T object);

}
