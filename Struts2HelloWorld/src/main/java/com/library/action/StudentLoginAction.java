package com.library.action;

import com.library.model.StudentLogin;
import com.library.service.StudentLoginService;
import com.opensymphony.xwork2.ModelDriven;

public class StudentLoginAction implements ModelDriven<StudentLogin> {
	
	StudentLogin studentLogin = new StudentLogin();
	StudentLoginService studentLoginService = new StudentLoginService();
	
	@Override
	public StudentLogin getModel()
	{
		return studentLogin;
	}

    public String create() {
    	
    	if(studentLogin != null)
    	{
    		return studentLoginService.authenticateStudent(studentLogin);
    	}
    	else {
    		return "error";
    	}
    	
    }

   
}