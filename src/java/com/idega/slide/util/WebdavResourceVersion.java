/*
 * $Id: WebdavResourceVersion.java,v 1.3 2004/12/21 18:25:29 eiki Exp $ Created on Dec
 * 19, 2004
 * 
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 * 
 * This software is the proprietary information of Idega hf. Use is subject to
 * license terms.
 */
package com.idega.slide.util;

import java.text.Collator;
import java.util.Map;
import org.apache.webdav.lib.BaseProperty;
import org.apache.webdav.lib.properties.CheckedinProperty;
import org.apache.webdav.lib.properties.CheckedoutProperty;
import com.idega.core.accesscontrol.business.LoginDBHandler;
import com.idega.core.accesscontrol.data.LoginTable;
import com.idega.user.data.User;

/**
 * 
 * Last modified: $Date: 2004/12/21 18:25:29 $ by $Author: eiki $
 * 
 * A little wrapper for version information
 * 
 * @author <a href="mailto:eiki@idega.com">Eirikur S. Hrafnsson </a>
 * @version $Revision: 1.3 $
 */
public class WebdavResourceVersion implements Comparable{

	private BaseProperty versionName;

	private BaseProperty creatorDisplayName;

	private BaseProperty lastModified;

	private BaseProperty contentLength;

	//private BaseProperty successorSet;
	
	private CheckedinProperty checkedIn;

	private CheckedoutProperty checkedOut;

	private BaseProperty comment;
	
	protected WebdavResourceVersion(Map propTable) {
		versionName = (BaseProperty) propTable.get(VersionHelper.PROPERTY_VERSION_NAME);
		creatorDisplayName = (BaseProperty) propTable.get(VersionHelper.PROPERTY_CREATOR_DISPLAY_NAME);
		lastModified = (BaseProperty) propTable.get(VersionHelper.PROPERTY_LAST_MODIFIED);
		contentLength = (BaseProperty) propTable.get(VersionHelper.PROPERTY_CONTENT_LENGTH);
		//successorSet = (BaseProperty) propTable.get(VersionHelper.PROPERTY_SUCCESSOR_SET);
		checkedIn = (CheckedinProperty) propTable.get(VersionHelper.PROPERTY_CHECKED_IN);
		checkedOut = (CheckedoutProperty) propTable.get(VersionHelper.PROPERTY_CHECKED_OUT);
		comment = (BaseProperty) propTable.get(VersionHelper.PROPERTY_COMMENT);
	}

	public String toString() {
		if (versionName != null) {
			return versionName.getPropertyAsString();
		}
		return null;
	}

	/**
	 * @return Returns the checkedIn.
	 */
	public String getCheckedIn() {
		return checkedIn.getPropertyAsString();
	}

	/**
	 * @return Returns the checkedOut.
	 */
	public String getCheckedOut() {
		return checkedOut.getPropertyAsString();
	}

	/**
	 * @return Returns the comment.
	 */
	public String getComment() {
		return comment.getPropertyAsString();
	}

	/**
	 * @return Returns the contentLength.
	 */
	public String getContentLength() {
		return contentLength.getPropertyAsString();
	}

	/**
	 * @return Returns the creatorDisplayName.
	 */
	public String getCreatorDisplayName() {
		String userName = creatorDisplayName.getPropertyAsString();
		String name = null;
		
		LoginTable login =  LoginDBHandler.getUserLoginByUserName(userName);
		if(login!=null){
			User user = (User)login.getUser();
			name = user.getName();
		}
		
		if(name!=null && !userName.equals(name)){
			return name+" ("+userName+")";
		}
		else{
			return userName;
		}
	}

	/**
	 * @return Returns the lastModified.
	 */
	public String getLastModified() {
		return lastModified.getPropertyAsString();
	}

	/**
	 * @return Returns the successorSet.
	 */
//	public BaseProperty getSuccessorSet() {
//		return successorSet;
//	}

	/**
	 * @return Returns the versionName.
	 */
	public String getVersionName() {
		return toString();
	}
	
	/**
	 * @return Returns the versionName.
	 */
	public String getURL() {
		return versionName.getOwningURL();
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Object version) {
		return Collator.getInstance().compare(this.toString(),version.toString());
	}
	
}