package com.idega.slide.business;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.slide.authenticate.CredentialsToken;
import org.apache.slide.authenticate.SecurityToken;
import org.apache.slide.common.Domain;
import org.apache.slide.common.NamespaceAccessToken;
import org.apache.slide.common.SlideToken;
import org.apache.slide.common.SlideTokenImpl;
import org.apache.slide.content.Content;
import org.apache.slide.content.NodeProperty;
import org.apache.slide.content.NodeRevisionContent;
import org.apache.slide.content.NodeRevisionDescriptor;
import org.apache.slide.content.NodeRevisionDescriptors;
import org.apache.slide.content.NodeRevisionNumber;
import org.apache.slide.security.Security;
import org.apache.slide.structure.ObjectNode;
import org.apache.slide.structure.ObjectNotFoundException;
import org.apache.slide.structure.Structure;
import org.apache.slide.structure.SubjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.idegaweb.IWMainApplication;
import com.idega.slide.authentication.AuthenticationBusiness;
import com.idega.slide.webdavservlet.DomainConfig;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.IOUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

/**
* Simple API of Slide implementation. It improves performance without breaking business logic.
* 
* @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
* @version $Revision: 1.2 $
*
* Last modified: $Date: 2009/05/08 08:10:02 $ by: $Author: valdas $
*/
@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class IWSimpleSlideServiceImp implements IWSimpleSlideService {

	private static final long serialVersionUID = 8065146986117553218L;
	private static final Logger LOGGER = Logger.getLogger(IWSimpleSlideServiceImp.class.getName());
	
	@Autowired
	private DomainConfig domainConfig;
	
	private NamespaceAccessToken namespace;
	private Structure structure;
	private Content content;
	private Security security;
	
	private AuthenticationBusiness authenticationBusiness;
	
	private boolean initialized;
	
	private synchronized void initializeSimpleSlideServiceBean() {
		if (initialized) {
			initialized = !(namespace == null || structure == null || content == null || security == null);
		}
		if (initialized) {
			return;
		}
		
		initialized = true;
		try {
			if (!Domain.isInitialized()) {
				domainConfig.initialize();
			}
			
			namespace = namespace == null ? Domain.accessNamespace(new SecurityToken(CoreConstants.EMPTY), Domain.getDefaultNamespace()) : namespace;
			structure = structure == null ? namespace.getStructureHelper() : structure;
			content = content == null ? namespace.getContentHelper(): content;
			security = security == null ? namespace.getSecurityHelper() : security;
		} catch(Throwable e) {
			initialized = false;
			LOGGER.warning("Error while initializing Simple Slide API, will try again on next request");
		}
	}
	
	private AuthenticationBusiness getAuthenticationBusiness() {
		if (authenticationBusiness == null) {
			try {
				authenticationBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), AuthenticationBusiness.class);
			} catch (IBOLookupException e) {
				LOGGER.log(Level.WARNING, "Error getting EJB bean:" + AuthenticationBusiness.class, e);
			}
		}
		return authenticationBusiness;
	}
	
	private SlideToken getSlideToken() {
		initializeSimpleSlideServiceBean();
		
		String userPrincipals = null;
		
		try {
			AuthenticationBusiness ab = getAuthenticationBusiness();
			userPrincipals = ab.getRootUserCredentials().getUserName();
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error getting user's principals", e);
		}
		
		SlideToken token = new SlideTokenImpl(new CredentialsToken(userPrincipals));
		token.setForceStoreEnlistment(true);
		return token;
	}
	
	private String getAuthorsXML(User user) {
		String firstName = null;
		String lastName = null;
		String unknown = "Unknown";
		if (user != null) {
			firstName = user.getFirstName();
			lastName = user.getLastName();
		}
		
		String authors = new StringBuilder("<authors><author><firstname>").append(firstName == null ? unknown : firstName).append("</firstname><lastname>")
						.append(lastName == null ? unknown : lastName).append("</lastname></author></authors>").toString();
		return authors;
	}
	
	private String computeEtag(String uri, NodeRevisionDescriptor nrd) throws Exception {
		StringBuffer result = new StringBuffer(String.valueOf(System.currentTimeMillis())).append(CoreConstants.UNDER).append(uri.hashCode())
			.append(CoreConstants.UNDER).append(nrd.getLastModified()).append(CoreConstants.UNDER).append(nrd.getContentLength());
		return DigestUtils.md5Hex(result.toString());
	}
	
	@SuppressWarnings("unchecked")
	private boolean doUploading(InputStream stream, SlideToken token, String uploadPath, String contentType, User user, boolean closeStream) {
		//	TODO: there is problem in uploadPath: API doesn't "see" different in case: /files/themes/ and /files/Themes/
		try {
			NodeRevisionNumber lastRevision = null;
			try {
				structure.retrieve(token, uploadPath);
				lastRevision = content.retrieve(token, uploadPath).getLatestRevision();
			}
			catch (ObjectNotFoundException e) {
				SubjectNode subject = new SubjectNode();
				//	Create object
				structure.create(token, subject, uploadPath);
			}
			if (lastRevision == null) {
				lastRevision = new NodeRevisionNumber();
			}
			else {
				lastRevision = new NodeRevisionNumber(lastRevision, false);
			}
			
			//	Node revision descriptor	//	TODO: check for existing descriptor
			IWTimestamp now = IWTimestamp.RightNow();
			NodeRevisionDescriptor revisionDescriptor = new NodeRevisionDescriptor(lastRevision, NodeRevisionDescriptors.MAIN_BRANCH, new Vector(),
					new ArrayList());
			revisionDescriptor.setResourceType(CoreConstants.EMPTY);
			revisionDescriptor.setSource(CoreConstants.EMPTY);
			revisionDescriptor.setContentLanguage(Locale.ENGLISH.getLanguage());
			revisionDescriptor.setLastModified(now.getDate());
			revisionDescriptor.setETag(computeEtag(uploadPath, revisionDescriptor));
			revisionDescriptor.setCreationDate(now.getDate());
			
			//	Owner
			String creator = ((SubjectNode)security.getPrincipal(token)).getPath().lastSegment();
			revisionDescriptor.setCreationUser(creator);
			revisionDescriptor.setOwner(creator);
			if (contentType != null) {
				revisionDescriptor.setContentType(contentType);
			}
			
			//	Properties (for now - just owner)
			NodeProperty newProperty = new NodeProperty("authors", getAuthorsXML(user));
			revisionDescriptor.setProperty(newProperty);
			
			//	Create content
			NodeRevisionContent revisionContent = new NodeRevisionContent();
			revisionContent.setContent(stream);
			
			//	Important to create NodeRevisionDescriptors separately to be able to tell it to use versioning
			if (lastRevision.toString().equals("1.0")) {
				content.create(token, uploadPath, true);
			}
			
			content.create(token, uploadPath, revisionDescriptor, revisionContent);
			return true;
		} catch(Throwable e) {
			LOGGER.log(Level.SEVERE, "Error while uploading!", e);
		}
		finally {
			if (closeStream) {
				IOUtil.closeInputStream(stream);
			}
			finishTransaction();
		}
		
		return false;
	}
	
	public boolean upload(InputStream stream, String uploadPath, String fileName, String contentType, User user, boolean closeStream) throws Exception {
		if (stream == null || uploadPath == null || fileName == null) {
			return false;
		}
		
		SlideToken token = getSlideToken();
		if (!startTransaction()) {
			return false;
		}
		
		boolean uploadingResult = doUploading(stream, token, uploadPath + fileName, contentType, user, closeStream);
		if (!uploadingResult) {
			rollbackTransaction();
		}
		
		return uploadingResult;
	}
	
	public boolean upload(InputStream stream, String uploadPath, String fileName, String contentType, User user) throws Exception {
		return upload(stream, uploadPath, fileName, contentType, user, true);
	}

	private NodeRevisionDescriptors getNodeRevisionDescriptors(String pathToNode) {
		if (StringUtil.isEmpty(pathToNode)) {
			return null;
		}
		
		SlideToken rootToken = getSlideToken();
		if (rootToken == null) {
			return null;
		}
		
		if (!startTransaction()) {
			return null;
		}
		
		pathToNode = getNormalizedPath(pathToNode);
		
		try {
			return content.retrieve(rootToken, pathToNode);
		} catch (Throwable e) {
			LOGGER.warning("Unable to retrieve requested object: " + pathToNode);
		} finally {
			rollbackTransaction();
		}
		
		return null;
	}
	
	private NodeRevisionDescriptor getNodeRevisionDescriptor(String pathToNode) {
		NodeRevisionDescriptors revisionDescriptors = getNodeRevisionDescriptors(pathToNode);
		return getNodeRevisionDescriptor(revisionDescriptors, pathToNode);
	}
	
	private NodeRevisionDescriptor getNodeRevisionDescriptor(NodeRevisionDescriptors revisionDescriptors, String pathToNode) {
		SlideToken rootToken = getSlideToken();
		if (rootToken == null) {
			return null;
		}
		
		if (!startTransaction()) {
			return null;
		}
		
		try {
			return content.retrieve(rootToken, revisionDescriptors);
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Error retrieving revision descriptor", e);
		} finally {
			rollbackTransaction();
		}
		
		return null;
	}
	
	public boolean checkExistance(String pathToFile) {
		SlideToken rootToken = getSlideToken();
		if (rootToken == null) {
			return false;
		}
		
		if (!startTransaction()) {
			return false;
		}

		pathToFile = getNormalizedPath(pathToFile);
		
		ObjectNode node = null;
		try {
			node = structure.retrieve(rootToken, pathToFile);
		} catch (Exception e) {
			rollbackTransaction();
			LOGGER.warning("Error retrieving object at " + pathToFile);
			return false;
		}
		
		finishTransaction();
		
		if (node == null) {
			LOGGER.warning("Object doesn't exist at " + pathToFile);
			return false;
		}
		
		return true;
	}
	
	private NodeRevisionContent getNodeContent(String pathToFile) {
		NodeRevisionDescriptors revisionDescriptors = getNodeRevisionDescriptors(pathToFile);
		if (revisionDescriptors == null || !revisionDescriptors.hasRevisions()) {
			return null;
		}
		
		NodeRevisionDescriptor revisionDescriptor = getNodeRevisionDescriptor(pathToFile);
		if (revisionDescriptor == null) {
			return null;
		}
		
		SlideToken rootToken = getSlideToken();
		if (rootToken == null) {
			return null;
		}
		
		if (!startTransaction()) {
			return null;
		}
		
		try {
			return content.retrieve(rootToken, revisionDescriptors, revisionDescriptor);
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Error getting InputStream for: " + pathToFile, e);
			rollbackTransaction();
		} finally {
			finishTransaction();
		}
		
		return null;
	}
	
	public InputStream getInputStream(String pathToFile) {
		NodeRevisionContent nodeContent = getNodeContent(pathToFile);
		
		if (nodeContent == null) {
			return null;
		}
		
		if (!startTransaction()) {
			return null;
		}
		
		InputStream stream = null;
		try {
			stream = nodeContent.streamContent();
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Error getting InputStream for: " + pathToFile, e);
			rollbackTransaction();
		}
		
		finishTransaction();
		return stream;
	}
	
	public boolean setContent(String pathToFile, InputStream contentStream) {
		pathToFile = getNormalizedPath(pathToFile);
		
		NodeRevisionContent nodeContent = getNodeContent(pathToFile);
		
		if (nodeContent == null) {
			return Boolean.FALSE;
		}
		
		SlideToken rootToken = getSlideToken();
		if (rootToken == null) {
			return Boolean.FALSE;
		}
		
		NodeRevisionDescriptors descriptors = getNodeRevisionDescriptors(pathToFile);
		NodeRevisionDescriptor descriptor = getNodeRevisionDescriptor(descriptors, pathToFile);
		
		if (!startTransaction()) {
			return Boolean.FALSE;
		}
		
		try {
			descriptor.setContentLength(contentStream.available());
			descriptor.setLastModified(new Date(System.currentTimeMillis()));
			
			nodeContent.setContent(contentStream);
			content.store(rootToken, pathToFile, descriptor, nodeContent);
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Error setting content InputStream for: " + pathToFile, e);
			rollbackTransaction();
			return Boolean.FALSE;
		}
		
		finishTransaction();
		return Boolean.TRUE;
	}
	
	private String getNormalizedPath(String path) {
		if (StringUtil.isEmpty(path)) {
			return path;
		}
		
		if (path.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
			path = StringHandler.replace(path, CoreConstants.WEBDAV_SERVLET_URI, CoreConstants.EMPTY);
		}
		
		return path;
	}
	
	private boolean startTransaction() {
		initializeSimpleSlideServiceBean();
		
		if (namespace == null) {
			return false;
		}
		
		try {
			if (namespace.getStatus() == 0) {
				//	Transaction was begun already	TODO: check if it's OK!
				return true;
			}
			
			namespace.begin();
		} catch(Throwable e) {
			LOGGER.log(Level.SEVERE, "Cannot start user transaction", e);
			return false;
		}
		
		return true;
	}
	
	private boolean rollbackTransaction() {
		if (namespace == null) {
			return false;
		}
		
		try {
			namespace.rollback();
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Cannot rollback user transaction", e);
			return false;
		}
		return true;
	}
	
	private boolean finishTransaction() {
		if (namespace == null) {
			return false;
		}
		
		try {
			namespace.commit();
		} catch (Throwable e) {
			LOGGER.log(Level.SEVERE, "Cannot finish user transaction", e);
			return false;
		}
		return true;
	}
}