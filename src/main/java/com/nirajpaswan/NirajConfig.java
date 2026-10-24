package com.nirajpaswan;

/**
 * Database connection ki settings.
 *
 * <pre>
 * NirajConfig cfg = NirajConfig.mysql("studentdb", "root", "mypassword");
 * </pre>
 *
 * Ya poora control chahiye to:
 * <pre>
 * NirajConfig cfg = NirajConfig.create()
 *         .host("localhost").port(3306)
 *         .database("studentdb")
 *         .username("root").password("mypassword")
 *         .ddlMode("update")      // create | update | validate | create-drop | none
 *         .showSql(true);
 * </pre>
 */
public final class NirajConfig {

    private String host = "localhost";
    private int port = 3306;
    private String database;
    private String username = "root";
    private String password = "";
    private String jdbcUrl;                         // optional: poora URL khud dena ho to
    private String driver = "com.mysql.cj.jdbc.Driver";
    private String ddlMode = "update";              // table na ho to khud ban jati hai
    private boolean showSql = false;
    private int poolSize = 10;

    private NirajConfig() {
    }

    public static NirajConfig create() {
        return new NirajConfig();
    }

    /** Sabse short tareeka: database naam, username, password. */
    public static NirajConfig mysql(String database, String username, String password) {
        return new NirajConfig().database(database).username(username).password(password);
    }

    public NirajConfig host(String host) {
        this.host = host;
        return this;
    }

    public NirajConfig port(int port) {
        this.port = port;
        return this;
    }

    public NirajConfig database(String database) {
        this.database = database;
        return this;
    }

    public NirajConfig username(String username) {
        this.username = username;
        return this;
    }

    public NirajConfig password(String password) {
        this.password = password == null ? "" : password;
        return this;
    }

    public NirajConfig jdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    public NirajConfig driver(String driver) {
        this.driver = driver;
        return this;
    }

    public NirajConfig ddlMode(String ddlMode) {
        this.ddlMode = ddlMode;
        return this;
    }

    public NirajConfig showSql(boolean showSql) {
        this.showSql = showSql;
        return this;
    }

    public NirajConfig poolSize(int poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    String buildUrl() {
        if (jdbcUrl != null && !jdbcUrl.isBlank()) {
            return jdbcUrl;
        }
        if (database == null || database.isBlank()) {
            throw new IllegalStateException("Database ka naam dena zaroori hai, e.g. NirajConfig.mysql(\"studentdb\", \"root\", \"pass\")");
        }
        // createDatabaseIfNotExist=true: database na ho to MySQL me khud ban jayega
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    String getDriver() {
        return driver;
    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    String getDdlMode() {
        return ddlMode;
    }

    boolean isShowSql() {
        return showSql;
    }

    int getPoolSize() {
        return poolSize;
    }
}
