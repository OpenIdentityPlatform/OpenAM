LIB_DIR="WEB-INF/lib"
CLASS_DIR="WEB-INF/classes"

CLASSPATH=.:WEB-INF/classes:resources
for i in ${LIB_DIR}/*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done
CLASSPATH=${CLASSPATH}

javac -classpath ${CLASSPATH} source/Hello.java -d ${CLASS_DIR}
