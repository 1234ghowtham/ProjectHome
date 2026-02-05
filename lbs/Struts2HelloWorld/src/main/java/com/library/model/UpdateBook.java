package com.library.model;

public class UpdateBook {
	private int bookId;
	private String bookTitle;
	private int bookTotalCopies;
	public int bookAvailableCopies;

	public int getBookId() {return bookId;}
	public void setBookId(int bookId) {this.bookId = bookId;}

	public String getBookTitle() {return bookTitle;}
	public void setBookTitle(String bookTitle) {this.bookTitle = bookTitle;}
	
	public int getBookTotalCopies() {return bookTotalCopies;}
	public void setBookTotalCopies(int bookTotalCopies) {this.bookTotalCopies = bookTotalCopies;}
}
