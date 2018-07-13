package org.voltdb.jpmml;

import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorFactory;
import org.jpmml.evaluator.OutputField;
import org.jpmml.evaluator.TargetField;
import org.jpmml.model.PMMLUtil;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

public class VoltDBJPMMLWrangler {

    ModelEvaluatorFactory modelEvaluatorFactory = ModelEvaluatorFactory.newInstance();
    HashMap<String, Evaluator> pmmlEvaluators = new HashMap<String, Evaluator>();

    /**
     * Create a wrangler for a single PMML mode element
     * 
     * @param pmmlXmlFile
     *            A file containing an XML definition
     * @throws Exception
     */
    public VoltDBJPMMLWrangler(File pmmlXmlFile) throws Exception {

        NamedInputStream pmmlXmlis = null;

        if (!pmmlXmlFile.exists()) {
            throw new Exception(pmmlXmlFile.getAbsolutePath() + " not found");
        }

        if (!pmmlXmlFile.canRead()) {
            throw new Exception(pmmlXmlFile.getAbsolutePath() + " not readable");
        }

        if (pmmlXmlFile.isDirectory()) {
            throw new Exception(pmmlXmlFile.getAbsolutePath() + " is a directory");
        }

        ByteArrayInputStream theBytes = new ByteArrayInputStream(
                java.nio.file.Files.readAllBytes(pmmlXmlFile.toPath()));
        pmmlXmlis = new NamedInputStream(pmmlXmlFile.getName(), theBytes);

        NamedInputStream[] nisArray = { pmmlXmlis };

        init(nisArray);
    }

    /**
     * Create a wrangler for an array of PMML model elements
     * 
     * @param pmmlXmlFiles
     * @throws Exception
     */
    public VoltDBJPMMLWrangler(File[] pmmlXmlFiles) throws Exception {
        super();

        if (pmmlXmlFiles == null) {
            throw new Exception("pmmlXmlFiles is null");
        }

        NamedInputStream[] pmmlXmlNIS = null;

        pmmlXmlNIS = new NamedInputStream[pmmlXmlFiles.length];

        for (int i = 0; i < pmmlXmlNIS.length; i++) {

            if (!pmmlXmlFiles[i].exists()) {
                throw new Exception(pmmlXmlFiles[i].getAbsolutePath() + " not found");
            }

            if (!pmmlXmlFiles[i].canRead()) {
                throw new Exception(pmmlXmlFiles[i].getAbsolutePath() + " not readable");
            }

            if (pmmlXmlFiles[i].isDirectory()) {
                throw new Exception(pmmlXmlFiles[i].getAbsolutePath() + " is a directory");
            }

            ByteArrayInputStream theBytes = new ByteArrayInputStream(
                    java.nio.file.Files.readAllBytes(pmmlXmlFiles[i].toPath()));
            pmmlXmlNIS[i] = new NamedInputStream(pmmlXmlFiles[i].getName(), theBytes);
        }

        init(pmmlXmlNIS);

    }

    /**
     * Create a mode for one or more PMML model elements. Note that we make some
     * assumptions about pmmlXmlFileTable: 1. It has columns called FILENAME and
     * TEXT 2. Rows are in order.
     * 
     * @param pmmlXmlFileTable
     * @throws Exception
     */
    public VoltDBJPMMLWrangler(VoltTable pmmlXmlFileTable) throws Exception {

        NamedInputStream[] pmmlXmlFiles = null;
        HashMap<String, StringBuffer> xmlText = new HashMap<String, StringBuffer>();

        while (pmmlXmlFileTable.advanceRow()) {

            String filename = pmmlXmlFileTable.getString("FILENAME");
            String text = pmmlXmlFileTable.getString("TEXT");

            StringBuffer thisFile = xmlText.get(filename);

            if (thisFile == null) {
                thisFile = new StringBuffer();
            }

            thisFile.append(text);
            thisFile.append(System.lineSeparator());
        }

        pmmlXmlFiles = new NamedInputStream[xmlText.size()];

        for (int i = 0; i < pmmlXmlFiles.length; i++) {
            StringBuffer thisBuffer = xmlText.get(pmmlXmlFiles[i].getName());
            ByteArrayInputStream theBytes = new ByteArrayInputStream(
                    thisBuffer.toString().getBytes(StandardCharsets.UTF_8));
            pmmlXmlFiles[i] = new NamedInputStream(pmmlXmlFiles[i].getName(), theBytes);
        }

        init(pmmlXmlFiles);
    }

