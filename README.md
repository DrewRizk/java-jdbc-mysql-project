

## Project Description
A Java application for managing grades in a class from the command line. 

## Running the program
(Exact steps you'd take to run the program on Onyx)


## General Reflection 

We began with ER Diagram. All of the entities and relationships were easy to create except for the relationship between students and classes. It was not explicitly mentioned in the project description, but we determined that an Enrollments entity must be created to easily track what students are in each class. 

After the ER diagram we moved on to creating the database connection, which took a lot of trial and error. Figuring out how to create embedded SQL queries in Java and work with prepared statements took a long time as well, but it was easy to carry over to each command after the first time. Still, it felt that each command took a signficant amount of time to implement, as working with so many SQL statements make it much more difficult to debug than a typical program. Its hard to know if the problem lies in the SQL query, or the Java code itself. 

Overall, the class activation, category and assignment managment, and student management commands took similar amounts of time to implement. The Grade Reporting section was much harder because the SQL query was much more complex than the previous ones and because you have to iterate through every student in a class. The project description said to do as much of the grade calculations in the SQL query as possible, but we did most of the calulations in Java because it seemed like it would have been very difficult to do in SQL.
 




