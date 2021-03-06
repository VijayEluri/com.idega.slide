package com.idega.slide.util;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.webdav.lib.methods.LockMethod;
import org.apache.webdav.lib.Property;
import org.apache.webdav.lib.WebdavResource;
import org.apache.webdav.lib.WebdavResources;
import org.apache.webdav.lib.methods.DepthSupport;

import com.idega.util.CoreConstants;

/**
 * 
 * This class is an extended version Slide's WebdavResource. <br>
 * It has been extended in order to retrieve the basic list of properties
 * (listBasic in WebdavResource) along <br>
 * with additional (versioncontrol) DeltaV properties. <br>
 * The new list method is listWithDeltaV() (to get the children with their
 * version info).
 * 
 * @author <a href="eiki@idega.is>Eirikur S. Hrafnsson </a> with a little help
 *         from Billy Joe McCue the author of the SwingDaver webdav client.
 *  
 */
public class WebdavExtendedResource extends WebdavResource {

	private String path;
	private String checkedIn;
	private String checkedOut;
	private String versionName;
	private String comment;
	private String parentPath;
	private String creationDate;
	
	public WebdavExtendedResource(String urlStr, Credentials cred, boolean followRedirects) throws IOException {
		super(urlStr, cred, followRedirects);
	}

	public WebdavExtendedResource(HttpClient client) {
		super(client);
	}
	

	public WebdavExtendedResource(HttpURL url) throws HttpException, IOException {
		super(url);
	}

	public String getParentPath() {
		if (this.parentPath == null) {
			if (this.path != null) {
				int index = this.path.lastIndexOf(CoreConstants.SLASH);
				if (index == 0) {
					this.parentPath = CoreConstants.EMPTY;
				} else {
					this.parentPath = this.path.substring(0, index);
				}
			} else {
				return null;
			}
		}
		return this.parentPath;
	}
	
	/**
	 * 
	 * Create a new WebdavResource object (as a seperate method so that it can
	 * be overridden by subclasses.
	 * @param client HttpClient to be used by this webdavresource.
	 * @return A new WebdavResource object.
	 */
	@Override
	protected WebdavResource createWebdavResource(HttpClient client) {
		WebdavResource resource = new WebdavExtendedResource(client);
		resource.setCredentials(this.hostCredentials);
		return resource;
	}

