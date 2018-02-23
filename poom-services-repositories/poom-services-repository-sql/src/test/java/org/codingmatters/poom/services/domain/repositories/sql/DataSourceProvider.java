package org.codingmatters.poom.services.domain.repositories.sql;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

public class DataSourceProvider extends ExternalResource {

    static private final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);

    private DataSource dataSource;

    public DataSource dataSource() {
        return this.dataSource;
    }

    @Override
    protected void before() throws Throwable {
        super.before();

        JDBCDataSource ds = new JDBCDataSource();
        ds.setUrl("jdbc:hsqldb:mem:" + UUID.randomUUID().toString());
        ds.setUser("sa");
        ds.setPassword("");

        this.dataSource = ds;
    }

    @Override
    protected void after() {
        try {
            this.dataSource.getConnection().createStatement().execute("SHUTDOWN");
        } catch (SQLException e) {
            log.error("failed shutting down test db");
        }
        super.after();
    }
}
