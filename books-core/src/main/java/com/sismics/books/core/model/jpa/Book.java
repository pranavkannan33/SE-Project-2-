package com.sismics.books.core.model.jpa;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.google.common.base.Objects;

/**
 * Book entity.
 * 
 * @author bgamard
 */
@Entity
@Table(name = "T_BOOK")
public class Book {
    @Id
    @Column(name = "BOK_ID_C", length = 36)
    private String id;

    @Column(name = "BOK_TITLE_C", nullable = false, length = 255)
    private String title;

    @Column(name = "BOK_SUBTITLE_C", length = 255)
    private String subtitle;

    @Column(name = "BOK_AUTHOR_C", nullable = false, length = 255)
    private String author;

    @Column(name = "BOK_DESCRIPTION_C", length = 4000)
    private String description;

    @Column(name = "BOK_ISBN10_C", length = 10)
    private String isbn10;

    @Column(name = "BOK_ISBN13_C", length = 13)
    private String isbn13;

    @Column(name = "BOK_PAGECOUNT_N")
    private Long pageCount;

    @Column(name = "BOK_LANGUAGE_C", length = 2)
    private String language;

    @Column(name = "BOK_PUBLISHDATE_D", nullable = false)
    private Date publishDate;

    // New field for thumbnail image URL
    @Column(name = "BOK_THUMBNAIL_URL_C")
    private String thumbnailImageUrl;

    // New relationship with Genre
    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "T_BOOK_GENRE",
            joinColumns = @JoinColumn(name = "BOK_ID_C"),
            inverseJoinColumns = @JoinColumn(name = "GEN_ID_C"))
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "book", fetch = FetchType.LAZY)
    private Set<Rating> ratings = new HashSet<>();

    // Assume ratings are calculated elsewhere, hence no direct field in this entity
    // Constructors, getters, and setters for new fields

    
    /**
     * Constructor without parameters
     * 
     */
    public Book() {
    }

    /**
     * Constructor with parameters
     * 
     * @param id
     * @param title
     * @param subtitle
     * @param author
     * @param description
     * @param isbn10
     * @param isbn13
     * @param pageCount
     * @param language
     * @param publishDate
     * @param thumbnailImageUrl
     * @param genres
     * @param ratings
     */
    public Book(String id, String title, String subtitle, String author, String description, String isbn10, String isbn13, Long pageCount, String language, Date publishDate) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.author = author;
        this.description = description;
        this.isbn10 = isbn10;
        this.isbn13 = isbn13;
        this.pageCount = pageCount;
        this.language = language;
        this.publishDate = publishDate;
        this.thumbnailImageUrl = thumbnailImageUrl;
        this.genres = genres;
        this.ratings = ratings;

    }

    /**
     * Getter of id.
     * 
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Setter of id.
     * 
     * @param id id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter of title.
     * 
     * @return title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Setter of title.
     * 
     * @param title title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Getter of subtitle.
     * 
     * @return subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Setter of subtitle.
     * 
     * @param subtitle subtitle
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Getter of author.
     * 
     * @return author
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Setter of author.
     * 
     * @param author author
     */
    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * Getter of description.
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Setter of description.
     * 
     * @param description description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Getter of isbn10.
     * 
     * @return isbn10
     */
    public String getIsbn10() {
        return isbn10;
    }

    /**
     * Setter of isbn10.
     * 
     * @param isbn10 isbn10
     */
    public void setIsbn10(String isbn10) {
        this.isbn10 = isbn10;
    }

    /**
     * Getter of isbn13.
     * 
     * @return isbn13
     */
    public String getIsbn13() {
        return isbn13;
    }

    /**
     * Setter of isbn13.
     * 
     * @param isbn13 isbn13
     */
    public void setIsbn13(String isbn13) {
        this.isbn13 = isbn13;
    }

    /**
     * Getter of pageCount.
     * 
     * @return pageCount
     */
    public Long getPageCount() {
        return pageCount;
    }

    /**
     * Setter of pageCount.
     * 
     * @param pageCount pageCount
     */
    public void setPageCount(Long pageCount) {
        this.pageCount = pageCount;
    }

    /**
     * Getter of language.
     * 
     * @return language
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Setter of language.
     * 
     * @param language language
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * Getter of publishDate.
     * 
     * @return publishDate
     */
    public Date getPublishDate() {
        return publishDate;
    }

    /**
     * Setter of publishDate.
     * 
     * @param publishedDate publishDate
     */
    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    /**
     * Getter of thumbnailImageUrl.
     * 
     * @return thumbnailImageUrl
     */
    public String getthumbnailImageUrl() {
        return thumbnailImageUrl;
    }

    /**
     * Setter of thumbnailImageUrl.
     * 
     * @param thumbnailImageUrl thumbnailImageUrl
     */
    public void setthumbnailImageUrl(String thumbnailImageUrl) {
        this.thumbnailImageUrl = thumbnailImageUrl;
    }

    /**
     * Getter of genres.
     * 
     * @return genres
     */
    public Set<genres> getgenres() {
        return genres;
    }

    /**
     * Setter of genres.
     * 
     * @param genres genres
     */
    public void setgenres(Set<genres> genres) {
        this.genres = genres;
    }

        /**
     * Getter of ratings.
     * 
     * @return ratings
     */
    public Set<ratings> getratings() {
        return ratings;
    }

    /**
     * Setter of ratings.
     * 
     * @param ratings ratings
     */
    public void setratings(Set<ratings> ratings) {
        this.ratings = ratings;
    }


    /**
     * Update the book with the given parameters.
     * 
     * @param title
     * @param subtitle
     * @param author
     * @param description
     * @param isbn10
     * @param isbn13
     * @param pageCount
     * @param language
     * @param publishDate
     * @param thumbnailImageUrl
     * @param genres
     * @param ratings
     * 
     */
    public void updateBook(String title, String subtitle, String author, String description, String isbn10, String isbn13, Long pageCount, String language, Date publishDate) {
        if (title != null) {
            this.setTitle(title);
        }

        if (subtitle != null) {
            this.setSubtitle(subtitle);
        }

        if (author != null) {
            this.setAuthor(author);
        }

        if (description != null) {
            this.setDescription(description);
        }

        if (isbn10 != null) {
            this.setIsbn10(isbn10);
        }

        if (isbn13 != null) {
            this.setIsbn13(isbn13);
        }

        if (pageCount != null) {
            this.setPageCount(pageCount);
        }

        if (language != null) {
            this.setLanguage(language);
        }

        if (publishDate != null) {
            this.setPublishDate(publishDate);
        }

        if (thumbnailImageUrl != null) {
            this.setthumbnailImageUrl(thumbnailImageUrl);
        }

        if (genres != null) {
            this.setgenres(genres);
        }

        if (ratings != null) {
            this.setratings(ratings);
        }
    }

    @Transient
    public Double getAverageRating() {
        return ratings.isEmpty() ? null :
            ratings.stream().mapToInt(Rating::getValue).average().orElse(Double.NaN);
    }

    @Transient
    public int getNumberOfRatings() {
        return ratings.size();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .add("title", title)
                .toString();
    }
}
