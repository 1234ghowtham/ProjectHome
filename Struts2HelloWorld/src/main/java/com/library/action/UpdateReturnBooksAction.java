package com.library.action;

import com.library.model.UpdateReturnBook;
import com.library.service.UpdateReturnBookService;
import com.opensymphony.xwork2.ModelDriven;

public class UpdateReturnBooksAction implements ModelDriven<UpdateReturnBook> {
	
	UpdateReturnBook updateReturnBook = new UpdateReturnBook();
	UpdateReturnBookService updateReturnBookService =new UpdateReturnBookService();
	
	@Override
	public UpdateReturnBook getModel() {
		return updateReturnBook;
	}
	
    public String create() {
    	
    	if(updateReturnBook != null)
    	{
    		return updateReturnBookService.updateReturnBook(updateReturnBook);
    	}
    	else {
    		return "error";
		}
    	
    }

   
}
