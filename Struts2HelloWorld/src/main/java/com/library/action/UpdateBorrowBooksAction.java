package com.library.action;

import com.library.model.UpdateBorrowBook;
import com.library.service.UpdateBorrowBookService;
import com.opensymphony.xwork2.ModelDriven;

public class UpdateBorrowBooksAction implements ModelDriven<UpdateBorrowBook> {
	
	UpdateBorrowBook updateBorrowBook = new UpdateBorrowBook();
	UpdateBorrowBookService updateBorrowBookService = new UpdateBorrowBookService();
	
	@Override 
	public UpdateBorrowBook getModel()
	{
		return updateBorrowBook;
	}
	
    public String create() {
    	
    	if(updateBorrowBook != null)
    	{
    		return updateBorrowBookService.updateBorrowBooks(updateBorrowBook);
    	}
    	else {
    		return "error";
    	}
    	
    }

   
}
