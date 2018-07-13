package jppml;

import org.voltdb.VoltTable;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

public class GolfDemoUDF {

    public String doiplay(double temperature, double humidity, String windy, String outlook) {

        final String modelName = "tree.model";
        VoltTable[] pmmlOut;
        JPMMLImpl i = null;
        VoltDBJPMMLWrangler w = null;
        
        try {

            i = JPMMLImpl.getInstance();
            w = i.getPool().borrowObject();     
            VoltTable paramtable = w.getEmptyTable(modelName);
            paramtable.addRow(temperature, humidity, windy, outlook);
            pmmlOut = w.runModel(modelName, paramtable);   
            pmmlOut[0].advanceRow();
            return pmmlOut[0].getString(0);
           
        } catch (Exception e) {
            System.err.println(e.getMessage());
           return null;
        } finally {
           if (w != null) {
               i.getPool().returnObject(w);
            }
 
        }

    } 
}
