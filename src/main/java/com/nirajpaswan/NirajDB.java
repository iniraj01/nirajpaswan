package com.nirajpaswan;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Hibernate ka poora kaam yahi class sambhalti hai:
 * connection, SessionFactory, Session, Transaction, commit/rollback.
 *
 * <pre>
 * try (NirajDB db = NirajDB.connect(NirajConfig.mysql("studentdb", "root", "pass"), Student.class)) {
 *     db.save(new Student("Niraj", "n@x.com", 22));
 *     List&lt;Student&gt; all = db.findAll(Student.class);
 * }
 * </pre>
 */
public final class NirajDB implements AutoCloseable {

    private final SessionFactory sessionFactory;

    private NirajDB(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    // ------------------------------------------------------------------
    // Connect
    // ------------------------------------------------------------------

    /**
     * MySQL se connect karo aur entity classes register karo.
     *
     * @param config        login id / password / database ki settings
     * @param entityClasses aapki {@code @Entity} classes (jinki table banani hai)
     */
    public static NirajDB connect(NirajConfig config, Class<?>... entityClasses) {
        Configuration c = new Configuration();
        c.setProperty("hibernate.connection.driver_class", config.getDriver());
        c.setProperty("hibernate.connection.url", config.buildUrl());
        c.setProperty("hibernate.connection.username", config.getUsername());
        c.setProperty("hibernate.connection.password", config.getPassword());
        c.setProperty("hibernate.connection.pool_size", String.valueOf(config.getPoolSize()));
        c.setProperty("hibernate.hbm2ddl.auto", config.getDdlMode());
        c.setProperty("hibernate.show_sql", String.valueOf(config.isShowSql()));
        c.setProperty("hibernate.format_sql", "true");

        for (Class<?> entity : entityClasses) {
            c.addAnnotatedClass(entity);
        }

        try {
            return new NirajDB(c.buildSessionFactory());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Database se connect nahi ho paya. MySQL chal raha hai? username/password/database sahi hain? Reason: "
                            + rootMessage(e), e);
        }
    }

    // ------------------------------------------------------------------
    // Create / Update / Delete
    // ------------------------------------------------------------------

    /** Naya object insert karta hai. Auto-generated id object me set ho jati hai. */
    public <T> T save(T entity) {
        return tx(s -> {
            s.persist(entity);
            return entity;
        });
    }

    /** Bahut saare objects ek hi transaction me insert. Ek bhi fail hua to sab rollback. */
    public <T> List<T> saveAll(Collection<T> entities) {
        return tx(s -> {
            for (T e : entities) {
                s.persist(e);
            }
            return new ArrayList<>(entities);
        });
    }

    /** Purane object ko update karta hai (id set honi chahiye). Updated object return hota hai. */
    public <T> T update(T entity) {
        return tx(s -> s.merge(entity));
    }

    /** Object delete. */
    public <T> void delete(T entity) {
        tx(s -> {
            s.remove(s.contains(entity) ? entity : s.merge(entity));
            return null;
        });
    }

    /** Id se delete. Mila aur delete hua to true, nahi mila to false. */
    public <T> boolean deleteById(Class<T> type, Object id) {
        return tx(s -> {
            T found = s.find(type, id);
            if (found == null) {
                return false;
            }
            s.remove(found);
            return true;
        });
    }

    /** Table ka saara data delete (table rehti hai). Kitni rows gayi wo number return hota hai. */
    public <T> int deleteAll(Class<T> type) {
        String entityName = sessionFactory.getMetamodel().entity(type).getName();
        return tx(s -> s.createMutationQuery("delete from " + entityName).executeUpdate());
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    /** Primary key se ek object. Nahi mila to null. */
    public <T> T findById(Class<T> type, Object id) {
        return read(s -> s.find(type, id));
    }

    public <T> boolean exists(Class<T> type, Object id) {
        return findById(type, id) != null;
    }

    public <T> List<T> findAll(Class<T> type) {
        return read(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<T> q = cb.createQuery(type);
            q.select(q.from(type));
            return s.createQuery(q).getResultList();
        });
    }

