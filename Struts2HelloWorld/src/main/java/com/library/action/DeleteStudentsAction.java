package com.library.action;

import com.library.model.DeleteStudent;
import com.library.service.DeleteStudentsService;
import com.opensymphony.xwork2.ModelDriven;

public class DeleteStudentsAction implements ModelDriven<DeleteStudent> {
	
	DeleteStudent deleteStudent = new DeleteStudent();
	DeleteStudentsService deleteStudentService = new DeleteStudentsService();
	
	@Override
	public DeleteStudent getModel()
	{
		return deleteStudent;
	}
	
	public String create() {
		
		if(deleteStudent != null)
		{
			return deleteStudentService.deleteStudents(deleteStudent);
		}
		else {
			return "error";
		}
		
	}
}
