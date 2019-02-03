package org.h2.expression.function.pojo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Set;
import java.util.TreeSet;
import static org.assertj.core.api.Assertions.*;

public class H2PojoAdapterTest {

    private Connection connection;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:testDb", "sa", "");
        connection.setAutoCommit(true);
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }


    @Test
    void testMemoryManagerMXBeanTable() throws Exception {
        try (Statement statement = connection.createStatement()) {
            String pojoTableAlias = "create alias MemoryManagerMXBeans as $$ \n" +
                    "import java.lang.management.ManagementFactory;\n" +
                    "import java.lang.management.MemoryManagerMXBean;\n" +
                    "import org.h2.expression.function.pojo.*;\n" +
                    "import java.sql.*;\n" +
                    "import java.util.Collections;\n" +
                    "@CODE\n" +
                    "    ResultSet getRuntimeStat(Connection connection) throws Exception{\n" +
                    "        return H2PojoAdapter.toTable(connection, new CollectionWraper<>(MemoryManagerMXBean.class," +
                    "           ManagementFactory::getMemoryManagerMXBeans, Collections.emptyMap()));\n" +
                    "    }\n" +
                    "\n$$";
            statement.executeUpdate(pojoTableAlias);
        }
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select * from MemoryManagerMXBeans()")) {
                int columnCount = assertResultSet(resultSet, new String[]{"memoryPoolNames", "name", "valid"});
                assertThat(columnCount).isGreaterThan(1);
            }
        }
    }

    @Test
    void testThreadMXBeansTable() throws Exception{
        try (Statement statement = connection.createStatement()){
            String pojoTableAlias = "create alias ThreadMxBean for " +
                    "\"org.h2.expression.function.pojo.ThreadMxBeanTableFunction.getRuntimeStat\"";
            statement.executeUpdate(pojoTableAlias);
        }
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select * from ThreadMxBean()")) {
                int columnCount = assertResultSet(resultSet, new String[]{"currentThreadCpuTime",
                        "currentThreadCpuTimeSupported","currentThreadUserTime","daemonThreadCount",
                        "objectMonitorUsageSupported","peakThreadCount","synchronizerUsageSupported",
                        "threadContentionMonitoringEnabled","threadContentionMonitoringSupported",
                        "threadCount","threadCpuTimeEnabled","threadCpuTimeSupported","totalStartedThreadCount"});
                assertThat(columnCount).isGreaterThan(1);

            }
        }
    }

    private int assertResultSet(ResultSet resultSet, String[] expectedColumns) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Set<String> columns = new TreeSet<>();
        for(int idx=1; idx<=columnCount;idx++){
            columns.add(metaData.getColumnName(idx));
        }
        while (resultSet.next()){
            for(int idx=1; idx<=columnCount;idx++){
                Object value = resultSet.getObject(idx);
                assertThat(value).isNotNull();
            }
        }
        assertThat(columns).contains(expectedColumns);
        return columnCount;
    }
}