    /** Page-by-page data. pageNo 0 se shuru hota hai. */
    public <T> List<T> findPage(Class<T> type, int pageNo, int pageSize) {
        return read(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<T> q = cb.createQuery(type);
            q.select(q.from(type));
            return s.createQuery(q)
                    .setFirstResult(pageNo * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
        });
    }

    /** Ek field ki value se dhundo: {@code findWhere(Student.class, "name", "Niraj")}. */
    public <T> List<T> findWhere(Class<T> type, String field, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(field, value);
        return findWhere(type, m);
    }

    /** Kai fields ek saath (AND): {@code findWhere(Student.class, Map.of("age", 22, "name", "Niraj"))}. */
    public <T> List<T> findWhere(Class<T> type, Map<String, Object> conditions) {
        return read(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<T> q = cb.createQuery(type);
            Root<T> root = q.from(type);
            q.select(root).where(predicates(cb, root, conditions));
            return s.createQuery(q).getResultList();
        });
    }

    /** Pehla matching object (ya empty). */
    public <T> Optional<T> findOne(Class<T> type, String field, Object value) {
        return findWhere(type, field, value).stream().findFirst();
    }

    public <T> long count(Class<T> type) {
        return read(s -> {
            CriteriaBuilder cb = s.getCriteriaBuilder();
            CriteriaQuery<Long> q = cb.createQuery(Long.class);
            q.select(cb.count(q.from(type)));
            return s.createQuery(q).getSingleResult();
        });
    }

    // ------------------------------------------------------------------
    // Custom HQL (jab upar ke methods kaafi na ho)
    // ------------------------------------------------------------------

    /**
     * Custom HQL SELECT. Parameters {@code ?1, ?2 ...} se pass hote hain.
     * <pre>db.query(Student.class, "from Student where age > ?1 order by name", 18)</pre>
     */
    public <T> List<T> query(Class<T> resultType, String hql, Object... params) {
        return read(s -> {
            Query<T> q = s.createQuery(hql, resultType);
            bind(q, params);
            return q.getResultList();
        });
    }

    /**
     * Custom HQL UPDATE / DELETE. Kitni rows badli wo return hota hai.
     * <pre>db.execute("update Student set age = ?1 where id = ?2", 23, 5L)</pre>
     */
    public int execute(String hql, Object... params) {
        return tx(s -> {
            var q = s.createMutationQuery(hql);
            bind(q, params);
            return q.executeUpdate();
        });
    }

    // ------------------------------------------------------------------
    // Advanced
    // ------------------------------------------------------------------

    /**
     * Kai kaam ek hi transaction me (jaise paisa ek account se doosre me).
     * Exception aayi to sab rollback.
     */
    public <R> R inTransaction(Function<Session, R> work) {
        return tx(work);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @Override
    public void close() {
        if (sessionFactory != null && sessionFactory.isOpen()) {
            sessionFactory.close();
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private <R> R tx(Function<Session, R> work) {
        try (Session session = sessionFactory.openSession()) {
            Transaction t = session.beginTransaction();
            try {
                R result = work.apply(session);
                t.commit();
                return result;
            } catch (RuntimeException e) {
                if (t.isActive()) {
                    t.rollback();
                }
                throw e;
            }
        }
    }

    private <R> R read(Function<Session, R> work) {
        try (Session session = sessionFactory.openSession()) {
            return work.apply(session);
        }
    }

    private static Predicate[] predicates(CriteriaBuilder cb, Root<?> root, Map<String, Object> conditions) {
        List<Predicate> list = new ArrayList<>();
        for (Map.Entry<String, Object> e : conditions.entrySet()) {
            Path<Object> path = root.get(e.getKey());
            list.add(e.getValue() == null ? cb.isNull(path) : cb.equal(path, e.getValue()));
        }
        return list.toArray(new Predicate[0]);
    }

    private static void bind(org.hibernate.query.CommonQueryContract q, Object[] params) {
        for (int i = 0; i < params.length; i++) {
            q.setParameter(i + 1, params[i]);
        }
    }

    private static String rootMessage(Throwable t) {
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t.getMessage();
    }
}
