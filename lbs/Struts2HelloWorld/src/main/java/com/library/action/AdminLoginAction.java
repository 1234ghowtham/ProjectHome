package com.library.action;
import com.library.model.AdminLogin;
import com.library.service.AdminLoginService;
import com.opensymphony.xwork2.ModelDriven;

public class AdminLoginAction implements ModelDriven<AdminLogin> {

	AdminLogin admin = new AdminLogin();
	AdminLoginService adminService = new AdminLoginService();
	
	@Override
	public AdminLogin getModel() {
		return admin;
	}

	public String create() {
		
		if(admin !=null)
		{
			return adminService.authenticateAdmin(admin);
		}
		else {
			return "error";
		}
		
	}
}

