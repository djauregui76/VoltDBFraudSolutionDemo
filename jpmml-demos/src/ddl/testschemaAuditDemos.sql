
load classes  ../../jpmml-util.jar;


create table auditdata (ID  int primary key not null
,Age	 int 
,Employment	 varchar(30)
,Education	 varchar(30)
, Marital	 varchar(30)
,Occupation	 	varchar(30)
,Income	  float
,	Gender	    	varchar(30)
,Deductions		 float
,Hours		 float
,IGNORE_Accounts		 varchar(30)
,RISK_Adjustment		 varchar(30)
,TARGET_Adjusted		 varchar(30)
);

partition table auditdata on column id;

CREATE FUNCTION auditTree FROM METHOD jppml.generated.AuditTreeProcedure.auditTree;

CREATE PROCEDURE auditId
PARTITION ON TABLE auditdata COLUMN id 
 AS
select id, age, employment, education, marital, occupation, income, gender, deductions, hours
, auditTree(age, employment, education, marital, occupation, income, gender, deductions, hours) auditScore
from auditdata
where id = 3959980
order by id;

CREATE PROCEDURE 
   PARTITION ON TABLE auditdata COLUMN id
   FROM CLASS jppml.AuditTreeStoredProc;
  
  
  -- vi ~/.bashrc
  export KAFKA_HOME=/kafka/kafka_2.12-1.1.0
  export JAVA_HOME=/usr/java/jdk1.8.0_171-amd64
  export PATH=$PATH:$KAFKA_HOME/bin