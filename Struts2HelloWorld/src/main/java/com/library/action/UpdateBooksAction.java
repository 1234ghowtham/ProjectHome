package com.library.action;

import com.library.model.UpdateBook;
import com.library.service.UpdateBooksService;
import com.opensymphony.xwork2.ModelDriven;

public class UpdateBooksAction implements ModelDriven<UpdateBook> {
	
	UpdateBook updateBook = new UpdateBook();
	UpdateBooksService updateBookService = new UpdateBooksService();
	
	
	@Override
	public UpdateBook getModel()
	{
		return updateBook;
	}
	
	public String create() {
		
		if(updateBook != null)
		{
			return updateBookService.updateBooks(updateBook);
		}
		else {
			return "error";
		}
		
	}
}
