package library.domain.model.loan;

import library.domain.model.book.Book;

/**
 * 貸出図書
 */
public class BookOnLoan {
    Book book;

    public BookOnLoan(Book book) {
        this.book = book;
    }
}
