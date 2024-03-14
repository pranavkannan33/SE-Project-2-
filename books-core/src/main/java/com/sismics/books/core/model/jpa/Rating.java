package com.sismics.books.core.model.jpa;

import javax.persistence.*;

@Entity
@Table(name = "T_RATING")
public class Rating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BOK_ID_C")
    private Book book;

    // Assuming a User entity exists; replace with actual User entity reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USR_ID_C")
    private User user;

    @Column(name = "RTG_VALUE_N")
    private Integer value;

    public Rating() {
    }

    // Constructor, getters and setters
    public Rating(Book book, User user, Integer value) {
        this.book = book;
        this.user = user;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
