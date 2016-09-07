package org.jorlib.alg.tsp.separation;

import java.util.Set;

public class SubtourCut<V>{
	private Set<V> cutSet;
	private double cutValue;

	public SubtourCut(Set<V> cutSet, double cutValue){
		this.cutSet=cutSet;
		this.cutValue=cutValue;
	}

	/**
	 * Returns the value of the cut, i.e., {@code \sum_{e\in \delta{S'}} x_e}.
	 * @return the value of the cut.
	 */
	public double getCutValue(){
		return cutValue;
	}

	/**
	 * Returns the set S' where {@code \sum_{e\in \delta{S'}} x_e <2, S'\subset V, S' \noteq \emptyset}
	 * @return the set S' where {@code \sum_{e\in \delta{S'}} x_e <2, S'\subset V, S' \noteq \emptyset}
	 */
	public Set<V> getCutSet(){
		return cutSet;
	}

	@Override
	public int hashCode(){
		return cutSet.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if(this==o)
			return true;
		else if(!(o instanceof SubtourCut))
			return false;
		SubtourCut other=(SubtourCut)o;
		return this.cutSet.equals(other.getCutSet());
	}

	@Override
	public String toString(){
		return "Cutvalue: "+cutValue+" cutSet: "+cutSet;
	}
}
