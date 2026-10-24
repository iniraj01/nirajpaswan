# nirajpaswan

Hibernate + MySQL ka saara boilerplate (SessionFactory, Session, Transaction, commit/rollback) ek jar me.
Aapko bas **database ka login**, **entity class** aur **operation** likhna hai.

## 1. Jar banao (ek baar)

Maven chahiye (internet ke saath, kyunki dependencies download hoti hain):

```
mvn clean package
```

Ban jayegi: `target/nirajpaswan-1.0.0-all.jar`
Ye "fat jar" hai: Hibernate, MySQL driver, sab andar hai. Ye ek jar hi kaafi hai.

## 2. Apne project me jar add karo

- **IntelliJ**: File > Project Structure > Libraries > `+` > Java > jar chuno
- **Eclipse**: Project > Build Path > Add External JARs
- **Command line**: `javac -cp nirajpaswan-1.0.0-all.jar *.java` aur `java -cp ".;nirajpaswan-1.0.0-all.jar" Main` (Linux/Mac me `;` ki jagah `:`)
- **Maven project me**:
  ```
  mvn install:install-file -Dfile=target/nirajpaswan-1.0.0-all.jar -DgroupId=com.nirajpaswan -DartifactId=nirajpaswan -Dversion=1.0.0 -Dpackaging=jar
  ```
  phir apni pom.xml me dependency `com.nirajpaswan:nirajpaswan:1.0.0` daalo.

## 3. Entity banao (attributes yahan decide hote hain)

Dekho `examples/Student.java`. Jitne fields, utne columns. Table MySQL me khud ban jati hai.

## 4. Use karo

```java
NirajConfig config = NirajConfig.mysql("studentdb", "root", "password");
try (NirajDB db = NirajDB.connect(config, Student.class)) {
    db.save(new Student("Niraj", "n@x.com", 22));
    List<Student> all = db.findAll(Student.class);
}
```

Pura example: `examples/Main.java`

## Operations ki list

| Kaam | Method |
|---|---|
| Insert | `save(obj)`, `saveAll(list)` |
| Update | `update(obj)`, `execute("update ...")` |
| Delete | `delete(obj)`, `deleteById(Class, id)`, `deleteAll(Class)` |
| Id se dhundo | `findById(Class, id)`, `exists(Class, id)` |
| Sab data | `findAll(Class)`, `findPage(Class, pageNo, pageSize)` |
| Condition se | `findWhere(Class, "field", value)`, `findWhere(Class, Map)`, `findOne(...)` |
| Gino | `count(Class)` |
| Custom HQL | `query(Class, "from X where a > ?1", 5)` |
| Ek transaction me kai kaam | `inTransaction(session -> { ... })` |

## NirajConfig options

| Option | Default |
|---|---|
| host / port | localhost / 3306 |
| ddlMode | `update` (table na ho to ban jati hai, data safe rehta hai). `create` = har baar table naye sire se (data ud jata hai!) |
| showSql | false |
| poolSize | 10 |
| jdbcUrl | MySQL ka URL khud na dena ho to chhod do |

## Dhyan rakhne wali baatein

- Entity ke imports `jakarta.persistence.*` se hone chahiye (`javax` se nahi).
- Entity me khali (no-arg) constructor zaroori hai.
- Password code me likhna sirf practice ke liye theek hai. Real project me environment variable se lo: `System.getenv("DB_PASSWORD")`.
- MySQL server chalu hona chahiye. Database `studentdb` na ho to khud ban jayega.
