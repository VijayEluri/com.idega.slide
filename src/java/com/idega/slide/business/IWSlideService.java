/*
 * $Id: IWSlideService.java,v 1.1 2004/11/01 10:42:18 gummi Exp $
 * Created on 23.10.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.slide.business;

import com.idega.business.IBOService;


/**
 * 
 *  Last modified: $Date: 2004/11/01 10:42:18 $ by $Author: gummi $
 * 
 * @author <a href="mailto:gummi@idega.com">Gudmundur Agust Saemundsson</a>
 * @version $Revision: 1.1 $
 */
public interface IWSlideService extends IBOService {

	/**
	 * @see com.idega.slide.business.IWSlideServiceBean#getWebdavServletURL
	 */
	public String getWebdavServletURL() throws java.rmi.RemoteException;

//	/**
//	 * @see com.idega.slide.business.IWSlideServiceBean#getGuestCredentials
//	 */
//	public Credentials getGuestCredentials() throws java.rmi.RemoteException;
}
