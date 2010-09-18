LIB_DIR="WEB-INF/lib"

#DEBUG="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=9000"

CLASSPATH=.:WEB-INF/classes:resources
for i in ${LIB_DIR}/*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done
CLASSPATH=${CLASSPATH}

java ${DEBUG} -classpath ${CLASSPATH} Hello
