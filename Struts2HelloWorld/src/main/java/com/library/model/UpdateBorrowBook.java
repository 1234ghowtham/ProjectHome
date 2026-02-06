package com.library.model;

public class UpdateBorrowBook {
	private int studentId;
	private int bookId;
	private int borrowBookCopies;

	public int getStudentId() { return studentId; }
	public void setStudentId(int studentId) { this.studentId = studentId; }
	    
	public int getBookId() { return bookId; }
	public void setBookId(int bookId) { this.bookId = bookId; }
	    
	public long getBorrowBookCopies() { return borrowBookCopies; }
	public void setBorrowBookCopies(int borrowBookCopies) { this.borrowBookCopies = borrowBookCopies; }
}