    /**
     * Create PMML engines for our metadata. Note that this method is horribly
     * slow. Expect >500ms.
     * 
     * @param pmmlXmlFiles
     * @throws Exception
     */
    private void init(NamedInputStream[] pmmlXmlFiles) throws Exception {

        for (int i = 0; i < pmmlXmlFiles.length; i++) {

            PMML newPMML = PMMLUtil.unmarshal(pmmlXmlFiles[i].getIs());

            Evaluator evaluator = (Evaluator) modelEvaluatorFactory.newModelEvaluator(newPMML);

            pmmlEvaluators.put(pmmlXmlFiles[i].getName(), evaluator);

        }

    }

    /**
     * Return an empty VoltTable with the right structure for a 
     * given pmmlFileName
     * @param pmmlFileName
     * @return an empty VoltTable with the right structure
     * @throws Exception
     */
    public VoltTable getEmptyTable(String pmmlFileName) throws Exception {

        VoltTable t = null;

        Evaluator evaluator = pmmlEvaluators.get(pmmlFileName);

        if (evaluator == null) {
            throw new Exception("Model " + pmmlFileName + " not found");
        }

        List<InputField> inputFields = evaluator.getInputFields();

        VoltTable.ColumnInfo[] newTabColumns = new VoltTable.ColumnInfo[inputFields.size()];

        for (int i = 0; i < inputFields.size(); i++) {
            InputField inField = inputFields.get(i);
            VoltType vt = mapPmmlDatatypeToVoltDBDatatype(inField.getDataType());
            newTabColumns[i] = new VoltTable.ColumnInfo(inField.getName().getValue(), vt);
        }

        t = new VoltTable(newTabColumns);

        return t;
    }

    /**
     * Run a model using input daya we provide.
     * 
     * @param pmmlFileName
     *            The short file name of the model e.g. 'tree.xml'
     * @param inputParams
     *            A VoltTable containing a single row of input params
     * @return A VoltTable array containing a row of processing results and a
     *         second table containing a row of output fields
     * @throws Exception
     */
    public VoltTable[] runModel(String pmmlFileName, VoltTable inputParams) throws Exception {

        Evaluator evaluator = pmmlEvaluators.get(pmmlFileName);

        if (evaluator == null) {
            throw new Exception("Model " + pmmlFileName + " not found");
        }

        List<InputField> inputFields = evaluator.getInputFields();
        Map<FieldName, FieldValue> arguments = new LinkedHashMap<FieldName, FieldValue>();

        // Sanity check input params

        if (inputParams == null) {
            throw new Exception("VoltTable inputParams can't be null");
        }

        if (inputParams.getRowCount() != 1) {
            throw new Exception("VoltTable inputParams must have one row");
        }

        if (inputParams.getColumnCount() != inputFields.size()) {
            throw new Exception("VoltTable inputParams must match length of inputFields. inputParams has "
                    + inputParams.getColumnCount() + " columns, expect " + inputFields.size());
        }

        inputParams.advanceRow();
        for (InputField inputField : inputFields) {
            mapVoltparamToPmmlParam(inputParams, arguments, inputField);
        }

        Map<FieldName, ?> result = evaluator.evaluate(arguments);

        // Processing results
        // Retrieving the values of target fields (ie. primary results):
        List<TargetField> targetFields = evaluator.getTargetFields();
        VoltTable resultTable = mapPmmlTargetFieldsToVoltTable(result, targetFields);

        // other fields
        List<OutputField> outputFields = evaluator.getOutputFields();
        VoltTable otherTable = mapPmmlOutputFieldsToVoltTable(result, outputFields);

        VoltTable[] outputParams = { resultTable, otherTable };

        return outputParams;

    }

