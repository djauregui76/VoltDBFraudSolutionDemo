package jppml;

import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

public class GolfDemo extends VoltProcedure {

    public VoltTable[] run(double temperature, double humidity,
            String windy, String outlook) throws VoltAbortException {

        VoltTable[] pmmlOut;

        try {

            JPMMLImpl i = JPMMLImpl.getInstance();
            VoltDBJPMMLWrangler w = i.getPool().borrowObject();
            final String modelName = "tree.model";
            VoltTable paramtable = w.getEmptyTable(modelName);
            paramtable.addRow(temperature, humidity, windy, outlook);
            pmmlOut = w.runModel(modelName, paramtable);
           
        } catch (Exception e) {
            
            System.err.println(e.getMessage());
            throw new VoltAbortException(e);
        
        }

        voltExecuteSQL(true);
        return pmmlOut;
    }

}