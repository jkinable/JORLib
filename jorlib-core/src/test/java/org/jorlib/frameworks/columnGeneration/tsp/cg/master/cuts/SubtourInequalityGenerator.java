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
 * SubtourInequalityGenerator.java
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
package org.jorlib.frameworks.columnGeneration.tsp.cg.master.cuts;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloRange;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jorlib.alg.tsp.separation.SubtourCut;
import org.jorlib.alg.tsp.separation.SubtourSeparator;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractCutGenerator;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.AbstractInequality;
import org.jorlib.frameworks.columnGeneration.tsp.cg.Matching;
import org.jorlib.frameworks.columnGeneration.tsp.cg.PricingProblemByColor;
import org.jorlib.frameworks.columnGeneration.tsp.cg.master.TSPMasterData;
import org.jorlib.frameworks.columnGeneration.tsp.model.TSP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * Checks for violated subtour inequalities in the master problem. Any violated inqualities are added to the master problem.
 *
 * @author Joris Kinable
 * @version 13-4-2015
 *
 */
public final class SubtourInequalityGenerator extends AbstractCutGenerator<TSP, TSPMasterData> {

	//We use the subtour separator provided in jORLib
	private final SubtourSeparator<Integer, DefaultWeightedEdge> separator;

	/**
	 * Creates a new subtour inequality generator
	 * @param modelData data model
	 */
	public SubtourInequalityGenerator(TSP modelData) {
		super(modelData, "subtourIneqGenerator");
		
		//Create a subtour separator
		separator=new SubtourSeparator<>(modelData);
	}

	/**
	 * Generate inequalities using the data originating from the master problem
	 * @return Returns true if a violated inequality has been found
	 */
	@Override
	public List<AbstractInequality> generateInqualities() {
		//Check for violated subtours. When found, generate an inequality
		SubtourCut<Integer> subtour=separator.separateSubtour(masterData.edgeValueMap);
		if(subtour != null){
			Set<Integer> cutSet=subtour.getCutSet();
			SubtourInequality inequality=new SubtourInequality(this, cutSet);
			this.addCut(inequality);
			return Collections.singletonList(inequality);
		}
		return Collections.emptyList();
	}

	/**
	 * If a violated inequality has been found add it to the master problem.
	 * @param subtourInequality subtour inequality
	 */
	private void addCut(SubtourInequality subtourInequality){
		if(masterData.subtourInequalities.containsKey(subtourInequality))
			throw new RuntimeException("Error, duplicate subtour cut is being generated! This cut should already exist in the master problem: "+subtourInequality);
		//Create the inequality in cplex
		try {
			IloLinearNumExpr expr=masterData.cplex.linearNumExpr();
			//Register the columns with this constraint.
			for(PricingProblemByColor pricingProblem : masterData.pricingProblems){
				for(Matching matching: masterData.getColumnsForPricingProblemAsList(pricingProblem)){
					//Test how many edges in the matching enter/leave the cutSet (edges with exactly one endpoint in the cutSet)
					int crossings=0;
					for(DefaultWeightedEdge edge: matching.edges){
						if(subtourInequality.cutSet.contains(dataModel.getEdgeSource(edge)) ^ subtourInequality.cutSet.contains(dataModel.getEdgeTarget(edge)))
							crossings++;
					}
					if(crossings>0){
						IloNumVar var=masterData.getVar(pricingProblem,matching);
						expr.addTerm(crossings, var);
					}
				}
			}
			IloRange subtourConstraint = masterData.cplex.addGe(expr, 2, "subtour");
			masterData.subtourInequalities.put(subtourInequality, subtourConstraint);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Add a subtour inequality to the master problem
	 * @param cut AbstractInequality
	 */
	@Override
	public void addCut(AbstractInequality cut) {
		if(!(cut instanceof SubtourInequality))
			throw new IllegalArgumentException("This AbstractCutGenerator can ONLY add SubtourInequalities");
		SubtourInequality subtourInequality=(SubtourInequality) cut;
		this.addCut(subtourInequality);
	}

	/**
	 * Retuns a list of inequalities that have been generated.
	 * @return Retuns a list of inequalities that have been generated.
	 */
	@Override
	public List<AbstractInequality> getCuts() {
		return new ArrayList<>(masterData.subtourInequalities.keySet());
	}

	/**
	 * Close the generator
	 */
	@Override
	public void close() {} //Nothing to do here
}