    /**
     * Eccentric method that returns a String containing a java class to run a
     * JPMML function. Idea is you run it in debug and then copy and paste the code...
     * @param pmmlFileName
     * @return
     * @throws Exception
     */
    public String getUDF(String pmmlFileName) throws Exception {

        StringBuffer udf = new StringBuffer();
        String[] p = pmmlFileName.split(Pattern.quote("."));
        String modelname = p[0];

        Evaluator evaluator = pmmlEvaluators.get(pmmlFileName);

        if (evaluator == null) {
            throw new Exception("Model " + pmmlFileName + " not found");
        }

        List<InputField> inputFields = evaluator.getInputFields();
        List<TargetField> targetFields = evaluator.getTargetFields();
        
        String parametersAndTypes = getParametersAndTypesFromInputFields(inputFields);
        String parameters = getParametersFromInputFields(inputFields);
        String returnDataType = "String";
        String returnDataTypeMethod = "getString";
        
        if (targetFields.size() == 1) {
            returnDataType = mapPmmlDatatypeToJavaDatatype(targetFields.get(0).getDataType());
            returnDataTypeMethod = mapPmmlDatatypeToVoltDBGetMethod(targetFields.get(0).getDataType());
        }

        udf.append("package jppml.generated;");
        udf.append(System.lineSeparator());
        udf.append("");
        udf.append(System.lineSeparator());
        udf.append("import org.voltdb.VoltTable;");
        udf.append(System.lineSeparator());
        udf.append("import org.voltdb.jpmml.JPMMLImpl;");
        udf.append(System.lineSeparator());
        udf.append("import org.voltdb.jpmml.VoltDBJPMMLWrangler;");
        udf.append(System.lineSeparator());
        udf.append("");
        udf.append(System.lineSeparator());
        udf.append("// This code is generated by getUDF");
        udf.append(System.lineSeparator());
        udf.append("public class " +  Character.toUpperCase(modelname.charAt(0)) + modelname.substring(1) + "Procedure {");
        udf.append(System.lineSeparator());
        udf.append("");
        udf.append(System.lineSeparator());
        udf.append("    public "+returnDataType+" " + Character.toLowerCase(modelname.charAt(0)) + modelname.substring(1)  + "(" + parametersAndTypes + ") {");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("        final String modelName = \"" + pmmlFileName + "\";");
        udf.append(System.lineSeparator());
        udf.append("        VoltTable[] pmmlOut = null;");
        udf.append(System.lineSeparator());
        udf.append("        JPMMLImpl i = null;");
        udf.append(System.lineSeparator());
        udf.append("        VoltDBJPMMLWrangler w = null;");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("        try {");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("            i = JPMMLImpl.getInstance();");
        udf.append(System.lineSeparator());
        udf.append("            w = i.getPool().borrowObject();");
        udf.append(System.lineSeparator());
        udf.append("            VoltTable paramtable = w.getEmptyTable(modelName);");
        udf.append(System.lineSeparator());
        udf.append("            paramtable.addRow(" + parameters + ");");
        udf.append(System.lineSeparator());
        udf.append("            pmmlOut = w.runModel(modelName, paramtable);");
        udf.append(System.lineSeparator());
        
        
        //TODO need to return correct data type
        if (targetFields.size() == 1) {
            udf.append("            return pmmlOut[0]."+returnDataTypeMethod+"(0);");
        } else {
            // If we are returning more than 1 value we throw our
            // hands in the air and return a String of the entire row.
            udf.append("            return pmmlOut[0].toString();");
        }
       
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("        } catch (Exception e) {");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        //TODO better error handling
        udf.append("           System.err.println(e.getMessage());");
        udf.append(System.lineSeparator());
        udf.append("           return null;");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("        } finally {");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("          if (i != null && w != null)");
        udf.append(System.lineSeparator());
        udf.append("              {");
        udf.append(System.lineSeparator());
        udf.append("              i.getPool().returnObject(w);");
        udf.append(System.lineSeparator());
        udf.append("              }");
        udf.append(System.lineSeparator());
        udf.append(System.lineSeparator());
        udf.append("        }");
        udf.append(System.lineSeparator());
        udf.append("    } ");
        udf.append(System.lineSeparator());
        udf.append("");
        udf.append(System.lineSeparator());
        udf.append("}");
        udf.append(System.lineSeparator());
        return udf.toString();

    }

    public static String mapPmmlDatatypeToVoltDBGetMethod(DataType dataType) throws Exception {
        String getMethod = "get";

        switch (dataType) {
        case FLOAT:
            getMethod = "getDouble";
            break;
        case DOUBLE:
            getMethod = "getDouble";
            break;
        case INTEGER:
            getMethod = "getLong";
            break;
        case STRING:
            getMethod = "getString";
            break;
        case DATE:
        case DATE_TIME:
            getMethod = "getTimestampAsTimestamp";
            break;

        // TODO
        case BOOLEAN:
        default:
            throw new Exception("PMML " + dataType.toString() + " not supported");
        }
        return getMethod;
   }

    public static  String getParametersAndTypesFromInputFields(List<InputField> inputFields) throws Exception {
        StringBuffer b = new StringBuffer();

        String comma = "";
        for (int i = 0; i < inputFields.size(); i++) {
            InputField inField = inputFields.get(i);
            b.append(comma);
            comma = ", ";
            b.append(mapPmmlDatatypeToJavaDatatype(inField.getDataType()));
            b.append(' ');
            b.append(inField.getName().toString().toLowerCase());

        }

        return b.toString();
    }

