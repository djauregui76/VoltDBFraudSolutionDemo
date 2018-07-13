#!/bin/sh -x 
for  i in ../../jpmml-util.jar /Users/drolfe/.m2/repository/org/jpmml/pmml-evaluator/1.4.1/pmml-evaluator-1.4.1.jar /Users/drolfe/.m2/repository/org/jpmml/pmml-model/1.4.1/pmml-model-1.4.1.jar /Users/drolfe/.m2/repository/com/google/guava/guava/24.0-jre/guava-24.0-jre.jar /Users/drolfe/Desktop/EclipseWorkspace/jpmml-util/lib/commons-pool2-2.5.0.jar
do
	#cp $i /Users/drolfe/Desktop/EclipseWorkspace/jpmml-util/lib
	cp $i /Users/drolfe/Desktop/InstallsOfVolt/voltdb-ent-8.0/lib
done


