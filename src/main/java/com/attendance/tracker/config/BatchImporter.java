package com.attendance.tracker.config;

import com.attendance.tracker.model.AppUser;
import com.attendance.tracker.model.ClassRoom;
import com.attendance.tracker.model.Role;
import com.attendance.tracker.model.Student;
import com.attendance.tracker.model.Subject;
import com.attendance.tracker.repository.ClassRoomRepository;
import com.attendance.tracker.repository.StudentRepository;
import com.attendance.tracker.repository.SubjectRepository;
import com.attendance.tracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AT-85: loads a real batch (classes, students, teachers and subjects) the first time the app starts.
 *
 * It only runs when BOTH are true:
 *  - the file seed-local/students.csv exists (columns: name,usn,branch), and
 *  - the database is brand new (only the admin account exists).
 * The file holds real student names, so it is NOT stored in git: it exists on your computer only.
 * If the file is missing, nothing happens.
 *
 * What is created:
 *  - one class per branch (for example AI&ML, CSE, ISE)
 *  - one student per row: roll number = the USN, username = branch letters + last 3 digits of the USN
 *    in lower case (1MP99AI123 becomes ai123), the same dummy password for everybody
 *  - six teachers t1 to t6 and five subjects in every class, shared out between the teachers
 * Everything is saved in ONE transaction: if anything is wrong, nothing is saved.
 */
@Component
public class BatchImporter {

    private static final Logger log = LoggerFactory.getLogger(BatchImporter.class);

    private static final Path FILE = Path.of("seed-local", "students.csv");

    // 1MP + two digit year + two letter branch code + three digits, for example 1MP99AI123
    private static final Pattern USN = Pattern.compile("^1MP\\d{2}([A-Z]{2})(\\d{3})$");

    // Dummy passwords for the demo. Users can change them on the Change password page.
    private static final String STUDENT_PASSWORD = "Student@123";
    private static final String TEACHER_PASSWORD = "Teacher@123";

    private static final int TEACHER_COUNT = 6;
    private static final String[] SUBJECTS = {
            "Software Engineering", "Database Management Systems", "Computer Networks",
            "Operating Systems", "Theory of Computation"};

    private final UserRepository users;
    private final ClassRoomRepository classes;
    private final SubjectRepository subjects;
    private final StudentRepository students;
    private final PasswordEncoder encoder;

    public BatchImporter(UserRepository users, ClassRoomRepository classes, SubjectRepository subjects,
                         StudentRepository students, PasswordEncoder encoder) {
        this.users = users;
        this.classes = classes;
        this.subjects = subjects;
        this.students = students;
        this.encoder = encoder;
    }

    /** One line of the file, already checked. */
    private record Row(String name, String usn, String branch, String username) { }

    /**
     * @return a short summary of what was loaded, or null when nothing was done
     * @throws IllegalStateException when the file has a problem (then nothing is saved)
     */
    @Transactional
    public String importIfPresent() {
        if (!Files.exists(FILE)) {
            log.info("No batch file at {}, so no batch data is loaded.", FILE.toAbsolutePath());
            return null;
        }
        if (users.count() != 1 || classes.count() > 0) {
            // the database already has data: leave it alone
            log.info("Batch file found, but the database already has data, so it is not loaded.");
            return null;
        }
        List<Row> rows = readRows();

        // classes: one per branch, in the order they first appear in the file
        Map<String, ClassRoom> classByBranch = new LinkedHashMap<>();
        for (Row row : rows) {
            classByBranch.computeIfAbsent(row.branch(), name -> classes.save(new ClassRoom(name)));
        }

        // teachers t1 .. t6
        List<AppUser> teachers = new ArrayList<>();
        for (int i = 1; i <= TEACHER_COUNT; i++) {
            teachers.add(users.save(new AppUser("t" + i, encoder.encode(TEACHER_PASSWORD),
                    "Teacher " + i, Role.TEACHER)));
        }

        // subjects: every class gets the same five, teachers are used in turn (t1, t2, ... t6, t1, ...)
        int next = 0;
        int subjectCount = 0;
        for (ClassRoom classRoom : classByBranch.values()) {
            for (String subjectName : SUBJECTS) {
                subjects.save(new Subject(subjectName, classRoom, teachers.get(next % TEACHER_COUNT)));
                next++;
                subjectCount++;
            }
        }

        // students: a login account plus the student profile (roll number and class)
        for (Row row : rows) {
            AppUser login = users.save(new AppUser(row.username(), encoder.encode(STUDENT_PASSWORD),
                    row.name(), Role.STUDENT));
            students.save(new Student(row.usn(), login, classByBranch.get(row.branch())));
        }

        return "Loaded " + rows.size() + " students in " + classByBranch.size() + " classes, "
                + TEACHER_COUNT + " teachers and " + subjectCount + " subjects from " + FILE + ".";
    }

    /** Reads and checks the file. Any problem stops the import with a message naming the line. */
    private List<Row> readRows() {
        List<String> lines;
        try {
            lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + FILE + ": " + e.getMessage(), e);
        }

        List<Row> rows = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        for (int i = 1; i < lines.size(); i++) { // line 0 is the header
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            int number = i + 1;

            // the name is everything before the last two commas (so a comma inside a name is fine)
            int second = line.lastIndexOf(',');
            int first = second < 0 ? -1 : line.lastIndexOf(',', second - 1);
            if (first < 0) {
                throw new IllegalStateException(FILE + " line " + number + ": expected name,usn,branch");
            }
            String name = line.substring(0, first).trim();
            String usn = line.substring(first + 1, second).trim().toUpperCase();
            String branch = line.substring(second + 1).trim();

            Matcher matcher = USN.matcher(usn);
            if (name.isEmpty() || branch.isEmpty() || !matcher.matches()) {
                throw new IllegalStateException(FILE + " line " + number + ": invalid row '" + line + "'");
            }
            String username = (matcher.group(1) + matcher.group(2)).toLowerCase();
            if (!usernames.add(username) || users.existsByUsername(username)) {
                throw new IllegalStateException(FILE + " line " + number + ": the username '" + username
                        + "' is used twice");
            }
            rows.add(new Row(name, usn, branch, username));
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException(FILE + " has no students.");
        }
        return rows;
    }
}
