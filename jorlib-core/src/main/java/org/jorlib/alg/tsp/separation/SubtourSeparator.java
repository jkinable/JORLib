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

import java.util.*;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.StoerWagnerMinimumCut;
import org.jgrapht.alg.flow.PushRelabelMFImpl;
import org.jgrapht.alg.interfaces.MinimumSTCutAlgorithm;
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
 *
 * WARNING: if the input graph is modified, i.e. edges or vertices are added/removed then the behavior of this class is undefined!
 * 			A new instance of this class should be created if this happens! A future extension of this class could add a graph
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
	 * complete or incomplete, weighted or without weights. Internally, the given graph is converted to a undirected graph.
	 * Multiple edges between two vertices i,j, for example two direct arcs (i,j) and (j,i)) are aggregated into an undirected edge (i,j).
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
	 * Separates the most violated subtour. More precisely, this method calculates a cutset S' for which {@code \sum_{e\in \delta{S'}} x_e} is minimized.
	 * The method returns a {@link SubtourCut} whenever \sum_{e\in \delta{S'}} x_e <2, or {@code null} otherwise.
	 * The implementation of this method relies on jgrapht's StoerWagnerMinimumCut implementation with runtime O(|V||E|log|E|).
	 * @param edgeValueMap Mapping of edges to their corresponding values, i.e. the x_e variable values for all e \in E. It suffices to provide the values
	 *                     of the non-zero edges. All other edges are presumed to have the value 0.
	 *  @return Returns the most violated subtour S' whenever \sum_{e\in \delta{S'}} x_e <2, or {@code null} otherwise.
	 */
	public SubtourCut<V> separateSubtour(Map<E, Double> edgeValueMap){
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
		hasSubtour= minCutValue<2-PRECISION;

		//If the cut value is smaller than 2, a subtour constraint has been violated
		if(hasSubtour)
			return new SubtourCut<>(cutSet, minCutValue);
		else
			return null;
	}


	/**
	 * Separates at most {@code maxNumberOfSubtours} subtour cuts. There are no guarantees given as to which subtour cuts
	 * are returned, e.g., the subtour for which the cut value is minimal may not be contained in the set returned. This method does guarantee
	 * to return an empty set whenever no violated subtours exist, i.e., when {@code\sum_{e\in \delta{S'}} w_e x_e >= 2 for all S'}.
	 * The method attempts to find {@code maxNumberOfSubtours} subtours by selecting a source vertex v1 and iterating over
	 * all other vertices v2. For each pair v1, v2, the method computes a minimum cut, thereby ensuring that v1 is on one side of the cut,
	 * and v2 on the other. If the weight of the cut is less than 2, a new subtour cut has been found and added to the return set. The method
	 * terminates as soon as {@code maxNumberOfSubtours} have been found, or whenever all pairs v1, v2 have been tested.
	 * The implementation of this method relies on jgrapht's Push Relabel Maximum Flow implementation with runtime O(V^3).
	 * Runtime: O(V^4).
	 *
	 * @param edgeValueMap Mapping of edges to their corresponding values, i.e. the x_e variable values for all e \in E. It suffices to provide the values
	 *                     of the non-zero edges. All other edges are presumed to have the value 0.
	 * @param maxNumberOfSubtours The maximum number of subtours returned.
	 * @return Returns at most {@code maxNumberOfSubtours} subtour cuts, or an empty list whenever there are no subtours.
	 */
	public Set<SubtourCut<V>> separateSubtours(Map<E, Double> edgeValueMap, int maxNumberOfSubtours){
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

//		System.out.println("Working graph:");
//		for(DefaultWeightedEdge edge : workingGraph.edgeSet()){
//			System.out.println(""+edge+" weight: "+workingGraph.getEdgeWeight(edge));
//		}

		//c. Calculate the subtour cuts
		Set<SubtourCut<V>> subtourCuts =new LinkedHashSet<>();

		MinimumSTCutAlgorithm<V, DefaultWeightedEdge> mssc=new PushRelabelMFImpl<>(workingGraph, PRECISION);
		Iterator<V> it=workingGraph.vertexSet().iterator();
		V source=it.next();

		while (it.hasNext() && subtourCuts.size() < maxNumberOfSubtours){
			V target=it.next();
//			System.out.println("Target: "+target);
			double cutWeight=mssc.calculateMinCut(source, target);
//			System.out.println("cutWeight: "+cutWeight+" sourcePartition: "+mssc.getSourcePartition()+" sinkPartition: "+mssc.getSinkPartition());

			//If the cut value is smaller than 2, a subtour cut has been found
			if(cutWeight<2-PRECISION)
				subtourCuts.add(new SubtourCut<>(mssc.getSourcePartition(), cutWeight));
		}
		hasSubtour= !subtourCuts.isEmpty();
		return subtourCuts;
	}



	/**
	 * Separates the most violated subtour cuts. At most {@code maxNumberOfSubtours} subtour cuts are returned. This method returns
	 * an empty set whenever no violated subtours exist, i.e., when {@code\sum_{e\in \delta{S'}} w_e x_e >= 2 for all S'}.
	 * The method attempts to find {@code maxNumberOfSubtours} subtours by selecting a source vertex v1 and iterating over
	 * all other vertices v2. For each pair v1, v2, the method computes a minimum cut, thereby ensuring that v1 is on one side of the cut,
	 * and v2 on the other. If the weight of the cut is less than 2, a new subtour cut has been found and added to the return set. The method
	 * terminates as soon as all pairs v1, v2 have been tested.
	 *
	 * Note: if you are only interested in the single most violated subtour cut, use {@link #separateSubtour(Map)} instead; this method is
	 * significantly faster!
	 * Note 2: the average runtime complexity of this method is significantly higher than {@link #separateSubtours(Map, int)}.
	 *
	 * The implementation of this method relies on jgrapht's Push Relabel Maximum Flow implementation with runtime O(V^3).
	 * Runtime: O(V^4).
	 *
	 * @param edgeValueMap Mapping of edges to their corresponding values, i.e. the x_e variable values for all e \in E. It suffices to provide the values
	 *                     of the non-zero edges. All other edges are presumed to have the value 0.
	 * @param maxNumberOfSubtours The maximum number of subtours returned.
	 * @return Returns at most {@code maxNumberOfSubtours} subtour cuts, or an empty list whenever there are no subtours.
	 */
	public Set<SubtourCut<V>> separateMostViolatedSubtours(Map<E, Double> edgeValueMap, int maxNumberOfSubtours){
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

//		System.out.println("Working graph:");
//		for(DefaultWeightedEdge edge : workingGraph.edgeSet()){
//			System.out.println(""+edge+" weight: "+workingGraph.getEdgeWeight(edge));
//		}

		//c. Calculate the subtour cuts
		TreeSet<SubtourCut<V>> subtourCuts =new TreeSet<>((s1, s2) -> Double.compare(s1.getCutValue(), s2.getCutValue()));

		MinimumSTCutAlgorithm<V, DefaultWeightedEdge> mssc=new PushRelabelMFImpl<>(workingGraph, PRECISION);
		Iterator<V> it=workingGraph.vertexSet().iterator();
		V source=it.next();

		while (it.hasNext()){
			V target=it.next();
//			System.out.println("Target: "+target);
			double cutWeight=mssc.calculateMinCut(source, target);
//			System.out.println("cutWeight: "+cutWeight+" sourcePartition: "+mssc.getSourcePartition()+" sinkPartition: "+mssc.getSinkPartition());

			//If the cut value is smaller than 2, a subtour cut has been found
			if(cutWeight<2-PRECISION) {
				subtourCuts.add(new SubtourCut<>(mssc.getSourcePartition(), cutWeight));
				if(subtourCuts.size() > maxNumberOfSubtours)
					subtourCuts.pollLast();
			}
		}
		hasSubtour= !subtourCuts.isEmpty();
		return subtourCuts;
	}

	/**
	 * Returns true when the last invocation of one of the separation methods returned one or more subtours.
	 * @return true when the last invocation of one of the separation methods returned one or more subtours, false otherwise
	 */
	@Deprecated
	public boolean hasSubtour(){
		return hasSubtour;
	}
	
	/**
	 * Returns the value of the minimum cut, i.e., {@code \sum_{e\in \delta{S'}} x_e}.
	 * @return the value of the minimum cut.
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







}
