
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
,Times	  float
,Amount float
,Classy float
);

CREATE STREAM DECISIONS PARTITION ON COLUMN USERID EXPORT TO TARGET EXPORTAUDITDATA (
   USERID integer NOT NULL,
   CLASSY float
);

CREATE VIEW DECISIONSLIST ( number, minimum, maximum )
   SELECT USERID,CLASSY,COUNT(*) FROM DECISIONS GROUP BY USERID,CLASSY;


partition table auditdata on column id;

CREATE FUNCTION auditTree FROM METHOD jppml.generated.AuditTreeProcedure.auditTree;

CREATE PROCEDURE auditId
PARTITION ON TABLE auditdata COLUMN id 
 AS
select id, age, employment, education, marital, occupation, income, gender, deductions, hours
, auditTree(age, employment, education, marital, occupation, income, gender, deductions, hours) auditScore
from auditdata
where id = ?
order by id;

--CREATE PROCEDURE 
--   PARTITION ON TABLE auditdata COLUMN id
--   FROM CLASS jppml.AuditTreeStoredProc;
  
  
  -- vi ~/.bashrc
  export KAFKA_HOME=/kafka/kafka_2.12-1.1.0
  export JAVA_HOME=/usr/java/jdk1.8.0_171-amd64
  export PATH=$PATH:$KAFKA_HOME/bin
  
CREATE PROCEDURE IMPORTAUDITDATA
AS  BEGIN  
Insert auditdata values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);
select id, age, employment, education, marital, occupation, income, gender, deductions, hours, auditTree(age, employment, education, marital, occupation, income, gender, deductions, hours) auditScore
from auditdata where id = 3959980 order by id;
END;
  -- Age, Employment, Education, Marital, Occupation, Income, Gender, Deductions, Hours, IGNORE_Accounts, RISK_Adjustment, TARGET_Adjusted

CREATE PROCEDURE IMPORTAUDITDATA Partition on TABLE auditdata COLUMN ID PARAMETER 0
AS  BEGIN  
Upsert INTO auditdata values (?,?,?,?,?,?,?,?,?,?,?,?,?);
INSERT into decisions (userid, classy) select id,classy from auditdata where id=?;
END;

DROP PROCEDURE IMPORTAUDITDATA
