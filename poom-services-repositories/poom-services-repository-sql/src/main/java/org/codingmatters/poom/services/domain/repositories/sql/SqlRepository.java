package org.codingmatters.poom.services.domain.repositories.sql;

import org.codingmatters.poom.services.domain.exceptions.RepositoryException;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.UUID;

public abstract class SqlRepository<V, Q> implements Repository<V, Q> {

    abstract protected PreparedStatement prepareInsert(Connection connection, Entity<V> entity) throws SQLException;
    abstract protected PreparedStatement prepareRetrieve(Connection connection, String id) throws SQLException;
    abstract protected PreparedStatement prepareUpdate(Connection connection, Entity<V> entity) throws SQLException;
    abstract protected PreparedStatement prepareDelete(Connection connection, Entity<V> entity) throws SQLException;
    abstract protected PreparedStatement prepareCount(Connection connection, Q query) throws SQLException;
    abstract protected PreparedStatement prepareSearch(Connection connection, Q query, long startIndex, long endIndex) throws SQLException;
    abstract protected Entity<V> fromResultSet(ResultSet rs) throws SQLException;

    private final DataSource dataSource;

    protected SqlRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Entity<V> create(V withValue) throws RepositoryException {
        Entity<V> entity = new ImmutableEntity<>(UUID.randomUUID().toString(), BigInteger.ONE, withValue);
        return withConnectionForEntity(connection -> {
            int affectedRows = this.prepareInsert(connection, entity).executeUpdate();
            if(affectedRows != 1) {
                throw new RepositoryException("something went wrong while creating entity " + entity + ", there where " + affectedRows + " affected rows");
            }
            return entity;
        });
    }


    @Override
    public Entity<V> retrieve(String id) throws RepositoryException {
        return this.withConnectionForEntity(connection -> {
            ResultSet rs = this.prepareRetrieve(connection, id).executeQuery();
            if(rs.next()) {
                return this.fromResultSet(rs);
            } else {
                return null;
            }
        });
    }

    @Override
    public Entity<V> update(Entity<V> entity, V withValue) throws RepositoryException {
        Entity<V> updated = new ImmutableEntity<>(entity.id(), entity.version().add(BigInteger.ONE), withValue);
        return this.withConnectionForEntity(connection -> {
            int affectedRows = this.prepareUpdate(connection, updated).executeUpdate();
            if(affectedRows != 1) {
                throw new RepositoryException("something went wrong while updating entity " + updated + ", there where " + affectedRows + " affected rows");
            }
            return updated;
        });
    }

    @Override
    public void delete(Entity<V> entity) throws RepositoryException {
        this.withConnectionForEntity(connection -> {
            int affectedRows = this.prepareDelete(connection, entity).executeUpdate();
            if(affectedRows != 1) {
                throw new RepositoryException("something went wrong while deleting entity " + entity + ", there where " + affectedRows + " affected rows");
            }
            return entity;
        });
    }

    @Override
    public PagedEntityList<V> all(long startIndex, long endIndex) throws RepositoryException {
        return this.search(null, startIndex, endIndex);
    }

    @Override
    public PagedEntityList<V> search(Q query, long startIndex, long endIndex) throws RepositoryException {
        return this.withConnectionForList(connection -> {
            ResultSet countRs = this.prepareCount(connection, query).executeQuery();
            countRs.next();
            long count = countRs.getLong(1);
            if( count > 0) {
                Collection<Entity<V>> page = new LinkedList<>();

                ResultSet rs = this.prepareSearch(connection, query, startIndex, endIndex).executeQuery();
                while(rs.next()) {
                    page.add(this.fromResultSet(rs));
                }

                return new PagedEntityList.DefaultPagedEntityList<>(startIndex, endIndex, count, page);
            } else {
                return new PagedEntityList.DefaultPagedEntityList<>(startIndex, endIndex, count, Collections.emptyList());
            }
        });
    }

    @FunctionalInterface
    private interface EntityOperation<V> {
        Entity<V> operate(Connection connection) throws RepositoryException, SQLException;
    }

    @FunctionalInterface
    private interface PagedEntityListOperation<V> {
        PagedEntityList<V> operate(Connection connection) throws RepositoryException, SQLException;
    }

    private Entity<V> withConnectionForEntity(EntityOperation<V> entityOperation) throws RepositoryException {
        try(Connection connection = this.dataSource.getConnection()) {
            return entityOperation.operate(connection);
        } catch (SQLException e) {
            throw new RepositoryException("error connecting to SQL store", e);
        }
    }

    private PagedEntityList<V> withConnectionForList(PagedEntityListOperation<V> entityOperation) throws RepositoryException {
        try(Connection connection = this.dataSource.getConnection()) {
            return entityOperation.operate(connection);
        } catch (SQLException e) {
            throw new RepositoryException("error connecting to SQL store", e);
        }
    }
}
