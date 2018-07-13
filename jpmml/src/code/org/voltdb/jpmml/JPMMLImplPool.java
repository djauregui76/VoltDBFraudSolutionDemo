package org.voltdb.jpmml;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;

public class JPMMLImplPool extends GenericObjectPool<VoltDBJPMMLWrangler> {

	public JPMMLImplPool(
			PooledObjectFactory<VoltDBJPMMLWrangler> factory) {
		super(factory);	
		
	}
	
	

}
