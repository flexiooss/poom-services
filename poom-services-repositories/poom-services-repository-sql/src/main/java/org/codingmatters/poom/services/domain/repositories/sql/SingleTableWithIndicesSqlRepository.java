package org.codingmatters.poom.services.domain.repositories.sql;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class SingleTableWithIndicesSqlRepository<V, Q> extends SqlRepository<V, Q> {

    private final String tableName;
    private final JsonFactory jsonFactory;

    abstract protected void write(V value, JsonGenerator to) throws IOException;
    abstract protected V read(JsonParser parser) throws IOException;

    public SingleTableWithIndicesSqlRepository(DataSource dataSource, String tableName, JsonFactory jsonFactory) {
        super(dataSource);
        this.tableName = tableName;
        this.jsonFactory = jsonFactory;
    }

    @Override
    protected PreparedStatement prepareInsert(Connection connection, Entity<V> entity) throws SQLException {
        PreparedStatement result = connection.prepareStatement(String.format(
                "INSERT INTO %s (id, version, value) VALUES (?, ?, ?)",
                this.tableName
        ));
        result.setString(1, entity.id());
        result.setBigDecimal(2, new BigDecimal(entity.version()));
        try {
            result.setString(3, this.marchallValue(entity.value()));
        } catch (IOException e) {
            throw new SQLException("error marshalling value : " + entity.value(), e);
        }
        return result;
    }

    @Override
    protected PreparedStatement prepareRetrieve(Connection connection, String id) throws SQLException {
        PreparedStatement result = connection.prepareStatement(String.format(
                "SELECT id, version, value FROM %s WHERE id = ?",
                this.tableName
        ));
        result.setString(1, id);
        return result;
    }

    @Override
    protected PreparedStatement prepareUpdate(Connection connection, Entity<V> entity) throws SQLException {
        PreparedStatement result = connection.prepareStatement(String.format(
                "UPDATE %s SET version = ?, value = ? WHERE id = ?",
                this.tableName
        ));
        result.setBigDecimal(1, new BigDecimal(entity.version()));
        try {
            result.setString(2, this.marchallValue(entity.value()));
        } catch (IOException e) {
            throw new SQLException("error marshalling value : " + entity.value(), e);
        }
        result.setString(3, entity.id());
        return result;
    }

    @Override
    protected PreparedStatement prepareDelete(Connection connection, Entity<V> entity) throws SQLException {
        PreparedStatement result = connection.prepareStatement(String.format(
                "DELETE FROM %s WHERE id = ?",
                this.tableName
        ));
        result.setString(1, entity.id());
        return result;
    }

    @Override
    protected PreparedStatement prepareCount(Connection connection, Q query) throws SQLException {
        return connection.prepareStatement(String.format("SELECT count(*) FROM %s", this.tableName));
    }

    @Override
    protected PreparedStatement prepareSearch(Connection connection, Q query, long startIndex, long endIndex) throws SQLException {
        PreparedStatement result = connection.prepareStatement(String.format(
                "SELECT id, version, value FROM %s OFFSET ? LIMIT ?",
                this.tableName
        ));
        result.setLong(1, startIndex);
        result.setLong(2, endIndex - startIndex + 1);
        return result;
    }

    @Override
    protected Entity<V> fromResultSet(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        BigInteger version = rs.getBigDecimal("version").toBigInteger();
        String json = rs.getString("value");

        try {
            return new ImmutableEntity<>(id, version, this.unmarshallValue(json));
        } catch (IOException e) {
            throw new SQLException("error unmarshalling value", e);
        }
    }

    private V unmarshallValue(String json) throws IOException {
        try(JsonParser parser = this.jsonFactory.createParser(json)) {
            return this.read(parser);
        }
    }

    private String marchallValue(V value) throws IOException {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = this.jsonFactory.createGenerator(out)) {
            this.write(value, generator);
            generator.close();
            return out.toString();
        }
    }
}
