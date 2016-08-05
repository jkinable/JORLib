/* ==========================================
 * jORLib : a free Java OR library
 * ==========================================
 *
 * Project Info:  https://github.com/jkinable/jorlib
 * Project Creator:  Joris Kinable (https://github.com/jkinable)
 *
 * (C) Copyright 2015, by Joris Kinable and Contributors.
 *
 * This program and the accompanying materials are licensed under LGPLv2.1
 *
 */
/* -----------------
 * SubtourSeparator.java
 * -----------------
 * (C) Copyright 2015, by Joris Kinable and Contributors.
 *
 * Original Author:  Joris Kinable
 * Contributor(s):   -
 *
 * $Id$
 *
 * Changes
 * -------
 *
 */
package org.jorlib.alg.tsp.separation;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.MinSourceSinkCut;
import org.jgrapht.alg.StoerWagnerMinimumCut;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * This class separates subtours. These subtours may be used to generate Dantzig Fulkerson Johnson (DFJ) subtour elimination constraints.
 * Let {@code G(V,E)} be a undirected graph with vertex set {@code V}, edge set {@code E}. A valid TSP solution (i.e. a solution without subtours) should satisfy
 * the following constraint: {@code \sum_{e\in \delta{S}} x_e >=2} for all {@code S\subset V, S \noteq \emptyset}. Here {@code \delta{S}\subset E} is the set of
 * edges where each edge has exactly one endpoint in {@code S}, and one endpoint in {@code V\setminus S}. {@code x_e} is a binary variable indicating
 * whether edge {@code e\in E} is used in the TSP solution. Obviously, if there is a set {@code S'\subset V, S' \noteq \emptyset} such that
 * {@code \sum_{e\in \delta{S'}} x_e <2}, then there exists a subtour within the partition S'.
 * <br><p>
 * 
 * Note: the graph must be provided as a JgraphT graph. The graph representing the problem can be directed, undirected, or mixed,
 * complete or incomplete, weighted or without weights. The directed graphs are often useful to model cases where a vehicle can only
 * drive from one city to the other in a particular direction.<p>
 * Note2: To separate the subtours, we rely on the StoerWagnerMinimumCut implementation from the JgraphT package. 
 * 		This implementation deterministically computes the minimum cut in a graph in {@code O(|V||E| + |V|log|V|)} time, see
 * 		{@literal M. Stoer and F. Wagner, "A Simple Min-Cut Algorithm", Journal of the ACM, volume 44, number 4. pp 585-591, 1997.}<p>
 * 
 * WARNING: if the input graph is modified, i.e. edges or vertices are added/removed then the behavior of this class is undefined!
 * 			A new instance should of this class should be made if this happens! A future extension of this class could add a graph
 * 			listener.
 * 
 * @author Joris Kinable
 * @since April 9, 2015
 *
 */
public class SubtourSeparator<V, E> {

	/** Precision 0.000001**/
	public static final double PRECISION=0.000001;
	
	//Solution
	private double minCutValue=-1;
	private boolean hasSubtour=false;
	private Set<V> cutSet;
	
	private Graph<V,E> inputGraph; //Original graph which defines the TSP problem
	private SimpleWeightedGraph<V, DefaultWeightedEdge> workingGraph; //Undirected graph
	
	/**
	 * This method instantiates the Subtour Separator. The input can be any type of graph: directed, undirected, or mixed,
	 * complete or incomplete, weighted or without weights. Internally, this class converts the given graph to a undirected graph. 
	 * Multiple edges between two vertices i,j, for example two direct arc (i,j) and (j,i)) are aggregated into in undirected edge (i,j).
	 * WARNING: if the input graph is modified, i.e. edges or vertices are added/removed then the behavior of this class is undefined!
	 * 			A new instance should of this class should be made if this happens!
	 * @param inputGraph input graph
	 */
	public SubtourSeparator(Graph<V,E> inputGraph){
		this.inputGraph=inputGraph;
		this.workingGraph=new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		Graphs.addAllVertices(workingGraph, inputGraph.vertexSet());
		for(E edge : inputGraph.edgeSet())
			Graphs.addEdge(workingGraph, inputGraph.getEdgeSource(edge), inputGraph.getEdgeTarget(edge),0);
	}
	
	/**
	 * Separates the most violated subtour. More precisely, this method calculates a cutset S' for which {@code \sum_{e\in \delta{S'}} w_e x_e} is minimized.
	 * The method returns a {@link SeparatedSubtour} whenever \sum_{e\in \delta{S'}} w_e x_e <2, or {@code null} otherwise.
	 * The implementation of this method relies on jgrapht's StoerWagnerMinimumCut implementation with runtime O(|V||E|log|E|).
	 * @param edgeValueMap Mapping of edges to their corresponding values, i.e. the x_e variable values for all e \in E. It suffices to provide the values
	 *                     of the non-zero edges. All other edges are presumed to have the value 0.
	 *  @return Returns the most violated subtour S' whenever \sum_{e\in \delta{S'}} w_e x_e <2, or {@code null} otherwise.
	 */
	public SeparatedSubtour separateSubtour(Map<E, Double> edgeValueMap){
		//Update the weights of our working graph
		//a. Reset all weights to zero
		for(DefaultWeightedEdge edge : workingGraph.edgeSet())
			workingGraph.setEdgeWeight(edge, 0);

		//b. Update the edge values with the values supplied in the edgeValueMap
		for(Map.Entry<E, Double> entry : edgeValueMap.entrySet()){
			if(entry.getValue() > PRECISION){
				V i=inputGraph.getEdgeSource(entry.getKey());
				V j=inputGraph.getEdgeTarget(entry.getKey());
				DefaultWeightedEdge edge = workingGraph.getEdge(i,j);
				workingGraph.setEdgeWeight(edge, entry.getValue() + workingGraph.getEdgeWeight(edge));
			}
		}
		//Compute the min cut in the graph
		//WARNING: The StoerWagnerMinimumCut class copies the workingGraph each time it is invoked! This is expensive and may be avoided.
		StoerWagnerMinimumCut<V,DefaultWeightedEdge> mc= new StoerWagnerMinimumCut<>(workingGraph);
		minCutValue=mc.minCutWeight();
		cutSet=mc.minCut();
		
		//If the cut value is smaller than 2, a subtour constraint has been violated
		hasSubtour= minCutValue<2-PRECISION;
		if(minCutValue<2-PRECISION)
			return new SeparatedSubtour(cutSet, minCutValue);
		else
			return null;
	}


	/**
	 * Separates at most {@code maxNumberOfSubtours} subtours. There are no guarantees given as to which subtours
	 * are returned, e.g., the subtour for which the cut value is minimal may not be contained in the set returned. This method does guarantee
	 * to return an empty set whenever no violated subtours exist, i.e., when {@code\sum_{e\in \delta{S'}} w_e x_e >= 2 for all S'}.
	 * The method attempts to find {@code maxNumberOfSubtours} subtours by selecting a source vertex v1 and iterating over
	 * all other vertices v2. For each pair v1, v2, the method computes a minimum cut, thereby ensuring that v1 is on one side of the cut,
	 * and v2 in the other. If the weight of the cut is less than 2, a new subtour has been found and added to the return set. The method
	 * terminates as soon as {@code maxNumberOfSubtours} have been found, or whenever all pairs v1, v2 have been tested.
	 * The implementation of this method relies on jgrapht's MinSourceSinkCut implementation with runtime O(V^3+E).
	 * Runtime: O(V^4+EV).
	 *
	 * @param edgeValueMap Mapping of edges to their corresponding values, i.e. the x_e variable values for all e \in E. It suffices to provide the values
	 *                     of the non-zero edges. All other edges are presumed to have the value 0.
	 * @param maxNumberOfSubtours The maximum number of subtours returned.
	 *  @return Returns at most {@code maxNumberOfSubtours} subtours, or an empty list whenever there are no subtours.
	 */
	public Set<SeparatedSubtour> separateSubtours(Map<E, Double> edgeValueMap, int maxNumberOfSubtours){
		//Update the weights of our working graph
		//a. Reset all weights to zero
		for(DefaultWeightedEdge edge : workingGraph.edgeSet())
			workingGraph.setEdgeWeight(edge, 0);

		//b. Update the edge values with the values supplied in the edgeValueMap
		for(Map.Entry<E, Double> entry : edgeValueMap.entrySet()){
			if(entry.getValue() > PRECISION){
				V i=inputGraph.getEdgeSource(entry.getKey());
				V j=inputGraph.getEdgeTarget(entry.getKey());
				DefaultWeightedEdge edge = workingGraph.getEdge(i,j);
				workingGraph.setEdgeWeight(edge, entry.getValue() + workingGraph.getEdgeWeight(edge));
			}
		}

		//c. Calculate the subtours
		Set<SeparatedSubtour> separatedSubtours=new LinkedHashSet<>();

		MinSourceSinkCut<V, DefaultWeightedEdge> mssc=new MinSourceSinkCut<V, DefaultWeightedEdge>(workingGraph, PRECISION);
		Iterator<V> it=workingGraph.vertexSet().iterator();
		V source=it.next();

		while (it.hasNext() && separatedSubtours.size() < maxNumberOfSubtours){
			V target=it.next();
			mssc.computeMinCut(source, target);
			double cutWeight=mssc.getCutWeight();

			//If the cut value is smaller than 2, a subtour constraint has been violated
			if(cutWeight<2-PRECISION)
				separatedSubtours.add(new SeparatedSubtour(mssc.getSourcePartition(), cutWeight));
		}
		return separatedSubtours;
	}

	/**
	 * Returns whether a subtour exists in the fractional TSP solution
	 * @return whether a subtour exists in the fractional TSP solution
	 */
	@Deprecated
	public boolean hasSubtour(){
		return hasSubtour;
	}
	
	/**
	 * Returns \sum_{e\in \delta{S'}} x_e for the separated subtour through S'.
	 * @return \sum_{e\in \delta{S'}} x_e for the separated subtour through S'.
	 */
	@Deprecated
	public double getCutValue(){
		return minCutValue;
	}
	
	/**
	 * Returns the set S' where {@code \sum_{e\in \delta{S'}} x_e <2, S'\subset V, S' \noteq \emptyset}
	 * @return the set S' where {@code \sum_{e\in \delta{S'}} x_e <2, S'\subset V, S' \noteq \emptyset}
	 */
	@Deprecated
	public Set<V> getCutSet(){
		return cutSet;
	}






	public class SeparatedSubtour<V>{
		private Set<V> cutSet;
		private double cutValue;

		public SeparatedSubtour(Set<V> cutSet, double cutValue){
			this.cutSet=cutSet;
			this.cutValue=cutValue;
		}

		/**
		 * Returns \sum_{e\in \delta{S'}} x_e for the separated subtour through S'.
		 * @return \sum_{e\in \delta{S'}} x_e for the separated subtour through S'.
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
			else if(!(o instanceof SeparatedSubtour))
				return false;
			SeparatedSubtour<V> other=(SeparatedSubtour<V>)o;
			return this.cutSet.equals(other.getCutSet());
		}
	}
}
