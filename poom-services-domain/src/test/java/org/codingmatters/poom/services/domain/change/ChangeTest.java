package org.codingmatters.poom.services.domain.change;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by nelt on 6/23/17.
 */
public class ChangeTest {


    @Test
    public void building() throws Exception {
        Change<String> change = StringChange.from("this").to("that");

        assertThat(change.validate(), is(new Validation(true, "")));
        assertThat(change.applied(), is("this->that"));
    }


    static public class StringChange extends Change<String> {

        static public ChangeBuilder<String, StringChange> from(String currentValue) {
            return new ChangeBuilder<>(currentValue, StringChange::new);
        }

        private StringChange(String currentValue, String newValue) {
            super(currentValue, newValue);
        }

        @Override
        protected Validation validate() {
            return new Validation(true, "");
        }

        @Override
        public String applied() {
            return this.currentValue() + "->" + this.newValue();
        }
    }
}