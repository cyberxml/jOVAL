// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import org.w3c.dom.Node;

import jsaf.intf.util.ILoggable;
import org.slf4j.cal10n.LocLogger;

import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.variables.OvalVariables;
import scap.oval.variables.VariablesType;
import scap.oval.variables.VariableType;

import org.joval.intf.scap.oval.IType;
import org.joval.intf.scap.oval.IVariables;
import org.joval.scap.oval.types.TypeFactory;
import org.joval.util.JOVALMsg;
import org.joval.xml.DOMTools;
import org.joval.xml.SchemaRegistry;

/**
 * Index to an OvalVariables object, for fast look-up of variable definitions.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Variables implements IVariables {
    /**
     * Unmarshal an XML file and return the OvalVariables root object.
     */
    public static final OvalVariables getOvalVariables(File f) throws OvalException {
	return getOvalVariables(new StreamSource(f));
    }

    public static final OvalVariables getOvalVariables(Source source) throws OvalException {
	try {
	    Unmarshaller unmarshaller = SchemaRegistry.OVAL_VARIABLES.getJAXBContext().createUnmarshaller();
	    Object rootObj = unmarshaller.unmarshal(source);
	    if (rootObj instanceof OvalVariables) {
		return (OvalVariables)rootObj;
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLES_BAD_SOURCE, source.getSystemId()));
	    }
	} catch (JAXBException e) {
	    throw new OvalException(e);
	}
    }

    private LocLogger logger;
    private Map <String, List<IType>>variables;
    private Map <String, String>comments;

    /**
     * Create Variables from a file.
     */
    Variables(File f) throws OvalException {
	this(getOvalVariables(f));
    }

    /**
     * Create Variables from parsed OvalVariables.
     */
    Variables(OvalVariables vars) throws OvalException {
	this();
	List <VariableType> varList = vars.getVariables().getVariable();
	int len = varList.size();
	for (int i=0; i < len; i++) {
	    VariableType vt = varList.get(i);
	    variables.put(vt.getId(), extractValue(vt));
	}
    }

    /**
     * Create empty Variables.
     */
    Variables() {
	logger = JOVALMsg.getLogger();
	variables = new HashMap<String, List<IType>>();
	comments = new HashMap<String, String>();
    }

    // Implement IVariables

    public void addValue(String id, String value) {
	addValue(id, SimpleDatatypeEnumeration.STRING, value);
    }

    public void addValue(String id, SimpleDatatypeEnumeration type, String value) {
	List<IType> values = variables.get(id);
	if (values == null) {
	    values = new ArrayList<IType>();
	    variables.put(id, values);
	}
	IType t = TypeFactory.createType(type, value);
	if (!values.contains(t)) {
	    values.add(t);
	}
    }

    public void setComment(String id, String comment) {
	comments.put(id, comment);
    }

    public void setValue(String id, List<String> value) {
	setValue(id, SimpleDatatypeEnumeration.STRING, value);
    }

    public void setValue(String id, SimpleDatatypeEnumeration type, List<String> value) {
	List<IType> list = new ArrayList<IType>();
	for (String s : value) {
	    list.add(TypeFactory.createType(type, s));
	}
	variables.put(id, list);
    }

    public void writeXML(File f) {
	OutputStream out = null;
	try {
	    Marshaller marshaller = SchemaRegistry.OVAL_VARIABLES.createMarshaller();
	    out = new FileOutputStream(f);
	    marshaller.marshal(getRootObject(), out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FileNotFoundException e) {
	    logger.warn(JOVALMsg.ERROR_FILE_GENERATE, f.toString());
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} finally {
	    if (out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		    logger.warn(JOVALMsg.ERROR_FILE_CLOSE, f.toString());
		}
	    }
	}
    }

    public List<IType> getValue(String id) throws NoSuchElementException {
	List<IType> values = variables.get(id);
	if (values == null) {
	    throw new NoSuchElementException(id);
	} else {
	    return values;
	}
    }

    // Implement ITransformable

    public Source getSource() throws JAXBException {
	return new JAXBSource(SchemaRegistry.OVAL_VARIABLES.getJAXBContext(), getRootObject());
    }

    public OvalVariables getRootObject() {
	OvalVariables vars = Factories.variables.createOvalVariables();
	vars.setGenerator(OvalFactory.getGenerator());

	VariablesType vt = Factories.variables.createVariablesType();
	for (String key : variables.keySet()) {
	    VariableType var = Factories.variables.createVariableType();
	    var.setId(key);
	    for (IType t : variables.get(key)) {
		var.getValue().add(t.getString());
	    }
	    String comment = comments.get(key);
	    if (comment != null) {
		var.setComment(comment);
	    }
	    vt.getVariable().add(var);
	}

	vars.setVariables(vt);
	return vars;
    }

    public OvalVariables copyRootObject() throws Exception {
	Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
	Object rootObj = unmarshaller.unmarshal(new DOMSource(DOMTools.toDocument(this).getDocumentElement()));
	if (rootObj instanceof OvalVariables) {
	    return (OvalVariables)rootObj;
	} else {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLES_BAD_SOURCE, toString()));
	}
    }

    public JAXBContext getJAXBContext() throws JAXBException {
	return SchemaRegistry.OVAL_VARIABLES.getJAXBContext();
    }

    // Implement ILogger

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    public LocLogger getLogger() {
	return logger;
    }

    // Private

    /**
     * Reads String (i.e., Text) data from the VariableType as a Node.
     */
    private List<IType> extractValue(VariableType var) throws OvalException {
	List<IType> list = new ArrayList<IType>();
	for (Object obj : var.getValue()) {
	    String value = null;
	    if (obj instanceof Node) {
		//
		// xsi:type was unspecified
		//
		value = ((Node)obj).getTextContent();
	    } else {
		value = obj.toString();
	    }
	    list.add(TypeFactory.createType(var.getDatatype(), value));
	}
	return list;
    }
}
