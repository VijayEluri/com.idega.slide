package com.idega.slide.schema;

import java.sql.Connection;

import com.idega.util.database.ConnectionBroker;
import com.idega.util.dbschema.SQLSchemaAdapter;
import com.idega.util.dbschema.SQLSchemaCreator;

/**
 * 
 * 
 *  Last modified: $Date: 2005/11/10 15:56:20 $ by $Author: gimmi $
 * 
 * @author <a href="mailto:aron@idega.com">aron</a>
 * @version $Revision: 1.4 $
 */
public class SlideSchemaCreator {
    
    public void createSchemas(){
        try {
            //PoolManager pManager = PoolManager.getInstance();
            Connection conn = ConnectionBroker.getConnection();
            String datastoreType = SQLSchemaAdapter.detectDataStoreType(conn);
            ConnectionBroker.freeConnection(conn);
            createSchemas(datastoreType);
        } catch (Exception e) {
           
            e.printStackTrace();
        }
    }
	
	public void createSchemas(String dataStoreType) throws Exception{
		
		SQLSchemaAdapter dsi = SQLSchemaAdapter.getInstance(dataStoreType);
		if (dsi.getSupportsSlide()) {
			SQLSchemaCreator tableCreator = dsi.getTableCreator();
	
			tableCreator.generateSchema(new UriSchema());
			tableCreator.generateSchema(new ObjectSchema());
			tableCreator.generateSchema(new BindingSchema());
			tableCreator.generateSchema(new ParentBindingSchema());
			tableCreator.generateSchema(new LinksSchema());
			tableCreator.generateSchema(new LocksSchema());
			tableCreator.generateSchema(new BranchSchema());
			tableCreator.generateSchema(new LabelSchema());
			tableCreator.generateSchema(new VersionSchema());
			tableCreator.generateSchema(new VersionHistorySchema());
			tableCreator.generateSchema(new VersionPredsSchema());
			tableCreator.generateSchema(new VersionLabelsSchema());
			tableCreator.generateSchema(new VersionContentSchema());
			tableCreator.generateSchema(new PropertiesSchema());
			tableCreator.generateSchema(new PermissionsSchema());
		} else {
			System.out.println("[Slide Startup] Database not supported");
		}
		
	}

}
