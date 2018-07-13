package org.voltdb.jpmml;

import java.io.File;
import java.util.Map;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;

public class JPMMLImplPoolFactory extends BasePooledObjectFactory<VoltDBJPMMLWrangler> {

    public static final String JPMML_FILES = "JPMML_FILES";

    @Override
    public VoltDBJPMMLWrangler create() throws Exception {

        String m_sourceFiles = "/tmp/AuditTree.xml"; //System.getenv(JPMML_FILES);

        if (m_sourceFiles == null || m_sourceFiles.length() == 0) {
            System.err.println("VoltDBJPMMLWrangler needs the environment variable " + JPMML_FILES + " to work");
            return null;
        }

        File[] m_sourceFileArray = {};

        if (m_sourceFiles.indexOf(',') < 0) {
            // See if is directory name
            File dirOrFile = new File(m_sourceFiles);

            if (dirOrFile.isFile()) {
                // Actually it's a single file...
                m_sourceFileArray = new File[1];
                m_sourceFileArray[0] = dirOrFile;

            } else {
                m_sourceFileArray = dirOrFile.listFiles();

                if (m_sourceFileArray == null || m_sourceFileArray.length == 0) {
                    System.err.println("VoltDBJPMMLWrangler : Directory " + m_sourceFiles + " not usable");
                    return null;
                }
            }
        } else {
            // Assume a comma delimited list of files
            String[] m_fileNames = m_sourceFiles.split(",");
            m_sourceFileArray = new File[m_fileNames.length];

            for (int i = 0; i < m_sourceFileArray.length; i++) {
                m_sourceFileArray[i] = new File(m_fileNames[i].trim());
            }

        }

        VoltDBJPMMLWrangler impl = new VoltDBJPMMLWrangler(m_sourceFileArray);

        return (VoltDBJPMMLWrangler) impl;
    }

    @Override
    public PooledObject<VoltDBJPMMLWrangler> wrap(VoltDBJPMMLWrangler theIface) {

        PooledObject<VoltDBJPMMLWrangler> newObj = new JPMMLImplPooledObject(theIface);

        return newObj;
    }

}
