package jppml;

import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.JPMMLImplPool;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

public class AuditTreeStoredProc extends VoltProcedure {

    public VoltTable[] run(int id, double age, String employment, String education, String marital, String occupation,
            double income, String gender, double deductions, double hours) {

        final String modelName = "AuditTree.xml";
        VoltTable[] pmmlOut = null;
        JPMMLImpl i = null;
        VoltDBJPMMLWrangler w = null;

        try {

            i = JPMMLImpl.getInstance();
            JPMMLImplPool p = i.getPool();
            w = p.borrowObject();
            VoltTable paramtable = w.getEmptyTable(modelName);
            paramtable.addRow(age, employment, education, marital, occupation, income, gender, deductions, hours);
            pmmlOut = w.runModel(modelName, paramtable);
            return pmmlOut;

        } catch (Exception e) {

            System.err.println(e.getMessage());
            e.printStackTrace();
            return null;

        } finally {

            if (i != null && w != null) {
                i.getPool().returnObject(w);
            }

        }
    }

}