package org.voltdb.jpmml.test;

import java.io.File;

import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.VoltType;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

public class SmokeTest {

    public static void main(String[] args) {
       
        try {
            JPMMLImpl i = JPMMLImpl.getInstance();
            VoltDBJPMMLWrangler w = i.getPool().borrowObject();
            
            String foo = w.getUDF("AuditKMeans.xml");
            System.out.println(foo);

            long startMs = System.currentTimeMillis();
            int count = 0;
            for (int iTemp = 0; iTemp < 200; iTemp++) {
                for (int iHumid = 0; iHumid < 100; iHumid++) {
                    count++;
                    VoltTable paramtable = 
                            
                     w.getEmptyTable("tree.model");
                            
                    paramtable.addRow(iTemp, iHumid, "false", "sunny");

                    VoltTable[] pmmlOut = w.runModel("tree.model", paramtable);
                    System.out.println(pmmlOut[0]);

                }
            }
            i.getPool().returnObject(w);
            System.out.println("Time=" + (System.currentTimeMillis() - startMs) + " " + count);

            startMs = System.currentTimeMillis();
            count = 0;
            for (int iTemp = 0; iTemp < 200; iTemp++) {

                count++;
                VoltTable paramtable =
                        
                        w.getEmptyTable("tree.model");
                        
//                        new VoltTable(
//
//                        // new VoltTable.ColumnInfo("ID", VoltType.BIGINT),
//                        new VoltTable.ColumnInfo("AGE", VoltType.BIGINT),
//                        new VoltTable.ColumnInfo("EDUCATION", VoltType.STRING),
//                        new VoltTable.ColumnInfo("Marital", VoltType.STRING),
//                        new VoltTable.ColumnInfo("Occupation", VoltType.STRING),
//                        new VoltTable.ColumnInfo("Income", VoltType.FLOAT),
//                        new VoltTable.ColumnInfo("Gender", VoltType.STRING),
//                        new VoltTable.ColumnInfo("Hours", VoltType.BIGINT),
//                        new VoltTable.ColumnInfo("Employment", VoltType.STRING),
//                        new VoltTable.ColumnInfo("Deductions", VoltType.FLOAT)
//
//                );

                paramtable.addRow(iTemp % 90, "Master", "Married", "Transport", 40000, "Male", 40, "Private", 1000);

                System.out.println(paramtable.toString());
               
                VoltTable[] pmmlOut = w.runModel("tree.model", paramtable);
                System.out.println(pmmlOut[0]);

            }
            System.out.println("Time=" + (System.currentTimeMillis() - startMs) + " " + count);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
