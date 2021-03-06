// Copyright (C) 2013 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.scap.oval.adapter.windows;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.xml.bind.JAXBElement;

import jsaf.intf.system.ISession;
import jsaf.intf.windows.powershell.IRunspace;
import jsaf.intf.windows.system.IWindowsSession;
import jsaf.util.Base64;
import jsaf.util.StringTools;

import scap.oval.common.MessageLevelEnumeration;
import scap.oval.common.MessageType;
import scap.oval.common.OperationEnumeration;
import scap.oval.common.SimpleDatatypeEnumeration;
import scap.oval.definitions.core.EntityObjectStringType;
import scap.oval.definitions.core.ObjectType;
import scap.oval.systemcharacteristics.core.EntityItemStringType;
import scap.oval.systemcharacteristics.core.FlagEnumeration;
import scap.oval.systemcharacteristics.core.ItemType;
import scap.oval.systemcharacteristics.core.StatusEnumeration;

import org.joval.intf.plugin.IAdapter;
import org.joval.scap.oval.CollectException;
import org.joval.scap.oval.Factories;
import org.joval.util.JOVALMsg;
import org.joval.xml.XSITools;

/**
 * Base class for Service-based IAdapters. Subclasses need only implement getItemClass and getItems methods.
 * The base class handles searches and caching of search results.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public abstract class BaseServiceAdapter<T extends ItemType> implements IAdapter {
    private HashSet<String> runspaceIds;
    private Collection<String> serviceNames;

    protected IWindowsSession session;

    // Implement IAdapter

    public Collection<Class> init(ISession session, Collection<Class> notapplicable) {
	Collection<Class> classes = new ArrayList<Class>();
	if (session instanceof IWindowsSession) {
	    this.session = (IWindowsSession)session;
	    runspaceIds = new HashSet<String>();
	    classes.add(getObjectClass());
	} else {
	    notapplicable.add(getObjectClass());
	}
	return classes;
    }

    public final Collection<T> getItems(ObjectType obj, IRequestContext rc) throws CollectException {
	if (serviceNames == null) {
	    serviceNames = new HashSet<String>();
	    try {
		String cmd = "Get-Service | %{$_.Name} | Transfer-Encode";
		String data = new String(Base64.decode(getRunspace().invoke(cmd)), StringTools.UTF8);
		for (String serviceName : data.split("\r\n")) {
		    serviceNames.add(serviceName);
		}
	    } catch (Exception e) {
		throw new CollectException(e, FlagEnumeration.ERROR);
	    }
	}

	Collection<T> items = new ArrayList<T>();
	ReflectedServiceObject sObj = new ReflectedServiceObject(obj);

	//
	// Find all the matching services 
	//
	OperationEnumeration op = sObj.getServiceName().getOperation();
	Collection<String> names = new ArrayList<String>();
	String name = (String)sObj.getServiceName().getValue();
	switch(op) {
	  case CASE_INSENSITIVE_EQUALS:
	  case EQUALS:
	    for (String serviceName : serviceNames) {
		if (name.equalsIgnoreCase(serviceName)) {
		    names.add(name);
		    break;
		}
	    }
	    break;

	  case CASE_INSENSITIVE_NOT_EQUAL:
	  case NOT_EQUAL:
	    for (String serviceName : serviceNames) {
		if (!name.equalsIgnoreCase(serviceName)) {
		    names.add(serviceName);
		}
	    }
	    break;

	  case PATTERN_MATCH:
	    Pattern p = StringTools.pattern(name);
	    for (String serviceName : serviceNames) {
		if (p.matcher(serviceName).find()) {
		    names.add(serviceName);
		}
	    }
	    break;

	  default:
	    String msg = JOVALMsg.getMessage(JOVALMsg.ERROR_UNSUPPORTED_OPERATION, op);
	    throw new CollectException(msg, FlagEnumeration.NOT_COLLECTED);
	}

	//
	// For each matching service name, get items.
	//
	for (String serviceName : names) {
	    try {
		//
		// Create the base ItemType for the path
		//
		ReflectedServiceItem sItem = new ReflectedServiceItem();
		EntityItemStringType serviceNameType = Factories.sc.core.createEntityItemStringType();
		serviceNameType.setValue(serviceName);
		sItem.setServiceName(serviceNameType);

		//
		// Add items retrieved by the subclass
		//
		items.addAll(getItems(obj, sItem.it, rc));
	    } catch (NoSuchElementException e) {
		// No match.
	    } catch (CollectException e) {
		throw e;
	    } catch (Exception e) {
		MessageType msg = Factories.common.createMessageType();
		msg.setLevel(MessageLevelEnumeration.ERROR);
		msg.setValue(e.getMessage());
		rc.addMessage(msg);
		session.getLogger().debug(JOVALMsg.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return items;
    }

    // Protected

    /**
     * Return the Class of the ObjectType generated by the subclass.
     */
    protected abstract Class getObjectClass();

    /**
     * Return the Class of the ItemType instances generated by the subclass.
     */
    protected abstract Class getItemClass();

    /**
     * Return a list of items to associate with the given ObjectType, based on the service specified by the base parameter.
     *
     * @arg base the base ItemType containing name information already populated
     *
     * @throws NoSuchElementException if no matching item is found
     * @throws CollectException collection cannot take place and should be halted
     */
    protected abstract Collection<T> getItems(ObjectType obj, ItemType base, IRequestContext rc) throws Exception;

    /**
     * Subclasses should override by supplying streams to any assemblies that must be loaded into requested runspaces using
     * the getRunspace method (such as those required by modules), below.
     */
    protected List<InputStream> getPowershellAssemblies() {
	return Collections.<InputStream>emptyList();
    }

    /**
     * Subclasses should override by supplying streams to any modules that must be loaded into requested runspaces using
     * the getRunspace method, below.
     */
    protected List<InputStream> getPowershellModules() {
	return Collections.<InputStream>emptyList();
    }

    /**
     * Get (or create) a runspace that includes the modules and assemblies specified by the subclass.
     */
    protected IRunspace getRunspace() throws Exception {
	IRunspace runspace = session.getRunspacePool().getRunspace();
	if (!runspaceIds.contains(runspace.getId())) {
	    for (InputStream in : getPowershellAssemblies()) {
		runspace.loadAssembly(in);
	    }
	    for (InputStream in : getPowershellModules()) {
		runspace.loadModule(in);
	    }
	    runspaceIds.add(runspace.getId());
	}
	return runspace;
    }

    // Private

    /**
     * A reflection proxy for:
     *     scap.oval.definitions.windows.ServiceObject
     *     scap.oval.definitions.windows.ServiceeffectiverightsObject
     */
    class ReflectedServiceObject {
	ObjectType obj;
	String id = null;
	EntityObjectStringType serviceName = null;

	ReflectedServiceObject(ObjectType obj) throws CollectException {
	    this.obj = obj;

	    try {
		Method getId = obj.getClass().getMethod("getId");
		Object o = getId.invoke(obj);
		if (o != null) {
		    id = (String)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }

	    try {
		Method getServiceName = obj.getClass().getMethod("getServiceName");
		Object o = getServiceName.invoke(obj);
		if (o != null) {
		    serviceName = (EntityObjectStringType)o;
		}
	    } catch (NoSuchMethodException e) {
	    } catch (IllegalAccessException e) {
	    } catch (IllegalArgumentException e) {
	    } catch (InvocationTargetException e) {
	    }
	}

	public ObjectType getObject() {
	    return obj;
	}

	public String getId() {
	    return id;
	}

	public boolean isSetServiceName() {
	    return serviceName != null;
	}

	public EntityObjectStringType getServiceName() {
	    return serviceName;
	}
    }

    /**
     * A reflection proxy for:
     *     scap.oval.systemcharacteristics.windows.ServiceItem
     *     scap.oval.systemcharacteristics.windows.ServiceeffectiverightsItem
     */
    class ReflectedServiceItem {
	ItemType it;
	Method setServiceName, setStatus;
	Object factory;

	ReflectedServiceItem() throws ClassNotFoundException, InstantiationException, NoSuchMethodException,
		IllegalAccessException, InvocationTargetException {

	    Class clazz = getItemClass();
	    String className = clazz.getName();
	    String packageName = clazz.getPackage().getName();
	    String unqualClassName = className.substring(packageName.length() + 1);
	    Class<?> factoryClass = Class.forName(packageName + ".ObjectFactory");
	    factory = factoryClass.newInstance();
	    Method createType = factoryClass.getMethod("create" + unqualClassName);
	    it = (ItemType)createType.invoke(factory);

	    Method[] methods = it.getClass().getMethods();
	    for (int i=0; i < methods.length; i++) {
		String name = methods[i].getName();
		if ("setServiceName".equals(name)) {
		    setServiceName = methods[i];
		} else if ("setStatus".equals(name)) {
		    setStatus = methods[i];
		}
	    }
	}

	void setServiceName(EntityItemStringType serviceName)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setServiceName != null) {
		setServiceName.invoke(it, serviceName);
	    }
	}

	void setStatus(StatusEnumeration status)
		throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
	    if (setStatus != null) {
		setStatus.invoke(it, status);
	    }
	}
    }
}
