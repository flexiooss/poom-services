package org.codingmatters.poom.services.domain.repositories.sql;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import org.codingmatters.poom.services.domain.repositories.Repository;
import org.codingmatters.poom.servives.domain.entities.Entity;
import org.codingmatters.poom.servives.domain.entities.ImmutableEntity;
import org.codingmatters.poom.servives.domain.entities.PagedEntityList;
import org.codingmatters.value.objects.demo.books.Book;
import org.codingmatters.value.objects.demo.books.Person;
import org.codingmatters.value.objects.demo.books.json.BookReader;
import org.codingmatters.value.objects.demo.books.json.BookWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SingleTableWithIndicesSqlRepositoryTest {

    @Rule
    public DataSourceProvider dataSourceProvider = new DataSourceProvider();

    private Repository<Book, Book> repository;

    @Before
    public void setUp() throws Exception {
        this.dataSourceProvider.dataSource().getConnection().createStatement().execute("create table books (" +
                "id VARCHAR(1024)," +
                "version BIGINT," +
                "value LONGVARCHAR" +
                ")");

        this.repository = new SingleTableWithIndicesSqlRepository<Book, Book>(this.dataSourceProvider.dataSource(), "books", new JsonFactory()) {
            @Override
            protected void write(Book value, JsonGenerator to) throws IOException {
                new BookWriter().write(to, value);
            }

            @Override
            protected Book read(JsonParser parser) throws IOException {
                return new BookReader().read(parser);
            }
        };
    }

    @Test
    public void create() throws Exception {
        Entity<Book> entity = this.repository.create(Book.builder()
                .name("The Lord of the Rings")
                .author(Person.builder().name("J. R. R. Tolkien").build())
                .tags(Book.Tags.LITERATURE)
                .build());

        try(Connection connection = this.dataSourceProvider.dataSource().getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("select * from books where id = '" + entity.id() + "'");

            assertThat(rs.next(), is(true));
            assertThat(rs.getString("id"), is(entity.id()));
            assertThat(rs.getBigDecimal("version"), is(BigDecimal.valueOf(1)));
            assertThat(rs.getString("value"), is(this.json(entity.value())));
        }
    }

    @Test
    public void retrieve() throws Exception {
        Book book = Book.builder()
                .name("The Lord of the Rings")
                .author(Person.builder().name("J. R. R. Tolkien").build())
                .tags(Book.Tags.LITERATURE)
                .build();
        String id = this.repository.create(book).id();

        assertThat(
                this.repository.retrieve(id),
                is(new ImmutableEntity<>(id, BigInteger.ONE, book))
        );
    }

    @Test
    public void update() throws Exception {
        Entity<Book> entity = this.repository.create(Book.builder()
                .name("The Lord of the Rings")
                .author(Person.builder().name("J. R. R. Tolkien").build())
                .tags(Book.Tags.LITERATURE)
                .build());

        Entity<Book> updated = this.repository.update(entity, Book
                .from(entity.value())
                .name("changed name")
                .build());

        try(Connection connection = this.dataSourceProvider.dataSource().getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("select * from books where id = '" + entity.id() + "'");

            assertThat(rs.next(), is(true));
            assertThat(rs.getString("id"), is(entity.id()));
            assertThat(rs.getBigDecimal("version"), is(BigDecimal.valueOf(2)));
            assertThat(rs.getString("value"), is(this.json(updated.value())));
        }
    }

    @Test
    public void delete() throws Exception {
        Entity<Book> entity = this.repository.create(Book.builder()
                .name("The Lord of the Rings")
                .author(Person.builder().name("J. R. R. Tolkien").build())
                .tags(Book.Tags.LITERATURE)
                .build());
        this.repository.delete(entity);

        try(Connection connection = this.dataSourceProvider.dataSource().getConnection()) {
            ResultSet rs = connection.createStatement().executeQuery("select * from books where id = '" + entity.id() + "'");
            assertThat(rs.next(), is(false));
        }
    }

    @Test
    public void all() throws Exception {
        for(int i = 0 ; i < 20 ; i++) {
            this.repository.create(Book.builder().name("" + i).build());
        }

        PagedEntityList<Book> page = this.repository.all(0, 9);

        assertThat(page.total(), is(20L));
        assertThat(page.size(), is(10));
        int index = 0;
        for (Entity<Book> entity : page) {
            assertThat(index + " th book", entity.value().name(), is("" + index));
            index++;
        }
    }

    private String json(Book book) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); JsonGenerator generator = jsonFactory.createGenerator(out)) {
            new BookWriter().write(generator, book);
            generator.close();
            return out.toString();
        }
    }

}