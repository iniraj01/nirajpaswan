import com.nirajpaswan.NirajConfig;
import com.nirajpaswan.NirajDB;

import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        // 1) Bas yahi do cheezein dena hai: login id/password + entity classes
        NirajConfig config = NirajConfig.mysql("studentdb", "root", "YOUR_PASSWORD")
                .showSql(true);                       // console me SQL dekhna ho to

        try (NirajDB db = NirajDB.connect(config, Student.class)) {

            // 2) CREATE
            Student s = db.save(new Student("Niraj", "niraj@example.com", 22));
            System.out.println("Saved: " + s);        // id auto-set ho chuki hai

            db.saveAll(List.of(
                    new Student("Amit", "amit@example.com", 21),
                    new Student("Riya", "riya@example.com", 23)));

            // 3) READ
            System.out.println("Sab: " + db.findAll(Student.class));
            System.out.println("Id=1: " + db.findById(Student.class, 1L));
            System.out.println("Naam se: " + db.findWhere(Student.class, "name", "Amit"));
            System.out.println("Kai conditions: " + db.findWhere(Student.class, Map.of("age", 23)));
            System.out.println("Total: " + db.count(Student.class));

            // 4) UPDATE
            s.setAge(25);
            db.update(s);

            // 5) Custom HQL
            List<Student> adults = db.query(Student.class,
                    "from Student where age > ?1 order by name", 21);
            System.out.println("21 se bade: " + adults);

            // 6) DELETE
            db.deleteById(Student.class, s.getId());
        }
    }
}
