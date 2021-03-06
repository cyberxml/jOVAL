// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.engine;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.FactoryConfigurationError;

import jsaf.intf.system.ISession;
import jsaf.intf.util.ILoggable;
import jsaf.util.StringTools;

import org.slf4j.cal10n.LocLogger;

import scap.oval.common.CheckEnumeration;
import scap.oval.common.ComplexDatatypeEnumeration;
import scap.oval.common.ExistenceEnumeration;
import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperatorEnumeration;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.ConstantVariable;
import scap.oval.definitions.core.CriteriaType;
import scap.oval.definitions.core.CriterionType;
import scap.oval.definitions.core.DefinitionType;
import scap.oval.definitions.core.DefinitionsType;
import scap.oval.definitions.core.EntityComplexBaseType;
import scap.oval.definitions.core.EntityObjectFieldType;
import scap.oval.definitions.core.EntityObjectRecordType;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.EntitySimpleBaseType;
import scap.oval.definitions.core.EntityStateFieldType;
import scap.oval.definitions.core.EntityStateRecordType;
import scap.oval.definitions.core.EntityStateSimpleBaseType;
import scap.oval.definitions.core.ExtendDefinitionType;
import scap.oval.definitions.core.ExternalVariable;
import scap.oval.definitions.core.Filter;
import scap.oval.definitions.core.LiteralComponentType;
import scap.oval.definitions.core.LocalVariable;
import scap.oval.definitions.core.ObjectComponentType;
import scap.oval.definitions.core.ObjectRefType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.definitions.core.ObjectsType;
import scap.oval.definitions.core.OvalDefinitions;
import scap.oval.definitions.core.Set;
import scap.oval.definitions.core.SetOperatorEnumeration;
import scap.oval.definitions.core.StateRefType;
import scap.oval.definitions.core.StateType;
import scap.oval.definitions.core.StatesType;
import scap.oval.definitions.core.TestsType;
import scap.oval.definitions.core.ValueType;
import scap.oval.definitions.core.VariableComponentType;
import scap.oval.definitions.core.VariableType;
import scap.oval.definitions.core.VariablesType;
import scap.oval.definitions.independent.EntityObjectVariableRefType;
import scap.oval.definitions.independent.UnknownTest;
import scap.oval.definitions.independent.VariableObject;
import scap.oval.definitions.independent.VariableTest;
import scap.oval.results.ResultEnumeration;
import scap.oval.results.TestedItemType;
import scap.oval.results.TestedVariableType;
import scap.oval.results.TestType;
import scap.oval.systemcharacteristics.core.EntityItemAnySimpleType;
import scap.oval.systemcharacteristics.core.EntityItemFieldType;
import scap.oval.systemcharacteristics.core.EntityItemRecordType;
import scap.oval.systemcharacteristics.core.EntityItemSimpleBaseType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;
import scap.oval.systemcharacteristics.core.VariableValueType;
import scap.oval.systemcharacteristics.independent.EntityItemVariableRefType;
import scap.oval.systemcharacteristics.independent.VariableItem;
import scap.oval.variables.OvalVariables;

import org.joval.intf.plugin.IPlugin;
import org.joval.intf.scap.oval.IBatch;
import org.joval.intf.scap.oval.IDefinitionFilter;
import org.joval.intf.scap.oval.IDefinitions;
import org.joval.intf.scap.oval.IOvalEngine;
import org.joval.intf.scap.oval.IProvider;
import org.joval.intf.scap.oval.IResults;
import org.joval.intf.scap.oval.ISystemCharacteristics;
import org.joval.intf.scap.oval.IType;
import org.joval.intf.scap.oval.IVariables;
import org.joval.intf.util.IObserver;
import org.joval.intf.util.IProducer;
import org.joval.scap.oval.Batch;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.DefinitionFilter;
import org.joval.scap.oval.Definitions;
import org.joval.scap.oval.Factories;
import org.joval.scap.oval.ItemSet;
import org.joval.scap.oval.OvalException;
import org.joval.scap.oval.OvalFactory;
import org.joval.scap.oval.Results;
import org.joval.scap.oval.SystemCharacteristics;
import org.joval.scap.oval.functions.IFunction;
import org.joval.scap.oval.functions.ResolveException;
import org.joval.scap.oval.types.IntType;
import org.joval.scap.oval.types.Ip4AddressType;
import org.joval.scap.oval.types.Ip6AddressType;
import org.joval.scap.oval.types.RecordType;
import org.joval.scap.oval.types.StringType;
import org.joval.scap.oval.types.TypeConversionException;
import org.joval.scap.oval.types.TypeFactory;
import org.joval.util.Index;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.Producer;
import org.joval.util.ReflectionTool;
import org.joval.util.Version;
import org.joval.xml.SchemaRegistry;
import org.joval.xml.XSITools;

