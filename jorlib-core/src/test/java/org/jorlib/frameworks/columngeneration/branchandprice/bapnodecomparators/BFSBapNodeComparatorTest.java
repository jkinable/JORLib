/* ==========================================
 * jORLib : Java Operations Research Library
 * ==========================================
 *
 * Project Info:  http://www.coin-or.org/projects/jORLib.xml
 * Project Creator:  Joris Kinable (https://github.com/jkinable)
 *
 * (C) Copyright 2016-2016, by Joris Kinable and Contributors.
 *
 * This program and the accompanying materials are licensed under LGPLv2.1
 * as published by the Free Software Foundation.
 */
package org.jorlib.frameworks.columngeneration.branchandprice.bapnodecomparators;

import org.jorlib.frameworks.columngeneration.branchandprice.BAPNode;
import org.jorlib.frameworks.columngeneration.colgenmain.AbstractColumn;
import org.jorlib.frameworks.columngeneration.model.ModelInterface;
import org.jorlib.frameworks.columngeneration.pricing.AbstractPricingProblem;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the BFSNodeComparator
 */
public final class BFSBapNodeComparatorTest
{

    @Test
    public <T extends ModelInterface, U extends AbstractColumn<T,? extends AbstractPricingProblem<T, U>>> void testBFSNodeComparator()
    {
        BAPNode<T,U> bapNode0 = new BAPNode<>(0, null, null, null, 4, null); // Node with bound equal to
                                                                        // 4
        BAPNode<T,U> bapNode1 = new BAPNode<>(1, null, null, null, 2, null); // Node with bound equal to
                                                                        // 2

        BFSBapNodeComparator<T,U> comparator = new BFSBapNodeComparator<>();

        Assert.assertEquals(1, comparator.compare(bapNode1, bapNode0));
        Assert.assertEquals(-1, comparator.compare(bapNode0, bapNode1));
    }

}