    public static String getParametersFromInputFields(List<InputField> inputFields) {
        StringBuffer b = new StringBuffer();

        String comma = "";
        for (int i = 0; i < inputFields.size(); i++) {
            InputField inField = inputFields.get(i);
            b.append(comma);
            comma = ", ";
            b.append(inField.getName().toString().toLowerCase());

        }

        return b.toString();
    }

    /**
     * map PMML output to a VoltTable
     * 
     * @param result
     * @param outputFields
     * @return A VoltTable containing 'result' in VoltDB form.
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private VoltTable mapPmmlOutputFieldsToVoltTable(Map<FieldName, ?> result, List<OutputField> outputFields)
            throws Exception {

        if (outputFields == null || outputFields.isEmpty()) {
            return nvlVoltTable(null);
        }

        // First - figure out shape of table
        ArrayList<VoltTable.ColumnInfo> outCols = new ArrayList<VoltTable.ColumnInfo>();

        for (OutputField outputField : outputFields) {
            FieldName targetFieldName = outputField.getName();
            DataType targetFieldDataType = outputField.getDataType();
            VoltType outputTableDataType = mapPmmlDatatypeToVoltDBDatatype(targetFieldDataType);

            // System.out.println(targetFieldName.toString());
            outCols.add(new VoltTable.ColumnInfo(targetFieldName.toString(), outputTableDataType));
        }

        VoltTable.ColumnInfo[] outColsAsArray = new VoltTable.ColumnInfo[outCols.size()];
        outColsAsArray = outCols.toArray(outColsAsArray);

        // Create second output VoltTable
        VoltTable outputTable = new VoltTable(outColsAsArray);

        // Now add row
        ArrayList newRowValues = new ArrayList();

        for (OutputField outputField : outputFields) {
            FieldName outputFieldName = outputField.getName();
            Object outputFieldValue = result.get(outputFieldName);

            outputFieldValue = EvaluatorUtil.decode(outputFieldValue);

            newRowValues.add(outputFieldValue);
        }

        Object[] newRowValuesAsArray = new Object[outputFields.size()];
        newRowValuesAsArray = newRowValues.toArray(newRowValuesAsArray);
        outputTable.addRow(newRowValuesAsArray);

        return outputTable;
    }

    /**
     * Map target fields to VoltTable
     * 
     * @param result
     * @param targetFields
     * @return A VoltTable containing 'target fields' in VoltDB form.
     * @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private VoltTable mapPmmlTargetFieldsToVoltTable(Map<FieldName, ?> result, List<TargetField> targetFields)
            throws Exception {

        int unknownFieldId = 1;

        // First - figure out shape of table
        ArrayList<VoltTable.ColumnInfo> outCols = new ArrayList<VoltTable.ColumnInfo>();

        for (TargetField targetField : targetFields) {
            FieldName targetFieldName = targetField.getName();
            DataType targetFieldDataType = targetField.getDataType();
            VoltType outputTableDataType = mapPmmlDatatypeToVoltDBDatatype(targetFieldDataType);

            if (targetFieldName == null) {
                targetFieldName = new FieldName("C" + unknownFieldId++);
            }

            outCols.add(new VoltTable.ColumnInfo(targetFieldName.toString(), outputTableDataType));
        }

        VoltTable.ColumnInfo[] outColsAsArray = new VoltTable.ColumnInfo[outCols.size()];
        outColsAsArray = outCols.toArray(outColsAsArray);
        // Create first output VoltTable
        VoltTable resultTable = new VoltTable(outColsAsArray);

        // Now add row

        ArrayList newRowValues = new ArrayList();

        for (TargetField targetField : targetFields) {
            FieldName targetFieldName = targetField.getName();
            Object targetFieldValue = result.get(targetFieldName);
            targetFieldValue = EvaluatorUtil.decode(targetFieldValue);

            newRowValues.add(targetFieldValue);
        }

        Object[] newRowValuesAsArray = new Object[targetFields.size()];
        newRowValuesAsArray = newRowValues.toArray(newRowValuesAsArray);
        resultTable.addRow(newRowValuesAsArray);
        resultTable.advanceRow();

        // TODO
        // Decoding a complex value to a Java primitive value:
        // if(targetFieldValue instanceof Computable){
        // Computable computable = (Computable)targetFieldValue;
        //
        // targetFieldValue = computable.getResult();
        // }
        //
        // Retrieving the values of output fields (ie. secondary
        // results):
        return resultTable;
    }

    /**
     * Map PMML data type to VoltDB data type.
     * 
     * @param targetFieldDataType
     * @return
     * @throws Exception
     */
    public static VoltType mapPmmlDatatypeToVoltDBDatatype(DataType targetFieldDataType) throws Exception {

        VoltType outputTableDataType = VoltType.NULL;

        switch (targetFieldDataType) {
        case FLOAT:
            outputTableDataType = VoltType.FLOAT;
            break;
        case DOUBLE:
            outputTableDataType = VoltType.FLOAT;
            break;
        case INTEGER:
            outputTableDataType = VoltType.BIGINT;
            break;
        case STRING:
            outputTableDataType = VoltType.STRING;
            break;
        case DATE:
        case DATE_TIME:
            outputTableDataType = VoltType.TIMESTAMP;
            break;

        // TODO
        case BOOLEAN:
        default:
            throw new Exception("PMML " + targetFieldDataType.toString() + " not supported");
        }
        return outputTableDataType;
    }
    
