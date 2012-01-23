// Copyright (C) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the AGPL 3.0 license available at http://www.joval.org/agpl_v3.txt

package org.joval.ssh.system;

import java.io.IOException;
import java.util.List;

import org.slf4j.cal10n.LocLogger;

import org.vngx.jsch.JSch;
import org.vngx.jsch.ChannelExec;
import org.vngx.jsch.ChannelType;
import org.vngx.jsch.Session;
import org.vngx.jsch.UIKeyboardInteractive;
import org.vngx.jsch.UserInfo;
import org.vngx.jsch.config.SSHConfigConstants;
import org.vngx.jsch.config.SessionConfig;
import org.vngx.jsch.exception.JSchException;
import org.vngx.jsch.userauth.Identity;
import org.vngx.jsch.userauth.IdentityManager;
import org.vngx.jsch.userauth.IdentityFile;
import org.vngx.jsch.util.SocketFactory;

import org.joval.identity.Credential;
import org.joval.intf.identity.ICredential;
import org.joval.intf.identity.ILocked;
import org.joval.intf.identity.ISshCredential;
import org.joval.intf.io.IFilesystem;
import org.joval.intf.system.IBaseSession;
import org.joval.intf.system.IEnvironment;
import org.joval.intf.system.IProcess;
import org.joval.intf.unix.system.IUnixSession;
import org.joval.util.JOVALMsg;
import org.joval.util.JOVALSystem;
import org.joval.util.JSchLogger;
import org.joval.util.SafeCLI;

/**
 * A representation of an SSH session, which simply uses JSch to implement an IBaseSession.
 *
 * @author David A. Solin
 * @version %I% %G%
 */
public class SshSession implements IBaseSession, ILocked, UserInfo, UIKeyboardInteractive {
    protected LocLogger logger;
    protected String hostname;
    protected ICredential cred;
    protected SshSession gateway;
    protected Session session;
    private boolean connected = false, debug = false;

    private static int connTimeout = JOVALSystem.getIntProperty(JOVALSystem.PROP_SSH_CONNECTION_TIMEOUT);
    private static int connRetries = JOVALSystem.getIntProperty(JOVALSystem.PROP_SSH_CONNECTION_RETRIES);
    static {
	if (JOVALSystem.getBooleanProperty(JOVALSystem.PROP_SSH_ATTACH_LOG)) {
	    JSch.setLogger(new JSchLogger(JOVALSystem.getLogger()));
	}
    }

    public SshSession(String hostname) {
	this(hostname, null);
    }

    public SshSession(String hostname, SshSession gateway) {
	this.hostname = hostname;
	this.gateway = gateway;
	this.debug = JOVALSystem.getBooleanProperty(JOVALSystem.PROP_SSH_DEBUG);
	logger = JOVALSystem.getLogger();
    }

    public Session getJschSession() {
	return session;
    }

    // Implement ILocked

    public boolean unlock(ICredential cred) {
	if (cred == null) {
	    logger.warn(JOVALMsg.ERROR_SESSION_CREDENTIAL);
	    return false;
	}
	this.cred = cred;
	if (cred instanceof ISshCredential) {
	    ISshCredential sshc = (ISshCredential)cred;
	    if (sshc.getPrivateKey() != null) {
		try {
		    Identity id = IdentityFile.newInstance(sshc.getPrivateKey().getPath(), null);
		    if (sshc.getPassphrase() == null) {
			IdentityManager.getManager().addIdentity(id, null);
		    } else {
			IdentityManager.getManager().addIdentity(id, sshc.getPassphrase().getBytes());
 		    }
		} catch (JSchException e) {
		    logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		}
	    }
	}
	return true;
    }

    // Implement ILoggable

    public LocLogger getLogger() {
	return logger;
    }

    public void setLogger(LocLogger logger) {
	this.logger = logger;
    }

    // Implement IBaseSession

    public String getHostname() {
	return hostname;
    }

    public void setDebug(boolean debug) {
	this.debug = debug;
    }

