import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.io.*;
import java.nio.file.*;

// Employee Model
class Employee implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;
    private String position;
    private double baseSalary;
    private String email;
    private String phone;
    private LocalDate joinDate;
    
    public Employee(int id, String name, String position, double baseSalary, String email, String phone) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.baseSalary = baseSalary;
        this.email = email;
        this.phone = phone;
        this.joinDate = LocalDate.now();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(double baseSalary) { this.baseSalary = baseSalary; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDate joinDate) { this.joinDate = joinDate; }
}

// Attendance Model
class Attendance implements Serializable {
    private static final long serialVersionUID = 1L;
    private int employeeId;
    private LocalDate date;
    private boolean present;
    private double hoursWorked;
    
    public Attendance(int employeeId, LocalDate date, boolean present, double hoursWorked) {
        this.employeeId = employeeId;
        this.date = date;
        this.present = present;
        this.hoursWorked = hoursWorked;
    }
    
    // Getters and Setters
    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public boolean isPresent() { return present; }
    public void setPresent(boolean present) { this.present = present; }
    public double getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(double hoursWorked) { this.hoursWorked = hoursWorked; }
}

// Salary Model
class Salary implements Serializable {
    private static final long serialVersionUID = 1L;
    private int employeeId;
    private String month;
    private int year;
    private double basicSalary;
    private double allowances;
    private double deductions;
    private double netSalary;
    private int daysWorked;
    
    public Salary(int employeeId, String month, int year, double basicSalary, double allowances, double deductions, int daysWorked) {
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.basicSalary = basicSalary;
        this.allowances = allowances;
        this.deductions = deductions;
        this.daysWorked = daysWorked;
        this.netSalary = basicSalary + allowances - deductions;
    }
    
    // Getters and Setters
    public int getEmployeeId() { return employeeId; }
    public String getMonth() { return month; }
    public int getYear() { return year; }
    public double getBasicSalary() { return basicSalary; }
    public double getAllowances() { return allowances; }
    public double getDeductions() { return deductions; }
    public double getNetSalary() { return netSalary; }
    public int getDaysWorked() { return daysWorked; }
    public void setNetSalary(double netSalary) { this.netSalary = netSalary; }
}

// Data Manager
class DataManager {
    private List<Employee> employees;
    private List<Attendance> attendanceRecords;
    private List<Salary> salaryRecords;
    private final String DATA_DIR = "payroll_data";
    
