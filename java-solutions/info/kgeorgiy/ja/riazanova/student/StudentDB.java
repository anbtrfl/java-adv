package info.kgeorgiy.ja.riazanova.student;

import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentQuery;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StudentDB implements StudentQuery {

    private final Comparator<Student> studentComparatorByName = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            // Student.compareTo
            .thenComparing(Student::getId, Comparator.reverseOrder());

            // Student.compareTo
    private final Comparator<Student> studentComparatorById = (Comparator.comparingInt(Student::getId));

    private List<Student> findStudentsBySmth(Collection<Student> students, Predicate<Student> predicate) {
        return students.stream()
                .filter(predicate)
                .sorted(studentComparatorByName)
                .collect(Collectors.toList());
    }

    private <T> List<T> getSmth(List<Student> students, Function<Student, T> mapper) {
        return students
                .stream()
                .map(mapper)
                .collect(Collectors.toList());
    }

    private List<Student> sortStudentsBySmth(Collection<Student> students, Comparator<Student> comparator) {
        return students.stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return getSmth(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return getSmth(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(List<Student> students) {
        return getSmth(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return getSmth(students, student -> student.getFirstName() + " " + student.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return students
                .stream()
                .map(Student::getFirstName)
                .collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students
                .stream()
                .max(studentComparatorById)
                .map(Student::getFirstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudentsBySmth(students, studentComparatorById);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudentsBySmth(students, studentComparatorByName);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsBySmth(students, student -> Objects.equals(student.getLastName(), name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsBySmth(students, student -> Objects.equals(student.getGroup(), group));
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsBySmth(students, student -> Objects.equals(student.getFirstName(), name));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return students.stream()
                .filter(student -> Objects.equals(student.getGroup(), group))
                .collect(
                        Collectors.groupingBy(
                                Student::getLastName,
                                Collectors.collectingAndThen(
                                        Collectors.minBy(Comparator.comparing(Student::getFirstName)),
                                        student -> student.map(Student::getFirstName).orElseThrow()
                                )
                        )
                );
    }
}

