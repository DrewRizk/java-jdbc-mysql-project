CREATE DATABASE IF NOT EXISTS grade_manager;
USE grade_manager;

-- Create Classes table
CREATE TABLE Classes (
    class_id INT AUTO_INCREMENT PRIMARY KEY,
    course_num VARCHAR(20),
    term VARCHAR(10),
    section_num INT,
    description VARCHAR(255),
    UNIQUE(course_num, term, section_num)
);

-- Create Categories table
CREATE TABLE Categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    class_id INT,
    name VARCHAR(50),
    weight FLOAT,
    FOREIGN KEY (class_id) REFERENCES Classes(class_id),
    UNIQUE(class_id, name)
);

-- Create Assignments table
CREATE TABLE Assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT,
    name VARCHAR(50),
    description VARCHAR(255),
    point_val INT,
    UNIQUE(category_id, name),
    FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);

-- Create Students table
CREATE TABLE Students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    name VARCHAR(100),
    UNIQUE(username)
);

-- Create Enrollments table (junction table for many-to-many relationship between Students and Classes)
CREATE TABLE Enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT,
    class_id INT,
    FOREIGN KEY (student_id) REFERENCES Students(student_id),
    FOREIGN KEY (class_id) REFERENCES Classes(class_id),
    UNIQUE(student_id, class_id)
);

-- Create Grades table
CREATE TABLE Grades (
    grade_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT,
    assignment_id INT,
    score FLOAT,
    FOREIGN KEY (student_id) REFERENCES Students(student_id),
    FOREIGN KEY (assignment_id) REFERENCES Assignments(assignment_id),
    UNIQUE(student_id, assignment_id)
);



-- Insert dummy data into Classes table
INSERT INTO Classes (course_num, term, section_num, description)
VALUES
  ('CS410', 'Sp20', 1, 'Introduction to Databases'),
  ('PH101', 'Fa20', 2, 'Introduction to Philosophy'),
  ('MATH202', 'Sp21', 1, 'Calculus II'),
  ('ENG101', 'Fa21', 3, 'Composition and Rhetoric');

-- Insert dummy data into Categories table
INSERT INTO Categories (class_id, name, weight)
VALUES
  (1, 'Homework', 0.3),
  (1, 'Exams', 0.5),
  (2, 'Essays', 0.4),
  (3, 'Problem Sets', 0.3),
  (4, 'Projects', 0.6);

-- Insert dummy data into Assignments table
INSERT INTO Assignments (category_id, name, description, point_val)
VALUES
  (1, 'Homework 1', 'Introduction to SQL', 10),
  (1, 'Homework 2', 'Database Design', 15),
  (2, 'Midterm Exam', 'Covering Chapters 1-5', 50),
  (3, 'Essay 1', 'Philosophy of Mind', 20),
  (4, 'Problem Set 1', 'Calculus II Integration', 30),
  (5, 'Project 1', 'Literary Analysis', 40);

-- Insert dummy data into Students table
INSERT INTO Students (username, name)
VALUES
  ('john_doe', 'John Doe'),
  ('jane_smith', 'Jane Smith'),
  ('bob_jones', 'Bob Jones'),
  ('emily_davis', 'Emily Davis');

-- Insert dummy data into Enrollments table
INSERT INTO Enrollments (student_id, class_id)
VALUES
  (1, 1),
  (2, 1),
  (3, 2),
  (4, 3),
  (1, 4);

-- Insert dummy data into Grades table
INSERT INTO Grades (student_id, assignment_id, score)
VALUES
  (1, 1, 8.5),
  (1, 2, 14),
  (2, 1, 9.5),
  (3, 3, 45),
  (4, 4, 18),
  (1, 5, 28),
  (2, 6, 36),
  (3, 2, 13.5),
  (4, 3, 48);