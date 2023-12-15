import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This classes represents a grade manager system, where things such as students, classes, and grades exist.
 * It is a dynamic system, so more students and classes can be added at any time. Finally, a students grades can
 * be analyzed and a gradebook can be looked at for a certain class. An important note is that most the queries deal
 * with a concept called the 'current active class' which is essentally just a reference to the most recent class returned 
 * by a 'select-class' query. Further, for the majority of the queries to work, we have to make sure we select a class first. 
 * It should be inferred that all of these methods are able to throw SQLExceptions if the query goes wrong.
 */
public class GradeManager {
	
	
	public static int currentActiveClass  = -1;

    /**
     * Parses arguments up from the command line and returns them as a list.
     * @param command gets added tk list
     * @return the list of command line arguments.
     */
    public static List<String> parseArguments(String command) {
        List<String> commandArguments = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) commandArguments.add(m.group(1).replace("\"", ""));
        return commandArguments;
    }
    

    /**
     * Creates a new class. 
     * @param courseNum course number of the class
     * @param term term of the class
     * @param sectionNum section number of the class
     * @param description description of the class
     */
    public static void createNewClass(String courseNum, String term, int sectionNum, String description) {
        Connection connection = null;
        Statement sqlStatement = null;

        try {
            connection = Database.getDatabaseConnection();
            sqlStatement = connection.createStatement();

            String insertQuery = String.format(
                    "INSERT INTO Classes (course_num, term, section_num, description) VALUES ('%s', '%s', %d, '%s')",
                    courseNum, term, sectionNum, description);

            sqlStatement.executeUpdate(insertQuery);
            
            System.out.println(String.format("Class with course number: %s was created", courseNum));


        
        } catch (SQLException sqlException) {
            System.out.println("Failed to create a new class");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Lists all the classes with the number of students in each classs.
     */
    public static void listClassesWithStudents() {
        Connection connection = null;
        Statement sqlStatement = null;

        try {
            connection = Database.getDatabaseConnection();
            sqlStatement = connection.createStatement();

            String query = "SELECT " +
                    "c.class_id, " +
                    "c.course_num, " +
                    "c.term, " +
                    "c.section_num, " +
                    "c.description, " +
                    "COUNT(e.student_id) AS num_students " +
                    "FROM Classes c " +
                    "LEFT JOIN Enrollments e ON c.class_id = e.class_id " +
                    "GROUP BY c.class_id";

            ResultSet resultSet = sqlStatement.executeQuery(query);

            while (resultSet.next()) {
                System.out.println("Class ID: " + resultSet.getInt("class_id"));
                System.out.println("Course Number: " + resultSet.getString("course_num"));
                System.out.println("Term: " + resultSet.getString("term"));
                System.out.println("Section Number: " + resultSet.getInt("section_num"));
                System.out.println("Description: " + resultSet.getString("description"));
                System.out.println("Number of Students: " + resultSet.getInt("num_students"));
                System.out.println("-".repeat(80));
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to list classes with students");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (sqlStatement != null)
                    sqlStatement.close();
            } catch (SQLException se2) {
            }
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Selects a class from most recent term given a course number. If there are multiple, 
     * it prints there was an error as there are multiple classes with that course number.
     * @param courseNum course number of the class
     */
    public static void selectClass(String courseNum) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String query = "SELECT class_id, term, section_num " +
                           "FROM Classes " +
                           "WHERE course_num = ? " +
                           "ORDER BY CAST(SUBSTRING(term, 3) AS UNSIGNED) DESC, " +
                           "         CASE SUBSTRING(term, 1, 2) " +
                           "           WHEN 'Fa' THEN 1 " +
                           "           WHEN 'Sp' THEN 2 " +
                           "           ELSE 3 " +
                           "         END " +
                           "LIMIT 2"; 

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, courseNum);

            resultSet = preparedStatement.executeQuery();
          

            // Process the result set
            if (resultSet.next()) {

            	//multiple results 
                if (resultSet.next()) {
                    System.out.println("Error: Multiple sections found for course number " + courseNum + " in the most recent term.");

                //one result
                }else {
                	resultSet.previous(); //move resultSet back to first class
            	
            	   int classId = resultSet.getInt("class_id");
                   String term = resultSet.getString("term");
                   int sectionNum = resultSet.getInt("section_num");

                   System.out.println("Selected Class:");
                   System.out.println("Class ID: " + classId);
                   System.out.println("Course Number: " + courseNum);
                   System.out.println("Term: " + term);
                   System.out.println("Section Number: " + sectionNum);
                   
                   currentActiveClass = classId; //update current active class
                }

              
            } else {
                // No section found for the most recent term
                System.out.println("Error: No section found for course " + courseNum + " in the most recent term.");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
       
    /**
     * Selects the most recent class up to the given given term, uses the given course number to do so. If there are multiple, 
     * it prints there was an error as there are multiple classes with that course number.
     * @param courseNum course number of the class
     * @param targetTerm term to go up to
     */
    public static void selectClassByTerm(String courseNum, String targetTerm) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String query = "SELECT class_id, term, section_num " +
                           "FROM Classes " +
                           "WHERE course_num = ? AND term <= ? " +
                           "ORDER BY CAST(SUBSTRING(term, 3) AS UNSIGNED) DESC, " +
                           "         CASE SUBSTRING(term, 1, 2) " +
                           "           WHEN 'Fa' THEN 1 " +
                           "           WHEN 'Sp' THEN 2 " +
                           "           ELSE 3 " +
                           "         END, " +
                           "         section_num DESC " +
                           "LIMIT 2";  

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, courseNum);
            preparedStatement.setString(2, targetTerm);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int classId = resultSet.getInt("class_id");
                String term = resultSet.getString("term");
                int sectionNum = resultSet.getInt("section_num");

                // Check if there is only one result
                if (!resultSet.next()) {
                    System.out.println("Selected Class:");
                    System.out.println("Class ID: " + classId);
                    System.out.println("Course Number: " + courseNum);
                    System.out.println("Term: " + term);
                    System.out.println("Section Number: " + sectionNum);
                    
                    currentActiveClass = classId; //update current active class
                } else {
                    // Multiple classes found for the specified course and term
                    System.out.println("Error: Multiple classes found for course " + courseNum + " up to term " + targetTerm + ".");
                }
            } else {
                // No section found for the specified course and term
                System.out.println("Error: No section found for course " + courseNum + " up to term " + targetTerm + ".");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    
    /**
     * Selects the class given a course number, term, and a section number.
     * Shouldn't be any duplicates in this method.
     * @param courseNum the course number for the class
     * @param targetTerm the term for the class
     * @param sectionNum the section number for the class
     */
    public static void selectClassByTermAndSection(String courseNum, String targetTerm, int sectionNum) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT class_id, term, section_num " +
                           "FROM Classes " +
                           "WHERE course_num = ? AND term = ? AND section_num = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, courseNum);
            preparedStatement.setString(2, targetTerm);
            preparedStatement.setInt(3, sectionNum);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int classId = resultSet.getInt("class_id");
                String term = resultSet.getString("term");
                int selectedSectionNum = resultSet.getInt("section_num");

                System.out.println("Selected Class:");
                System.out.println("Class ID: " + classId);
                System.out.println("Course Number: " + courseNum);
                System.out.println("Term: " + term);
                System.out.println("Section Number: " + selectedSectionNum);
                
                currentActiveClass = classId;
            } else {
                // No class found for the specified course, term, and section
                System.out.println("Error: No class found for course " + courseNum +
                        " in term " + targetTerm + " with section number " + sectionNum + ".");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Selects the current active class if it has been set.
     * @param classId
     */
    public static void selectCurrentActiveClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT * " +
                           "FROM Classes " +
                           "WHERE class_id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String courseNum = resultSet.getString("course_num");
                String term = resultSet.getString("term");
                int sectionNum = resultSet.getInt("section_num");
                String description = resultSet.getString("description");

                System.out.println("Selected Class:");
                System.out.println("Class ID: " + classId);
                System.out.println("Course Number: " + courseNum);
                System.out.println("Term: " + term);
                System.out.println("Section Number: " + sectionNum);
                System.out.println("Description: " + description);
            } else {
                // No class found for the specified class_id
                System.out.println("Error: No class found for class_id " + classId + ".");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    
    /**
     * Shows the categories that exist for the current active class.
     * @param classId the given class_id from current active class that will uniquely identify a class
     */
    public static void showCategoriesForActiveClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT category_id, class_id, name, weight " +
                           "FROM Categories " +
                           "WHERE class_id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int categoryId = resultSet.getInt("category_id");
                int retrievedClassId = resultSet.getInt("class_id");
                String name = resultSet.getString("name");
                float weight = resultSet.getFloat("weight");

                System.out.println("Category ID: " + categoryId);
                System.out.println("Class ID: " + retrievedClassId);
                System.out.println("Name: " + name);
                System.out.println("Weight: " + weight);
                System.out.println("-".repeat(80));
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Adds a category to the current active class.
     * @param class_id the class_id for the current active class
     * @param categoryName name of the category
     * @param weight the weight of the category
     */
    public static void addCategory(int class_id, String categoryName, float weight) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String query = "INSERT INTO Categories (class_id, name, weight) VALUES (?, ?, ?)";

            preparedStatement = connection.prepareStatement(query);
      
            preparedStatement.setInt(1, class_id);
            preparedStatement.setString(2, categoryName);
            preparedStatement.setFloat(3, weight);

            // Keep track of the affected rows to help determine success
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Category added successfully");
            } else {
                System.out.println("Failed to add category");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Shows the assignments for the current active class.
     * @param classId the class_id of the current active class
     */
    public static void showAssignmentsForClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT c.name AS category_name, a.name AS assignment_name, a.point_val " +
                           "FROM Assignments a " +
                           "JOIN Categories c ON a.category_id = c.category_id " +
                           "WHERE c.class_id = ? " +
                           "ORDER BY c.name, a.name";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            resultSet = preparedStatement.executeQuery();

            String currentCategory = null;

            while (resultSet.next()) {
                String category = resultSet.getString("category_name");
                String assignmentName = resultSet.getString("assignment_name");
                int pointValue = resultSet.getInt("point_val");

                // Check if the category has changed
                if (!category.equals(currentCategory)) {
                    System.out.println("Category: " + category);
                    currentCategory = category;
                }

                System.out.println("  Assignment: " + assignmentName);
                System.out.println("  Point Value: " + pointValue);
                System.out.println("-".repeat(80));
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (resultSet != null)
                    resultSet.close();
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Add an assignment to the current active class.
     * @param assignmentName the name of the assignment
     * @param categoryName the name of the category
     * @param description the description of the assignment
     * @param points how many points the assignment is worth
     * @param classId the class_id of the current active class
     */
    public static void addAssignment(String assignmentName, String categoryName, String description, int points, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Query to retrieve category_id based on the given category name and class_id
            String categoryIdQuery = "SELECT category_id FROM Categories WHERE name = ? AND class_id = ?";
            preparedStatement = connection.prepareStatement(categoryIdQuery);
            preparedStatement.setString(1, categoryName);
            preparedStatement.setInt(2, classId);
            int categoryId;

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    categoryId = resultSet.getInt("category_id");

                    // Insert new assignment query
                    String insertQuery = "INSERT INTO Assignments (category_id, name, description, point_val) VALUES (?, ?, ?, ?)";
                    preparedStatement = connection.prepareStatement(insertQuery);
                    preparedStatement.setInt(1, categoryId);
                    preparedStatement.setString(2, assignmentName);
                    preparedStatement.setString(3, description);
                    preparedStatement.setInt(4, points);

                    // Keep track of affected rows
                    int rowsAffected = preparedStatement.executeUpdate();

                    if (rowsAffected > 0) {
                        System.out.println("Assignment added successfully");
                    } else {
                        System.out.println("Failed to add assignment");
                    }
                } else {
                    System.out.println("Error: Category not found for the specified class and name.");
                }
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    

    /**
     * Updates a student's name.
     * @param username the username of the student
     * @param newName the new name of the student
     */
    public static void updateStudentName(String username, String newName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Check if the student already exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If the student exists we update their name
                int studentId = resultSet.getInt("student_id");
                updateStudentNameById(studentId, newName); //helper method
                System.out.println("Student name updated successfully");
            } else {
                System.out.println("Error: Student not found with username " + username);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Uses a student's ID to update their name.
     * @param studentId the student's student_id
     * @param newName their new name
     */
    private static void updateStudentNameById(int studentId, String newName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            String updateQuery = "UPDATE Students SET name = ? WHERE student_id = ?";
            preparedStatement = connection.prepareStatement(updateQuery);
            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, studentId);

            // Keep track of rows affected
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Student name updated successfully");
            } else {
                System.out.println("Failed to update student name");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Insert a new student.
     * @param username the username of the new student
     * @param name the name of the new student
     */
    public static void insertNewStudent(String username, String name) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Check if the student exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Error: Student already exists with username " + username);
            } else {
                // If the student doesn't exist, insert the new student
                String insertQuery = "INSERT INTO Students (username, name) VALUES (?, ?)";
                preparedStatement = connection.prepareStatement(insertQuery);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, name);

                // Keep track of rows affected
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected > 0) {
                    System.out.println("New student added successfully");
                } else {
                    System.out.println("Failed to add new student");
                }
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Adds a student to the current active class.
     * @param username the username of the student
     * @param studentId the student's ID
     * @param name the name of the student
     * @param classId the class_id of the current active class
     */
    public static void addStudent(String username, int studentId, String name, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); 

            // Check if the student exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If student exists, update their name, then enroll them in class
                int existingStudentId = resultSet.getInt("student_id");
                String existingName = resultSet.getString("name");

                // If student_id matches, enroll them 
                if (existingStudentId == studentId) {
                    enrollStudentInClass(existingStudentId, classId);
                } else {
                    // If name matches, enroll them in the class
                    if (existingName.equals(name)) {
                        enrollStudentInClass(existingStudentId, classId);
                    } else {
                        // If name doesn't match, update the name
                        updateStudentNameById(existingStudentId, name);
                        System.out.println("Warning: Name updated for existing student with username " + username);
                        enrollStudentInClass(existingStudentId, classId);
                    }
                }
            } else {
                // If student doesn't exist, insert a new student, then enroll them
                insertNewStudent(username, name);
                enrollStudentInClass(studentId, classId);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Enrolls a student in the current active class.
     * @param studentId the id of the student 
     * @param classId the class_id of the current active class
     */
    private static void enrollStudentInClass(int studentId, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String insertQuery = "INSERT INTO Enrollments (student_id, class_id) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertQuery);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, classId);

            // Keep track of rows affected
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Student enrolled in class successfully");
            } else {
                System.out.println("Failed to enroll student in class");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Adds a student to current active class by their username.
     * @param username the username of the student
     * @param classId the class_id of the current active class
     */
    public static void addStudentByUsername(String username, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Check if student exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If student exists, enroll them
                int studentId = resultSet.getInt("student_id");
                enrollStudentInClass(studentId, classId);
            } else {
                System.out.println("Error: Student not found with username " + username);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Shows the students enrolled in the current active class.
     * @param classId the class_id of the current active class
     */
    public static void showStudentsInClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String query = "SELECT s.username, s.name FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "WHERE e.class_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Students in the Class " + classId + ":");
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String name = resultSet.getString("name");
                System.out.println("Username: " + username + ", Name: " + name);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Shows the students in the current active class with the given string in their name.
     * @param searchString the string to look for
     * @param classId the class_id of the current active class
     */
    public static void showStudentsWithStringInClass(String searchString, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT s.username, s.name FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "WHERE (LOWER(s.username) LIKE ? OR LOWER(s.name) LIKE ?) AND e.class_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "%" + searchString.toLowerCase() + "%");
            preparedStatement.setString(2, "%" + searchString.toLowerCase() + "%");
            preparedStatement.setInt(3, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            System.out.println("Students with '" + searchString + "' in their name or username in the Class " + classId + ":");
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                String name = resultSet.getString("name");
                System.out.println("Username: " + username + ", Name: " + name);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Grade an assignment.
     * @param assignmentName the name of the assignment
     * @param username the username of the student
     * @param grade the grade they will get
     * @param classId the class_id for the current active class
     */
    public static void gradeAssignment(String assignmentName, String username, float grade, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Check if assignment exists
            String assignmentQuery = "SELECT a.assignment_id, a.point_val FROM Assignments a " +
                                     "JOIN Categories c ON a.category_id = c.category_id " +
                                     "JOIN Classes cl ON c.class_id = cl.class_id " +
                                     "WHERE a.name = ? AND cl.class_id = ?";
            preparedStatement = connection.prepareStatement(assignmentQuery);
            preparedStatement.setString(1, assignmentName);
            preparedStatement.setInt(2, classId);

            ResultSet assignmentResultSet = preparedStatement.executeQuery();

            if (assignmentResultSet.next()) {
                int assignmentId = assignmentResultSet.getInt("assignment_id");
                float maxPoints = assignmentResultSet.getFloat("point_val");

                // Check student exists
                String studentQuery = "SELECT student_id FROM Students WHERE username = ?";
                preparedStatement = connection.prepareStatement(studentQuery);
                preparedStatement.setString(1, username);

                ResultSet studentResultSet = preparedStatement.executeQuery();

                if (studentResultSet.next()) {
                    int studentId = studentResultSet.getInt("student_id");

                    // Does student have a grade for assignment
                    String existingGradeQuery = "SELECT * FROM Grades WHERE student_id = ? AND assignment_id = ?";
                    preparedStatement = connection.prepareStatement(existingGradeQuery);
                    preparedStatement.setInt(1, studentId);
                    preparedStatement.setInt(2, assignmentId);

                    ResultSet existingGradeResultSet = preparedStatement.executeQuery();

                    if (existingGradeResultSet.next()) {
                        // Update the grade
                        updateGrade(studentId, assignmentId, grade, maxPoints);
                    } else {
                        // Insert a grade
                        insertGrade(studentId, assignmentId, grade, maxPoints);
                    }
                } else {
                    System.out.println("Error: Student not found having the username " + username);
                }
            } else {
                System.out.println("Error: Assignment not found having the name " + assignmentName + " in the Class " + classId);
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Updates a grade for a student.
     * @param studentId the student_id of the student
     * @param assignmentId the assignment_id of the assignment
     * @param grade the updated grade for the assignment
     * @param maxPoints max amount of points for the assignment
     */
    private static void updateGrade(int studentId, int assignmentId, float grade, float maxPoints) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // Update the existing grade
            String updateGradeQuery = "UPDATE Grades SET score = ? WHERE student_id = ? AND assignment_id = ?";
            preparedStatement = connection.prepareStatement(updateGradeQuery);
            
            // Grade can't be larger than the maximum
            if (grade > maxPoints) {
                System.out.println("Warning: Grade exceeds maximum points (" + maxPoints + ")");
                preparedStatement.setFloat(1, maxPoints);
            } else {
                preparedStatement.setFloat(1, grade);
            }

            preparedStatement.setInt(2, studentId);
            preparedStatement.setInt(3, assignmentId);

            // Keep track of the affected rows
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Grade updated successfully");
            } else {
                System.out.println("Failed to update grade");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Inserts a new grade.
     * @param studentId the student_id of the student
     * @param assignmentId the assignment_id of the assignment
     * @param grade the new grade for the assignment
     * @param maxPoints max amount of points for the assignment
     */
    private static void insertGrade(int studentId, int assignmentId, float grade, float maxPoints) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); 

            String insertGradeQuery = "INSERT INTO Grades (student_id, assignment_id, score) VALUES (?, ?, ?)";
            preparedStatement = connection.prepareStatement(insertGradeQuery);
            
            // New grade can't be larger than maxPoints
            if (grade > maxPoints) {
                System.out.println("Warning: Grade exceeds maximum points (" + maxPoints + ")");
                preparedStatement.setFloat(3, maxPoints);
            } else {
                preparedStatement.setFloat(3, grade);
            }

            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, assignmentId);

            // Keep track of the rows affected
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Grade assigned successfully");
            } else {
                System.out.println("Failed to assign grade");
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());
            
        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Shows the grades for a student with various other info like subtotal for each category, and overall grade, both total and attempted.
     * @param username the username of the student
     * @param classId the class_id of the current active class
     */
    public static void showStudentGrades(String username, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT s.username, s.name AS student_name, " +
                           "c.name AS category_name, a.name AS assignment_name, " +
                           "a.point_val AS assignment_points, COALESCE(g.score, 0) AS student_score, " +
                           "COALESCE((g.score / a.point_val) * 100, 0) AS student_percentage " +
                           "FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "JOIN Classes cl ON e.class_id = cl.class_id " +
                           "JOIN Categories c ON cl.class_id = c.class_id " +
                           "LEFT JOIN Assignments a ON c.category_id = a.category_id " +
                           "LEFT JOIN Grades g ON s.student_id = g.student_id AND a.assignment_id = g.assignment_id " +
                           "WHERE s.username = ? AND cl.class_id = ? " +
                           "ORDER BY c.category_id, a.assignment_id";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setInt(2, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            float categoryPercentage = 0;
            float overallGrade = 0;
            float overallGradeAttempted = 0;
            int totalScoreAcrossAllCats = 0;
            int totalAssignmentsPointsAcrossAllCats = 0;
            
            int totalScoreAcrossAllCatsAttempted = 0;
            int totalAssignmentsPointsAcrossAllCatsAttempted = 0;

            int totalScoreForCat = 0;
            int assignmentPointsTotalForCat = 0;
            String categoryName = null;
            if (resultSet.next()) {
            	   categoryName = resultSet.getString("category_name");
            	   resultSet.previous(); //set resultSet back to where it was
            }

            while (resultSet.next()) {
            	if (!categoryName.equals(resultSet.getString("category_name"))) {
            		categoryPercentage = ((float) totalScoreForCat / assignmentPointsTotalForCat) * 100; // Update the category-wise total
            		System.out.printf("Subtotal for Category %s: %d/%d %.2f%%\n", categoryName, totalScoreForCat, assignmentPointsTotalForCat, categoryPercentage);
                    totalScoreForCat = 0;
                    assignmentPointsTotalForCat = 0;
            	}

            	//populate fields
                categoryName = resultSet.getString("category_name");
                String assignmentName = resultSet.getString("assignment_name");
                float studentPercentage = resultSet.getFloat("student_percentage");
                int assignmentPoints = resultSet.getInt("assignment_points");
                int studentScore = resultSet.getInt("student_score");
           
                //update totals
                totalScoreForCat += studentScore;
                assignmentPointsTotalForCat += assignmentPoints;
                
                if (studentScore != 0) { //attempted grade 
                	totalScoreAcrossAllCatsAttempted += studentScore;
                	totalAssignmentsPointsAcrossAllCatsAttempted += assignmentPoints;
                }
                totalScoreAcrossAllCats += studentScore;
                totalAssignmentsPointsAcrossAllCats += assignmentPoints;
           
                // Print grade
                System.out.printf("Username: %s, Student Name: %s, Category: %s, Assignment Name: %s, Assignment Points: %d, Student Score: %d, Percentage: %.2f%%\n",
                        username, resultSet.getString("student_name"), categoryName, assignmentName, assignmentPoints, studentScore, studentPercentage);
            }
            categoryPercentage = ((float) totalScoreForCat / assignmentPointsTotalForCat) * 100; 
    		System.out.printf("Subtotal for Category %s: %d/%d %.2f%%\n", categoryName, totalScoreForCat, assignmentPointsTotalForCat, categoryPercentage);
            
            overallGrade = ((float) totalScoreAcrossAllCats / totalAssignmentsPointsAcrossAllCats) * 100; 
 
            // Display totoal overall grade
            System.out.printf("Total Overall Grade: %.2f%%\n", overallGrade);
            
            overallGradeAttempted = ((float) totalScoreAcrossAllCatsAttempted / totalAssignmentsPointsAcrossAllCatsAttempted) * 100; 
            
            // Display attempted overall grade
            System.out.printf("Attempted Overall Grade: %.2f%%\n", overallGradeAttempted);

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());
            sqlException.printStackTrace();

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
    
    /**
     * Shows the current gradebook for the current active class.
     * @param classId the class_id of the current active class.
     */
    public static void showGradebook(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            String query = "SELECT s.username, s.student_id, s.name AS student_name, " +
                           "COALESCE(SUM(COALESCE(g.score, 0)), 0) AS total_score, " +
                           "COALESCE(SUM(a.point_val), 0) AS total_points, " +
                           "COALESCE((SUM(COALESCE(g.score, 0)) / SUM(a.point_val)) * 100, 0) AS overall_percentage " +
                           "FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "JOIN Classes cl ON e.class_id = cl.class_id " +
                           "LEFT JOIN Categories c ON cl.class_id = c.class_id " +
                           "LEFT JOIN Assignments a ON c.category_id = a.category_id " +
                           "LEFT JOIN Grades g ON s.student_id = g.student_id AND a.assignment_id = g.assignment_id " +
                           "WHERE cl.class_id = ? " +
                           "GROUP BY s.username, s.student_id, s.name " +
                           "ORDER BY s.username";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                System.out.printf("Username: %s, Student ID: %d, Student Name: %s, Total Score: %d, Total Points: %d, Overall Percentage: %.2f%%\n",
                        resultSet.getString("username"),
                        resultSet.getInt("student_id"),
                        resultSet.getString("student_name"),
                        resultSet.getInt("total_score"),
                        resultSet.getInt("total_points"),
                        resultSet.getFloat("overall_percentage"));
            }

        } catch (SQLException sqlException) {
            System.out.println("Failed to execute query");
            System.out.println(sqlException.getMessage());

        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
                if (connection != null)
                    connection.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    /**
     * Main method that runs the console app. Takes in command line arguments from the user to determine what they want
     * to do with the grade manager system.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("Welcome to the School Management System");
        System.out.println("-".repeat(80));

        Scanner scan = new Scanner(System.in);
        String command = "";

        do {
            System.out.print("Command: ");
            command = scan.nextLine();
            List<String> commandArguments = parseArguments(command);
            command = commandArguments.get(0);
            commandArguments.remove(0);

            
            if (command.equals("test") && commandArguments.get(0).equals("connection")) {
                Database.testConnection();
                
            } else if (command.equals("new-class")) {
            	String sectionNumString = commandArguments.get(2);
            	int sectionNum = Integer.parseInt(sectionNumString);
            	createNewClass(commandArguments.get(0), commandArguments.get(1), sectionNum, commandArguments.get(3));
            	
            } else if (command.equals("list-classes")) {
            	listClassesWithStudents();
            	
            } else if (command.equals("select-class")) {
            	// select-class CS410 Sp20 1
               	if (commandArguments.size() == 3) { //3 arguments
               		String sectionNumString = commandArguments.get(2);
                	int sectionNum = Integer.parseInt(sectionNumString);
            		selectClassByTermAndSection(commandArguments.get(0), commandArguments.get(1), sectionNum);
            	}
            	// select-class CS410 Sp20
               	else if (commandArguments.size() == 2) { //2 arguments
            		selectClassByTerm(commandArguments.get(0), commandArguments.get(1));
            		
            	// select-class CS410
            	}else if (commandArguments.size() == 1) { //1 argument
                	selectClass(commandArguments.get(0));

            	}else {
                    System.out.println("Error: Incorrect number of args for select-class");
            	}
               	
        
            } else if (command.equals("show-class")) {
            	selectCurrentActiveClass(currentActiveClass); //we pass in the current active class
            	
            	
            	   
            } else if (command.equals("show-categories")) {
            	showCategoriesForActiveClass(currentActiveClass); //we pass in the current active class
            	
            } else if (command.equals("add-category")) {
            	String weightString = commandArguments.get(1);
            	float weightFloat = Float.parseFloat(weightString);
            	addCategory(currentActiveClass, commandArguments.get(0),weightFloat); //we pass in the current active class
            	
            } else if (command.equals("show-assignment")) {
            	showAssignmentsForClass(currentActiveClass); //we pass in the current active class
            	
            } else if (command.equals("add-assignment")) {
            	// add-assignment name Category Description points – add a new assignment
            	String pointsString = commandArguments.get(3);
            	int pointsInt = Integer.parseInt(pointsString);
            	addAssignment(commandArguments.get(0), commandArguments.get(1),commandArguments.get(2), pointsInt, currentActiveClass);
            	
            	//String username, int studentId, String name, int classId
            } else if (command.equals("add-student")) {
            	if (commandArguments.size() == 3) { //3 arguments
                   	String studentIdString = commandArguments.get(1);
                	int studentIdInt = Integer.parseInt(studentIdString);
                	addStudent(commandArguments.get(0), studentIdInt, commandArguments.get(2), currentActiveClass); //we pass in the current active class
            	}else {
                	addStudentByUsername(commandArguments.get(0), currentActiveClass);	
            	}
            	
            } else if (command.equals("show-students")) {
            	if (commandArguments.size() == 1) { //1 arguments (string)
            		showStudentsWithStringInClass(commandArguments.get(0), currentActiveClass);
            		
            	}else { //0 paramaters
                	showStudentsInClass(currentActiveClass); //we pass in the current active class

            	}
            //grade assignmentname username grade
            } else if (command.equals("grade")) {
            	String gradeString = commandArguments.get(2);
            	float gradeFloat = Float.parseFloat(gradeString);
            	gradeAssignment(commandArguments.get(0), commandArguments.get(1), gradeFloat, currentActiveClass); //we pass in the current active class
            	
            	
            } else if (command.equals("student-grades")) {
            	showStudentGrades(commandArguments.get(0),currentActiveClass); //pass in username and current active class 
            	
            } else if (command.equals("gradebook")) {
            	showGradebook(currentActiveClass); //we pass in the current active class
       
            } else if (!(command.equals("quit") || command.equals("exit"))) {
                System.out.println(command);
                System.out.println("Command not found. Enter 'help' for list of commands");
            }
            
            
            System.out.println("-".repeat(80));
        } while (!(command.equals("quit") || command.equals("exit")));
        System.out.println("Bye!");
    }
    
    // select-class CS321 Sp20 1
    // list-classes
    // select-class CS410
    // select-class MATH202
    // select-class CS410 Sp20
    // select-class CS410 Sp20 1 
    // show-class
    // show-categories
    // add-category Participation .1
    // show-categories
    // show-assignment
    // add-assignment "Homework 6" Homework "Another HW" 30
    // show-assignment
    // add-student arthur_putnam 8 "Arthur Putnam" 
    // list-classes
    // select-class MATH202
    // add-student sayre_pet
    // show-students
    // show-students arth
    // grade "Midterm Exam" "jane_smith" 40
    // student-grades "jane_smith"
    
   
}