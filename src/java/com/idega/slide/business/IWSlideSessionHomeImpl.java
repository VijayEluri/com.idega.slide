/*
 * $Id: IWSlideSessionHomeImpl.java,v 1.1 2004/11/01 10:42:18 gummi Exp $
 * Created on 25.10.2004
 *
 * Copyright (C) 2004 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.slide.business;

import com.idega.business.IBOHomeImpl;


/**
 * 
 *  Last modified: $Date: 2004/11/01 10:42:18 $ by $Author: gummi $
 * 
 * @author <a href="mailto:gummi@idega.com">Gudmundur Agust Saemundsson</a>
 * @version $Revision: 1.1 $
 */
public class IWSlideSessionHomeImpl extends IBOHomeImpl implements IWSlideSessionHome {

	protected Class getBeanInterfaceClass() {
		return IWSlideSession.class;
	}

	public IWSlideSession create() throws javax.ejb.CreateException {
		return (IWSlideSession) super.createIBO();
	}
}
