package com.library.model;

public class UpdateReturnBook {
    private int bookId;
    private int returnBookCopies;
    private int studentId;
	
    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public int getReturnBookCopies() { return returnBookCopies; }
    public void setReturnBookCopies(int returnBookCopies) { this.returnBookCopies = returnBookCopies; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
}
