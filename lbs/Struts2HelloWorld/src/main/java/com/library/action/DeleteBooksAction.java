package com.library.action;

import com.library.model.DeleteBook;
import com.library.service.DeleteBooksService;
import com.opensymphony.xwork2.ModelDriven;

public class DeleteBooksAction implements ModelDriven<DeleteBook> {
	
	DeleteBook deleteBook = new DeleteBook();
	DeleteBooksService deleteBookService = new DeleteBooksService();

	@Override
	public DeleteBook getModel() {
		return deleteBook;
	}

	public String create() {
		
		if(deleteBook != null)
		{
			return deleteBookService.deleteBook(deleteBook);
		}
		else {
			return "error";
		}
		
	}
}