/**
 * Engine that evaluates OVAL tests using an IPlugin.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class Engine implements IOvalEngine, IProvider {
    /**
     * Internal states for the engine.
     */
    private static enum State {
	CONFIGURE,
	RUNNING,
	COMPLETE_OK,
	COMPLETE_ERR;
    }

    private static final String FUNCTIONS_RESOURCE = "functions.txt";
    private static Map<Class<?>, IFunction> FUNCTIONS = new HashMap<Class<?>, IFunction>();
    static {
	try {
	    ClassLoader cl = Engine.class.getClassLoader();
	    InputStream rsc = cl.getResourceAsStream(FUNCTIONS_RESOURCE);
	    if (rsc == null) {
		JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_MISSING_RESOURCE, FUNCTIONS_RESOURCE));
	    } else {
		BufferedReader reader = new BufferedReader(new InputStreamReader(rsc));
		String line = null;
		while ((line = reader.readLine()) != null) {
		    if (!line.startsWith("#")) {
			try {
			    Class<?> clazz = Class.forName(line.trim());
			    for (Class<?> iface : ReflectionTool.getInterfaces(clazz)) {
				if ("org.joval.scap.oval.functions.IFunction".equals(iface.getName())) {
				    @SuppressWarnings("unchecked")
				    Class<IFunction> functionClass = (Class<IFunction>)clazz;
				    IFunction function = functionClass.newInstance();
				    FUNCTIONS.put(function.getFunctionType(), function);
				    break;
				}
			    }
			} catch (ClassNotFoundException e) {
			    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			} catch (InstantiationException e) {
			    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			} catch (IllegalAccessException e) {
			    JOVALMsg.getLogger().warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
			}
		    }
		}
	    }
	} catch (IOException e) {
	    JOVALMsg.getLogger().error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
    }

    private static Map<Class<? extends ObjectType>, Class<? extends ItemType>> OBJECT_ITEM_MAP
	= new HashMap<Class<? extends ObjectType>, Class<? extends ItemType>>();

    /**
     * Create a static class mapping between an OVAL object type and an item type in the data model.
     */
    protected static void setObjectItem(Class<? extends ObjectType> objectType, Class<? extends ItemType> itemType) {
	OBJECT_ITEM_MAP.put(objectType, itemType);
    }

    private Hashtable <String, Collection<IType>>variableMap;
    private IVariables externalVariables = null;
    private IDefinitions definitions = null;
    private IPlugin plugin = null;
    private ISystemCharacteristics sc = null;
    private IDefinitionFilter filter = null;
    private IOvalEngine.Mode mode;
    private Map<String, ObjectGroup> scanQueue = null;
    private Exception error;
    private Results results;
    private State state;
    private Thread thread;
    private boolean evalEnabled = true, abortScan = false, abortEval = false;
    private Producer<Message> producer;
    private LocLogger logger;
    private Marshaller marshaller;
    private Unmarshaller unmarshaller;
    private Index<Integer> objectIndex;
    private Map<Integer, Collection<ItemType>> objectItems;
    private int objectCounter = 0;
    private int maxItems = 0;

    /**
     * Create an engine for evaluating OVAL definitions using a plugin.
     */
    protected Engine(IOvalEngine.Mode mode, IPlugin plugin) {
	if (plugin == null) {
	    logger = JOVALMsg.getLogger();
	} else {
	    logger = plugin.getLogger();
	    this.plugin = plugin;
	    maxItems = plugin.getSession().getProperties().getIntProperty(PROP_MAX_ITEMS);
	}
	this.mode = mode;
	producer = new Producer<Message>();
	filter = new DefinitionFilter();
	try {
	    marshaller = SchemaRegistry.OVAL_DEFINITIONS.createMarshaller();
	    unmarshaller = SchemaRegistry.OVAL_DEFINITIONS.getJAXBContext().createUnmarshaller();
	} catch (JAXBException e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (FactoryConfigurationError e) {
	    logger.error(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	reset();
    }

    // Implement IProvider

    public Collection<? extends ItemType> getItems(ObjectType obj, IProvider.IRequestContext rc) throws CollectException {
	//
	// If the exact same object has been scanned before (without error), then return the previously-gathered results.
	//
	Integer indexId = objectIndex.getId(toCanonicalBytes(obj), objectCounter);
	if (objectCounter == indexId) {
	    objectCounter++;
	} else if (objectItems.containsKey(indexId)) {
	    return objectItems.get(indexId);
	}

	Collection<ItemType> items = null;
	if (obj instanceof VariableObject) {
	    items = new ArrayList<ItemType>();
	    VariableObject vObj = (VariableObject)obj;
	    try {
		Collection<IType> values = resolveVariable((String)vObj.getVarRef().getValue(), (RequestContext)rc);
		if (values.size() > 0) {
		    VariableItem item = Factories.sc.independent.createVariableItem();
		    EntityItemVariableRefType ref = Factories.sc.independent.createEntityItemVariableRefType();
		    ref.setValue(vObj.getVarRef().getValue());
		    item.setVarRef(ref);
		    for (IType value : values) {
			EntityItemAnySimpleType valueType = Factories.sc.core.createEntityItemAnySimpleType();
			valueType.setValue(value.getString());
			valueType.setDatatype(value.getType().getSimple().value());
			item.getValue().add(valueType);
		    }
		    items.add(item);
		}
	    } catch (UnsupportedOperationException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    } catch (ResolveException e) {
		throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    } catch (OvalException e) {
		throw new CollectException(e.getMessage(), FlagEnumeration.ERROR);
	    }
	} else if (plugin == null) {
	    throw new CollectException(JOVALMsg.getMessage(JOVALMsg.ERROR_MODE_SC), FlagEnumeration.NOT_COLLECTED);
	} else if (abortScan) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_SCAN_CANCELLED);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	} else {
	    @SuppressWarnings("unchecked")
	    Collection<ItemType> c = (Collection<ItemType>)plugin.getOvalProvider().getItems(obj, rc);
	    items = c;
	}
	objectItems.put(indexId, items);
	return items;
    }

    // Implement IOvalEngine

    public void setDefinitions(IDefinitions definitions) throws IllegalThreadStateException {
	switch(state) {
	  case RUNNING:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));

	  case COMPLETE_OK:
	  case COMPLETE_ERR:
	    reset();
	    // fall-through

	  default:
	    this.definitions = definitions;
	    break;
	}
    }

    public void setDefinitionFilter(IDefinitionFilter filter) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    mode = Mode.DIRECTED;
	    this.filter = filter;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setSystemCharacteristics(ISystemCharacteristics sc) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    mode = Mode.EXHAUSTIVE;
	    this.sc = sc;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public void setExternalVariables(IVariables variables) throws IllegalThreadStateException {
	switch(state) {
	  case CONFIGURE:
	    externalVariables = variables;
	    break;

	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public IProducer<Message> getNotificationProducer() {
	return producer;
    }

    public Result getResult() throws IllegalThreadStateException {
	switch(state) {
	  case COMPLETE_OK:
	    return Result.OK;

	  case COMPLETE_ERR:
	    return Result.ERR;

	  case CONFIGURE:
	  case RUNNING:
	  default:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public IResults getResults() throws IllegalThreadStateException {
	getResult();
	return results;
    }

    public Exception getError() throws IllegalThreadStateException {
	getResult();
	return error;
    }

    public void cancelScan(boolean cancelEval) throws IllegalThreadStateException {
	switch(state) {
	  case RUNNING:
	    abortScan = true;
	    abortEval = cancelEval;
	    if (thread != null && thread.isAlive()) {
		thread.interrupt();
	    }
	    break;
	  case CONFIGURE:
	    throw new IllegalThreadStateException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_STATE, state));
	}
    }

    public ResultEnumeration evaluateDefinition(String id)
		throws IllegalStateException, NoSuchElementException, OvalException {

	try {
	    state = State.RUNNING;
	    if (sc == null) {
		sc = new SystemCharacteristics(plugin.getSystemInfo());
		((ILoggable)sc).setLogger(logger);
	    }
	    if (results == null) {
		results = new Results(definitions, sc);
		results.setLogger(logger);
	    }
	    return evaluateDefinition(definitions.getDefinition(id)).getResult();
	} finally {
	    state = State.COMPLETE_OK;
	}
    }

    // Implement Runnable

    /**
     * The engine runs differently depending on the mode that was used to initialize it:
     *
     * DIRECTED:
     *   The Engine will iterate through the [filtered] definitions and probe only the objects that must be collected in
     *   order to evaluate them.
     *
     * EXHAUSTIVE:
     *   First the Engine probes all the objects in the OVAL definitions, or it uses the supplied ISystemCharacteristics.
     *   Then, all the definitions are evaluated.  This mirrors the way that ovaldi processes OVAL definitions.
     *
     * Note: if the plugin is connected before running, it will remain connected after the run has completed.  If it
     * was not connected before running, the engine will temporarily connect, then disconnect when finished.
     */
    public void run() {
	state = State.RUNNING;
	boolean doDisconnect = false;
	thread = Thread.currentThread();
	try {
	    //
	    // Connect if necessary
	    //
	    boolean scanRequired = sc == null;
	    if (scanRequired) {
	    	if (plugin == null) {
		    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_NONE));
		}
		if (!plugin.isConnected()) {
		    if (plugin.connect()) {
			doDisconnect = true;
		    } else {
			throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_SESSION_CONNECT));
		    }
		}
		sc = new SystemCharacteristics(plugin.getSystemInfo());
		((ILoggable)sc).setLogger(logger);
	    }

	    //
	    // Use the filter to separate the definitions into allowed and disallowed lists.
	    //
	    Collection<DefinitionType>allowed = filter.filter(definitions, true, logger);
	    Collection<DefinitionType>disallowed = filter.filter(definitions, false, logger);

	    if (scanRequired) {
		//
		// First analyze the definitions:
		//  - Determine which objects will be scanned (depending on the mode)
		//  - Determine which of those objects relate to variables (so they can be scanned first)
		//
		HashSet<ObjectType> allowedObjects = new HashSet<ObjectType>();
		HashSet<ObjectType> variableObjects = new HashSet<ObjectType>();
		switch(mode) {
		  case EXHAUSTIVE:
		    for (ObjectType obj : definitions.getObjects()) {
			allowedObjects.add(obj);
		    }
		    for (VariableType var : definitions.getVariables()) {
			for (String objectId : getObjectReferences(var, true)) {
			    variableObjects.add(definitions.getObject(objectId).getValue());
			}
		    }
		    break;

		  case DIRECTED:
		  default:
		    for (DefinitionType def : allowed) {
			for (String objectId : getObjectReferences(def, true)) {
			    allowedObjects.add(definitions.getObject(objectId).getValue());
			}
		    }
		    for (VariableType var : definitions.getVariables()) {
			for (String objectId : getObjectReferences(var, true)) {
			    ObjectType obj = definitions.getObject(objectId).getValue();
			    if (allowedObjects.contains(obj)) {
				variableObjects.add(obj);
			    }
			}
		    }
		    break;
		}

		//
		// Scan all the allowed objects. First, by collecting all the items for objects that are referenced by
		// variables. Then, by collecting all the remaining objects by batching (which is potentially faster).
		// Sets are deferred until the end, and run singly, since they cannot be batched easily.
		//
		// We could simply iterate through all the objects without this analysis, and objects and sets would be
		// resolved when they're encountered, but proceeding methodically should be faster.
		//
		producer.sendNotify(Message.OBJECT_PHASE_START, null);
		for (ObjectType obj : variableObjects) {
		    if (abortEval) {
			throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
		    }
		    scanObject(new RequestContext(obj));
		}
		HashSet<ObjectType> deferred = new HashSet<ObjectType>();
		for (ObjectType obj : allowedObjects) {
		    if (getObjectSet(obj) == null) {
			queueObject(new RequestContext(obj));
		    } else {
			deferred.add(obj);
		    }
		}
		scanQueue();
		for (ObjectType obj : deferred) {
		    if (abortEval) {
			throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
		    }
		    scanObject(new RequestContext(obj));
		}
		producer.sendNotify(Message.OBJECT_PHASE_END, null);

		producer.sendNotify(Message.SYSTEMCHARACTERISTICS, sc);
		if (doDisconnect) {
		    plugin.disconnect();
		    doDisconnect = false;
		}
		results = new Results(definitions, sc);
	    } else if (sc.unmapped()) {
		results = new Results(definitions, mapSystemCharacteristics());
	    } else {
		results = new Results(definitions, sc);
	    }
	    results.setLogger(logger);
	    producer.sendNotify(Message.DEFINITION_PHASE_START, null);

	    //
	    // First evaluate all the allowed definitions, then go through the disallowed definitions.  This makes it
	    // possible to cache both test and definition results without having to double-check if they were previously
	    // intentionally skipped.
	    //
	    evalEnabled = true;
	    for (DefinitionType definition : allowed) {
		evaluateDefinition(definition);
	    }
	    evalEnabled = false;
	    for (DefinitionType definition : disallowed) {
		evaluateDefinition(definition);
	    }

	    producer.sendNotify(Message.DEFINITION_PHASE_END, null);
	    state = State.COMPLETE_OK;
	} catch (Exception e) {
	    error = e;
	    state = State.COMPLETE_ERR;
	} finally {
	    if (doDisconnect) {
		plugin.disconnect();
	    }
	}
    }

    // Private

    private void reset() {
	sc = null;
	state = State.CONFIGURE;
	thread = null;
	variableMap = new Hashtable<String, Collection<IType>>();
	scanQueue = null;
	error = null;
	objectIndex = new Index<Integer>();
	objectItems = new HashMap<Integer, Collection<ItemType>>();
	objectCounter = 0;
    }

    /**
     * Recursively determine all the Object IDs referred to by the specified definition, extend_definition, criteria,
     * criterion, test, object, state, filter, set, variable or component.
     *
     * @param obj the object whose object references are to be recursively resolved
     * @param indirect specifies whether or not indirect references should be followed (i.e., variables, filters and sets).
     */
    private Collection<String> getObjectReferences(Object obj, boolean indirect) throws OvalException {
	Collection<String> results = new HashSet<String>();
	String reflectionId = null;
	try {
	    if (obj instanceof DefinitionType) {
		DefinitionType def = (DefinitionType)obj;
		if (def.isSetCriteria()) {
		    for (Object sub : def.getCriteria().getCriteriaOrCriterionOrExtendDefinition()) {
			results.addAll(getObjectReferences(sub, indirect));
		    }
		}
	    } else if (obj instanceof CriteriaType) {
		for (Object sub : ((CriteriaType)obj).getCriteriaOrCriterionOrExtendDefinition()) {
		    results.addAll(getObjectReferences(sub, indirect));
		}
	    } else if (obj instanceof ExtendDefinitionType) {
		Object next = definitions.getDefinition(((ExtendDefinitionType)obj).getDefinitionRef());
		results = getObjectReferences(next, indirect);
	    } else if (obj instanceof CriterionType) {
		results = getObjectReferences(definitions.getTest(((CriterionType)obj).getTestRef()).getValue(), indirect);
	    } else if (obj instanceof scap.oval.definitions.core.TestType) {
		ObjectRefType oRef = (ObjectRefType)ReflectionTool.invokeMethod(obj, "getObject");
		if (oRef != null) {
		    results.addAll(getObjectReferences(definitions.getObject(oRef.getObjectRef()).getValue(), indirect));
		}
		Object oRefs = ReflectionTool.invokeMethod(obj, "getState");
		@SuppressWarnings("unchecked")
		List<StateRefType> sRefs = (List<StateRefType>)oRefs;
		if (sRefs != null) {
		    for (StateRefType sRef : sRefs) {
			results.addAll(getObjectReferences(definitions.getState(sRef.getStateRef()).getValue(), indirect));
		    }
		}
	    } else if (obj instanceof ObjectType) {
		ObjectType ot = (ObjectType)obj;
		reflectionId = ot.getId();
		results.add(reflectionId);
		results.addAll(getObjectReferences(getObjectFilters(ot), indirect));
		results.addAll(getObjectReferences(getObjectSet(ot), indirect));
		if (ot instanceof VariableObject) {
		    VariableObject vo = (VariableObject)ot;
		    if (vo.isSetVarRef()) {
			Object next = definitions.getVariable((String)vo.getVarRef().getValue());
			results.addAll(getObjectReferences(next, indirect));
		    }
		} else {
		    for (Method method : ReflectionTool.getMethods(ot.getClass()).values()) {
			String methodName = method.getName();
			if (methodName.startsWith("get") && !OBJECT_METHOD_NAMES.contains(methodName)) {
			    results.addAll(getObjectReferences(method.invoke(ot), indirect));
			}
		    }
		}
	    } else if (obj instanceof StateType) {
		StateType st = (StateType)obj;
		reflectionId = st.getId();
		for (Method method : ReflectionTool.getMethods(obj.getClass()).values()) {
		    String methodName = method.getName();
		    if (methodName.startsWith("get") && !STATE_METHOD_NAMES.contains(methodName)) {
			results.addAll(getObjectReferences(method.invoke(st), indirect));
		    }
		}
	    } else if (obj instanceof Filter) {
		if (indirect) {
		    results = getObjectReferences(definitions.getState(((Filter)obj).getValue()), indirect);
		}
	    } else if (obj instanceof Set) {
		if (indirect) {
		    Set set = (Set)obj;
		    if (set.isSetObjectReference()) {
			for (String id : set.getObjectReference()) {
			    results.addAll(getObjectReferences(definitions.getObject(id).getValue(), indirect));
			}
			results.addAll(getObjectReferences(set.getFilter(), indirect));
		    } else {
			results = getObjectReferences(set.getSet(), indirect);
		    }
		}
	    } else if (obj instanceof JAXBElement) {
		results = getObjectReferences(((JAXBElement)obj).getValue(), indirect);
	    } else if (obj instanceof EntitySimpleBaseType) {
		EntitySimpleBaseType simple = (EntitySimpleBaseType)obj;
		if (indirect && simple.isSetVarRef()) {
		    results = getObjectReferences(definitions.getVariable(simple.getVarRef()), indirect);
		}
	    } else if (obj instanceof EntityComplexBaseType) {
		EntityComplexBaseType complex = (EntityComplexBaseType)obj;
		if (indirect && complex.isSetVarRef()) {
		    results = getObjectReferences(definitions.getVariable(complex.getVarRef()), indirect);
		}
	    } else if (obj instanceof List) {
		for (Object elt : (List)obj) {
		    results.addAll(getObjectReferences(elt, indirect));
		}
	    } else if (obj instanceof ObjectComponentType) {
		Object next = definitions.getObject(((ObjectComponentType)obj).getObjectRef()).getValue();
		results = getObjectReferences(next, indirect);
	    } else if (obj instanceof VariableComponentType) {
		VariableType var = definitions.getVariable(((VariableComponentType)obj).getVarRef());
		if (var instanceof LocalVariable) {
		    results = getObjectReferences(var, indirect);
		}
	    } else if (obj != null) {
		try {
		    results = getObjectReferences(getComponent(obj, true), indirect);
		} catch (OvalException e) {
		    // not a component
		}
	    }
	} catch (NoSuchElementException e) {
	    // this will lead to an error evaluating the definition later on
	    logger.warn(e.getMessage());
	} catch (ClassCastException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), reflectionId));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), reflectionId));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), reflectionId));
	}
	return results;
    }

    /**
     * Generate object item references in the system-characteristics.
     */
    private ISystemCharacteristics mapSystemCharacteristics() throws Exception {
	//
	// First, create a Queue of objects related to variables.  These must be mapped first.
	//
	Queue<String> varObjQueue = new LinkedList<String>();
	for (VariableType var : definitions.getVariables()) {
	    varObjQueue.addAll(getObjectReferences(var, true));
	}

	//
	// Map objects in the queue if all their dependencies (if any) are already mapped, or if not, push to the
	// end of the queue.
	//
	int counter = 0;
	String objectId = null;
	while((objectId = varObjQueue.poll()) != null) {
	    if (sc.containsObject(objectId)) {
		// already mapped
	    } else if (hasUnmappedReferences(objectId)) {
		// try again later
		varObjQueue.add(objectId);
	    } else {
		mapObject(objectId);
	    }
	    if (counter++ == 1000000) {
		throw new Exception(JOVALMsg.getMessage(JOVALMsg.ERROR_SC_MAP_OVERFLOW));
	    }
	}

	//
	// Map objects that are not already mapped.
	//
	for (ObjectType obj : definitions.getObjects()) {
	    objectId = obj.getId();
	    try {
		sc.getObject(objectId);
	    } catch (NoSuchElementException e) {
		mapObject(objectId);
	    }
	}
	return sc;
    }

    /**
     * Returns true if the object with the specified ID has any object references not already in the S-C.
     *
     * TBD: Add a reference cache for improved performance?
     */
    private boolean hasUnmappedReferences(String objectId) throws NoSuchElementException, OvalException {
	for (String dependencyId : getObjectReferences(definitions.getObject(objectId).getValue(), true)) {
	    if (objectId.equals(dependencyId)) {
		// ignore self
	    } else if (!sc.containsObject(dependencyId)) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Compare all the items corresponding to the object's type to the object specification, and map them together
     * in the System-Characteristics.
     */
    private void mapObject(String objectId) throws Exception {
	ObjectType obj = definitions.getObject(objectId).getValue();
	if (!OBJECT_ITEM_MAP.containsKey(obj.getClass())) {
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNMAPPABLE_OBJECT, objectId, obj.getClass().getName()));
	}
	Collection<? extends ItemType> items = sc.getItemsByType(OBJECT_ITEM_MAP.get(obj.getClass()));
	ObjectGroup group = new ObjectGroup(new RequestContext(definitions.getObject(objectId).getValue()));
	Collection<IBatch.IResult> results = new ArrayList<IBatch.IResult>();
	for (IBatch.IRequest request : group.getRequests()) {
	    RequestContext rc = (RequestContext)request.getContext();
	    Collection<ItemType> requestItems = new ArrayList<ItemType>();
	    for (ItemType item : items) {
		switch(compare(request.getObject(), item, rc)) {
		  case TRUE:
		    requestItems.add(item);
		    break;
		}
	    }
	    results.add(new Batch.Result(requestItems, request.getContext()));
	}
	items = group.combineItems(results, new FlagData());
	if (items.size() == 0) {
	    logger.debug(JOVALMsg.STATUS_OBJECT_NOITEM_MAP, objectId);
	    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, null);
	} else {
	    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.COMPLETE, null);
	    for (ItemType item : items) {
		logger.debug(JOVALMsg.STATUS_OBJECT_ITEM_MAP, objectId, item.getId().toString());
		sc.relateItem(objectId, item.getId());
	    }
	}
	for (IBatch.IResult result : results) {
	    for (VariableValueType var : ((RequestContext)result.getContext()).getVars()) {
		sc.storeVariable(var);
		sc.relateVariable(objectId, var.getVariableId());
	    }
	}
    }

    /**
     * Scan an object immediately using an adapter, including crawling down any encountered Sets, variables, etc.  Items
     * are stored in the system-characteristics as they are collected.
     *
     * If for some reason (like an error) no items can be obtained, this method just returns an empty list so processing
     * can continue.
     */
    private Collection<ItemType> scanObject(RequestContext rc) {
	ObjectType obj = rc.getObject();
	String objectId = obj.getId();
	if (sc.containsObject(objectId)) {
	    ArrayList<ItemType> result = new ArrayList<ItemType>();
	    for (JAXBElement<? extends ItemType> elt : sc.getItemsByObjectId(objectId)) {
		result.add(elt.getValue());
	    }
	    return result;
	}

	@SuppressWarnings("unchecked")
	Collection<ItemType> items = Collections.<ItemType>emptyList();
	if (abortScan) {
	    MessageType msg = Factories.common.createMessageType();
	    msg.setLevel(MessageLevelEnumeration.WARNING);
	    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_SCAN_CANCELLED));
	    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, msg);
	} else {
	    try {
		producer.sendNotify(Message.OBJECT, objectId);
		FlagData flags = new FlagData();
		List<MessageType> messages = new ArrayList<MessageType>();
		Set s = getObjectSet(obj);
		if (s == null) {
		    try {
			items = new ObjectGroup(rc).getItems(flags);
		    } catch (ResolveException e) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.ERROR);
			msg.setValue(e.getMessage());
			messages.add(msg);
			flags.add(FlagEnumeration.ERROR);
		    }
		} else {
		    items = getSetItems(s, rc, flags);
		    if (items.size() == 0) {
			MessageType msg = Factories.common.createMessageType();
			msg.setLevel(MessageLevelEnumeration.INFO);
			msg.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_SET));
			messages.add(msg);
		    }
		}
		sc.setObject(objectId, obj.getComment(), obj.getVersion(), null, null);
		messages.addAll(rc.getMessages());
		for (MessageType msg : messages) {
		    sc.setObject(objectId, null, null, null, msg);
		    switch(msg.getLevel()) {
		      case FATAL:
		      case ERROR:
			flags.add(FlagEnumeration.INCOMPLETE);
			break;
		    }
		}
		if (flags.getFlag() == FlagEnumeration.COMPLETE && items.size() == 0) {
		    sc.setObject(objectId, null, null, FlagEnumeration.DOES_NOT_EXIST, null);
		} else {
		    sc.setObject(objectId, null, null, flags.getFlag(), null);
		}
		for (VariableValueType var : rc.getVars()) {
		    sc.storeVariable(var);
		    sc.relateVariable(objectId, var.getVariableId());
		}
		for (ItemType item : items) {
		    sc.relateItem(objectId, sc.storeItem(item));
		}
	    } catch (OvalException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		String err = JOVALMsg.getMessage(JOVALMsg.ERROR_OVAL, e.getMessage());
		msg.setValue(err);
		sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.ERROR, msg);
	    }
	}
	return items;
    }

    /**
     * Attempt to queue an object request for batched scanning. If the object cannot be queued in a batch, then
     * it is scanned immediately.
     */
    private void queueObject(RequestContext rc) {
	ObjectType obj = rc.getObject();
	String id = obj.getId();
	if (sc.containsObject(id)) {
	    // object has already been scanned
	} else if (obj instanceof VariableObject) {
	    scanObject(rc);
	} else if (!abortScan && getObjectSet(obj) == null) {
	    if (scanQueue == null) {
		scanQueue = new HashMap<String, ObjectGroup>();
	    }
	    try {
		IBatch batch = (IBatch)plugin.getOvalProvider();
		ObjectGroup group = new ObjectGroup(rc);
		boolean queued = false;
		for (IBatch.IRequest request : group.getRequests()) {
		    if (batch.queue(request)) {
			queued = true;
		    } else {
			queued = false;
			break;
		    }
		}
		if (queued) {
		    logger.debug(JOVALMsg.STATUS_OBJECT_QUEUE, id);
		    scanQueue.put(id, group);
		} else {
		    scanObject(rc);
		}
	    } catch (ResolveException e) {
		logger.warn(e.getMessage());
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		sc.setObject(id, obj.getComment(), obj.getVersion(), FlagEnumeration.ERROR, msg);
	    } catch (OvalException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_OVAL, e.getMessage()));
		sc.setObject(id, obj.getComment(), obj.getVersion(), FlagEnumeration.ERROR, msg);
	    }
	} else {
	    //
	    // Set objects cannot be batched.
	    //
	    scanObject(rc);
	}
    }

    /**
     * Scan all the objects in the request queue in batch.
     */
    private synchronized void scanQueue() throws OvalException {
	if (scanQueue != null && scanQueue.size() > 0) {
	    if (abortScan) {
		for (String objectId : scanQueue.keySet()) {
		    ObjectType obj = definitions.getObject(objectId).getValue();
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.WARNING);
		    msg.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_SCAN_CANCELLED));
		    sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.NOT_COLLECTED, msg);
		}
		return;
	    }

	    //
	    // Organize results by object ID
	    //
	    Map<String, Collection<IBatch.IResult>> results = new HashMap<String, Collection<IBatch.IResult>>();
	    logger.debug(JOVALMsg.STATUS_OBJECT_BATCH, scanQueue.size());
	    ArrayList<String> ids = new ArrayList<String>();
	    ids.addAll(scanQueue.keySet());
	    producer.sendNotify(Message.OBJECTS, ids.toArray(new String[ids.size()]));

	    for (IBatch.IResult result : ((IBatch)plugin.getOvalProvider()).exec()) {
		ObjectType obj = ((RequestContext)result.getContext()).getObject();
		String objectId = obj.getId();
		if (!results.containsKey(objectId)) {
		    results.put(objectId, new ArrayList<IBatch.IResult>());
		}
		results.get(objectId).add(result);
	    }

	    //
	    // Recombine result sets into discrete object item lists
	    //
	    for (Map.Entry<String, Collection<IBatch.IResult>> entry : results.entrySet()) {
		String objectId = entry.getKey();
		RequestContext rc = scanQueue.get(objectId).getContext();
		ObjectType obj = rc.getObject();
		if (!sc.containsObject(objectId)) {
		    Collection<ItemType> items = new ArrayList<ItemType>();
		    List<MessageType> messages = new ArrayList<MessageType>();
		    FlagData flags = new FlagData();
		    items = scanQueue.get(objectId).combineItems(entry.getValue(), flags);
		    messages.addAll(rc.getMessages());
		    sc.setObject(objectId, null, null, null, null);
		    for (VariableValueType var : rc.getVars()) {
			sc.storeVariable(var);
			sc.relateVariable(objectId, var.getVariableId());
		    }
		    for (MessageType msg : messages) {
			sc.setObject(objectId, null, null, null, msg);
			switch(msg.getLevel()) {
			  case FATAL:
			  case ERROR:
			    flags.add(FlagEnumeration.INCOMPLETE);
			    break;
			}
		    }
		    if (flags.getFlag() == FlagEnumeration.COMPLETE && items.size() == 0) {
			sc.setObject(objectId, obj.getComment(), obj.getVersion(), FlagEnumeration.DOES_NOT_EXIST, null);
		    } else {
			sc.setObject(objectId, obj.getComment(), obj.getVersion(), flags.getFlag(), null);
		    }
		    for (ItemType item : items) {
			sc.relateItem(objectId, sc.storeItem(item));
		    }
		}
	    }
	    scanQueue = null;
	}
    }

    /**
     * An ObjectGroup is a container for ObjectType information, where the constituent entities of the ObjectType
     * may be formed by references to multi-valued variables.
     */
    class ObjectGroup {
	RequestContext rc;
	String id;
	Class<?> clazz;
	Object behaviors = null, factory;
	Method create;
	int size = 1;
	Map<Collection<ObjectType>, CheckEnumeration> objects = null;

	/**
	 * Lists of entity values, indexed by getter method name.
	 */
	Hashtable<String, List<Object>> entities;

	/**
	 * Check operations to perform on entities, indexed by getter method name. Only set for entites sourced from
	 * variable references.
	 */
	Hashtable<String, CheckEnumeration> varChecks;

	/**
	 * Create a new ObjectGroup for the specified request context.
	 */
	ObjectGroup(RequestContext rc) throws OvalException, ResolveException {
	    this.rc = rc;
	    ObjectType obj = rc.getObject();
	    id = obj.getId();
	    clazz = obj.getClass();
	    try {
		//
		// Collect everything necessary to manufacture new ObjectTypes
		//
		String pkgName = clazz.getPackage().getName();
		Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
		factory = factoryClass.newInstance();
		String unqualClassName = clazz.getName().substring(pkgName.length() + 1);
		create = factoryClass.getMethod("create" + unqualClassName);
		behaviors = ReflectionTool.invokeMethod(obj, "getBehaviors");

		//
		// Collect all the entity values
		//
		entities = new Hashtable<String, List<Object>>();
		varChecks = new Hashtable<String, CheckEnumeration>();
		for (Method method : ReflectionTool.getMethods(clazz).values()) {
		    String methodName = method.getName();
		    if (methodName.startsWith("get") && !OBJECT_METHOD_NAMES.contains(methodName)) {
			Object entity = method.invoke(obj);
			if (entity == null) {
			    //
			    // entity was unspeficied in the object definition, so it must be optional, and its absence
			    // will not impact the number of available permutations.
			    //
			} else {
			    VarData vd = new VarData();
			    List<Object> values = resolveUnknownEntity(methodName, entity, rc, vd);
			    if (vd.isRef()) {
				varChecks.put(methodName, vd.getCheck());
			    }
			    size = size * values.size();
			    if (values.size() == 0) {
				MessageType message = Factories.common.createMessageType();
				message.setLevel(MessageLevelEnumeration.INFO);
				String entityName = methodName.substring(3).toLowerCase();
				message.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_ENTITY, entityName));
				rc.addMessage(message);
			    }
			    entities.put(methodName, values);
			}
		    }
		}

		//
		// For any NONE_SATISFY var_checks, invert all its entities, so it can be treated like an ALL.
		//
		for (String methodName : varChecks.keySet()) {
		    if (varChecks.get(methodName) == CheckEnumeration.NONE_SATISFY) {
			for (Object entity : entities.get(methodName)) {
			    try {
				invertEntity(rc.getObject(), methodName, entity);
			    } catch (Exception e) {
				throw new ResolveException(e);
			    }
			}
		    }
		}
	    } catch (ClassNotFoundException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (InstantiationException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (NoSuchMethodException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (IllegalAccessException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (InvocationTargetException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    }
	}

	/**
	 * Get the object ID.
	 */
	String getId() {
	    return id;
	}

	RequestContext getContext() {
	    return rc;
	}

	/**
	 * Get all the items for this object group (immediately).
	 */
	Collection<ItemType> getItems(FlagData flags) throws OvalException {
	    if (varChecks.size() == 0) {
		//
		// There are no variables, so the group only contains the input object from the initializing context.
		//
		try {
		    @SuppressWarnings("unchecked")
		    Collection<ItemType> items = (Collection<ItemType>)Engine.this.getItems(rc.getObject(), rc);
		    filterItems(items, getObjectFilters(rc.getObject()), rc);
		    flags.add(FlagEnumeration.COMPLETE);
		    return items;
		} catch (CollectException e) {
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.INFO);
		    String err = JOVALMsg.getMessage(JOVALMsg.STATUS_ADAPTER_COLLECTION, e.getMessage());
		    msg.setValue(err);
		    rc.addMessage(msg);
		    flags.add(e.getFlag());
		} catch (Exception e) {
		    //
		    // Handle an uncaught, unexpected exception emanating from the adapter
		    //
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    MessageType msg = Factories.common.createMessageType();
		    msg.setLevel(MessageLevelEnumeration.ERROR);
		    if (e.getMessage() == null) {
			msg.setValue(e.getClass().getName());
		    } else {
			msg.setValue(e.getMessage());
		    }
		    rc.addMessage(msg);
		    flags.add(FlagEnumeration.ERROR);
		}
	    } else if (size > 0) {
		//
		// Collect results from the object permutations, then combine them into a single result.
		//
		Collection<IBatch.IResult> results = new ArrayList<IBatch.IResult>();
		for (IBatch.IRequest request : getRequests()) {
		    ObjectType obj = request.getObject();
		    RequestContext ctx = (RequestContext)request.getContext();
		    try {
			@SuppressWarnings("unchecked")
			Collection<ItemType> unfiltered = (Collection<ItemType>)Engine.this.getItems(obj, ctx);
			flags.add(FlagEnumeration.COMPLETE);
			// Note - items will be filtered later, in the combineItems method
			results.add(new Batch.Result(unfiltered, ctx));
		    } catch (CollectException e) {
			results.add(new Batch.Result(e, ctx));
		    } catch (Exception e) {
			results.add(new Batch.Result(new CollectException(e, FlagEnumeration.ERROR), ctx));
		    }
		}
		return combineItems(results, flags);
	    }
	    return Collections.<ItemType>emptyList();
	}

	/**
	 * Get all the requests needed to determine the matching items for this object group.
	 */
	Collection<IBatch.IRequest> getRequests() throws OvalException {
	    if (varChecks.size() == 0) {
		Collection<IBatch.IRequest> requests = new ArrayList<IBatch.IRequest>();
		requests.add(new Batch.Request(rc.getObject(), rc));
		return requests;
	    } else if (size > 0) {
		Collection<IBatch.IRequest> requests = new ArrayList<IBatch.IRequest>();
		for (Map.Entry<String, CheckEnumeration> entry : varChecks.entrySet()) {
		    RequestContext vrc = rc.variant(entry.getKey(), entry.getValue());
		    for (Object value : entities.get(entry.getKey())) {
			for (ObjectType obj : getPermutations(entry.getKey(), value)) {
			    requests.add(new Batch.Request(obj, vrc));
			}
		    }
		}
		return requests;
	    } else {
		return Collections.<IBatch.IRequest>emptyList();
	    }
	}

	/**
	 * Combine batched permutation results into a single item list.
	 */
	Collection<ItemType> combineItems(Collection<IBatch.IResult> results, FlagData flags) {
	    try {
		if (size > 0) {
		    Map<RequestContext, Collection<ItemSet<ItemType>>> sets = null;
		    sets = new HashMap<RequestContext, Collection<ItemSet<ItemType>>>();
		    for (IBatch.IResult result : results) {
			RequestContext ctx = (RequestContext)result.getContext();
			if (!sets.containsKey(ctx)) {
			    sets.put(ctx, new ArrayList<ItemSet<ItemType>>());
			}
			@SuppressWarnings("unchecked")
			Collection<ItemType> items = (Collection<ItemType>)result.getItems();
			filterItems(items, getObjectFilters(rc.getObject()), rc);
			flags.add(FlagEnumeration.COMPLETE);
			sets.get(ctx).add(new ItemSet<ItemType>(items));
		    }
		    ItemSet<ItemType> items = new ItemSet<ItemType>();
		    for (Map.Entry<RequestContext, Collection<ItemSet<ItemType>>> entry : sets.entrySet()) {
			ItemSet<ItemType> checked = null;
			for (ItemSet<ItemType> set : entry.getValue()) {
			    if (checked == null) {
				checked = set;
			    } else {
				CheckEnumeration check = entry.getKey().getVariant().getCheck();
				switch(check) {
				  case NONE_SATISFY:
				  case ALL:
				    checked = checked.intersection(set);
				    break;
				  case AT_LEAST_ONE:
				    checked = checked.union(set);
				    break;
				  case ONLY_ONE:
				    checked = checked.complement(set).union(set.complement(checked));
				    break;
				  default:
				    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_CHECK, check));
				}
			    }
			}
			items = items.union(checked);
		    }
		    return items.toList();
		}
	    } catch (CollectException e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.INFO);
		String err = JOVALMsg.getMessage(JOVALMsg.STATUS_ADAPTER_COLLECTION, e.getMessage());
		msg.setValue(err);
		rc.addMessage(msg);
		flags.add(e.getFlag());
	    } catch (Exception e) {
		//
		// Handle an uncaught, unexpected exception emanating from the adapter
		//
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		if (e.getMessage() == null) {
		    msg.setValue(e.getClass().getName());
		} else {
		    msg.setValue(e.getMessage());
		}
		rc.addMessage(msg);
		flags.add(FlagEnumeration.ERROR);
	    }
	    return Collections.<ItemType>emptyList();
	}

	// Private

	/**
	 * Given a particular value of an entity, create a collection of all the possible resulting ObjectTypes.
	 */
	private Collection<ObjectType> getPermutations(String getter, Object value) throws OvalException {
	    if (!entities.get(getter).contains(value)) {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_OBJECT_PERMUTATION, getter, value.toString()));
	    } else if (size == 0) {
		return Collections.<ObjectType>emptyList();
	    }
	    int numPermutations = size / entities.get(getter).size();
	    ArrayList<Hashtable<String, Object>> valList = new ArrayList<Hashtable<String, Object>>(numPermutations);
	    for (int i=0; i < numPermutations; i++) {
		Hashtable<String, Object> values = new Hashtable<String, Object>();
		values.put(getter, value);
		valList.add(values);
	    }
	    for (String key : entities.keySet()) {
		if (!getter.equals(key)) {
		    List<Object> list = entities.get(key);
		    int numRepeats = numPermutations / list.size();
		    int index = 0;
		    for (Object val : list) {
			for (int i=0; i < numRepeats; i++) {
			    valList.get(index++).put(key, val);
			}
		    }
		}
	    }
	    ArrayList<ObjectType> objects = new ArrayList<ObjectType>();
	    try {
		for (Hashtable<String, Object> values : valList) {
		    objects.add(newObject(values));
		}
	    } catch (InstantiationException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (NoSuchMethodException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (IllegalAccessException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    } catch (InvocationTargetException e) {
		logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), id));
	    }
	    return objects;
	}

	/**
	 * Make a new instance of an ObjectType, with the specified group of entity values.
	 */
	private ObjectType newObject(Hashtable<String, Object> values)
		throws InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

	    ObjectType obj = (ObjectType)create.invoke(factory);
	    obj.setId(id);
	    if (behaviors != null) {
		Method setBehaviors = clazz.getMethod("setBehaviors", behaviors.getClass());
		setBehaviors.invoke(obj, behaviors);
	    }
	    for (String getter : values.keySet()) {
		Object entity = values.get(getter);
		String setter = new StringBuffer("s").append(getter.substring(1)).toString();
		@SuppressWarnings("unchecked")
		Method setObj = clazz.getMethod(setter, entity.getClass());
		setObj.invoke(obj, entity);
	    }
	    return obj;
	}
    }

    class Variant {
	private String name;
	private CheckEnumeration check;

	Variant(String name, CheckEnumeration check) {
	    this.name = name;
	    this.check = check;
	}

	String getName() {
	    return name;
	}

	CheckEnumeration getCheck() {
	    return check;
	}
    }

    /**
     * Implementation of IFunction.IFunctionContext.
     */
    class RequestContext implements IFunction.IFunctionContext {
	private Stack<Level> levels;
	private Map<Variant, RequestContext> variants;
	private Variant variant = null;

	RequestContext(ObjectType object) {
	    levels = new Stack<Level>();
	    levels.push(new Level(object));
	}

	Collection<VariableValueType> getVars() {
	    return getVars(levels.peek().vars);
	}

	Collection<MessageType> getMessages() {
	    return levels.peek().messages;
	}

	void addVar(VariableValueType var) {
	    String id = var.getVariableId();
	    String value = (String)var.getValue();
	    Hashtable<String, HashSet<String>> vars = levels.peek().vars;
	    if (vars.containsKey(id)) {
		vars.get(id).add(value);
	    } else {
		HashSet<String> vals = new HashSet<String>();
		vals.add(value);
		vars.put(id, vals);
	    }
	}

	ObjectType getObject() {
	    return levels.peek().object;
	}

	void pushObject(ObjectType obj) throws OvalException {
	    for (Level level : levels) {
		if (level.object.equals(obj)) {
		    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_OVAL_LOOP, obj.getId()));
		}
	    }
	    levels.push(new Level(obj));
	}

	ObjectType popObject() {
	    Level level = levels.pop();
	    for (VariableValueType var : getVars(level.vars)) {
		addVar(var);
	    }
	    levels.peek().messages.addAll(level.messages);
	    return level.object;
	}

	RequestContext variant(String name, CheckEnumeration varCheck) {
	    if (variants == null) {
		variants = new HashMap<Variant, RequestContext>();
	    }
	    Variant v = new Variant(name, varCheck);
	    RequestContext ctx = new RequestContext(levels.peek(), v);
	    variants.put(v, ctx);
	    return ctx;
	}

	Variant getVariant() {
	    return variant;
	}

	Collection<Map.Entry<Variant, RequestContext>> variants() {
	    if (variants == null) {
		return null;
	    } else {
		return variants.entrySet();
	    }
	}

	// Implement IRequestContext

	public void addMessage(MessageType msg) {
	    levels.peek().messages.add(msg);
	}

	// Implement IFunction.IFunctionContext

	public ISession getSession() {
	    return Engine.this.plugin.getSession();
	}

	public Object getComponent(Object obj) throws OvalException {
	    return Engine.this.getComponent(obj, false);
	}

	public Collection<IType> resolveComponent(Object component) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {

	    return Engine.this.resolveComponent(component, this);
	}

	// Private

	private RequestContext(Level level, Variant variant) {
	    levels = new Stack<Level>();
	    levels.push(level);
	    this.variant = variant;
	}

	private Collection<VariableValueType> getVars(Hashtable<String, HashSet<String>> vars) {
	    Collection<VariableValueType> result = new ArrayList<VariableValueType>();
	    for (String id : vars.keySet()) {
		for (String value : vars.get(id)) {
		    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(id);
		    variableValueType.setValue(value);
		    result.add(variableValueType);
		}
	    }
	    return result;
	}

	private class Level {
	    ObjectType object;
	    Hashtable<String, HashSet<String>> vars;
	    Collection<MessageType> messages;

	    Level(ObjectType object) {
		this.object = object;
		this.vars = new Hashtable<String, HashSet<String>>();
		this.messages = new ArrayList<MessageType>();
	    }
	}
    }

    /**
     * A container class for communicating discovered variable reference information from the resolveUnknownEntity method.
     */
    private class VarData {
	boolean isVar;
	CheckEnumeration check;

	VarData() {
	    isVar = false;
	}

	boolean isRef() {
	    return isVar;
	}

	void setCheck(CheckEnumeration check) {
	    isVar = true;
	    if (check == null) {
		this.check = CheckEnumeration.ALL;
	    } else {
		this.check = check;
	    }
	}

	CheckEnumeration getCheck() {
	    return check;
	}
    }

    private OperationEnumeration invert(OperationEnumeration op) throws IllegalArgumentException {
	switch(op) {
	  case EQUALS:
	    return OperationEnumeration.NOT_EQUAL;
	  case GREATER_THAN:
	    return OperationEnumeration.LESS_THAN_OR_EQUAL;
	  case GREATER_THAN_OR_EQUAL:
	    return OperationEnumeration.LESS_THAN;
	  case LESS_THAN:
	    return OperationEnumeration.GREATER_THAN_OR_EQUAL;
	  case LESS_THAN_OR_EQUAL:
	    return OperationEnumeration.GREATER_THAN;
	  case NOT_EQUAL:
	    return OperationEnumeration.EQUALS;
	  case CASE_INSENSITIVE_EQUALS:
	    return OperationEnumeration.CASE_INSENSITIVE_NOT_EQUAL;
	  case CASE_INSENSITIVE_NOT_EQUAL:
	    return OperationEnumeration.CASE_INSENSITIVE_EQUALS;

	  case SUBSET_OF:
	  case SUPERSET_OF:
	  case BITWISE_AND:
	  case BITWISE_OR:
	  case PATTERN_MATCH:
	  default:
	    throw new IllegalArgumentException(op.value());
	}
    }

    private Object invertEntity(ObjectType obj, String methodName, Object entity) throws IllegalArgumentException,
		OvalException, ResolveException, InstantiationException, ClassNotFoundException,
		NoSuchMethodException, IllegalAccessException, InvocationTargetException {

	if (entity instanceof JAXBElement) {
	    String pkgName = obj.getClass().getPackage().getName();
	    Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
	    Object factory = factoryClass.newInstance();
	    String unqualClassName = obj.getClass().getName().substring(pkgName.length() + 1);
	    String entityName = methodName.substring(3);
	    if (((JAXBElement)entity).getValue() == null) {
		throw new IllegalArgumentException(((JAXBElement)entity).getName().toString());
	    } else {
		Class targetClass = ((JAXBElement)entity).getValue().getClass();
		Method method = factoryClass.getMethod("create" + unqualClassName + entityName, targetClass);
		return method.invoke(factory, invertEntity(obj, methodName, ((JAXBElement)entity).getValue()));
	    }
	} else if (entity instanceof EntitySimpleBaseType) {
	    EntitySimpleBaseType base = (EntitySimpleBaseType)entity;
	    base.setOperation(invert(base.getOperation()));
	    return base;
	} else if (entity instanceof EntityObjectRecordType) {
	    EntityObjectRecordType record = (EntityObjectRecordType)entity;
	    record.setOperation(invert(record.getOperation()));
	    return record;
	} else {
	    String id = obj.getId();
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY, entity.getClass().getName(), id);
	    throw new OvalException(message);
	}
    }

    /**
     * Take an entity that may be a var_ref, and return a list of all resulting concrete entities (i.e., isSetValue == true).
     * The result may be a JAXBElement list, or EntitySimpleBaseType list or an EntityObjectRecordType list.
     */
    private List<Object> resolveUnknownEntity(String methodName, Object entity, RequestContext rc, VarData vd)
		throws OvalException, ResolveException, InstantiationException, ClassNotFoundException,
		NoSuchMethodException, IllegalAccessException, InvocationTargetException {

	List<Object> result = new ArrayList<Object>();
	//
	// JAXBElement-wrapped entities are unwrapped, resolved, and re-wrapped using introspection.
	//
	if (entity instanceof JAXBElement) {
	    String pkgName = rc.getObject().getClass().getPackage().getName();
	    Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
	    Object factory = factoryClass.newInstance();
	    String unqualClassName = rc.getObject().getClass().getName().substring(pkgName.length() + 1);
	    String entityName = methodName.substring(3);
	    if (((JAXBElement)entity).getValue() == null) {
		result.add(entity);
	    } else {
		Class targetClass = ((JAXBElement)entity).getValue().getClass();
		Method method = factoryClass.getMethod("create" + unqualClassName + entityName, targetClass);
		for (Object resolved : resolveUnknownEntity(methodName, ((JAXBElement)entity).getValue(), rc, vd)) {
		    result.add(method.invoke(factory, resolved));
		}
	    }
	//
	// All the simple types are handled here using introspection.
	//
	} else if (entity instanceof EntitySimpleBaseType) {
	    EntitySimpleBaseType simple = (EntitySimpleBaseType)entity;
	    if (simple.isSetVarRef()) {
		vd.setCheck((CheckEnumeration)ReflectionTool.invokeMethod(simple, "getVarCheck"));
		try {
		    IType.Type t = TypeFactory.convertType(TypeFactory.getSimpleDatatype(simple.getDatatype()));
		    Class objClass = entity.getClass();
		    String pkgName = objClass.getPackage().getName();
		    Class<?> factoryClass = Class.forName(pkgName + ".ObjectFactory");
		    Object factory = factoryClass.newInstance();
		    String unqualClassName = objClass.getName().substring(pkgName.length() + 1);
		    Method method = factoryClass.getMethod("create" + unqualClassName);
		    for (IType type : resolveVariable(simple.getVarRef(), rc)) {
			EntitySimpleBaseType instance = (EntitySimpleBaseType)method.invoke(factory);
			instance.setDatatype(simple.getDatatype());
			instance.setValue(type.cast(t).getString());
			instance.setOperation(simple.getOperation());
			result.add(instance);
		    }
		} catch (TypeConversionException e) {
		    MessageType message = Factories.common.createMessageType();
		    message.setLevel(MessageLevelEnumeration.ERROR);
		    message.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, e.getMessage()));
		    rc.addMessage(message);
		}
	    } else {
		result.add(entity);
	    }
	//
	// In the future, it may be necessary to support additional complex types, but right now the only complex type
	// is the RECORD.
	//
	} else if (entity instanceof EntityObjectRecordType) {
	    EntityObjectRecordType record = (EntityObjectRecordType)entity;
	    if (record.isSetVarRef()) {
		vd.setCheck((CheckEnumeration)ReflectionTool.invokeMethod(record, "getVarCheck"));
		for (IType type : resolveVariable(record.getVarRef(), rc)) {
		    switch(type.getType()) {
		      case RECORD: {
			EntityObjectRecordType instance = Factories.definitions.core.createEntityObjectRecordType();
			instance.setDatatype(ComplexDatatypeEnumeration.RECORD.value());
			RecordType rt = (RecordType)type;
			for (String fieldName : rt.fields()) {
			    IType fieldType = rt.getField(fieldName);
			    try {
				EntityObjectFieldType fieldEntity = Factories.definitions.core.createEntityObjectFieldType();
				fieldEntity.setName(fieldName);
				fieldEntity.setDatatype(type.getType().getSimple().value());
				fieldEntity.setValue(type.getString());
				//
				// A resolved entity field cannot have any check information, so use the default ALL.
				//
				fieldEntity.setEntityCheck(CheckEnumeration.ALL);
				instance.getField().add(fieldEntity);
			    } catch (UnsupportedOperationException e) {
				MessageType message = Factories.common.createMessageType();
				message.setLevel(MessageLevelEnumeration.ERROR);
				message.setValue(e.getMessage());
				rc.addMessage(message);
			    }
			}
			result.add(instance);
			break;
		      }

		      default:
			MessageType message = Factories.common.createMessageType();
			message.setLevel(MessageLevelEnumeration.ERROR);
			String s = JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, type.getType(), IType.Type.RECORD);
			message.setValue(s);
			rc.addMessage(message);
			break;
		    }
		}
	    } else {
		//
		// Resolve any var_refs in the fields, and return a permutation list of the resulting record types.
		//
		int numPermutations = 1;
		List<List<EntityObjectFieldType>> lists = new ArrayList<List<EntityObjectFieldType>>();
		for (EntityObjectFieldType field : record.getField()) {
		    List<EntityObjectFieldType> resolved = resolveField(field, rc);
		    if (resolved.size() == 0) {
			//
			// This condition means that the field was a variable reference with no value.  The
			// OVAL specification implies that this should mean the record does not exist.  Returning
			// an empty entity list makes this assertion.
			//
			// http://oval.mitre.org/language/version5.10.1/ovaldefinition/documentation/oval-definitions-schema.html#EntityAttributeGroup
			//
			numPermutations = 0;
			MessageType message = Factories.common.createMessageType();
			message.setLevel(MessageLevelEnumeration.INFO);
			String entityName = methodName.substring(3).toLowerCase();
			String s = JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_RECORD, entityName, field.getName());
			message.setValue(s);
			rc.addMessage(message);
		    } else if (resolved.size() > 0) {
			numPermutations = numPermutations * resolved.size();
			lists.add(resolved);
		    }
		}
		if (numPermutations > 0) {
		    List<EntityObjectRecordType> records = new ArrayList<EntityObjectRecordType>();
		    for (int i=0; i < numPermutations; i++) {
			EntityObjectRecordType base = Factories.definitions.core.createEntityObjectRecordType();
			base.setDatatype(ComplexDatatypeEnumeration.RECORD.value());
			base.setMask(record.getMask());
			base.setOperation(record.getOperation());
			records.add(base);
		    }
		    for (List<EntityObjectFieldType> list : lists) {
			int divisor = list.size();
			int groupSize = records.size() / divisor;
			int index = 0;
			for (int i=0; i < list.size(); i++) {
			    for (int j=0; j < groupSize; j++) {
				records.get(index++).getField().add(list.get(i));
			    }
			}
		    }
		    result.addAll(records);
		}
	    }
	} else {
	    String id = rc.getObject().getId();
	    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY, entity.getClass().getName(), id);
	    throw new OvalException(message);
	}
	return result;
    }

    /**
     * Take a field that may contain a var_ref, and return the resolved list of fields (i.e., isSetVarRef() == false).
     */
    private List<EntityObjectFieldType> resolveField(EntityObjectFieldType field, RequestContext rc)
		throws ResolveException, OvalException {

	List<EntityObjectFieldType> result = new ArrayList<EntityObjectFieldType>();
	if (field.isSetVarRef()) {
	    try {
		IType.Type t = TypeFactory.convertType(TypeFactory.getSimpleDatatype(field.getDatatype()));
		for (IType type : resolveVariable(field.getVarRef(), rc)) {
		    EntityObjectFieldType fieldEntity = Factories.definitions.core.createEntityObjectFieldType();
		    fieldEntity.setName(field.getName());
		    fieldEntity.setDatatype(field.getDatatype());
		    fieldEntity.setValue(type.cast(t).getString());
		    fieldEntity.setEntityCheck(field.getEntityCheck());
		    result.add(fieldEntity);
		}
	    } catch (TypeConversionException e) {
		MessageType message = Factories.common.createMessageType();
		message.setLevel(MessageLevelEnumeration.ERROR);
		message.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, e.getMessage()));
		rc.addMessage(message);
	    }
	} else {
	    result.add(field);
	}
	return result;
    }

    /**
     * If getSet() were a method of ObjectType (instead of only some of its subclasses), this is what it would return.
     */
    private Set getObjectSet(ObjectType obj) {
	Set objectSet = null;
	try {
	    Method isSetSet = obj.getClass().getMethod("isSetSet");
	    if (((Boolean)isSetSet.invoke(obj)).booleanValue()) {
		Method getSet = obj.getClass().getMethod("getSet");
		objectSet = (Set)getSet.invoke(obj);
	    }
	} catch (NoSuchMethodException e) {
	    // Object doesn't support Sets; no big deal.
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return objectSet;
    }

    /**
     * If getFilter() were a method of ObjectType (instead of only some of its subclasses), this is what it would return.
     */
    private List<Filter> getObjectFilters(ObjectType obj) {
	List<Filter> filters = new ArrayList<Filter>();
	Object oFilters = ReflectionTool.invokeMethod(obj, "getFilter");
	if (oFilters != null && oFilters instanceof List) {
	    for (Object oFilter : (List)oFilters) {
		if (oFilter instanceof Filter) {
		    filters.add((Filter)oFilter);
		}
	    }
	}
	return filters;
    }

    /**
     * Filter the Collection of items.
     */
    private void filterItems(Collection<ItemType> items, List<Filter> filters, RequestContext rc)
		throws NoSuchElementException, OvalException {

	if (filters.size() > 0) {
	    for (Filter filter : filters) {
		StateType state = definitions.getState(filter.getValue()).getValue();
		Iterator<ItemType> iter = items.iterator();
		while (iter.hasNext()) {
		    ItemType item = iter.next();
		    try {
			ResultEnumeration result = compare(state, item, rc);
			switch(filter.getAction()) {
			  case INCLUDE:
			    if (result == ResultEnumeration.TRUE) {
				logger.debug(JOVALMsg.STATUS_FILTER, filter.getAction().value(),
					     item.getId() == null ? "(unassigned)" : item.getId(), rc.getObject().getId());
			    } else {
				iter.remove();
			    }
			    break;

			  case EXCLUDE:
			    if (result == ResultEnumeration.TRUE) {
				iter.remove();
				logger.debug(JOVALMsg.STATUS_FILTER, filter.getAction().value(),
					     item.getId() == null ? "(unassigned)" : item.getId(), rc.getObject().getId());
			    }
			    break;
			}
		    } catch (TestException e) {
			logger.debug(JOVALMsg.ERROR_COMPONENT_FILTER, e.getMessage());
			logger.trace(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    }
		}
	    }
	}
	int resultCount = items.size();
	if (0 < maxItems && maxItems < resultCount) {
	    //
	    // Add an error so the object will be "incomplete"
	    //
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_OBJECT_OVERFLOW, resultCount, maxItems);
	    logger.warn(msg);
	    MessageType message = Factories.common.createMessageType();
	    message.setLevel(MessageLevelEnumeration.ERROR);
	    message.setValue(msg);
	    rc.addMessage(message);

	    //
	    // Trim the excess items from the result
	    //
	    int counter = 0;
	    Iterator<ItemType> iter = items.iterator();
	    for (int i=0; iter.hasNext(); i++) {
		iter.next();
		if (i >= maxItems) {
		    iter.remove();
		}
	    }
	}
    }

    /**
     * Get a list of items belonging to a Set.
     */
    private Collection<ItemType> getSetItems(Set s, RequestContext rc, FlagData flags)
		throws NoSuchElementException, OvalException {

	//
	// First, retrieve the filtered list of items in the Set, recursively.
	//
	Collection<Collection<ItemType>> lists = new ArrayList<Collection<ItemType>>();
	if (s.isSetSet()) {
	    for (Set set : s.getSet()) {
		lists.add(getSetItems(set, rc, flags));
	    }
	} else {
	    for (String objectId : s.getObjectReference()) {
		Collection<ItemType> items = new ArrayList<ItemType>();
		try {
		    for (JAXBElement<? extends ItemType> elt : sc.getItemsByObjectId(objectId)) {
			items.add(elt.getValue());
		    }
		} catch (NoSuchElementException e) {
		    rc.pushObject(definitions.getObject(objectId).getValue());
		    items = scanObject(rc);
		    rc.popObject();
		}
		filterItems(items, s.getFilter(), rc);
		lists.add(items);
		for (MessageType m : sc.getObjectMessages(objectId)) {
		    MessageType message = Factories.common.createMessageType();
		    message.setLevel(m.getLevel());
		    message.setValue(JOVALMsg.getMessage(JOVALMsg.STATUS_OBJECT_MESSAGE, objectId, m.getValue()));
		    rc.addMessage(message);
		}
		flags.add(sc.getObjectFlag(objectId));
	    }
	}

	switch(s.getSetOperator()) {
	  case INTERSECTION: {
	    ItemSet<ItemType> intersection = null;
	    for (Collection<ItemType> items : lists) {
		if (intersection == null) {
		    intersection = new ItemSet<ItemType>(items);
		} else {
		    intersection = intersection.intersection(new ItemSet<ItemType>(items));
		}
	    }
	    return intersection == null ? new ArrayList<ItemType>() : intersection.toList();
	  }

	  case COMPLEMENT: {
	    switch(lists.size()) {
	      case 0:
		return new ArrayList<ItemType>();

	      case 1:
		return lists.iterator().next();

	      case 2:
		Iterator<Collection<ItemType>> iter = lists.iterator();
		Collection<ItemType> set1 = iter.next();
		Collection<ItemType> set2 = iter.next();
		return new ItemSet<ItemType>(set1).complement(new ItemSet<ItemType>(set2)).toList();

	      default:
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SET_COMPLEMENT, new Integer(lists.size())));
	    }
	  }

	  case UNION:
	  default: {
	    ItemSet<ItemType> union = new ItemSet<ItemType>();
	    for (Collection<ItemType> items : lists) {
		union = union.union(new ItemSet<ItemType>(items));
	    }
	    return union.toList();
	  }
	}
    }

    /**
     * Evaluate a DefinitionType.
     */
    private scap.oval.results.DefinitionType evaluateDefinition(DefinitionType definition) throws OvalException {
	if (abortEval) {
	    throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
	}
	String id = definition.getId();
	try {
	    return results.getDefinition(id);
	} catch (NoSuchElementException e) {
	}
	scap.oval.results.DefinitionType definitionResult = Factories.results.createDefinitionType();
	producer.sendNotify(Message.DEFINITION, id);
	definitionResult.setDefinitionId(id);
	definitionResult.setVersion(definition.getVersion());
	definitionResult.setClazz(definition.getClazz());
	try {
	    ResultEnumeration criteriaResult = ResultEnumeration.NOT_EVALUATED;
	    if (definition.isSetCriteria()) {
		scap.oval.results.CriteriaType criteria = evaluateCriteria(definition.getCriteria());
		definitionResult.setCriteria(criteria);
		criteriaResult = criteria.getResult();
	    }
	    definitionResult.setResult(criteriaResult);
	} catch (NoSuchElementException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    definitionResult.setResult(ResultEnumeration.ERROR);
	    MessageType message = Factories.common.createMessageType();
	    message.setLevel(MessageLevelEnumeration.ERROR);
	    message.setValue(e.getMessage());
	    definitionResult.getMessage().add(message);
	}
	results.storeDefinitionResult(definitionResult);
	return definitionResult;
    }

    private scap.oval.results.CriteriaType evaluateCriteria(CriteriaType criteriaDefinition)
		throws NoSuchElementException, OvalException {

	if (abortEval) {
	    throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
	}
	scap.oval.results.CriteriaType criteriaResult = Factories.results.createCriteriaType();
	criteriaResult.setOperator(criteriaDefinition.getOperator());
	criteriaResult.setNegate(criteriaDefinition.getNegate());
	OperatorData operator = new OperatorData(criteriaDefinition.getNegate());
	for (Object child : criteriaDefinition.getCriteriaOrCriterionOrExtendDefinition()) {
	    Object resultObject = null;
	    if (child instanceof CriteriaType) {
		CriteriaType ctDefinition = (CriteriaType)child;
		scap.oval.results.CriteriaType ctResult = evaluateCriteria(ctDefinition);
		operator.addResult(ctResult.getResult());
		resultObject = ctResult;
	    } else if (child instanceof CriterionType) {
		CriterionType ctDefinition = (CriterionType)child;
		scap.oval.results.CriterionType ctResult = evaluateCriterion(ctDefinition);
		operator.addResult(ctResult.getResult());
		resultObject = ctResult;
	    } else if (child instanceof ExtendDefinitionType) {
		ExtendDefinitionType edtDefinition = (ExtendDefinitionType)child;
		String defId = edtDefinition.getDefinitionRef();
		DefinitionType defDefinition = definitions.getDefinition(defId);
		scap.oval.results.DefinitionType defResult = evaluateDefinition(defDefinition);
		scap.oval.results.ExtendDefinitionType edtResult;
		edtResult = Factories.results.createExtendDefinitionType();
		edtResult.setDefinitionRef(defId);
		edtResult.setVersion(defDefinition.getVersion());
		edtResult.setNegate(edtDefinition.getNegate());
		OperatorData od = new OperatorData(edtDefinition.getNegate());
		od.addResult(defResult.getResult());
		edtResult.setResult(od.getResult(OperatorEnumeration.AND));
		if (edtDefinition.isSetApplicabilityCheck() && edtDefinition.getApplicabilityCheck()) {
		    switch(edtResult.getResult()) {
		      case FALSE:
			edtResult.setResult(ResultEnumeration.NOT_APPLICABLE);
			break;
		    }
		}
		operator.addResult(edtResult.getResult());
		resultObject = edtResult;
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_BAD_COMPONENT, child.getClass().getName()));
	    }
	    criteriaResult.getCriteriaOrCriterionOrExtendDefinition().add(resultObject);
	}

	ResultEnumeration result = operator.getResult(criteriaDefinition.getOperator());
	if (criteriaDefinition.isSetApplicabilityCheck() && criteriaDefinition.getApplicabilityCheck()) {
	    switch(result) {
	      case FALSE:
		result = ResultEnumeration.NOT_APPLICABLE;
		break;
	    }
	}
	criteriaResult.setResult(result);
	return criteriaResult;
    }

    private scap.oval.results.CriterionType evaluateCriterion(CriterionType criterionDefinition)
		throws NoSuchElementException, OvalException {

	if (abortEval) {
	    throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
	}
	String testId = criterionDefinition.getTestRef();
	TestType testResult = results.getTest(testId);
	if (testResult == null) {
	    scap.oval.definitions.core.TestType testDefinition = definitions.getTest(testId).getValue();
	    testResult = Factories.results.createTestType();
	    testResult.setTestId(testDefinition.getId());
	    testResult.setCheck(testDefinition.getCheck());
	    testResult.setCheckExistence(testDefinition.getCheckExistence());
	    testResult.setStateOperator(testDefinition.getStateOperator());

	    if (evalEnabled) {
		if (testDefinition instanceof UnknownTest) {
		    testResult.setResult(ResultEnumeration.UNKNOWN);
		} else {
		    evaluateTest(testResult);
		}
	    } else {
		testResult.setResult(ResultEnumeration.NOT_EVALUATED);
	    }

	    results.storeTestResult(testResult);
	}

	scap.oval.results.CriterionType criterionResult = Factories.results.createCriterionType();
	criterionResult.setTestRef(testId);
	criterionResult.setNegate(criterionDefinition.getNegate());
	OperatorData od = new OperatorData(criterionDefinition.getNegate());
	od.addResult(testResult.getResult());
	criterionResult.setResult(od.getResult(OperatorEnumeration.AND));
	if (criterionDefinition.isSetApplicabilityCheck() && criterionDefinition.getApplicabilityCheck()) {
	    switch(criterionResult.getResult()) {
	      case FALSE:
		criterionResult.setResult(ResultEnumeration.NOT_APPLICABLE);
		break;
	    }
	}
	return criterionResult;
    }

    private void evaluateTest(TestType testResult) throws NoSuchElementException, OvalException {
	if (abortEval) {
	    throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
	}
	String testId = testResult.getTestId();
	logger.debug(JOVALMsg.STATUS_TEST, testId);
	scap.oval.definitions.core.TestType testDefinition = definitions.getTest(testId).getValue();
	String objectId = getObjectRef(testDefinition);
	List<String> stateIds = getStateRef(testDefinition);

	//
	// Create all the structures we'll need to store information about the evaluation of the test.
	//
	RequestContext rc = new RequestContext(definitions.getObject(objectId).getValue());
	ExistenceData existence = new ExistenceData();
	CheckData check = new CheckData();
	switch(sc.getObjectFlag(objectId)) {
	  //
	  // If the object is flagged as incomplete, then potentially at least one item will not have been collected, and
	  // if so, it would not have been checked against the state, so we record these facts here.
	  //
	  case INCOMPLETE:
	    existence.addStatus(StatusEnumeration.NOT_COLLECTED);
	    check.addResult(ResultEnumeration.UNKNOWN);
	    // fall-thru

	  //
	  // Note: If the object is flagged as complete but there are no items, existenceResult will remain at its
	  // default value of DOES_NOT_EXIST (which is, of course, exactly what we want to happen).
	  //
	  case COMPLETE:
	    for (JAXBElement<? extends ItemType> elt : sc.getItemsByObjectId(objectId)) {
		ItemType item = elt.getValue();
		existence.addStatus(item.getStatus());

		TestedItemType testedItem = Factories.results.createTestedItemType();
		testedItem.setItemId(item.getId());
		testedItem.setResult(ResultEnumeration.NOT_EVALUATED);

		//
		// Note: items with a status of DOES_NOT_EXIST have no impact on the result.
		//
		switch(item.getStatus()) {
		  case EXISTS:
		    if (stateIds.size() > 0) {
			OperatorData result = new OperatorData(false);
			for (String stateId : stateIds) {
			    StateType state = definitions.getState(stateId).getValue();
			    try {
				result.addResult(compare(state, item, rc));
			    } catch (TestException e) {
				logger.warn(JOVALMsg.ERROR_TESTEXCEPTION, testId, e.getMessage());
				logger.debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);

				MessageType message = Factories.common.createMessageType();
				message.setLevel(MessageLevelEnumeration.ERROR);
				message.setValue(e.getMessage());
				testedItem.getMessage().add(message);
				result.addResult(ResultEnumeration.ERROR);
			    }
			}
			testedItem.setResult(result.getResult(testDefinition.getStateOperator()));
			check.addResult(testedItem.getResult());
		    }
		    break;

		  case ERROR:
		    check.addResult(ResultEnumeration.ERROR);
		    break;

		  //
		  // This should not be possible.
		  //
		  case NOT_COLLECTED:
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_OVAL_STATES, objectId, "COMPLETE", "NOT_COLLECTED");
		    throw new OvalException(msg);
		}

		testResult.getTestedItem().add(testedItem);
	    }
	    break;

	  case DOES_NOT_EXIST:
	    existence.addStatus(StatusEnumeration.DOES_NOT_EXIST);
	    break;
	  case ERROR:
	    existence.addStatus(StatusEnumeration.ERROR);
	    break;
	  case NOT_APPLICABLE:
	    // No impact on existence check
	    break;
	  case NOT_COLLECTED:
	    existence.addStatus(StatusEnumeration.NOT_COLLECTED);
	    check.addResult(ResultEnumeration.UNKNOWN);
	    break;
	}

	//
	// Add all the tested variables that were resolved for the object and state (stored in the RequestContext).
	//
	for (VariableValueType var : rc.getVars()) {
	    TestedVariableType testedVariable = Factories.results.createTestedVariableType();
	    testedVariable.setVariableId(var.getVariableId());
	    testedVariable.setValue(var.getValue());
	    testResult.getTestedVariable().add(testedVariable);
	}

	//
	// DAS: Note that the NONE_EXIST check is deprecated as of 5.3, and will be eliminated in 6.0.
	// Per D. Haynes, in this case, any state and/or check should be ignored.
	//
	if (testDefinition.getCheck() == CheckEnumeration.NONE_EXIST) {
	    logger.warn(JOVALMsg.STATUS_CHECK_NONE_EXIST, testDefinition.getCheckExistence(), testId);
	    testResult.setResult(existence.getResult(ExistenceEnumeration.NONE_EXIST));

	//
	// If the object is not applicable, then the result is NOT_APPLICABLE.
	//
	} else if (sc.getObjectFlag(objectId) == FlagEnumeration.NOT_APPLICABLE) {
	    testResult.setResult(ResultEnumeration.NOT_APPLICABLE);

	//
	// If there is no state (or nothing in the check counters -- meaning no items and no "virtual" items), then the
	// result of the test is simply the result of the existence check.
	//
	} else if (stateIds.size() == 0 || check.size() == 0) {
	    testResult.setResult(existence.getResult(testDefinition.getCheckExistence()));

	//
	// If there is at least one state, then check the existence check, then (if successful) the check.
	//
	} else {
	    ResultEnumeration existenceResult = existence.getResult(testDefinition.getCheckExistence());
	    switch(existenceResult) {
	      case TRUE:
		testResult.setResult(check.getResult(testDefinition.getCheck()));
		break;

	      default:
		testResult.setResult(existenceResult);
		break;
	    }
	}
    }

    /**
     * If getObject() were a method of TestType (instead of only some of its subclasses), this is what it would return.
     */
    private String getObjectRef(scap.oval.definitions.core.TestType test) throws OvalException {
	try {
	    Method getObject = test.getClass().getMethod("getObject");
	    ObjectRefType objectRef = (ObjectRefType)getObject.invoke(test);
	    if (objectRef != null) {
		String ref = objectRef.getObjectRef();
		if (ref != null) {
		    return objectRef.getObjectRef();
		}
	    }
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}

	throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_TEST_NOOBJREF, test.getId()));
    }

    /**
     * If getState() were a method of TestType (instead of only some of its subclasses), this is what it would return.
     */
    private List<String> getStateRef(scap.oval.definitions.core.TestType test) {
	List<String> refs = new ArrayList<String>();
	try {
	    Method getObject = test.getClass().getMethod("getState");
	    Object o = getObject.invoke(test);
	    if (o instanceof List && ((List)o).size() > 0) {
		for (Object stateRefObj : (List)o) {
		    refs.add(((StateRefType)stateRefObj).getStateRef());
		}
	    } else if (o instanceof StateRefType) {
		refs.add(((StateRefType)o).getStateRef());
	    }
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return refs;
    }

    /**
     * Determine whether or not the specified item matches the specified object.
     */
    private ResultEnumeration compare(ObjectType object, ItemType item, RequestContext rc) throws OvalException, TestException {
	if (!OBJECT_ITEM_MAP.get(object.getClass()).equals(item.getClass())) {
	    return ResultEnumeration.FALSE;
	}
	try {
	    OperatorData result = new OperatorData(false);
	    int facets = 0;
	    for (Method method : ReflectionTool.getMethods(object.getClass()).values()) {
		String methodName = method.getName();
		if (methodName.startsWith("get") && !OBJECT_METHOD_NAMES.contains(methodName)) {
		    facets++;
		    Object objectEntityObj = method.invoke(object);
		    if (objectEntityObj instanceof JAXBElement) {
			if (XSITools.isNil((JAXBElement)objectEntityObj)) {
			    Object itemEntityObj = ReflectionTool.getMethod(item.getClass(), methodName).invoke(item);
			    if (itemEntityObj == null) {
				result.addResult(ResultEnumeration.TRUE);
			    } else if (itemEntityObj instanceof JAXBElement) {
				if (XSITools.isNil((JAXBElement)itemEntityObj)) {
				    result.addResult(ResultEnumeration.TRUE);
				} else {
				    result.addResult(ResultEnumeration.FALSE);
				}
			    } else {
				result.addResult(ResultEnumeration.FALSE);
			    }
			    continue; // move on
			} else {
			    objectEntityObj = ((JAXBElement)objectEntityObj).getValue(); // keep processing
			}
		    }
		    if (objectEntityObj == null) {
			// continue
		    } else if (objectEntityObj instanceof EntitySimpleBaseType) {
			EntitySimpleBaseType objectEntity = (EntitySimpleBaseType)objectEntityObj;
			Object itemEntityObj = ReflectionTool.getMethod(item.getClass(), methodName).invoke(item);
			if (itemEntityObj instanceof JAXBElement) {
			    itemEntityObj = ((JAXBElement)itemEntityObj).getValue();
			}
			if (itemEntityObj instanceof EntityItemSimpleBaseType || itemEntityObj == null) {
			    result.addResult(compare(objectEntity, (EntityItemSimpleBaseType)itemEntityObj, rc));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else if (objectEntityObj instanceof EntityObjectRecordType) {
			EntityObjectRecordType objectEntity = (EntityObjectRecordType)objectEntityObj;
			Object itemEntityObj = ReflectionTool.getMethod(item.getClass(), methodName).invoke(item);
			if (itemEntityObj instanceof JAXBElement) {
			    itemEntityObj = ((JAXBElement)itemEntityObj).getValue();
			}
			if (itemEntityObj instanceof EntityItemRecordType) {
			    result.addResult(compare(objectEntity, (EntityItemRecordType)itemEntityObj, rc));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else {
			String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
							     objectEntityObj.getClass().getName(), object.getId());
	    		throw new OvalException(message);
		    }
		}
	    }
	    if (facets > 0) {
		return result.getResult(OperatorEnumeration.AND);
	    } else {
		// Singleton object type
		return ResultEnumeration.TRUE;
	    }
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), object.getId()));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), object.getId()));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), object.getId()));
	}
    }

    /**
     * Determine whether or not the specified item matches the specified state.
     */
    private ResultEnumeration compare(StateType state, ItemType item, RequestContext rc) throws OvalException, TestException {
	try {
	    OperatorData result = new OperatorData(false);
	    for (Method method : ReflectionTool.getMethods(state.getClass()).values()) {
		String methodName = method.getName();
		if (methodName.startsWith("get") && !STATE_METHOD_NAMES.contains(methodName)) {
		    Object stateEntityObj = method.invoke(state);
		    if (stateEntityObj == null) {
			// continue
		    } else if (stateEntityObj instanceof EntityStateSimpleBaseType) {
			EntityStateSimpleBaseType stateEntity = (EntityStateSimpleBaseType)stateEntityObj;
			Object itemEntityObj = ReflectionTool.getMethod(item.getClass(), methodName).invoke(item);
			if (itemEntityObj instanceof JAXBElement) {
			    itemEntityObj = ((JAXBElement)itemEntityObj).getValue();
			}
			if (itemEntityObj instanceof EntityItemSimpleBaseType || itemEntityObj == null) {
			    result.addResult(compare(stateEntity, (EntityItemSimpleBaseType)itemEntityObj, rc));
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    Collection entityObjs = (Collection)itemEntityObj;
			    if (entityObjs.size() == 0) {
				cd.addResult(ResultEnumeration.FALSE);
			    } else {
				for (Object entityObj : entityObjs) {
				    EntityItemSimpleBaseType itemEntity = (EntityItemSimpleBaseType)entityObj;
				    cd.addResult(compare(stateEntity, itemEntity, rc));
				}
			    }
			    result.addResult(cd.getResult(stateEntity.getEntityCheck()));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else if (stateEntityObj instanceof EntityStateRecordType) {
			EntityStateRecordType stateEntity = (EntityStateRecordType)stateEntityObj;
			Object itemEntityObj = ReflectionTool.getMethod(item.getClass(), methodName).invoke(item);
			if (itemEntityObj instanceof JAXBElement) {
			    itemEntityObj = ((JAXBElement)itemEntityObj).getValue();
			}
			if (itemEntityObj instanceof EntityItemRecordType) {
			    result.addResult(compare(stateEntity, (EntityItemRecordType)itemEntityObj, rc));
			} else if (itemEntityObj instanceof Collection) {
			    CheckData cd = new CheckData();
			    Collection entityObjs = (Collection)itemEntityObj;
			    if (entityObjs.size() == 0) {
				cd.addResult(ResultEnumeration.FALSE);
			    } else {
				for (Object entityObj : entityObjs) {
				    if (entityObj instanceof EntityItemRecordType) {
					cd.addResult(compare(stateEntity, (EntityItemRecordType)entityObj, rc));
				    } else {
					String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
									 entityObj.getClass().getName(), item.getId());
					throw new OvalException(msg);
				    }
				}
			    }
			    result.addResult(cd.getResult(stateEntity.getEntityCheck()));
			} else {
			    String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
								 itemEntityObj.getClass().getName(), item.getId());
	    		    throw new OvalException(message);
			}
		    } else {
			String message = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_ENTITY,
							     stateEntityObj.getClass().getName(), state.getId());
	    		throw new OvalException(message);
		    }
		}
	    }
	    return result.getResult(state.getOperator());
	} catch (NoSuchMethodException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (IllegalAccessException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	} catch (InvocationTargetException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), state.getId()));
	}
    }

    /**
     * Compare an object record and item record.  All the object record's fields must match for a TRUE result (i.e.,
     * the item may have additional fields).
     */
    private ResultEnumeration compare(EntityObjectRecordType objectRecord, EntityItemRecordType itemRecord, RequestContext rc)
	    throws TestException, OvalException {

	Map<String, EntityObjectFieldType> objectFields = new HashMap<String, EntityObjectFieldType>();
	for (EntityObjectFieldType objectField : objectRecord.getField()) {
	    objectFields.put(objectField.getName(), objectField);
	}
	Map<String, Collection<EntityItemFieldType>> itemFields = new HashMap<String, Collection<EntityItemFieldType>>();
	for (EntityItemFieldType itemField : itemRecord.getField()) {
	    String name = itemField.getName();
	    if (!itemFields.containsKey(name)) {
		itemFields.put(name, new ArrayList<EntityItemFieldType>());
	    }
	    itemFields.get(name).add(itemField);
	}
	OperatorData od = new OperatorData(false);
	for (Map.Entry<String, EntityObjectFieldType> entry : objectFields.entrySet()) {
	    String name = entry.getKey();
	    EntityObjectFieldType objectField = entry.getValue();
	    if (itemFields.containsKey(name)) {
		EntitySimpleBaseType object = new ObjectFieldBridge(objectField);
		CheckData cd = new CheckData();
		for (EntityItemFieldType itemField : itemFields.get(name)) {
		    cd.addResult(compare(object, new ItemFieldBridge(itemField), rc));
		}
		od.addResult(cd.getResult(objectField.getEntityCheck()));
	    } else {
		od.addResult(ResultEnumeration.FALSE);
	    }
	}
	return od.getResult(OperatorEnumeration.AND);
    }

    /**
     * Compare a state record and item record.  All the state record's fields must match for a TRUE result (i.e.,
     * the item may have additional fields).
     */
    private ResultEnumeration compare(EntityStateRecordType stateRecord, EntityItemRecordType itemRecord, RequestContext rc)
	    throws TestException, OvalException {

	Map<String, EntityStateFieldType> stateFields = new HashMap<String, EntityStateFieldType>();
	for (EntityStateFieldType stateField : stateRecord.getField()) {
	    stateFields.put(stateField.getName(), stateField);
	}
	Map<String, Collection<EntityItemFieldType>> itemFields = new HashMap<String, Collection<EntityItemFieldType>>();
	for (EntityItemFieldType itemField : itemRecord.getField()) {
	    String name = itemField.getName();
	    if (!itemFields.containsKey(name)) {
		itemFields.put(name, new ArrayList<EntityItemFieldType>());
	    }
	    itemFields.get(name).add(itemField);
	}
	OperatorData od = new OperatorData(false);
	for (Map.Entry<String, EntityStateFieldType> entry : stateFields.entrySet()) {
	    String name = entry.getKey();
	    EntityStateFieldType stateField = entry.getValue();
	    if (itemFields.containsKey(name)) {
		EntityStateSimpleBaseType state = new StateFieldBridge(stateField);
		CheckData cd = new CheckData();
		for (EntityItemFieldType itemField : itemFields.get(name)) {
		    cd.addResult(compare(state, new ItemFieldBridge(itemField), rc));
		}
		od.addResult(cd.getResult(stateField.getEntityCheck()));
	    } else {
		od.addResult(ResultEnumeration.FALSE);
	    }
	}
	return od.getResult(OperatorEnumeration.AND);
    }

    /**
     * Compare a state or object SimpleBaseType to an item SimpleBaseType.  If the item is null, this method returns false.
     */
    private ResultEnumeration compare(EntitySimpleBaseType base, EntityItemSimpleBaseType item, RequestContext rc)
		throws TestException, OvalException {

	if (item == null) {
	    // Absence of the item translates to UNKNOWN per D. Haynes
	    return ResultEnumeration.UNKNOWN;
	} else {
	    switch(item.getStatus()) {
	      case NOT_COLLECTED:
		return ResultEnumeration.NOT_EVALUATED;

	      case ERROR:
		return ResultEnumeration.ERROR;

	      case DOES_NOT_EXIST:
		return ResultEnumeration.FALSE;
	    }
	}

	//
	// Handle the variable_ref case
	//
	if (base.isSetVarRef()) {
	    CheckData cd = new CheckData();
	    EntitySimpleBaseType varInstance = Factories.definitions.core.createEntityObjectAnySimpleType();
	    varInstance.setDatatype(base.getDatatype());
	    varInstance.setOperation(base.getOperation());
	    varInstance.setMask(base.getMask());
	    String ref = base.getVarRef();
	    try {
		Collection<IType> values = resolveVariable(ref, rc);
		if (values.size() == 0) {
		    //
		    // According to the specification, the test must result in an error condition in this case.  See:
		    // http://oval.mitre.org/language/version5.10.1/ovaldefinition/documentation/oval-definitions-schema.html#EntityAttributeGroup
		    //
		    String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLE_NO_VALUES);
		    throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, ref, reason));
		} else {
		    for (IType value : values) {
			value = value.cast(TypeFactory.getSimpleDatatype(base.getDatatype()));
			varInstance.setValue(value.getString());
			cd.addResult(testImpl(varInstance, item));
		    }
		}
	    } catch (TypeConversionException e) {
		String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, e.getMessage());
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, ref, reason));
	    } catch (NoSuchElementException e) {
		String reason = JOVALMsg.getMessage(JOVALMsg.ERROR_VARIABLE_MISSING);
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, ref, reason));
	    } catch (ResolveException e) {
		throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_VAR, ref, e.getMessage()));
	    }
	    return cd.getResult(base.getVarCheck());
	} else {
	    return testImpl(base, item);
	}
    }

    private class AbortException extends RuntimeException {
       AbortException(String message) {
           super(message);
       }
    }

    /**
     * Perform the the OVAL test by comparing the state/object (AKA base) and item.
     */
    private ResultEnumeration testImpl(EntitySimpleBaseType base, EntityItemSimpleBaseType item) throws TestException {
	if (abortEval) {
	    throw new AbortException(JOVALMsg.getMessage(JOVALMsg.ERROR_ENGINE_EVAL_CANCELLED));
	}

	if (!item.isSetValue() || !base.isSetValue()) {
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_TEST_INCOMPARABLE, item.getValue(), base.getValue());
	    throw new TestException(msg);
	}

	//
	// Let the base dictate the datatype
	//
	IType baseValue=null, itemValue=null;
	try {
	    baseValue = TypeFactory.createType(base);
	    itemValue = TypeFactory.createType(item).cast(baseValue.getType());
	} catch (IllegalArgumentException e) {
	    throw new TestException(e);
	} catch (TypeConversionException e) {
	    throw new TestException(e);
	}

	//
	// Validate the operation by datatype, then execute it. See section 5.3.6.3.1 of the specification:
	// http://oval.mitre.org/language/version5.10.1/OVAL_Language_Specification_01-20-2012.pdf
	//
	// DAS: Does it make sense to migrate comparisons to the type implementation classes, for improved modularity?
	//
	OperationEnumeration op = base.getOperation();
	switch(baseValue.getType()) {
	  case BINARY:
	  case BOOLEAN:
	  case RECORD:
	    return trivialComparison(baseValue, itemValue, op);

	  case EVR_STRING:
	  case FLOAT:
	  case FILESET_REVISION:
	  case IOS_VERSION:
	  case VERSION:
	    return basicComparison(baseValue, itemValue, op);

	  case INT: {
	    int sInt = ((IntType)baseValue).getData().intValue();
	    int iInt = ((IntType)itemValue).getData().intValue();
	    switch(op) {
	      case BITWISE_AND:
		if (sInt == (iInt & sInt)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      case BITWISE_OR:
		if (sInt == (iInt | sInt)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      default:
		return basicComparison(baseValue, itemValue, op);
	    }
	  }

	  case IPV_4_ADDRESS: {
	    Ip4AddressType sIp = (Ip4AddressType)baseValue;
	    Ip4AddressType iIp = (Ip4AddressType)itemValue;
	    switch(op) {
	      case SUBSET_OF:
		if (iIp.contains(sIp)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      case SUPERSET_OF:
		if (sIp.contains(iIp)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      default:
		return basicComparison(baseValue, itemValue, op);
	    }
	  }

	  case IPV_6_ADDRESS: {
	    Ip6AddressType sIp = (Ip6AddressType)baseValue;
	    Ip6AddressType iIp = (Ip6AddressType)itemValue;
	    switch(op) {
	      case SUBSET_OF:
		if (iIp.contains(sIp)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      case SUPERSET_OF:
		if (iIp.contains(sIp)) {
		    return ResultEnumeration.FALSE;
		} else {
		    return ResultEnumeration.TRUE;
		}
	      default:
		return basicComparison(baseValue, itemValue, op);
	    }
	  }

	  case STRING: {
	    String sStr = ((StringType)baseValue).getData();
	    String iStr = ((StringType)itemValue).getData();
	    switch(op) {
	      case CASE_INSENSITIVE_EQUALS:
		if (iStr.equalsIgnoreCase(sStr)) {
		    return ResultEnumeration.TRUE;
		} else {
		    return ResultEnumeration.FALSE;
		}
	      case CASE_INSENSITIVE_NOT_EQUAL:
		if (iStr.equalsIgnoreCase(sStr)) {
		    return ResultEnumeration.FALSE;
		} else {
		    return ResultEnumeration.TRUE;
		}
	      case PATTERN_MATCH:
		try {
		    if (StringTools.pattern(sStr).matcher(iStr).find()) {
			return ResultEnumeration.TRUE;
		    } else {
			return ResultEnumeration.FALSE;
		    }
		} catch (PatternSyntaxException e) {
		    throw new TestException(e);
		}
	      default:
		return trivialComparison(baseValue, itemValue, op);
	    }
	  }
	}
	throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
    }

    /**
     * =, !=, or throws a TestException
     */
    private ResultEnumeration trivialComparison(IType base, IType item, OperationEnumeration op) throws TestException {
	switch(op) {
	  case EQUALS:
	    if (item.equals(base)) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case NOT_EQUAL:
	    if (item.equals(base)) {
		return ResultEnumeration.FALSE;
	    } else {
		return ResultEnumeration.TRUE;
	    }
	}
	throw new TestException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op));
    }

    /**
     * =, !=, <, <=, >, >=, or throws a TestException
     */
    private ResultEnumeration basicComparison(IType base, IType item, OperationEnumeration op) throws TestException {
	switch(op) {
	  case GREATER_THAN:
	    if (item.compareTo(base) > 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case GREATER_THAN_OR_EQUAL:
	    if (item.compareTo(base) >= 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case LESS_THAN:
	    if (item.compareTo(base) < 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  case LESS_THAN_OR_EQUAL:
	    if (item.compareTo(base) <= 0) {
		return ResultEnumeration.TRUE;
	    } else {
		return ResultEnumeration.FALSE;
	    }
	  default:
	    return trivialComparison(base, item, op);
	}
    }

    /**
     * Return the value of the Variable with the specified ID, and also add any chained variables to the provided list.
     */
    private Collection<IType> resolveVariable(String variableId, RequestContext rc)
		throws NoSuchElementException, ResolveException, OvalException {

	Collection<IType> result = variableMap.get(variableId);
	if (result == null) {
	    logger.trace(JOVALMsg.STATUS_VARIABLE_CREATE, variableId);
	    try {
		result = resolveComponent(definitions.getVariable(variableId), rc);
	    } catch (IllegalArgumentException e) {
		throw new ResolveException(e);
	    } catch (UnsupportedOperationException e) {
		throw new ResolveException(e);
	    }
	    variableMap.put(variableId, result);
	} else {
	    for (IType value : result) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(variableId);
		variableValueType.setValue(value.getString());
		rc.addVar(variableValueType);
	    }
	    logger.trace(JOVALMsg.STATUS_VARIABLE_RECYCLE, variableId);
	}
	return result;
    }

    /**
     * Recursively resolve a component. Since there is no base class for component types, this method accepts an Object.
     *
     * @see http://oval.mitre.org/language/version5.10/ovaldefinition/documentation/oval-definitions-schema.html#FunctionGroup
     */
    private Collection<IType> resolveComponent(Object object, RequestContext rc) throws NoSuchElementException,
		UnsupportedOperationException, IllegalArgumentException, ResolveException, OvalException {
	//
	// Resolve a local variable
	//
	if (object instanceof LocalVariable) {
	    LocalVariable localVariable = (LocalVariable)object;
	    Collection<IType> values = resolveComponent(getComponent(localVariable, false), rc);
	    if (values.size() == 0) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(localVariable.getId());
		rc.addVar(variableValueType);
	    } else {
		Collection<IType> convertedValues = new ArrayList<IType>();
		for (IType value : values) {
		    try {
			//
			// Convert values from the originating type to the variable's defined datatype
			//
			convertedValues.add(value.cast(localVariable.getDatatype()));

			VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
			variableValueType.setVariableId(localVariable.getId());
			variableValueType.setValue(value.getString());
			rc.addVar(variableValueType);
		    } catch (TypeConversionException e) {
			MessageType message = Factories.common.createMessageType();
			message.setLevel(MessageLevelEnumeration.ERROR);
			message.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, e.getMessage()));
			rc.addMessage(message);
		    }
		}
		values = convertedValues;
	    }
	    return values;

	//
	// Add an externally-defined variable.
	//
	} else if (object instanceof ExternalVariable) {
	    ExternalVariable externalVariable = (ExternalVariable)object;
	    String id = externalVariable.getId();
	    if (externalVariables == null) {
		throw new ResolveException(JOVALMsg.getMessage(JOVALMsg.ERROR_EXTERNAL_VARIABLE_SOURCE, id));
	    } else {
		Collection<IType> values = new ArrayList<IType>();
		try {
		    for (IType value : externalVariables.getValue(id)) {
			try {
			    values.add(value.cast(externalVariable.getDatatype()));
			} catch (TypeConversionException e) {
			    MessageType message = Factories.common.createMessageType();
			    message.setLevel(MessageLevelEnumeration.ERROR);
			    message.setValue(JOVALMsg.getMessage(JOVALMsg.ERROR_TYPE_CONVERSION, e.getMessage()));
			    rc.addMessage(message);
			}
		    }
		} catch (NoSuchElementException e) {
		    throw new ResolveException(JOVALMsg.getMessage(JOVALMsg.ERROR_EXTERNAL_VARIABLE, e.getMessage()));
		}
		if (values.size() == 0) {
		    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(externalVariable.getId());
		    rc.addVar(variableValueType);
		} else {
		    for (IType value : values) {
			VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
			variableValueType.setVariableId(externalVariable.getId());
			variableValueType.setValue(value.getString());
			rc.addVar(variableValueType);
		    }
		}
		return values;
	    }

	//
	// Resolve a constant variable.
	//
	} else if (object instanceof ConstantVariable) {
	    ConstantVariable constantVariable = (ConstantVariable)object;
	    String id = constantVariable.getId();
	    Collection<IType> values = new ArrayList<IType>();
	    List<ValueType> valueTypes = constantVariable.getValue();
	    if (valueTypes.size() == 0) {
		VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		variableValueType.setVariableId(constantVariable.getId());
		rc.addVar(variableValueType);
	    } else {
		for (ValueType value : valueTypes) {
		    VariableValueType variableValueType = Factories.sc.core.createVariableValueType();
		    variableValueType.setVariableId(id);
		    String s = (String)value.getValue();
		    variableValueType.setValue(s);
		    rc.addVar(variableValueType);
		    values.add(TypeFactory.createType(constantVariable.getDatatype(), s));
		}
	    }
	    return values;

	//
	// Return a static (literal) value.
	//
	} else if (object instanceof LiteralComponentType) {
	    LiteralComponentType literal = (LiteralComponentType)object;
	    Collection<IType> values = new ArrayList<IType>();
	    values.add(TypeFactory.createType(literal.getDatatype(), (String)literal.getValue()));
	    return values;

	//
	// Resolve from an ItemType (which possibly has to be fetched from an adapter)
	//
	} else if (object instanceof ObjectComponentType) {
	    ObjectComponentType oc = (ObjectComponentType)object;
	    String objectId = oc.getObjectRef();
	    Collection<ItemType> items = new ArrayList<ItemType>();
	    try {
		//
		// First, we scan the SystemCharacteristics for items related to the object.
		//
		for (JAXBElement<? extends ItemType> elt : sc.getItemsByObjectId(objectId)) {
		    items.add(elt.getValue());
		}
	    } catch (NoSuchElementException e) {
		//
		// If the object has not yet been scanned, then it must be retrieved live from the adapter
		//
		rc.pushObject(definitions.getObject(objectId).getValue());
		items = scanObject(rc);
		rc.popObject();
	    }
	    List<IType> result = extractItemData(objectId, oc, items);
	    if (result.size() == 0) {
		//
		// DAS: Per the schema documentation, object components are required to resolve to one or more
		//      values, or else generate an error.
		//
		String msg = null;
		String itemField = oc.getItemField();
		if (oc.isSetRecordField()) {
		    String recordField = oc.getRecordField();
		    msg = JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_OBJECT_COMPONENT_RECORD, objectId, itemField, recordField);
		} else {
		    msg = JOVALMsg.getMessage(JOVALMsg.STATUS_EMPTY_OBJECT_COMPONENT, objectId, itemField);
		}
		throw new ResolveException(msg);
	    } else {
		return result;
	    }

	//
	// Resolve a wrapper pointing to another variable
	//
	} else if (object instanceof VariableComponentType) {
	    return resolveComponent(definitions.getVariable(((VariableComponentType)object).getVarRef()), rc);

	//
	// Check IFunctions
	//
	} else {
	    Class<?> clazz = object.getClass();
	    if (FUNCTIONS.containsKey(clazz)) {
		return FUNCTIONS.get(clazz).compute(clazz.cast(object), rc);
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_COMPONENT, clazz.getName()));
	    }
	}
    }

    /**
     * The final step in resolving an object reference variable's value is extracting the item field or record from the items
     * associated with that ObjectType, which is the function of this method.
     */
    private List<IType> extractItemData(String objectId, ObjectComponentType oc, Collection list)
		throws OvalException, ResolveException, NoSuchElementException, IllegalArgumentException {

	List<IType> values = new ArrayList<IType>();
	for (Object o : list) {
	    if (o instanceof ItemType) {
		String fieldName = oc.getItemField();
		try {
		    ItemType item = (ItemType)o;
		    String methodName = getAccessorMethodName(fieldName);
		    Method method = item.getClass().getMethod(methodName);
		    o = method.invoke(item);
		} catch (NoSuchMethodException e) {
		    //
		    // The specification indicates that an object_component must have an error flag in this case.
		    //
		    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_RESOLVE_ITEM_FIELD, fieldName, o.getClass().getName());
		    throw new ResolveException(msg);
		} catch (IllegalAccessException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		} catch (InvocationTargetException e) {
		    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		    return null;
		}
	    }

	    if (o instanceof JAXBElement) {
		o = ((JAXBElement)o).getValue();
	    }
	    if (o == null) {
		// skip nulls
	    } else if (o instanceof EntityItemSimpleBaseType) {
		EntityItemSimpleBaseType base = (EntityItemSimpleBaseType)o;
		switch(base.getStatus()) {
		  case EXISTS:
		    SimpleDatatypeEnumeration type = TypeFactory.getSimpleDatatype(base.getDatatype());
		    values.add(TypeFactory.createType(type, (String)base.getValue()));
		    break;
		  default:
		    logger.debug(JOVALMsg.WARNING_ITEM_FIELD_STATUS, oc.getItemField(), base.getStatus(), objectId);
		    break;
		}
	    } else if (o instanceof List) {
		values.addAll(extractItemData(objectId, oc, (List)o));
	    } else if (o instanceof EntityItemRecordType) {
		EntityItemRecordType record = (EntityItemRecordType)o;
		if (oc.isSetRecordField()) {
		    String fieldName = oc.getRecordField();
		    for (EntityItemFieldType field : record.getField()) {
			if (fieldName.equals(field.getName())) {
			    switch(field.getStatus()) {
			      case EXISTS:
		    		SimpleDatatypeEnumeration type = TypeFactory.getSimpleDatatype(field.getDatatype());
				values.add(TypeFactory.createType(type, (String)field.getValue()));
				break;
			      default:
				logger.debug(JOVALMsg.WARNING_RECORD_FIELD_STATUS,field.getName(),field.getStatus(),objectId);
				break;
			    }
			}
		    }
		} else {
		    values.add(new RecordType(record));
		}
	    } else {
		throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, o.getClass().getName(), objectId));
	    }
	}
	return values;
    }

    /**
     * Use reflection to get the child component of a function type.  Since there is no base class for all the OVAL function
     * types, this method accepts any Object.
     */
    private Object getComponent(Object unknown, boolean allowList) throws OvalException {
	// Components

	Object obj = null;
	if (allowList) {
	    obj = ReflectionTool.invokeMethod(unknown, "getObjectComponentOrVariableComponentOrLiteralComponent");
	    if (obj != null) {
		return obj;
	    }
	}
	obj = ReflectionTool.invokeMethod(unknown, "getObjectComponent");
	if (obj != null) {
	    return obj;
	}
	obj = ReflectionTool.invokeMethod(unknown, "getVariableComponent");
	if (obj != null) {
	    return obj;
	}
	obj = ReflectionTool.invokeMethod(unknown, "getLiteralComponent");
	if (obj != null) {
	    return obj;
	}

	// Functions

	for (Class<?> clazz : FUNCTIONS.keySet()) {
	    String className = clazz.getName();
	    if (className.endsWith("FunctionType")) {
		int ptr = className.lastIndexOf(".");
		String baseName = className.substring(ptr+1, className.length() - 12);
		obj = ReflectionTool.invokeMethod(unknown, new StringBuffer("get").append(baseName).toString());
		if (obj != null) {
		    return obj;
		}
	    }
	}

	throw new OvalException(JOVALMsg.getMessage(JOVALMsg.ERROR_SUBCOMPONENT, unknown.getClass().getName()));
    }

    private static java.util.Set<String> OBJECT_METHOD_NAMES;
    static {
	OBJECT_METHOD_NAMES = getNames(ReflectionTool.getMethods(ObjectType.class).values());
	OBJECT_METHOD_NAMES.add("getBehaviors");
	OBJECT_METHOD_NAMES.add("getFilter");
	OBJECT_METHOD_NAMES.add("getSet");
    }
    private static java.util.Set<String> STATE_METHOD_NAMES = getNames(ReflectionTool.getMethods(StateType.class).values());

    /**
     * List the unique names of all the no-argument methods. This is not necessarily a fast method.
     */
    private static java.util.Set<String> getNames(Collection<Method> methods) {
	java.util.Set<String> names = new HashSet<String>();
	for (Method m : methods) {
	    names.add(m.getName());
	}
	return names;
    }

    /**
     * Given the name of an XML node, guess the name of the accessor field that JAXB would generate.
     * For example, field_name -> getFieldName.
     */
    private String getAccessorMethodName(String fieldName) {
	StringTokenizer tok = new StringTokenizer(fieldName, "_");
	StringBuffer sb = new StringBuffer("get");
	while(tok.hasMoreTokens()) {
	    byte[] ba = tok.nextToken().toLowerCase().getBytes(StringTools.ASCII);
	    if (97 <= ba[0] && ba[0] <= 122) {
		ba[0] -= 32; // Capitalize the first letter.
	    }
	    sb.append(new String(ba, StringTools.ASCII));
	}
	return sb.toString();
    }

    /**
     * An EntityObjectSimpleBaseType wrapper for an EntityStateFieldType.
     */
    private class ObjectFieldBridge extends EntitySimpleBaseType {
	ObjectFieldBridge(EntityObjectFieldType field) {
	    datatype = field.getDatatype();
	    mask = field.getMask();
	    operation = field.getOperation();
	    value = field.getValue();
	    varCheck = field.getVarCheck();
	    varRef = field.getVarRef();
	}
    }

    /**
     * An EntityStateSimpleBaseType wrapper for an EntityStateFieldType.
     */
    private class StateFieldBridge extends EntityStateSimpleBaseType {
	StateFieldBridge(EntityStateFieldType field) {
	    datatype = field.getDatatype();
	    mask = field.getMask();
	    operation = field.getOperation();
	    value = field.getValue();
	    varCheck = field.getVarCheck();
	    varRef = field.getVarRef();
	    entityCheck = field.getEntityCheck();
	}
    }

    /**
     * An EntityItemSimpleBaseType wrapper for an EntityItemFieldType.
     */
    private class ItemFieldBridge extends EntityItemSimpleBaseType {
	ItemFieldBridge(EntityItemFieldType field) {
	    datatype = field.getDatatype();
	    mask = field.getMask();
	    value = field.getValue();
	}
    }

    /**
     * Canonicalize an ObjectType by making a copy, removing the ID, marshalling it, and returning the bytes.
     */
    private byte[] toCanonicalBytes(ObjectType objectType) {
	JAXBElement<? extends ObjectType> object = wrapObject(objectType);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    marshaller.marshal(object, out);
	    Object copy = unmarshaller.unmarshal(new ByteArrayInputStream(out.toByteArray()));
	    ((ObjectType)((JAXBElement)copy).getValue()).setId(null);
	    marshaller.marshal(copy, out);
	} catch (JAXBException e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	}
	return out.toByteArray();
    }

    private Map<Class, Object> wrapperFactories = new HashMap<Class, Object>();
    private Map<Class, Method> wrapperMethods = new HashMap<Class, Method>();

    private JAXBElement<? extends ObjectType> wrapObject(ObjectType object) {
	try {
	    Class clazz = object.getClass();
	    Method method = wrapperMethods.get(clazz);
	    Object factory = wrapperFactories.get(clazz);
	    if (method == null || factory == null) {
		String packageName = clazz.getPackage().getName();
		String unqualClassName = clazz.getName().substring(packageName.length()+1);
		Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
		factory = factoryClass.newInstance();
		wrapperFactories.put(clazz, factory);
		method = factoryClass.getMethod("create" + unqualClassName, object.getClass());
		wrapperMethods.put(clazz, method);
	    }
	    @SuppressWarnings("unchecked")
	    JAXBElement<ObjectType> wrapped = (JAXBElement<ObjectType>)method.invoke(factory, object);
	    return wrapped;
	} catch (Exception e) {
	    logger.warn(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    throw new RuntimeException(JOVALMsg.getMessage(JOVALMsg.ERROR_REFLECTION, e.getMessage(), object.getId()));
	}
    }
}
