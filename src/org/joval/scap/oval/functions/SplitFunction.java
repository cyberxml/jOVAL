// Copyright (C) 2014 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import jsaf.util.StringTools;

import scap.oval.definitions.core.SplitFunctionType;

import org.joval.intf.scap.oval.IType;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.types.TypeFactory;

/**
 * Implementation for the split function type.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SplitFunction implements IFunction {
    public SplitFunction() {
    }

    // Implement IFunction

    public Class<?> getFunctionType() {
	return SplitFunctionType.class;
    }

    public Collection<IType> compute(Object obj, IFunctionContext fc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	if (getFunctionType().isInstance(obj)) {
	    SplitFunctionType function = (SplitFunctionType)obj;
	    Collection<IType> values = new ArrayList<IType>();
	    for (IType value : fc.resolveComponent(fc.getComponent(function))) {
		for (String s : StringTools.toList(StringTools.tokenize(value.getString(), function.getDelimiter(), false))) {
		    values.add(TypeFactory.createType(IType.Type.STRING, s));
		}
	    }
	    return values;
	} else {
	    throw new IllegalArgumentException(obj.toString());
	}
    }
}
