package com.civicdesk.common.id;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.LongJavaType;
import org.hibernate.type.descriptor.jdbc.BigIntJdbcType;
import org.hibernate.type.internal.BasicTypeImpl;

import java.util.Properties;

/**
 * Produces sequential numeric ids (e.g. {@code 10000001}, {@code 10000002}, …) drawn
 * from a database sequence, but stored in a {@code String} {@code @Id} column.
 *
 * <p>The rest of the codebase treats user/audit ids as opaque {@code String}s
 * (JWT {@code userId} claim, the Spring Security principal, repository keys, DTOs),
 * so keeping the id a {@code String} avoids a type change rippling through every layer
 * — yet the value is now a human-friendly running number instead of a UUID.
 *
 * <p>The id property is declared as {@code String}, but {@link SequenceStyleGenerator}
 * needs a numeric (integral) type to drive the sequence and its optimizer. We therefore
 * hand the parent a {@code Long} type in {@link #configure} and stringify the generated
 * value in {@link #generate}.
 *
 * <p>Sequences are native on H2 (tests); on MySQL — which has no native sequences —
 * Hibernate emulates one with a backing table, so the same mapping works on both.
 */
public class NumericStringSequenceGenerator extends SequenceStyleGenerator {

    @Override
    public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry)
            throws MappingException {
        super.configure(new BasicTypeImpl<>(LongJavaType.INSTANCE, BigIntJdbcType.INSTANCE),
                parameters, serviceRegistry);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return String.valueOf(super.generate(session, object));
    }
}