    /**
     * Return a String containing a Java data type that matches targetFieldDataType
     * @param targetFieldDataType
     * @return a String containing a Java data type that matches targetFieldDataType
     * @throws Exception
     */
    public static String mapPmmlDatatypeToJavaDatatype(DataType targetFieldDataType) throws Exception {

        String outputDataType = "Object";

        switch (targetFieldDataType) {
        case FLOAT:
            outputDataType = "double";
            break;
        case DOUBLE:
            outputDataType =  "double";
            break;
        case INTEGER:
            outputDataType = "long";
            break;
        case STRING:
            outputDataType = "String";
            break;
        case DATE:
        case DATE_TIME:
            outputDataType = "TimestampType";
            break;

        // TODO
        case BOOLEAN:
        default:
            throw new Exception("PMML " + targetFieldDataType.toString() + " not supported");
        }
        return outputDataType;
    }
    
    /**
     * Map VoltDB data type to PMML data type.
     * 
     * @param inputParams
     * @param arguments
     * @param inputField
     * @throws Exception
     */
    public static void mapVoltparamToPmmlParam(VoltTable inputParams, Map<FieldName, FieldValue> arguments,
            InputField inputField) throws Exception {

        FieldName inputFieldName = inputField.getName();

        int colId = inputParams.getColumnIndex(inputFieldName.toString());

        if (colId < 0) {
            throw new Exception("parameter " + inputFieldName.toString() + " not found in " + inputParams);
        }

        VoltType paramType = inputParams.getColumnType(colId);

        Object theValue = null;

        switch (paramType) {
        case STRING:
            theValue = inputParams.getString(colId);
            break;
        case BIGINT:
        case SMALLINT:
        case TINYINT:
        case INTEGER:
            theValue = inputParams.getLong(colId);
            break;
        case FLOAT:
            theValue = inputParams.getDouble(colId);
            break;
        case DECIMAL:
            BigDecimal bd = inputParams.getDecimalAsBigDecimal(colId);
            theValue = new Double(bd.doubleValue());
            break;
        case TIMESTAMP:
            TimestampType tt = inputParams.getTimestampAsTimestamp(colId);
            Timestamp ts = tt.asJavaTimestamp();
            theValue = ts;
            break;
        // TODO
        case VOLTTABLE:
        case BOOLEAN:
        case VARBINARY:
        case GEOGRAPHY:
        case GEOGRAPHY_POINT:

        default:
            throw new Exception("VoltType " + paramType.toString() + " not supported");
        }

        FieldValue inputFieldValue = inputField.prepare(theValue);
        arguments.put(inputFieldName, inputFieldValue);
    }

    /**
     * VoltDB doesn't like it if an array of VoltTable has a null element. Make
     * sure this doesn't happen.
     * 
     * @param theTable
     * @return theTable or a stub table if theTable is null
     */
    private VoltTable nvlVoltTable(VoltTable theTable) {

        if (theTable == null) {
            theTable = new VoltTable(new VoltTable.ColumnInfo("A_COLUMN", VoltType.STRING));
        }

        return theTable;
    }

    /**
     * Class use to associate a name with an input stream.
     * 
     * @author drolfe
     *
     */
    private class NamedInputStream {

        public String name = null;
        public InputStream is = null;

        public NamedInputStream(String name, InputStream is) {
            super();
            this.name = name;
            this.is = is;
        }

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @return the is
         */
        public InputStream getIs() {
            return is;
        }
    }
}
