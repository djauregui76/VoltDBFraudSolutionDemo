package org.voltdb.jpmml;

import org.apache.commons.pool2.impl.DefaultPooledObject;

public class JPMMLImplPooledObject extends DefaultPooledObject<VoltDBJPMMLWrangler> {

	public JPMMLImplPooledObject(VoltDBJPMMLWrangler object) {
		super(object);
	}

}
