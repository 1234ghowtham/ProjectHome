package com.library.action;

import com.library.model.UpdateStudent;
import com.library.service.UpdateStudentsService;
import com.opensymphony.xwork2.ModelDriven;

public class UpdateStudentsAction implements ModelDriven<UpdateStudent> {
	
	UpdateStudent updateStudent = new UpdateStudent();
	UpdateStudentsService updateStudentService = new UpdateStudentsService();

   @Override
   public UpdateStudent getModel()
   {
	   return updateStudent;
   }
   
   public String create() {
	   
	   if(updateStudent != null)
	   {
		   return updateStudentService.updateStudent(updateStudent);
	   }
	   else {
		   return "error";
	   }
    	
    }
}


