package org.codingmatters.poom.i18n.spec.gen;

import org.codingmatters.poom.i18n.bundle.spec.descriptors.ArgSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.BundleSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.MessageSpec;
import org.codingmatters.poom.i18n.bundle.spec.descriptors.ValueList;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BundleSpecValidator {

    public BundleValidity validate(BundleSpec spec) {
        if(! spec.opt().name().isPresent()) return invalid("must have a name");
        if(! spec.opt().defaultLocale().isPresent()) return invalid("must have a default locale");
        if(! this.parseableLocale(spec.defaultLocale())) return invalid("default locale is not parseable (must follow the <language>-<country>, i.e., fr or fr-FR");

        if(spec.opt().messages().isPresent()) {
            for (MessageSpec message : spec.messages()) {
                BundleValidity messageValidity = this.validate(message);
                if(! messageValidity.isValid()) return messageValidity;
            }
        }

        return valid();
    }

    private boolean parseableLocale(String locale) {
        if(locale.matches("[a-z]{2}") || locale.matches("[a-z]{2}-[A-Z]{2}")) return true;
        return false;
    }

    static private Pattern ARG_PATTERN = Pattern.compile("\\{(\\w+):\\w+\\}");

    private BundleValidity validate(MessageSpec message) {
        if(! message.opt().key().isPresent()) {
            return invalid(String.format("all messages must have a key"));
        }
        if(! message.opt().message().isPresent()) {
            return invalid(String.format("must provide a default message for key %s", message.key()));
        }
        if(message.opt().args().isPresent()) {
            for (ArgSpec arg : message.args()) {
                if(! arg.opt().name().isPresent()) return invalid(String.format("arguments for key %s must be named", message.key()));
                if(! arg.opt().type().isPresent()) return invalid(String.format("argument %s for key %s must be typed", arg.name(), message.key()));
            }
        }


        Matcher args = ARG_PATTERN.matcher(message.message());
        while(args.find()) {
            String argName = args.group(1);
            if(! message.opt().args().isPresent() || message.args().stream().noneMatch(argSpec -> argName.equals(argSpec.name()))) {
                return invalid(String.format("message for key %s uses undeclared argument : %s", message.key(), argName));
            }
        }

        return valid();
    }

    static public BundleValidity valid() {
        return new BundleValidity(true, null);
    }

    static public BundleValidity invalid(String message) {
        return new BundleValidity(false, message);
    }

    static public class BundleValidity {
        private final boolean valid;
        private final String message;

        public BundleValidity(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String message() {
            return message;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BundleValidity that = (BundleValidity) o;
            return valid == that.valid &&
                    Objects.equals(message, that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(valid, message);
        }

        @Override
        public String toString() {
            return "BundleValidity{" +
                    "valid=" + valid +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
