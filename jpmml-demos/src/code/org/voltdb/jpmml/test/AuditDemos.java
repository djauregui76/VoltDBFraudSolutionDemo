package org.voltdb.jpmml.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.NoConnectionsException;
import org.voltdb.client.ProcCallException;
import org.voltdb.jpmml.JPMMLImpl;
import org.voltdb.jpmml.JPMMLImplPool;
import org.voltdb.jpmml.VoltDBJPMMLWrangler;

import jppml.generated.AuditTreeProcedure;

public class AuditDemos {
    
    
    public static ArrayList<String> loadFile (String filename) throws FileNotFoundException {
        Scanner s = new Scanner(new File(filename));
        ArrayList<String> list = new ArrayList<String>();
        while (s.hasNext()){
            list.add(s.next());
        }
        s.close();
        return list;
    }

    public static void main(String[] args) {
        
        String filename = args[0];
        String demoname = args[1];
        
        try {
            Client c = connectVoltDB("192.168.17.131");

            
            ArrayList<String> theList = AuditDemos.loadFile(filename);
            String[][] values = new String[theList.size()][];
            
            for (int i=0; i < theList.size(); i++) {
                String[] lineValues = theList.get(i).split(",");
                values[i] = lineValues;
                
                for (int j=0; j < values[i].length; j++) {
                    values[i][j] =    values[i][j].replace("\"", "");
                    if (values[i][j].equals("NA")) {
                        
                        values[i][j] = "Unemployed";
                    }
                }
            }

            JPMMLImpl impl = null;
            VoltDBJPMMLWrangler w = null;

           

            impl = JPMMLImpl.getInstance();
                JPMMLImplPool p = impl.getPool();
                w = p.borrowObject();
                
    //            String foo = w.getUDF("AuditTree.xml");
                
                
     //           System.out.println(foo);
            
            if (demoname.equals("AuditTree")) {
             
                msg ("Insert " +  theList.size() + " rows of audit data" );
                
                for (int i=1; i < theList.size(); i++) {
                                       
                    c.callProcedure("AUDITDATA.UPSERT", Long.parseLong(values[i][0])
                            , Long.parseLong(values[i][1])
                            , values[i][2],values[i][3],values[i][4],values[i][5]
                            , Double.parseDouble(values[i][6])
                            , values[i][7],Double.parseDouble(values[i][8]),Long.parseLong(values[i][9])
                            ,values[i][10],Long.parseLong(values[i][11]),Long.parseLong(values[i][12]));
                }
              
                msg ("Done" );
                
                int howMany = 50;
                int transThisMs = 0;
                int tpMs = 50;
                
                msg ("Do " + howMany + " random calls to the audit ML function" );
                
                Random r = new Random(42);
                long startMs = System.currentTimeMillis(); 
                long currentMs = startMs;
                
               for (int i=0; i < howMany; i++) {
                   int id = r.nextInt(theList.size()-1) + 1;
                   ComplainOnErrorCallback coec = new ComplainOnErrorCallback(); 
                   ClientResponse cr = c.callProcedure("auditId",Long.parseLong(values[id][0]));
                   System.out.println(cr.getResults()[0].toFormattedString());
                   transThisMs++;
                   
                   if (currentMs == System.currentTimeMillis()) {

                       if (transThisMs > tpMs) {
                           while (currentMs == System.currentTimeMillis()) {
                               Thread.sleep(0, 1000);
                           }
                           currentMs = System.currentTimeMillis();
                           transThisMs = 0;
                       }

                   } else {
                       currentMs = System.currentTimeMillis();
                       transThisMs = 0;
                   }

                   
               }
               
               c.drain();
               msg ("Done" );
               int actualTpMs = (int) (howMany / (System.currentTimeMillis() - startMs));
               msg ("TPMS=" + actualTpMs );
               msg ("Do " + howMany + " random calls to the ML proc" );
               
                startMs = System.currentTimeMillis(); 
                currentMs = startMs;
               
              for (int i=1; i < howMany; i++) {
                  
                  int id = r.nextInt(theList.size()-1) + 1;
                  ComplainOnErrorCallback coec = new ComplainOnErrorCallback(); 
                  
                  if (id == 2001) {
                      System.out.println("f");
                  }
                  c.callProcedure(coec,"AuditTreeStoredProc", Long.parseLong(values[id][0])
                          , Long.parseLong(values[id][1])
                          , values[id][2],values[id][3],values[id][4],values[id][5]
                          , Double.parseDouble(values[id][6])
                          , values[id][7],Double.parseDouble(values[id][8]),Long.parseLong(values[id][9]));
                  transThisMs++;
                  
                  if (currentMs == System.currentTimeMillis()) {

                      if (transThisMs > tpMs) {
                          while (currentMs == System.currentTimeMillis()) {
                              Thread.sleep(0, 1000);
                          }
                          currentMs = System.currentTimeMillis();
                          transThisMs = 0;
                      }

                  } else {
                      currentMs = System.currentTimeMillis();
                      transThisMs = 0;
                  }

                  
              }

              
              c.drain();
               msg ("Done" );
                actualTpMs = (int) (howMany / (System.currentTimeMillis() - startMs));
               msg ("TPMS=" + actualTpMs );
               
               
               
               
            }
                   
 
           
            
            
            
            
            
            //VoltDBJPMMLWrangler w = new VoltDBJPMMLWrangler(new File("/Users/drolfe/Desktop/EclipseWorkspace/jpmml-demos/models/AuditTree.xml"));
            
           // String at = w.getUDF("AuditTree.xml");
            
           // System.out.println(at);
            
            
         //   atp.
            
            
            c.close();
            
            
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoConnectionsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ProcCallException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        

    }
    
    public static void msg(String message) {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();
        String strDate = sdfDate.format(now);
        System.out.println(strDate + ":" + message);
    }

    private static Client connectVoltDB(String hostname) throws Exception {
        Client client = null;
        ClientConfig config = null;

        try {
            //msg("Logging into VoltDB");

            config = new ClientConfig(); // "admin", "idontknow");
            config.setMaxOutstandingTxns(20000);
            config.setMaxTransactionsPerSecond(200000);
            config.setTopologyChangeAware(true);
            config.setReconnectOnConnectionLoss(true);

            client = ClientFactory.createClient(config);

            client.createConnection(hostname);

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("VoltDB connection failed.." + e.getMessage(), e);
        }

        return client;

    }

}
