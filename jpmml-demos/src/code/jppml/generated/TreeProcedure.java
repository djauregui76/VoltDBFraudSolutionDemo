package jppml.generated;

import org.voltdb.VoltTable;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

public class TreeProcedure {

    public String tree(double temperature, double humidity, String windy, String outlook) {

        VoltTable[] pmmlOut;
        
        try {

            JPMMLImpl i = JPMMLImpl.getInstance();
            VoltDBJPMMLWrangler w = i.getPool().borrowObject();
            final String modelName = "tree.model";
            VoltTable paramtable = w.getEmptyTable(modelName);
            paramtable.addRow(temperature, humidity, windy, outlook);
            pmmlOut = w.runModel(modelName, paramtable);
            i.getPool().returnObject(w);
            pmmlOut[0].advanceRow();
            return pmmlOut[0].getString(0);
           
        } catch (Exception e) {
            System.err.println(e.getMessage());
           return null;
        }

    } 
}