    public boolean connect() {
	if (gateway != null && !gateway.connect()) {
	    return false;
	}

	if (cred == null) {
	    return false;
	} else if (session == null) {
	    try {
		SessionConfig config = new SessionConfig();
		config.setProperty(SSHConfigConstants.STRICT_HOST_KEY_CHECKING, "no");
		JSch jsch = JSch.getInstance();
		if (gateway == null) {
		    session = jsch.createSession(cred.getUsername(), hostname, 22, config);
		} else {
		    // use gateway port forwarding
		    int localPort = gateway.session.setPortForwardingL(0, hostname, 22);
		    session = jsch.createSession(cred.getUsername(), LOCALHOST, localPort, config);
		}
		session.setUserInfo(this);
	    } catch (JSchException e) {
		logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
		return false;
	    }
	}
	if (session.isConnected()) {
	    return true; // already connected
	} else {
	    if (connected) {
		logger.warn(JOVALMsg.ERROR_SSH_DROPPED_CONN, hostname);
	    }
	    Exception lastError = null;
	    for (int i=1; i <= connRetries; i++) {
		try {
		    session.setSocketFactory(SocketFactory.DEFAULT_SOCKET_FACTORY);
		    session.connect(connTimeout);
		    logger.info(JOVALMsg.STATUS_SSH_CONNECT, hostname);
		    connected = true;
		    return true;
		} catch (JSchException e) {
		    lastError = e;
		    logger.warn(JOVALMsg.ERROR_SSH_CONNECT, hostname, i, e.getMessage());
		}
	    }
	    if (lastError != null) {
		logger.error(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), lastError);
	    }
	    return false;
	}
    }

    public void disconnect() {
	if (session != null) {
	    synchronized(session) {
	        if (session != null && connected) {
		    logger.info(JOVALMsg.STATUS_SSH_DISCONNECT, hostname);
		    session.disconnect();
		    connected = false;
		}
	    }
	}
	if (gateway != null) {
	    gateway.disconnect();
	}
    }

    public IProcess createProcess(String command) throws Exception {
	if (connect()) {
	    ChannelExec ce = session.openChannel(ChannelType.EXEC);
	    return new SshProcess(ce, command, debug, logger);
	} else {
	    throw new RuntimeException(JOVALSystem.getMessage(JOVALMsg.ERROR_SSH_DISCONNECTED));
	}
    }

    public Type getType() {
	if (cred != null) {
	    try {
		for (String line : SafeCLI.multiLine("pwd", this, IUnixSession.TIMEOUT_S)) {
		    if (line.startsWith("flash")) {
			return Type.CISCO_IOS;
		    } else if (line.startsWith("/")) {
			return Type.UNIX;
		    } else if (line.equals("")) {
			// ignore;
		    } else {
			logger.warn(JOVALMsg.ERROR_SSH_UNEXPECTED_RESPONSE, line);
		    }
		}
	    } catch (Exception e) {
		logger.warn(JOVALSystem.getMessage(JOVALMsg.ERROR_EXCEPTION), e);
	    }
	}
	return Type.SSH;
    }

    // Implement UserInfo

    public String getPassphrase() {
	if (cred instanceof ISshCredential) {
	    return ((ISshCredential)cred).getPassphrase();
	} else {
	    return "";
	}
    }

    public String getPassword() {
	return cred.getPassword();
    }

    public boolean promptPassphrase(String message) {
	return true;
    }

    public boolean promptPassword(String message) {
	return true;
    }

    public boolean promptYesNo(String message) {
	return true;
    }

    public void showMessage(String message) {
	logger.debug(JOVALMsg.STATUS_AUTHMESSAGE, message);
    }

    // Implement UIKeyboardInteractive

    /**
     * Mac OS X allows private key and interactive logins only by default, so this interface must be implemented.
     */
    public String[] promptKeyboardInteractive(String dest, String name, String instruction, String[] prompt, boolean[] echo) {
	return null;
    }
}
