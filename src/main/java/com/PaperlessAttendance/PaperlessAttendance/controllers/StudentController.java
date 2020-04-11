package com.PaperlessAttendance.PaperlessAttendance.controllers;

import com.PaperlessAttendance.PaperlessAttendance.entities.Student;
import com.PaperlessAttendance.PaperlessAttendance.entities.Attendance;
import com.PaperlessAttendance.PaperlessAttendance.entities.DateAttend;
import com.PaperlessAttendance.PaperlessAttendance.repositories.StudentRepository;
import com.PaperlessAttendance.PaperlessAttendance.repositories.AttendanceRepository;
import com.PaperlessAttendance.PaperlessAttendance.repositories.DateAttendRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.IOException;
import java.io.File;
import java.util.Iterator;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

import com.fasterxml.jackson.databind.MappingIterator;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import com.opencsv.CSVWriter;
import com.opencsv.CSVReader;
import java.io.PrintWriter;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.springframework.web.multipart.MultipartFile;



@RestController
@CrossOrigin(origins = "http://localhost:4200")
public class StudentController {

    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final DateAttendRepository dateAttendRepository;

    public StudentController(StudentRepository studentRepository, AttendanceRepository attendanceRepository, DateAttendRepository dateAttendRepository) {
        this.studentRepository = studentRepository;
        this.dateAttendRepository = dateAttendRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/users")
    public List<Student> getStudents() {
        return (List<Student>) studentRepository.findAll();
    }

    @GetMapping("/exportstudents")
    public void exportCSV(HttpServletResponse response) throws Exception {
        String filename = "students.csv";
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        PrintWriter out = response.getWriter();
        try { 
            CSVWriter writer = new CSVWriter(out, ';', 
                                             CSVWriter.NO_QUOTE_CHARACTER, 
                                             CSVWriter.DEFAULT_ESCAPE_CHARACTER, 
                                             CSVWriter.DEFAULT_LINE_END); 
    
            // adding header to csv 
            String[] header = { "panther ID", "dates present", "dates not present"}; 
            writer.writeNext(header);

            Iterable<Student> studentIterable = studentRepository.findAll();
            List<String[]> outputData = new ArrayList<String[]>();
            Iterator<Student> i = studentIterable.iterator(); 
            while (i.hasNext())  { // go through students
                Student s = i.next();
                List<Long> pantherID = Arrays.asList(s.getPantherID());
                List<String> datesPresent = new ArrayList<>();
                List<String> datesNotPresent = new ArrayList<>();

                Iterable<Attendance> attendanceForID = attendanceRepository.findAll();
                Iterator<Attendance> h = attendanceForID.iterator();
                int attendanceSize = 0;
                // go through attendance record 
                while (h.hasNext()) {
                    attendanceSize++;
                    Attendance a = h.next();
                    // if the panther id of attendance record matches student panther id, then let's get the date for it
                    if (a.getPantherID() == s.getPantherID()) {
                        List<Long> dateID = Arrays.asList(a.getDateID());
                        Iterable<DateAttend> dateAttendForAttendance = dateAttendRepository.findAllById(dateID);
                        Iterator<DateAttend> b = dateAttendForAttendance.iterator();
                        // find the date for the attendance record
                        while (b.hasNext()) {
                            DateAttend d = b.next();
                            System.out.println(d.getAttended().getClass());
                            // get the date, if they were present or not
                            if (d.getAttended().equals("yes")) {
                                datesPresent.add(d.getDate());
                            } else {
                                datesNotPresent.add(d.getDate());
                            }
                            System.out.println(d.getDate());
                        } // exit date loop
                    } // exit condutional attendance
                } // exit attendance

                // now print out info for student
                System.out.println("attendance size for pantherID " + String.valueOf(pantherID) + " is " + String.valueOf(attendanceSize));
                String datePresentString = String.join(", ", datesPresent);
                System.out.println(datePresentString);
                
                String dateNotPresentString = String.join(", ", datesNotPresent);
                System.out.println(dateNotPresentString);
                
                String pantherIDString = pantherID.stream()
                    .map(n -> String.valueOf(n))
                    .collect(Collectors.joining());
                String[] data = {pantherIDString, datePresentString, dateNotPresentString};
                System.out.println(Arrays.toString(data));
                outputData.add(data);
            }
            writer.writeAll(outputData);
    
            // closing writer connection 
            writer.close();
        } 
        catch (IOException e) { 
            // TODO Auto-generated catch block 
            e.printStackTrace(); 
        }
    }

    @RequestMapping(value="/uploadstudents", method = RequestMethod.POST) 
    public void uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file != null) {
                System.out.println(file.getOriginalFilename());
            }
            String content = new String(file.getBytes());
            System.out.println(content);
            CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
            CsvMapper mapper = new CsvMapper();
            MappingIterator<Student> readValues = mapper.readerFor(Student.class).with(bootstrapSchema).readValues(content);
            List<Student> allValues= readValues.readAll();

            for (int i = 0; i < allValues.size(); i++) {
                long id = allValues.get(i).getPantherID();
                if (studentRepository.existsById(id)) {
                    System.out.println(allValues.get(i) + " already exists in DB");
                    continue;
                }
                System.out.println(allValues.get(i) + " has been uploaded");
                studentRepository.save(allValues.get(i));
            }
        } catch (Exception e) {
            System.out.println("AN ERROR HAS OCCURRED - ");
            System.out.println(e);
        }
    }
}