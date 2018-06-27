package org.codingmatters.poom.services.support;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class EnvProviderTest {

    @Rule
    public TemporaryFolder dir = new TemporaryFolder();

    Properties testEnv = new Properties();

    @Before
    public void setUp() throws Exception {
        EnvProvider.provider(s -> testEnv.getProperty(s));
    }

    @After
    public void tearDown() throws Exception {
        EnvProvider.reset();
        System.clearProperty("defined.with.property");
    }

    @Test
    public void givenVarExists__whenVarRequested__thenVarIsPresent() {
        this.testEnv.setProperty("EXISTS", "YES");
        assertTrue(Env.optional("EXISTS").isPresent());
    }

    @Test
    public void givenVarDoesntExists__whenVarRequested__thenVarIsNotPresent() {
        assertFalse(Env.optional("NO_SUCH_VAR").isPresent());
    }

    @Test
    public void givenVarDoesntExist_andVarFilePropertyExists__whenVarRequested__thenVarFileContentIsReturned() throws Exception {
        this.testEnv.setProperty("VAR_FILE", this.writeContent("YOP", this.dir.newFile()).getAbsolutePath());

        assertThat(Env.optional("VAR").get().asString(), is("YOP"));
    }

    @Test
    public void givenVarExist_andVarFilePropertyExists__whenVarRequested__thenVarFileContentIsReturned() throws Exception {
        this.testEnv.setProperty("VAR", "YIP");
        this.testEnv.setProperty("VAR_FILE", this.writeContent("YOP", this.dir.newFile()).getAbsolutePath());

        assertThat(Env.optional("VAR").get().asString(), is("YOP"));
    }

    @Test
    public void givenVarExist_andFileEndsWithNewLine_andVarFilePropertyExists__whenVarRequested__thenVarFileContentIsReturnedWithoutTrailingNewline() throws Exception {
        this.testEnv.setProperty("VAR", "YIP");
        this.testEnv.setProperty("VAR_FILE", this.writeContent("YOP\n", this.dir.newFile()).getAbsolutePath());

        assertThat(Env.optional("VAR").get().asString(), is("YOP"));
    }

    @Test
    public void givenFileDoesntExistsAndVarDoesntExist__whenPropertyExists__theyPropertyValueReturned() throws Exception {
        System.setProperty("defined.with.property", "from-prop");

        assertThat(Env.optional("DEFINED_WITH_PROPERTY").get().asString(), is("from-prop"));


    }

    @Test
    public void givenFileDoesntExistsAndVarExists__whenPropertyExists__theVarPreceeds() throws Exception {
        System.setProperty("defined.with.property", "from-prop");
        this.testEnv.setProperty("DEFINED_WITH_PROPERTY", "from-env");

        assertThat(Env.optional("DEFINED_WITH_PROPERTY").get().asString(), is("from-env"));
    }

    @Test
    public void givenFileExistsAndVarExists__whenPropertyExists__theFilePreceeds() throws Exception {
        System.setProperty("defined.with.property", "from-prop");
        this.testEnv.setProperty("DEFINED_WITH_PROPERTY", "from-env");
        this.testEnv.setProperty("DEFINED_WITH_PROPERTY_FILE", this.writeContent("from-file", this.dir.newFile()).getAbsolutePath());

        assertThat(Env.optional("DEFINED_WITH_PROPERTY").get().asString(), is("from-file"));
    }

    private File writeContent(String content, File f) throws IOException {
        try(OutputStream out = new FileOutputStream(f)) {
            out.write(content.getBytes());
            out.flush();
        }
        return f;
    }
}