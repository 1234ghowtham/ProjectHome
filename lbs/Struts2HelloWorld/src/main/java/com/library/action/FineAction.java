package com.library.action;

import com.library.model.Fine;
import com.library.service.FineService;
import com.opensymphony.xwork2.ModelDriven;

public class FineAction implements ModelDriven<Fine> {

	Fine fine = new Fine();
	FineService fineservice = new FineService();

	@Override
	public Fine getModel() {
		return fine;
	}
	
	public String create() {
		
		if(fine != null)
		{
			return fineservice.fetchingFineAmount(fine);
		}
		else {
			return "error";
		}
		
	}
}