    public DataManager() {
        employees = new ArrayList<>();
        attendanceRecords = new ArrayList<>();
        salaryRecords = new ArrayList<>();
        createDataDirectory();
        loadData();
    }
    
    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }
    
    public void saveData() {
        try {
            saveEmployees();
            saveAttendance();
            saveSalaries();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error saving data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void saveEmployees() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "/employees.dat"))) {
            oos.writeObject(employees);
        }
    }
    
    private void saveAttendance() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "/attendance.dat"))) {
            oos.writeObject(attendanceRecords);
        }
    }
    
    private void saveSalaries() throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_DIR + "/salaries.dat"))) {
            oos.writeObject(salaryRecords);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadData() {
        try {
            loadEmployees();
            loadAttendance();
            loadSalaries();
        } catch (Exception e) {
            // First run or corrupted data, start fresh
            System.out.println("Starting with fresh data...");
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadEmployees() throws IOException, ClassNotFoundException {
        File file = new File(DATA_DIR + "/employees.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                employees = (List<Employee>) ois.readObject();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadAttendance() throws IOException, ClassNotFoundException {
        File file = new File(DATA_DIR + "/attendance.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                attendanceRecords = (List<Attendance>) ois.readObject();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadSalaries() throws IOException, ClassNotFoundException {
        File file = new File(DATA_DIR + "/salaries.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                salaryRecords = (List<Salary>) ois.readObject();
            }
        }
    }
    
    // Employee Management
    public void addEmployee(Employee employee) {
        employees.add(employee);
        saveData();
    }
    
    public void updateEmployee(Employee employee) {
        for (int i = 0; i < employees.size(); i++) {
            if (employees.get(i).getId() == employee.getId()) {
                employees.set(i, employee);
                break;
            }
        }
        saveData();
    }
    
    public void deleteEmployee(int id) {
        employees.removeIf(emp -> emp.getId() == id);
        saveData();
    }
    
    public List<Employee> getAllEmployees() {
        return new ArrayList<>(employees);
    }
    
    public Employee getEmployeeById(int id) {
        return employees.stream().filter(emp -> emp.getId() == id).findFirst().orElse(null);
    }
    
    public int getNextEmployeeId() {
        return employees.stream().mapToInt(Employee::getId).max().orElse(0) + 1;
    }
    
    // Attendance Management
    public void addAttendance(Attendance attendance) {
        attendanceRecords.add(attendance);
        saveData();
    }
    
    public List<Attendance> getAttendanceByEmployee(int employeeId) {
        return attendanceRecords.stream()
                .filter(att -> att.getEmployeeId() == employeeId)
                .toList();
    }
    
    public List<Attendance> getAllAttendance() {
        return new ArrayList<>(attendanceRecords);
    }
    
    // Salary Management
    public void addSalary(Salary salary) {
        salaryRecords.add(salary);
        saveData();
    }
    
    public List<Salary> getSalariesByEmployee(int employeeId) {
        return salaryRecords.stream()
                .filter(sal -> sal.getEmployeeId() == employeeId)
                .toList();
    }
    
    public List<Salary> getAllSalaries() {
        return new ArrayList<>(salaryRecords);
    }
}

// Main Application Class
public class PayrollManagementSystem extends JFrame {
    private DataManager dataManager;
    private JTabbedPane tabbedPane;
    
    // Employee Management Components
    private DefaultTableModel employeeTableModel;
    private JTable employeeTable;
    private JTextField empNameField, empPositionField, empSalaryField, empEmailField, empPhoneField;
    
    // Attendance Management Components
    private DefaultTableModel attendanceTableModel;
    private JTable attendanceTable;
    private JComboBox<String> attEmpComboBox;
    private JTextField attHoursField;
    private JCheckBox attPresentCheckBox;
    
    // Salary Management Components
    private DefaultTableModel salaryTableModel;
    private JTable salaryTable;
    private JComboBox<String> salEmpComboBox, salMonthComboBox;
    private JTextField salYearField, salAllowancesField, salDeductionsField;
    
    public PayrollManagementSystem() {
        dataManager = new DataManager();
        initializeUI();
        loadAllData();
        
        // Add shutdown hook to save data
        Runtime.getRuntime().addShutdownHook(new Thread(() -> dataManager.saveData()));
    }
    
    private void initializeUI() {
        setTitle("Payroll Management System - OpenJDK 21");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        
        // Set Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        tabbedPane = new JTabbedPane();
        
        // Create tabs
        tabbedPane.addTab("Employee Management", createEmployeePanel());
        tabbedPane.addTab("Attendance Management", createAttendancePanel());
        tabbedPane.addTab("Salary Management", createSalaryPanel());
        tabbedPane.addTab("Reports", createReportsPanel());
        
        add(tabbedPane);
    }
    
    private JPanel createEmployeePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Employee Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Form fields
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        empNameField = new JTextField(20);
        formPanel.add(empNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Position:"), gbc);
        gbc.gridx = 1;
        empPositionField = new JTextField(20);
        formPanel.add(empPositionField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Base Salary:"), gbc);
        gbc.gridx = 1;
        empSalaryField = new JTextField(20);
        formPanel.add(empSalaryField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        empEmailField = new JTextField(20);
        formPanel.add(empEmailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Phone:"), gbc);
        gbc.gridx = 1;
        empPhoneField = new JTextField(20);
        formPanel.add(empPhoneField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Employee");
        JButton updateBtn = new JButton("Update Employee");
        JButton deleteBtn = new JButton("Delete Employee");
        JButton clearBtn = new JButton("Clear");
        
        addBtn.addActionListener(e -> addEmployee());
        updateBtn.addActionListener(e -> updateEmployee());
        deleteBtn.addActionListener(e -> deleteEmployee());
        clearBtn.addActionListener(e -> clearEmployeeForm());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);
        
        // Table
        String[] columns = {"ID", "Name", "Position", "Base Salary", "Email", "Phone", "Join Date"};
        employeeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        employeeTable = new JTable(employeeTableModel);
        employeeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedEmployee();
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createAttendancePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Attendance Record"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Employee:"), gbc);
        gbc.gridx = 1;
        attEmpComboBox = new JComboBox<>();
        formPanel.add(attEmpComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Present:"), gbc);
        gbc.gridx = 1;
        attPresentCheckBox = new JCheckBox();
        formPanel.add(attPresentCheckBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Hours Worked:"), gbc);
        gbc.gridx = 1;
        attHoursField = new JTextField(20);
        formPanel.add(attHoursField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Mark Attendance");
        JButton clearBtn = new JButton("Clear");
        
        addBtn.addActionListener(e -> addAttendance());
        clearBtn.addActionListener(e -> clearAttendanceForm());
        
        buttonPanel.add(addBtn);
        buttonPanel.add(clearBtn);
        
        // Table
        String[] columns = {"Employee ID", "Employee Name", "Date", "Present", "Hours Worked"};
        attendanceTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        attendanceTable = new JTable(attendanceTableModel);
        JScrollPane scrollPane = new JScrollPane(attendanceTable);
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createSalaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Salary Calculation"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Employee:"), gbc);
        gbc.gridx = 1;
        salEmpComboBox = new JComboBox<>();
        formPanel.add(salEmpComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Month:"), gbc);
        gbc.gridx = 1;
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        salMonthComboBox = new JComboBox<>(months);
        formPanel.add(salMonthComboBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Year:"), gbc);
        gbc.gridx = 1;
        salYearField = new JTextField(20);
        salYearField.setText(String.valueOf(LocalDate.now().getYear()));
        formPanel.add(salYearField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Allowances:"), gbc);
        gbc.gridx = 1;
        salAllowancesField = new JTextField(20);
        formPanel.add(salAllowancesField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Deductions:"), gbc);
        gbc.gridx = 1;
        salDeductionsField = new JTextField(20);
        formPanel.add(salDeductionsField, gbc);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton calculateBtn = new JButton("Calculate Salary");
        JButton clearBtn = new JButton("Clear");
        
        calculateBtn.addActionListener(e -> calculateSalary());
        clearBtn.addActionListener(e -> clearSalaryForm());
        
        buttonPanel.add(calculateBtn);
        buttonPanel.add(clearBtn);
        
        // Table
        String[] columns = {"Employee ID", "Employee Name", "Month", "Year", "Basic Salary", "Allowances", "Deductions", "Net Salary", "Days Worked"};
        salaryTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        salaryTable = new JTable(salaryTableModel);
        JScrollPane scrollPane = new JScrollPane(salaryTable);
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        JButton generateBtn = new JButton("Generate Monthly Report");
        generateBtn.addActionListener(e -> generateReport(reportArea));
        
        panel.add(generateBtn, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    // Employee Management Methods
    private void addEmployee() {
        try {
            String name = empNameField.getText().trim();
            String position = empPositionField.getText().trim();
            double salary = Double.parseDouble(empSalaryField.getText().trim());
            String email = empEmailField.getText().trim();
            String phone = empPhoneField.getText().trim();
            
            if (name.isEmpty() || position.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all required fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int id = dataManager.getNextEmployeeId();
            Employee employee = new Employee(id, name, position, salary, email, phone);
            dataManager.addEmployee(employee);
            
            loadEmployeeTable();
            updateEmployeeComboBoxes();
            clearEmployeeForm();
            
            JOptionPane.showMessageDialog(this, "Employee added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid salary amount", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateEmployee() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int id = (Integer) employeeTableModel.getValueAt(selectedRow, 0);
            String name = empNameField.getText().trim();
            String position = empPositionField.getText().trim();
            double salary = Double.parseDouble(empSalaryField.getText().trim());
            String email = empEmailField.getText().trim();
            String phone = empPhoneField.getText().trim();
            
            Employee employee = dataManager.getEmployeeById(id);
            if (employee != null) {
                employee.setName(name);
                employee.setPosition(position);
                employee.setBaseSalary(salary);
                employee.setEmail(email);
                employee.setPhone(phone);
                
                dataManager.updateEmployee(employee);
                loadEmployeeTable();
                updateEmployeeComboBoxes();
                clearEmployeeForm();
                
                JOptionPane.showMessageDialog(this, "Employee updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid salary amount", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void deleteEmployee() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an employee to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this employee?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            int id = (Integer) employeeTableModel.getValueAt(selectedRow, 0);
            dataManager.deleteEmployee(id);
            loadEmployeeTable();
            updateEmployeeComboBoxes();
            clearEmployeeForm();
            
            JOptionPane.showMessageDialog(this, "Employee deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void clearEmployeeForm() {
        empNameField.setText("");
        empPositionField.setText("");
        empSalaryField.setText("");
        empEmailField.setText("");
        empPhoneField.setText("");
    }
    
    private void loadSelectedEmployee() {
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow != -1) {
            empNameField.setText((String) employeeTableModel.getValueAt(selectedRow, 1));
            empPositionField.setText((String) employeeTableModel.getValueAt(selectedRow, 2));
            empSalaryField.setText(employeeTableModel.getValueAt(selectedRow, 3).toString());
            empEmailField.setText((String) employeeTableModel.getValueAt(selectedRow, 4));
            empPhoneField.setText((String) employeeTableModel.getValueAt(selectedRow, 5));
        }
    }
    
    // Attendance Management Methods
    private void addAttendance() {
        String selectedEmp = (String) attEmpComboBox.getSelectedItem();
        if (selectedEmp == null) {
            JOptionPane.showMessageDialog(this, "Please select an employee", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int empId = Integer.parseInt(selectedEmp.split(" - ")[0]);
            boolean present = attPresentCheckBox.isSelected();
            double hours = present ? Double.parseDouble(attHoursField.getText().trim()) : 0;
            
            if (present && hours <= 0) {
                JOptionPane.showMessageDialog(this, "Please enter valid hours worked", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Attendance attendance = new Attendance(empId, LocalDate.now(), present, hours);
            dataManager.addAttendance(attendance);
            
            loadAttendanceTable();
            clearAttendanceForm();
            
            JOptionPane.showMessageDialog(this, "Attendance marked successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid hours", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void clearAttendanceForm() {
        attEmpComboBox.setSelectedIndex(-1);
        attPresentCheckBox.setSelected(false);
        attHoursField.setText("");
    }
    
    // Salary Management Methods
    private void calculateSalary() {
        String selectedEmp = (String) salEmpComboBox.getSelectedItem();
        if (selectedEmp == null) {
            JOptionPane.showMessageDialog(this, "Please select an employee", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            int empId = Integer.parseInt(selectedEmp.split(" - ")[0]);
            String month = (String) salMonthComboBox.getSelectedItem();
            int year = Integer.parseInt(salYearField.getText().trim());
            double allowances = Double.parseDouble(salAllowancesField.getText().trim());
            double deductions = Double.parseDouble(salDeductionsField.getText().trim());
            
            Employee employee = dataManager.getEmployeeById(empId);
            if (employee == null) {
                JOptionPane.showMessageDialog(this, "Employee not found", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Calculate working days for the month
            int daysWorked = calculateWorkingDays(empId, month, year);
            
            Salary salary = new Salary(empId, month, year, employee.getBaseSalary(), allowances, deductions, daysWorked);
            dataManager.addSalary(salary);
            
            loadSalaryTable();
            clearSalaryForm();
            
            JOptionPane.showMessageDialog(this, "Salary calculated successfully!\nNet Salary: $" + String.format("%.2f", salary.getNetSalary()), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numeric values", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int calculateWorkingDays(int empId, String month, int year) {
        List<Attendance> attendanceList = dataManager.getAttendanceByEmployee(empId);
        return (int) attendanceList.stream()
                .filter(att -> att.getDate().getYear() == year && 
                              att.getDate().getMonth().toString().toLowerCase().startsWith(month.toLowerCase().substring(0, 3)))
                .filter(Attendance::isPresent)
                .count();
    }
    
    private void clearSalaryForm() {
        salEmpComboBox.setSelectedIndex(-1);
        salMonthComboBox.setSelectedIndex(0);
        salYearField.setText(String.valueOf(LocalDate.now().getYear()));
        salAllowancesField.setText("");
        salDeductionsField.setText("");
    }
    
    // Data Loading Methods
    private void loadAllData() {
        loadEmployeeTable();
        loadAttendanceTable();
        loadSalaryTable();
        updateEmployeeComboBoxes();
    }
    
    private void loadEmployeeTable() {
        employeeTableModel.setRowCount(0);
        List<Employee> employees = dataManager.getAllEmployees();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (Employee emp : employees) {
            Object[] row = {
                emp.getId(),
                emp.getName(),
                emp.getPosition(),
                String.format("$%.2f", emp.getBaseSalary()),
                emp.getEmail(),
                emp.getPhone(),
                emp.getJoinDate().format(formatter)
            };
            employeeTableModel.addRow(row);
        }
    }
    
    private void loadAttendanceTable() {
        attendanceTableModel.setRowCount(0);
        List<Attendance> attendanceList = dataManager.getAllAttendance();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (Attendance att : attendanceList) {
            Employee emp = dataManager.getEmployeeById(att.getEmployeeId());
            String empName = emp != null ? emp.getName() : "Unknown";
            
            Object[] row = {
                att.getEmployeeId(),
                empName,
                att.getDate().format(formatter),
                att.isPresent() ? "Yes" : "No",
                att.getHoursWorked()
            };
            attendanceTableModel.addRow(row);
        }
    }
    
    private void loadSalaryTable() {
        salaryTableModel.setRowCount(0);
        List<Salary> salaryList = dataManager.getAllSalaries();
        
        for (Salary sal : salaryList) {
            Employee emp = dataManager.getEmployeeById(sal.getEmployeeId());
            String empName = emp != null ? emp.getName() : "Unknown";
            
            Object[] row = {
                sal.getEmployeeId(),
                empName,
                sal.getMonth(),
                sal.getYear(),
                String.format("$%.2f", sal.getBasicSalary()),
                String.format("$%.2f", sal.getAllowances()),
                String.format("$%.2f", sal.getDeductions()),
                String.format("$%.2f", sal.getNetSalary()),
                sal.getDaysWorked()
            };
            salaryTableModel.addRow(row);
        }
    }
    
    private void updateEmployeeComboBoxes() {
        attEmpComboBox.removeAllItems();
        salEmpComboBox.removeAllItems();
        
        List<Employee> employees = dataManager.getAllEmployees();
        for (Employee emp : employees) {
            String item = emp.getId() + " - " + emp.getName();
            attEmpComboBox.addItem(item);
            salEmpComboBox.addItem(item);
        }
    }
    
    private void generateReport(JTextArea reportArea) {
        StringBuilder report = new StringBuilder();
        report.append("PAYROLL MANAGEMENT SYSTEM - MONTHLY REPORT\n");
        report.append("==========================================\n\n");
        
        LocalDate now = LocalDate.now();
        String currentMonth = now.getMonth().toString();
        int currentYear = now.getYear();
        
        report.append("Report Generated: ").append(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        report.append("Current Month: ").append(currentMonth).append(" ").append(currentYear).append("\n\n");
        
        // Employee Summary
        List<Employee> employees = dataManager.getAllEmployees();
        report.append("EMPLOYEE SUMMARY\n");
        report.append("----------------\n");
        report.append("Total Employees: ").append(employees.size()).append("\n\n");
        
        if (!employees.isEmpty()) {
            report.append(String.format("%-5s %-20s %-15s %-12s\n", "ID", "Name", "Position", "Base Salary"));
            report.append("-".repeat(60)).append("\n");
            
            for (Employee emp : employees) {
                report.append(String.format("%-5d %-20s %-15s $%-11.2f\n",
                    emp.getId(),
                    emp.getName().length() > 20 ? emp.getName().substring(0, 17) + "..." : emp.getName(),
                    emp.getPosition().length() > 15 ? emp.getPosition().substring(0, 12) + "..." : emp.getPosition(),
                    emp.getBaseSalary()));
            }
        }
        
        report.append("\n");
        
        // Attendance Summary for Current Month
        List<Attendance> currentMonthAttendance = dataManager.getAllAttendance().stream()
            .filter(att -> att.getDate().getMonth().toString().equals(currentMonth) && 
                          att.getDate().getYear() == currentYear)
            .toList();
        
        report.append("ATTENDANCE SUMMARY - ").append(currentMonth).append(" ").append(currentYear).append("\n");
        report.append("-".repeat(40)).append("\n");
        
        Map<Integer, Long> attendanceCount = currentMonthAttendance.stream()
            .filter(Attendance::isPresent)
            .collect(java.util.stream.Collectors.groupingBy(
                Attendance::getEmployeeId, 
                java.util.stream.Collectors.counting()));
        
        if (attendanceCount.isEmpty()) {
            report.append("No attendance records found for current month.\n");
        } else {
            report.append(String.format("%-5s %-20s %-12s\n", "ID", "Employee Name", "Days Present"));
            report.append("-".repeat(40)).append("\n");
            
            for (Map.Entry<Integer, Long> entry : attendanceCount.entrySet()) {
                Employee emp = dataManager.getEmployeeById(entry.getKey());
                String empName = emp != null ? emp.getName() : "Unknown";
                report.append(String.format("%-5d %-20s %-12d\n",
                    entry.getKey(),
                    empName.length() > 20 ? empName.substring(0, 17) + "..." : empName,
                    entry.getValue()));
            }
        }
        
        report.append("\n");
        
        // Salary Summary for Current Month
        List<Salary> currentMonthSalaries = dataManager.getAllSalaries().stream()
            .filter(sal -> sal.getMonth().toUpperCase().startsWith(currentMonth.substring(0, 3)) && 
                          sal.getYear() == currentYear)
            .toList();
        
        report.append("SALARY SUMMARY - ").append(currentMonth).append(" ").append(currentYear).append("\n");
        report.append("-".repeat(50)).append("\n");
        
        if (currentMonthSalaries.isEmpty()) {
            report.append("No salary records found for current month.\n");
        } else {
            double totalSalaries = 0;
            report.append(String.format("%-5s %-20s %-12s %-10s\n", "ID", "Employee Name", "Net Salary", "Days Worked"));
            report.append("-".repeat(50)).append("\n");
            
            for (Salary sal : currentMonthSalaries) {
                Employee emp = dataManager.getEmployeeById(sal.getEmployeeId());
                String empName = emp != null ? emp.getName() : "Unknown";
                totalSalaries += sal.getNetSalary();
                
                report.append(String.format("%-5d %-20s $%-11.2f %-10d\n",
                    sal.getEmployeeId(),
                    empName.length() > 20 ? empName.substring(0, 17) + "..." : empName,
                    sal.getNetSalary(),
                    sal.getDaysWorked()));
            }
            
            report.append("-".repeat(50)).append("\n");
            report.append(String.format("Total Salaries Paid: $%.2f\n", totalSalaries));
        }
        
        // Statistics
        report.append("\n");
        report.append("SYSTEM STATISTICS\n");
        report.append("-----------------\n");
        report.append("Total Employees: ").append(employees.size()).append("\n");
        report.append("Total Attendance Records: ").append(dataManager.getAllAttendance().size()).append("\n");
        report.append("Total Salary Records: ").append(dataManager.getAllSalaries().size()).append("\n");
        
        if (!employees.isEmpty()) {
            double avgSalary = employees.stream().mapToDouble(Employee::getBaseSalary).average().orElse(0);
            report.append("Average Base Salary: $").append(String.format("%.2f", avgSalary)).append("\n");
        }
        
        reportArea.setText(report.toString());
    }
    
    public static void main(String[] args) {
        // Set system properties for better look and feel
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        SwingUtilities.invokeLater(() -> {
            try {
                // Print system information
                System.out.println("Payroll Management System");
                System.out.println("Java Version: " + System.getProperty("java.version"));
                System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
                System.out.println("OS: " + System.getProperty("os.name"));
                System.out.println("Architecture: " + System.getProperty("os.arch"));
                System.out.println("OpenJDK Runtime Environment detected");
                
                PayrollManagementSystem app = new PayrollManagementSystem();
                app.setVisible(true);
                
                // Show welcome message
                JOptionPane.showMessageDialog(app, 
                    "Welcome to Payroll Management System!\n\n" +
                    "Features:\n" +
                    "• Employee Management\n" +
                    "• Attendance Tracking\n" +
                    "• Salary Calculation\n" +
                    "• Monthly Reports\n\n" +
                    "Built with OpenJDK 21 and Swing", 
                    "Welcome", 
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Error starting application: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
