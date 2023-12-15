import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This application will keep track of things like what classes are offered by
 * the school, and which students are registered for those classes and provide
 * basic reporting. This application interacts with a database to store and
 * retrieve data.
 */
public class GradeManager {
	
	
	public static int currentActiveClass  = -1;

   
    public static List<String> parseArguments(String command) {
        List<String> commandArguments = new ArrayList<String>();
        Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(command);
        while (m.find()) commandArguments.add(m.group(1).replace("\"", ""));
        return commandArguments;
    }
    
    // Create a class: new-class CS410 Sp20 1 "Databases"
    public static void createNewClass(String courseNum, String term, int sectionNum, String description) {
        Connection connection = null;
        Statement sqlStatement = null;

        try {
            connection = Database.getDatabaseConnection();
            sqlStatement = connection.createStatement();

            // Create the SQL query for inserting a new class
            String insertQuery = String.format(
                    "INSERT INTO Classes (course_num, term, section_num, description) VALUES ('%s', '%s', %d, '%s')",
                    courseNum, term, sectionNum, description);

            // Execute the query
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

            // Print the result set
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
    
    public static void selectClass(String courseNum) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
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

            // Execute the query
            resultSet = preparedStatement.executeQuery();
          

            // Process the result set
            if (resultSet.next()) {
            
            	  
            	//multiple results 
                if (resultSet.next()) {
                    System.out.println("Error: Multiple sections found for course number " + courseNum + " in the most recent term.");

                //one result
                }else {
                	resultSet.previous(); //move it back to first class
            	
            	   int classId = resultSet.getInt("class_id");
                   String term = resultSet.getString("term");
                   int sectionNum = resultSet.getInt("section_num");

                   System.out.println("Selected Class:");
                   System.out.println("Class ID: " + classId);
                   System.out.println("Course Number: " + courseNum);
                   System.out.println("Term: " + term);
                   System.out.println("Section Number: " + sectionNum);
                   
                   currentActiveClass = classId;
                	
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
    
    public static void selectClassByTerm(String courseNum, String targetTerm) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
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

            // Execute the query
            resultSet = preparedStatement.executeQuery();

            // Process the result set
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
                    
                    currentActiveClass = classId;
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
    
    
    public static void selectClassByTermAndSection(String courseNum, String targetTerm, int sectionNum) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
            String query = "SELECT class_id, term, section_num " +
                           "FROM Classes " +
                           "WHERE course_num = ? AND term = ? AND section_num = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, courseNum);
            preparedStatement.setString(2, targetTerm);
            preparedStatement.setInt(3, sectionNum);

            // Execute the query
            resultSet = preparedStatement.executeQuery();

            // Process the result set
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
    
    public static void selectCurrentActiveClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
            String query = "SELECT * " +
                           "FROM Classes " +
                           "WHERE class_id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            // Execute the query
            resultSet = preparedStatement.executeQuery();

            // Process the result set
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
    
    
    public static void showCategoriesForActiveClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
            String query = "SELECT category_id, class_id, name, weight " +
                           "FROM Categories " +
                           "WHERE class_id = ?";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            // Execute the query
            resultSet = preparedStatement.executeQuery();

            // Process the result set
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
    
    public static void addCategory(int class_id, String categoryName, float weight) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
            String query = "INSERT INTO Categories (class_id, name, weight) VALUES (?, ?, ?)";

            preparedStatement = connection.prepareStatement(query);
      
            preparedStatement.setInt(1, class_id);
            preparedStatement.setString(2, categoryName);
            preparedStatement.setFloat(3, weight);
            // Execute the query
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
    
    
    public static void showAssignmentsForClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query
            String query = "SELECT c.name AS category_name, a.name AS assignment_name, a.point_val " +
                           "FROM Assignments a " +
                           "JOIN Categories c ON a.category_id = c.category_id " +
                           "WHERE c.class_id = ? " +
                           "ORDER BY c.name, a.name";

            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            // Execute the query
            resultSet = preparedStatement.executeQuery();

            // Process the result set
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
    
    
    public static void addAssignment(String assignmentName, String categoryName, String description, int points, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query to retrieve category_id based on the provided category name and class_id
            String categoryIdQuery = "SELECT category_id FROM Categories WHERE name = ? AND class_id = ?";
            preparedStatement = connection.prepareStatement(categoryIdQuery);
            preparedStatement.setString(1, categoryName);
            preparedStatement.setInt(2, classId);
            int categoryId;

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    categoryId = resultSet.getInt("category_id");

                    // Prepare the SQL query to insert the new assignment
                    String insertQuery = "INSERT INTO Assignments (category_id, name, description, point_val) VALUES (?, ?, ?, ?)";
                    preparedStatement = connection.prepareStatement(insertQuery);
                    preparedStatement.setInt(1, categoryId);
                    preparedStatement.setString(2, assignmentName);
                    preparedStatement.setString(3, description);
                    preparedStatement.setInt(4, points);

                    // Execute the query
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
    

    
    public static void updateStudentName(String username, String newName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Check if the student already exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If the student exists, update the name
                int studentId = resultSet.getInt("student_id");
                updateStudentNameById(studentId, newName);
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
    
    private static void updateStudentNameById(int studentId, String newName) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query to update the student's name
            String updateQuery = "UPDATE Students SET name = ? WHERE student_id = ?";
            preparedStatement = connection.prepareStatement(updateQuery);
            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, studentId);

            // Execute the update query
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

    public static void insertNewStudent(String username, String name) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Check if the student already exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println("Error: Student already exists with username " + username);
            } else {
                // If the student does not exist, insert a new student
                String insertQuery = "INSERT INTO Students (username, name) VALUES (?, ?)";
                preparedStatement = connection.prepareStatement(insertQuery);
                preparedStatement.setString(1, username);
                preparedStatement.setString(2, name);

                // Execute the insert query
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
    
    public static void addStudent(String username, int studentId, String name, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Check if the student already exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If the student exists, update the name and enroll in the class
                int existingStudentId = resultSet.getInt("student_id");
                String existingName = resultSet.getString("name");

                // If the provided student_id matches, enroll them in the class
                if (existingStudentId == studentId) {
                    enrollStudentInClass(existingStudentId, classId);
                } else {
                    // If the provided name matches the stored name, enroll them in the class
                    if (existingName.equals(name)) {
                        enrollStudentInClass(existingStudentId, classId);
                    } else {
                        // If the provided name does not match the stored name, update the name
                        updateStudentNameById(existingStudentId, name);
                        System.out.println("Warning: Name updated for existing student with username " + username);
                        enrollStudentInClass(existingStudentId, classId);
                    }
                }
            } else {
                // If the student does not exist, insert a new student and enroll them in the class
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

    
    private static void enrollStudentInClass(int studentId, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Prepare the SQL query to insert a new enrollment
            String insertQuery = "INSERT INTO Enrollments (student_id, class_id) VALUES (?, ?)";
            preparedStatement = connection.prepareStatement(insertQuery);
            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, classId);

            // Execute the insert query
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
    
//    private static void enrollStudentByUsername(String username, int classId) {
//        Connection connection = null;
//        PreparedStatement preparedStatement = null;
//
//        try {
//            connection = Database.getDatabaseConnection(); // Use your method to get a database connection
//
//            // Prepare the SQL query to insert a new enrollment
//            String insertQuery = "INSERT INTO Enrollments (username, class_id) VALUES (?, ?)";
//            preparedStatement = connection.prepareStatement(insertQuery);
//            preparedStatement.setString(1, username);
//            preparedStatement.setInt(2, classId);
//
//            // Execute the insert query
//            int rowsAffected = preparedStatement.executeUpdate();
//
//            if (rowsAffected > 0) {
//                System.out.println("Student enrolled in class successfully");
//            } else {
//                System.out.println("Failed to enroll student in class");
//            }
//
//        } catch (SQLException sqlException) {
//            System.out.println("Failed to execute query");
//            System.out.println(sqlException.getMessage());
//
//        } finally {
//            try {
//                if (preparedStatement != null)
//                    preparedStatement.close();
//                if (connection != null)
//                    connection.close();
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }
//        }
//    }
    
    public static void addStudentByUsername(String username, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Check if the student already exists
            String checkStudentQuery = "SELECT * FROM Students WHERE username = ?";
            preparedStatement = connection.prepareStatement(checkStudentQuery);
            preparedStatement.setString(1, username);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                // If the student exists, enroll them in the class
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

//    private static void enrollStudentInClassByUsername(int studentId, int classId) {
//        Connection connection = null;
//        PreparedStatement preparedStatement = null;
//
//        try {
//            connection = Database.getDatabaseConnection(); // Use your method to get a database connection
//
//            // Check if the student is already enrolled in the class
//            String checkEnrollmentQuery = "SELECT * FROM Enrollments WHERE student_id = ? AND class_id = ?";
//            preparedStatement = connection.prepareStatement(checkEnrollmentQuery);
//            preparedStatement.setInt(1, studentId);
//            preparedStatement.setInt(2, classId);
//
//            ResultSet resultSet = preparedStatement.executeQuery();
//
//            if (!resultSet.next()) {
//                // If the student is not already enrolled, enroll them in the class
//                String insertEnrollmentQuery = "INSERT INTO Enrollments (student_id, class_id) VALUES (?, ?)";
//                preparedStatement = connection.prepareStatement(insertEnrollmentQuery);
//                preparedStatement.setInt(1, studentId);
//                preparedStatement.setInt(2, classId);
//
//                // Execute the insert query
//                int rowsAffected = preparedStatement.executeUpdate();
//
//                if (rowsAffected > 0) {
//                    System.out.println("Student enrolled in class successfully");
//                } else {
//                    System.out.println("Failed to enroll student in class");
//                }
//            } else {
//                System.out.println("Error: Student is already enrolled in the specified class");
//            }
//
//        } catch (SQLException sqlException) {
//            System.out.println("Failed to execute query");
//            System.out.println(sqlException.getMessage());
//
//        } finally {
//            try {
//                if (preparedStatement != null)
//                    preparedStatement.close();
//                if (connection != null)
//                    connection.close();
//            } catch (SQLException se) {
//                se.printStackTrace();
//            }
//        }
//    }

    public static void showStudentsInClass(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Query to retrieve all students in the current class
            String query = "SELECT s.username, s.name FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "WHERE e.class_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setInt(1, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            // Display the students
            System.out.println("Students in Class " + classId + ":");
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
    
    
    public static void showStudentsWithStringInClass(String searchString, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            String query = "SELECT s.username, s.name FROM Students s " +
                           "JOIN Enrollments e ON s.student_id = e.student_id " +
                           "WHERE (LOWER(s.username) LIKE ? OR LOWER(s.name) LIKE ?) AND e.class_id = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, "%" + searchString.toLowerCase() + "%");
            preparedStatement.setString(2, "%" + searchString.toLowerCase() + "%");
            preparedStatement.setInt(3, classId);

            ResultSet resultSet = preparedStatement.executeQuery();

            // Display the students
            System.out.println("Students with '" + searchString + "' in their name or their username in Class " + classId + ":");
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
    
    public static void gradeAssignment(String assignmentName, String username, float grade, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Check if the assignment exists and get its maximum point value
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

                // Check if the student exists
                String studentQuery = "SELECT student_id FROM Students WHERE username = ?";
                preparedStatement = connection.prepareStatement(studentQuery);
                preparedStatement.setString(1, username);

                ResultSet studentResultSet = preparedStatement.executeQuery();

                if (studentResultSet.next()) {
                    int studentId = studentResultSet.getInt("student_id");

                    // Check if the student already has a grade for the assignment
                    String existingGradeQuery = "SELECT * FROM Grades WHERE student_id = ? AND assignment_id = ?";
                    preparedStatement = connection.prepareStatement(existingGradeQuery);
                    preparedStatement.setInt(1, studentId);
                    preparedStatement.setInt(2, assignmentId);

                    ResultSet existingGradeResultSet = preparedStatement.executeQuery();

                    if (existingGradeResultSet.next()) {
                        // If the student already has a grade, update it
                        updateGrade(studentId, assignmentId, grade, maxPoints);
                    } else {
                        // If the student does not have a grade, insert a new grade
                        insertGrade(studentId, assignmentId, grade, maxPoints);
                    }
                } else {
                    System.out.println("Error: Student not found with username " + username);
                }
            } else {
                System.out.println("Error: Assignment not found with name " + assignmentName + " in Class " + classId);
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

    private static void updateGrade(int studentId, int assignmentId, float grade, float maxPoints) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Update the existing grade for the student and assignment
            String updateGradeQuery = "UPDATE Grades SET score = ? WHERE student_id = ? AND assignment_id = ?";
            preparedStatement = connection.prepareStatement(updateGradeQuery);
            
            // If the grade exceeds the maximum points, print a warning
            if (grade > maxPoints) {
                System.out.println("Warning: Grade exceeds maximum points (" + maxPoints + ")");
                preparedStatement.setFloat(1, maxPoints);
            } else {
                preparedStatement.setFloat(1, grade);
            }

            preparedStatement.setInt(2, studentId);
            preparedStatement.setInt(3, assignmentId);

            // Execute the update query
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

    private static void insertGrade(int studentId, int assignmentId, float grade, float maxPoints) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Use your method to get a database connection

            // Insert a new grade for the student and assignment
            String insertGradeQuery = "INSERT INTO Grades (student_id, assignment_id, score) VALUES (?, ?, ?)";
            preparedStatement = connection.prepareStatement(insertGradeQuery);
            
            // If the grade exceeds the maximum points, print a warning
            if (grade > maxPoints) {
                System.out.println("Warning: Grade exceeds maximum points (" + maxPoints + ")");
                preparedStatement.setFloat(3, maxPoints);
            } else {
                preparedStatement.setFloat(3, grade);
            }

            preparedStatement.setInt(1, studentId);
            preparedStatement.setInt(2, assignmentId);

            // Execute the insert query
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
    
    public static void showStudentGrades(String username, int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection();

            // SQL query to retrieve student grades
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

            categoryPercentage = ((float) totalScoreForCat / assignmentPointsTotalForCat) * 100; // Update the category-wise total
    		System.out.printf("Subtotal for Category %s: %d/%d %.2f%%\n", categoryName, totalScoreForCat, assignmentPointsTotalForCat, categoryPercentage);
            
            
            overallGrade = ((float) totalScoreAcrossAllCats / totalAssignmentsPointsAcrossAllCats) * 100; // Update the category-wise total
 
            // Calculate and display overall grade
            System.out.printf("Total Overall Grade: %.2f%%\n", overallGrade);
            
            overallGradeAttempted = ((float) totalScoreAcrossAllCatsAttempted / totalAssignmentsPointsAcrossAllCatsAttempted) * 100; // Update the category-wise total
            
            // Calculate and display overall grade
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
    
    
    public static void showGradebook(int classId) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            connection = Database.getDatabaseConnection(); // Replace with your method to get a database connection

            // SQL query to retrieve gradebook
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

            // Display gradebook
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