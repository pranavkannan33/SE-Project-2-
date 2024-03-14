package com.sismics.books.core.dao.jpa;

import com.sismics.books.core.model.jpa.Rating;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

public class RatingDao {
    
    @PersistenceContext
    private EntityManager em;

    /**
     * Creates a new rating.
     *
     * @param rating The rating to create
     */
    public void create(Rating rating) {
        em.persist(rating);
    }

    /**
     * Retrieves all ratings for a specific book.
     *
     * @param bookId The ID of the book
     * @return A list of ratings for the book
     */
    public List<Rating> findByBookId(String bookId) {
        TypedQuery<Rating> query = em.createQuery("SELECT r FROM Rating r WHERE r.book.id = :bookId", Rating.class);
        query.setParameter("bookId", bookId);
        return query.getResultList();
    }

    /**
     * Calculates the average rating of a specific book.
     *
     * @param bookId The ID of the book
     * @return The average rating of the book
     */
    public Double calculateAverageRating(String bookId) {
        TypedQuery<Double> query = em.createQuery(
                "SELECT AVG(r.value) FROM Rating r WHERE r.book.id = :bookId", Double.class);
        query.setParameter("bookId", bookId);
        return query.getSingleResult();
    }
}
