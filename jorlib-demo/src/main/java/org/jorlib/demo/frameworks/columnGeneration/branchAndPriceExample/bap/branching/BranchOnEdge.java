/* ==========================================
 * jORLib : a free Java OR library
 * ==========================================
 *
 * Project Info:  https://github.com/jkinable/jorlib
 * Project Creator:  Joris Kinable (https://github.com/jkinable)
 *
 * (C) Copyright 2015, by Joris Kinable and Contributors.
 *
 * This program and the accompanying materials are licensed under GPLv3
 *
 */
/* -----------------
 * BranchOnEdge.java
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
package org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.bap.branching;

import org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.bap.branching.branchingDecisions.FixEdge;
import org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.bap.branching.branchingDecisions.RemoveEdge;
import org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.cg.Matching;
import org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.cg.PricingProblemByColor;
import org.jorlib.demo.frameworks.columnGeneration.branchAndPriceExample.model.TSP;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.AbstractBranchCreator;
import org.jorlib.frameworks.columnGeneration.branchAndPrice.BAPNode;
import org.jorlib.frameworks.columnGeneration.master.cutGeneration.Inequality;
import org.jorlib.frameworks.columnGeneration.util.MathProgrammingUtil;
import org.jorlib.io.tspLibReader.graph.Edge;

import java.util.*;

/**
 * Class which creates new branches in the branch and price tree. This particular class branches on an edge. More precisely,
 * the class checks whether there is a fractional edge in the red resp. blue matchings. The edge with a fractional value
 * closest to 0.5 is selected for branching.
 *
 * @author Joris Kinable
 * @version 22-4-2015
 */
public class BranchOnEdge extends AbstractBranchCreator<TSP, Matching, PricingProblemByColor>{

    private Edge edgeForBranching=null; //Edge to branch on
    private PricingProblemByColor pricingProblemForMatching=null; //Edge is fractional in red or blue matching

    public BranchOnEdge(TSP modelData, List<PricingProblemByColor> pricingProblems){
        super(modelData, pricingProblems);
    }

    /**
     * Determine on which edge from the red or blue matchings we are going to branch.
     * @param solution Fractional column generation solution
     * @return true if a fractional edge exists
     */
    @Override
    protected boolean canPerformBranching(List<Matching> solution) {
        //Reset values
        pricingProblemForMatching=null;
        edgeForBranching=null;
        double bestEdgeValue = 0;

        //For each color, determine whether there's a fractional edge for branching
        Map<PricingProblemByColor,Map<Edge, Double>> edgeValueMap=new HashMap<>();
        for(PricingProblemByColor pricingProblem : pricingProblems)
            edgeValueMap.put(pricingProblem, new LinkedHashMap<>());

        //Aggregate edge values
        for(Matching matching : solution){
            for(Edge edge : matching.edges){
                Double edgeValue=edgeValueMap.get(matching.associatedPricingProblem).get(edge);
                if(edgeValue == null)
                    edgeValueMap.get(matching.associatedPricingProblem).put(edge,matching.value);
                else
                    edgeValueMap.get(matching.associatedPricingProblem).put(edge,matching.value+edgeValue);
            }
        }

        //Select the edge with a fractional value closest to 0.5
        for(PricingProblemByColor pricingProblem : pricingProblems){
            Map<Edge, Double> edgeValues=edgeValueMap.get(pricingProblem);
            for(Edge edge : edgeValues.keySet()){
                double value=edgeValues.get(edge);
                if(Math.abs(0.5-value) < Math.abs(0.5- bestEdgeValue)){
                    pricingProblemForMatching=pricingProblem;
                    edgeForBranching=edge;
                    bestEdgeValue =value;
                }
            }
        }

        return MathProgrammingUtil.isFractional(bestEdgeValue);
    }

    /**
     * Create the branches:
     *   -branch 1: edge <edgeForBranching> must be used by <PricingProblemByColor>,
     *   -branch 2: edge <edgeForBranching> may NOT used by <PricingProblemByColor>,
     * @param parentNode Fractional node on which we branch
     * @param solution fractional solution
     * @param cuts Valid inequalities active at the parent node
     * @return List of child nodes
     */
    @Override
    protected List<BAPNode<TSP,Matching>> getBranches(BAPNode<TSP,Matching> parentNode, List<Matching> solution, List<Inequality> cuts) {
        //Branch 1: remove the edge:
        RemoveEdge branchingDecision1=new RemoveEdge(pricingProblemForMatching, edgeForBranching);
        BAPNode<TSP,Matching> node2=this.createBranch(parentNode, branchingDecision1, solution, cuts);

        //Branch 2: fix the edge:
        FixEdge branchingDecision2=new FixEdge(pricingProblemForMatching, edgeForBranching);
        BAPNode<TSP,Matching> node1=this.createBranch(parentNode, branchingDecision2, solution, cuts);

        return Arrays.asList(node1,node2);
    }
}