	/**
	 * Create an updated version of itself
	 */
	public WebdavExtendedResource createUpdatedResource() {
		WebdavExtendedResource resource = (WebdavExtendedResource) createWebdavResource(this.client);
		try {
			resource.setFinalHttpURL(getHttpURL());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return resource;
	}

	/**
	 * 
	 * Initial purpose of overiding this method is because
	 * the WebdavResources addResource(WebdavResource) method
	 * calls this method to use the return value as a key
	 * in it's hashtable called hreftable which in turn is
	 * used for retrieving all of it's child resources.
	 * This being said, the original getName method seems to return
	 * duplicate names of siblings for some resources, thus, overwriting
	 * it's sibling's key in the href table.
	 * We should probably try to return something unique, yet relavent
	 * to the name.
	 * Get the name of this WebdavResource.
	 * If the decoding of the name fails, this method will not throw an
	 * exception but return the escaped name instead.
	 * 
	 * @return the name of this WebdavResource.
	 * @see org.apache.commons.httpclient.HttpURL#getName()
	 *  
	 */
	@Override
	public String getName() {
		return this.path;
	}

	public void setFinalHttpURL(HttpURL url) throws IOException {
		//eiki not sure this is needed String scheme = url.getScheme();
		if (url instanceof HttpsURL) {
			this.httpURL = new HttpsURL((HttpsURL) url, this.path);
		}
		else {
			this.httpURL = new HttpURL(url, getPath());
		}
	}

	public void putMethod(String path, File file, String comment) throws IOException {
		super.putMethod(file);
		/*
		 * super.putMethod(path, file); // now we do all this to set a fricken
		 * comment // on the new version created (if any) setCheckedIn(null);
		 * Vector vProps = new Vector(1); vProps.addElement(CHECKED_IN);
		 * Enumeration responses = propfindMethod(DepthSupport.DEPTH_0, vProps);
		 * if(responses.hasMoreElements()) { ResponseEntity response =
		 * (ResponseEntity) responses.nextElement();
		 *  // Process the resource's properties Enumeration properties =
		 * response.getProperties(); while (properties.hasMoreElements()) {
		 * 
		 * Property property = (Property) properties.nextElement();
		 *  // ------------------------------ Checking WebDAV properties
		 * processProperty(property); } } String cIn = getCheckedIn(); if(cIn !=
		 * null) { proppatchMethod(cIn, COMMENT,comment,true); }
		 */
	}

	/**
	 * Process a property, setting various member variables depending on what the property is.
	 * @param property The property to process.
	 */
	@Override
	protected void processProperty(Property property) {
		String propName = property.getLocalName();
		String strVal = property.getPropertyAsString();
		// Take the first property we get to
		// properly set the path.
		if (this.path == null) {
			// we'll have to use this when our
			// setFinalHttpURL is called.
			this.path = property.getOwningURL();
		}
		if (propName.equals(VersionHelper.PROPERTY_CHECKED_IN)) {
			setCheckedIn(strVal);
		}
		else if (propName.equals(VersionHelper.PROPERTY_CHECKED_OUT)) {
			setCheckedOut(strVal);
		}
		else if (propName.equals(VersionHelper.PROPERTY_VERSION_NAME)) {
			setVersionName(strVal);
			if (strVal != null && strVal.length() > 0) {
				// We're in the history section, looking
				// at a version of a resource
				// So we're interested in showing the version-name
				// as the display-name
				setDisplayName(strVal);
			}
		}
		else if(propName.equals(VersionHelper.PROPERTY_COMMENT)){
			setComment(strVal);
		}
		else if (propName.equals(VersionHelper.PROPERTY_CREATION_DATE)) {
			strVal = strVal.replaceFirst("T", " ");
			strVal = strVal.replaceFirst("Z", "");
			setCreationDateString(strVal);
		}
		else {
			super.processProperty(property);
		}
	}
	
	protected void setCreationDateString(String value) {
		this.creationDate = value;
	}
	
	public String getCreationDateString() {
		return this.creationDate;
	}

	protected void setCheckedIn(String value) {
		this.checkedIn = value;
	}

	public String getCheckedIn() {
		return this.checkedIn;
	}

	protected void setCheckedOut(String value) {
		this.checkedOut = value;
	}

	public String getCheckedOut() {
		return this.checkedOut;
	}

	protected void setVersionName(String value) {
		this.versionName = value;
	}

	public String getVersionName() {
		return this.versionName;
	}

	/**
	 * 
	 * Sets the basic properties and DeltaV properties
	 * on a resource by indirectly issuing a PROPFIND
	 * on the resource.<p>
	 * Properties retrieved include:
	 * <ul>
	 * <li>displayname</li>
	 * <li>getcontentlength</li>
	 * <li>getcontenttype</li>
	 * <li>resourcetype</li>
	 * <li>getlastmodified</li>
	 * <li>lockdiscovery</li>
	 *  // DeltaV support
	 * <li>checked-in</li>
	 * <li>checked-out</li>
	 * <li>version-name</li>
	 * <li>creationdate</li>
	 * </ul>
	 */
	public WebdavResources listWithDeltaV() throws IOException {
		Vector properties = new Vector();
		properties.addElement(DISPLAYNAME);
		properties.addElement(GETCONTENTLENGTH);
		properties.addElement(GETCONTENTTYPE);
		properties.addElement(RESOURCETYPE);
		properties.addElement(GETLASTMODIFIED);
		properties.addElement(CREATIONDATE);
		properties.addElement(LOCKDISCOVERY);
		// DeltaV support
		properties.addElement(VersionHelper.PROPERTY_CHECKED_IN);
		properties.addElement(VersionHelper.PROPERTY_CHECKED_OUT);
		properties.addElement(VersionHelper.PROPERTY_VERSION_NAME);
		properties.addElement(VersionHelper.PROPERTY_COMMENT);
		properties.addElement(VersionHelper.PROPERTY_CREATION_DATE);
		// The following call should in turn, call
		// Enumeration responses = super.propfindMethod(DEPTH_1, properies)
		// super.setWebdavProperties(reponses)
		// in turn, calls
		// this.createWebdavResource(client); (see above)
		// in turn, calls a series of
		// processProperty(property)
		setNamedProp(DepthSupport.DEPTH_1, properties);
		// Here we use childResources directly, because
		// getChildResources() calls another setProperties
		// call before returning childResources, which we
		// don't really want to do, since we've
		// already called a setNamedProp above.
		return this.childResources;
	}
	
	/**
	 * 
	 * Sets the basic properties and DeltaV properties
	 * on a resource by indirectly issuing a PROPFIND
	 * on the resource.<p>
	 * Properties retrieved include:
	 * <ul>
	 * <li>displayname</li>
	 * <li>getcontentlength</li>
	 * <li>getcontenttype</li>
	 * <li>resourcetype</li>
	 * <li>getlastmodified</li>
	 * <li>lockdiscovery</li>
	 *  // DeltaV support
	 * <li>checked-in</li>
	 * <li>checked-out</li>
	 * <li>version-name</li>
	 * <li>creationdate</li>
	 * </ul>
	 */
	public void setProperties() throws IOException {
		Vector properties = new Vector();
		properties.addElement(DISPLAYNAME);
		properties.addElement(GETCONTENTLENGTH);
		properties.addElement(GETCONTENTTYPE);
		properties.addElement(RESOURCETYPE);
		properties.addElement(GETLASTMODIFIED);
		properties.addElement(LOCKDISCOVERY);
		// DeltaV support
		properties.addElement(VersionHelper.PROPERTY_CHECKED_IN);
		properties.addElement(VersionHelper.PROPERTY_CHECKED_OUT);
		properties.addElement(VersionHelper.PROPERTY_VERSION_NAME);
		properties.addElement(VersionHelper.PROPERTY_COMMENT);
		properties.addElement(VersionHelper.PROPERTY_CREATION_DATE);
		// The following call should in turn, call
		// Enumeration responses = super.propfindMethod(DEPTH_1, properies)
		// super.setWebdavProperties(reponses)
		// in turn, calls
		// this.createWebdavResource(client); (see above)
		// in turn, calls a series of
		// processProperty(property)
		setNamedProp(DepthSupport.DEPTH_0, properties);
	}
	
	public String getDecodedPath() {
		try {
			return URIUtil.decode(getPath());
		}
		catch (URIException e) {
			e.printStackTrace();
			return getPath();
		}
	}
	
	public String getEncodedPath() {
		try {
			return URIUtil.encodePath(getPath());
		}
		catch (URIException e) {
			e.printStackTrace();
			return getPath();
		}
	}
	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return this.comment;
	}
	/**
	 * @param comment The comment to set.
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public boolean lockMethodNoTimeout() throws HttpException, IOException {
		String owner = httpURL.getUser() == null ? "Slide" : httpURL.getUser();
	    boolean result = lockMethod(httpURL.getPath(), owner, LockMethod.TIMEOUT_INFINITY);
	    if (result)
	    	refresh();
	    return result;
	}
}