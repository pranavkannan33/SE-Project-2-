package com.sismics.books.rest.resource;

import static com.sismics.books.rest.constant.ConstantResource.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.introproventures.graphql.jpa.query.schema.model.book.Genre;
import com.sismics.books.core.dao.jpa.BookDao;
import com.sismics.books.core.dao.jpa.TagDao;
import com.sismics.books.core.dao.jpa.UserBookDao;
import com.sismics.books.core.dao.jpa.UserDao;
import com.sismics.books.core.dao.jpa.criteria.UserBookCriteria;
import com.sismics.books.core.dao.jpa.dto.TagDto;
import com.sismics.books.core.dao.jpa.dto.UserBookDto;
import com.sismics.books.core.event.BookImportedEvent;
import com.sismics.books.core.model.context.AppContext;
import com.sismics.books.core.model.jpa.Book;
import com.sismics.books.core.model.jpa.Rating;
import com.sismics.books.core.model.jpa.Tag;
import com.sismics.books.core.model.jpa.User;
import com.sismics.books.core.model.jpa.UserBook;
import com.sismics.books.core.util.DirectoryUtil;
import com.sismics.books.core.util.jpa.PaginatedList;
import com.sismics.books.core.util.jpa.PaginatedLists;
import com.sismics.books.core.util.jpa.SortCriteria;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;

/**
 * Book REST resources.
 * 
 * @author bgamard
 */
@Path("/book")
public class BookResource extends BaseResource {
    /**
     * Creates a new book.
     * 
     * @param isbn ISBN Number
     * @return Response
     * @throws JSONException
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(
            @FormParam(ISBN) String isbn) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate input data
        ValidationUtil.validateRequired(isbn, ISBN);

        // Fetch the book
        BookDao bookDao = new BookDao();
        Book book = bookDao.getByIsbn(isbn);
        if (book == null) {
            // Try to get the book from a public API
            try {
                book = AppContext.getInstance().getBookDataService().searchBook(isbn);
            } catch (Exception e) {
                throw new ClientException(BOOK_NOT_FOUND, e.getCause().getMessage(), e);
            }

            // Save the new book in database
            bookDao.create(book);
        }

        // Create the user book if needed
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getByBook(book.getId(), principal.getId());
        if (userBook == null) {
            userBook = new UserBook();
            userBook.setUserId(principal.getId());
            userBook.setBookId(book.getId());
            userBook.setCreateDate(new Date());
            userBookDao.create(userBook);
        } else {
            throw new ClientException(BOOK_ALREADY_ADDED, BOOK_ALREADY_ADDED_MSG);
        }

        JSONObject response = new JSONObject();
        response.put(ID, userBook.getId());
        return Response.ok().entity(response).build();
    }

    /**
     * Deletes a book.
     * 
     * @param userBookId User book ID
     * @return Response
     * @throws JSONException
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(
            @PathParam(ID) String userBookId) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId, principal.getId());
        if (userBook == null) {
            throw new ClientException(BOOK_NOT_FOUND, BOOK_NOT_FOUND_ID + userBookId);
        }

        // Delete the user book
        userBookDao.delete(userBook.getId());

        // Always return ok
        JSONObject response = new JSONObject();
        response.put(STATUS, OK);
        return Response.ok().entity(response).build();
    }

    /**
     * Add a book book manually.
     * 
     * @param title       Title
     * @param description Description
     * @return Response
     * @throws JSONException
     */
    @PUT
    @Path("manual")
    @Produces(MediaType.APPLICATION_JSON)
    public Response add(
            @FormParam(TITLE) String title,
            @FormParam(SUBTITLE) String subtitle,
            @FormParam(AUTHOR) String author,
            @FormParam(DESCRIPTION) String description,
            @FormParam(ISBN10) String isbn10,
            @FormParam(ISBN13) String isbn13,
            @FormParam(PAGE_COUNT) Long pageCount,
            @FormParam(LANGUAGE) String language,
            @FormParam(PUBLISH_DATE) String publishDateStr,
            @FormParam(THUMBNAIL_IMAGE_URL) String thumbnailImageUrl,
            // @FormParam(GENRES) Set<Genre> genres,
            @FormParam(GENRES) Set<Genre> genres,
            @FormParam(RATINGS) Set<Rating> ratings,

            @FormParam(TAGS) List<String> tagList) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate input data
        title = ValidationUtil.validateLength(title, TITLE, 1, 255, false);
        subtitle = ValidationUtil.validateLength(subtitle, SUBTITLE, 1, 255, true);
        author = ValidationUtil.validateLength(author, AUTHOR, 1, 255, false);
        description = ValidationUtil.validateLength(description, DESCRIPTION, 1, 4000, true);
        isbn10 = ValidationUtil.validateLength(isbn10, ISBN10, 10, 10, true);
        isbn13 = ValidationUtil.validateLength(isbn13, ISBN13, 13, 13, true);
        language = ValidationUtil.validateLength(language, LANGUAGE, 2, 2, true);
        Date publishDate = ValidationUtil.validateDate(publishDateStr, PUBLISH_DATE, false);
        thumbnailImageUrl = ValidationUtil.validateLength(thumbnailImageUrl, THUMBNAIL_IMAGE_URL, 1, 255, true);
        Set<String> genres = new HashSet<>();
        Set<Rating> ratings = new HashSet<>();
        // ratings = ValidationUtil.validateLength(ratings, RATINGS, 1, 255, true);

        if (Strings.isNullOrEmpty(isbn10) && Strings.isNullOrEmpty(isbn13)) {
            throw new ClientException("ValidationError", "At least one ISBN number is mandatory");
        }

        // Check if this book is not already in database
        BookDao bookDao = new BookDao();
        Book bookIsbn10 = bookDao.getByIsbn(isbn10);
        Book bookIsbn13 = bookDao.getByIsbn(isbn13);
        if (bookIsbn10 != null || bookIsbn13 != null) {
            throw new ClientException(BOOK_ALREADY_ADDED, BOOK_ALREADY_ADDED_MSG);
        }

        // Create the book

        String bookId = UUID.randomUUID().toString();   
// Example call to the updated constructor
Book book = new Book(bookId, title, subtitle, author, description, isbn10, isbn13, pageCount, language, publishDate, thumbnailImageUrl, genres, ratings);
                
        bookDao.create(book);

        // Create the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = new UserBook();
        userBook.setUserId(principal.getId());
        userBook.setBookId(book.getId());
        userBook.setCreateDate(new Date());
        userBookDao.create(userBook);

        // Update tags

        updateTags(tagList, userBook);

        // Returns the book ID
        JSONObject response = new JSONObject();
        response.put(ID, userBook.getId());
        return Response.ok().entity(response).build();
    }

    /**
     * Updates the book.
     * 
     * @param title       Title
     * @param description Description
     * @return Response
     * @throws JSONException
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @PathParam(ID) String userBookId,
            @FormParam(TITLE) String title,
            @FormParam(SUBTITLE) String subtitle,
            @FormParam(AUTHOR) String author,
            @FormParam(DESCRIPTION) String description,
            @FormParam(ISBN10) String isbn10,
            @FormParam(ISBN13) String isbn13,
            @FormParam(PAGE_COUNT) Long pageCount,
            @FormParam(LANGUAGE) String language,
            @FormParam(PUBLISH_DATE) String publishDateStr,
            @FormParam(TAGS) List<String> tagList) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate input data

        title = ValidationUtil.validateLength(title, TITLE, 1, 255, true);
        subtitle = ValidationUtil.validateLength(subtitle, SUBTITLE, 1, 255, true);
        author = ValidationUtil.validateLength(author, AUTHOR, 1, 255, true);
        description = ValidationUtil.validateLength(description, DESCRIPTION, 1, 4000, true);
        isbn10 = ValidationUtil.validateLength(isbn10, ISBN10, 10, 10, true);
        isbn13 = ValidationUtil.validateLength(isbn13, ISBN13, 13, 13, true);
        language = ValidationUtil.validateLength(language, LANGUAGE, 2, 2, true);
        Date publishDate = ValidationUtil.validateDate(publishDateStr, PUBLISH_DATE, true);

        // Get the user book
        UserBookDao userBookDao = new UserBookDao();
        BookDao bookDao = new BookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId, principal.getId());
        if (userBook == null) {
            throw new ClientException(BOOK_NOT_FOUND, BOOK_NOT_FOUND_ID + userBookId);
        }

        // Get the book
        Book book = bookDao.getById(userBook.getBookId());

        // Check that new ISBN number are not already in database
        if (!Strings.isNullOrEmpty(isbn10) && book.getIsbn10() != null && !book.getIsbn10().equals(isbn10)) {
            Book bookIsbn10 = bookDao.getByIsbn(isbn10);
            if (bookIsbn10 != null) {
                throw new ClientException(BOOK_ALREADY_ADDED, BOOK_ALREADY_ADDED_MSG);
            }
        }

        if (!Strings.isNullOrEmpty(isbn13) && book.getIsbn13() != null && !book.getIsbn13().equals(isbn13)) {
            Book bookIsbn13 = bookDao.getByIsbn(isbn13);
            if (bookIsbn13 != null) {
                throw new ClientException(BOOK_ALREADY_ADDED, BOOK_ALREADY_ADDED_MSG);
            }
        }

        // Update the book

        book.updateBook(title, subtitle, author, description, isbn10, isbn13, pageCount, language, publishDate);

        // Update tags

        updateTags(tagList, userBook);

        // Returns the book ID
        JSONObject response = new JSONObject();
        response.put(ID, userBookId);
        return Response.ok().entity(response).build();
    }

    /**
     * Get a book.
     * 
     * @param id User book ID
     * @return Response
     * @throws JSONException
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @PathParam(ID) String userBookId) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Fetch the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId, principal.getId());
        if (userBook == null) {
            throw new ClientException(BOOK_NOT_FOUND, BOOK_NOT_FOUND_ID + userBookId);
        }

        // Fetch the book
        BookDao bookDao = new BookDao();
        Book bookDb = bookDao.getById(userBook.getBookId());

        // Return book data
        JSONObject book = new JSONObject();
        book.put(ID, userBook.getId());
        book.put(TITLE, bookDb.getTitle());
        book.put(SUBTITLE, bookDb.getSubtitle());
        book.put(AUTHOR, bookDb.getAuthor());
        book.put(PAGE_COUNT, bookDb.getPageCount());
        book.put(DESCRIPTION, bookDb.getDescription());
        book.put(ISBN10, bookDb.getIsbn10());
        book.put(ISBN13, bookDb.getIsbn13());
        book.put(LANGUAGE, bookDb.getLanguage());
        if (bookDb.getPublishDate() != null) {
            book.put(PUBLISH_DATE, bookDb.getPublishDate().getTime());
        }
        book.put(CREATE_DATE, userBook.getCreateDate().getTime());
        if (userBook.getReadDate() != null) {
            book.put(READ_DATE, userBook.getReadDate().getTime());
        }

        // Add tags
        TagDao tagDao = new TagDao();
        List<TagDto> tagDtoList = tagDao.getByUserBookId(userBookId);
        List<JSONObject> tags = new ArrayList<>();
        for (TagDto tagDto : tagDtoList) {
            JSONObject tag = new JSONObject();
            tag.put(ID, tagDto.getId());
            tag.put(NAME, tagDto.getName());
            tag.put(COLOR, tagDto.getColor());
            tags.add(tag);
        }
        book.put(TAGS, tags);

        return Response.ok().entity(book).build();
    }

    /**
     * Returns a book cover.
     * 
     * @param id User book ID
     * @return Response
     * @throws JSONException
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}/cover")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response cover(
            @PathParam(ID) final String userBookId) throws JSONException {
        // Get the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId);

        // Get the cover image
        File file = Paths.get(DirectoryUtil.getBookDirectory().getPath(), userBook.getBookId()).toFile();
        InputStream inputStream = null;
        try {
            if (file.exists()) {
                inputStream = new FileInputStream(file);
            } else {
                inputStream = new FileInputStream(new File(getClass().getResource("/dummy.png").getFile()));
            }
        } catch (FileNotFoundException e) {
            throw new ServerException("FileNotFound", "Cover file not found", e);
        }

        return Response.ok(inputStream)
                .header("Content-Type", "image/jpeg")
                .header("Expires",
                        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(new Date().getTime() + 3600000))
                .build();
    }

    /**
     * Updates a book cover.
     * 
     * @param id User book ID
     * @return Response
     * @throws JSONException
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/cover")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCover(
            @PathParam(ID) String userBookId,
            @FormParam("url") String imageUrl) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId, principal.getId());
        if (userBook == null) {
            throw new ClientException(BOOK_NOT_FOUND, BOOK_NOT_FOUND_ID + userBookId);
        }

        // Get the book
        BookDao bookDao = new BookDao();
        Book book = bookDao.getById(userBook.getBookId());

        // Download the new cover
        try {
            AppContext.getInstance().getBookDataService().downloadThumbnail(book, imageUrl);
        } catch (Exception e) {
            throw new ClientException("DownloadCoverError", "Error downloading the cover image");
        }

        // Always return ok
        JSONObject response = new JSONObject();
        response.put(STATUS, OK);
        return Response.ok(response).build();
    }

    /**
     * Returns all books.
     * 
     * @param limit  Page limit
     * @param offset Page offset
     * @return Response
     * @throws JSONException
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @QueryParam("sort_column") Integer sortColumn,
            @QueryParam("asc") Boolean asc,
            @QueryParam("search") String search,
            @QueryParam("read") Boolean read,
            @QueryParam("tag") String tagName) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        JSONObject response = new JSONObject();
        List<JSONObject> books = new ArrayList<>();

        UserBookDao userBookDao = new UserBookDao();
        TagDao tagDao = new TagDao();
        PaginatedList<UserBookDto> paginatedList = PaginatedLists.create(limit, offset);
        SortCriteria sortCriteria = new SortCriteria(sortColumn, asc);
        UserBookCriteria criteria = new UserBookCriteria();
        criteria.setSearch(search);
        criteria.setRead(read);
        criteria.setUserId(principal.getId());
        if (!Strings.isNullOrEmpty(tagName)) {
            Tag tag = tagDao.getByName(principal.getId(), tagName);
            if (tag != null) {
                criteria.setTagIdList(Lists.newArrayList(tag.getId()));
            }
        }
        try {
            userBookDao.findByCriteria(paginatedList, criteria, sortCriteria);
        } catch (Exception e) {
            throw new ServerException("SearchError", "Error searching in books", e);
        }

        for (UserBookDto userBookDto : paginatedList.getResultList()) {
            JSONObject book = new JSONObject();
            book.put(ID, userBookDto.getId());
            book.put(TITLE, userBookDto.getTitle());
            book.put(SUBTITLE, userBookDto.getSubtitle());
            book.put(AUTHOR, userBookDto.getAuthor());
            book.put(LANGUAGE, userBookDto.getLanguage());
            book.put(PUBLISH_DATE, userBookDto.getPublishTimestamp());
            book.put("create_date", userBookDto.getCreateTimestamp());
            book.put("read_date", userBookDto.getReadTimestamp());
            book.put("rating", userBookDto.getRating());
            book.put("thumbnail", userBookDto.getThumbnail());

            // Get tags
            List<TagDto> tagDtoList = tagDao.getByUserBookId(userBookDto.getId());
            List<JSONObject> tags = new ArrayList<>();
            for (TagDto tagDto : tagDtoList) {
                JSONObject tag = new JSONObject();
                tag.put(ID, tagDto.getId());
                tag.put("name", tagDto.getName());
                tag.put("color", tagDto.getColor());
                tags.add(tag);
            }
            book.put("tags", tags);

            books.add(book);
        }
        response.put("total", paginatedList.getResultCount());
        response.put("books", books);

        return Response.ok().entity(response).build();
    }

    /**
     * Imports books.
     * 
     * @param fileBodyPart File to import
     * @return Response
     * @throws JSONException
     */
    @PUT
    @Consumes("multipart/form-data")
    @Path("import")
    public Response importFile(
            @FormDataParam("file") FormDataBodyPart fileBodyPart) throws JSONException {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate input data
        ValidationUtil.validateRequired(fileBodyPart, "file");

        UserDao userDao = new UserDao();
        User user = userDao.getById(principal.getId());

        FileOutputStream fileOutputStream = null;
        try(InputStream in = fileBodyPart.getValueAs(InputStream.class)) {
            // Copy the incoming stream content into a temporary file
            File importFile = File.createTempFile("books_import", null);
            fileOutputStream = new FileOutputStream(importFile);
            IOUtils.copy(in, fileOutputStream);

            BookImportedEvent event = new BookImportedEvent();
            event.setUser(user);
            event.setImportFile(importFile);
            AppContext.getInstance().getImportEventBus().post(event);

            // Always return ok
            JSONObject response = new JSONObject();
            response.put(STATUS, OK);
            return Response.ok().entity(response).build();
        } catch (Exception e) {
            throw new ServerException("ImportError", "Error importing books", e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    throw new ServerException("ImportError", "Error closing file stream", e);
                }
            }
        }
    }

    /**
     * Set a book as read/unread.
     * 
     * @param id   User book ID
     * @param read Read state
     * @return Response
     * @throws JSONException
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(
            @PathParam(ID) final String userBookId,
            @FormParam("read") boolean read) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the user book
        UserBookDao userBookDao = new UserBookDao();
        UserBook userBook = userBookDao.getUserBook(userBookId, principal.getId());

        // Update the read date
        userBook.setReadDate(read ? new Date() : null);

        // Always return ok
        JSONObject response = new JSONObject();
        response.put(STATUS, "ok");
        return Response.ok().entity(response).build();
    }

    private void updateTags(List<String> tagList, UserBook userBook) throws JSONException{
        if (tagList != null) {
            TagDao tagDao = new TagDao();
            Set<String> tagSet = new HashSet<>();
            Set<String> tagIdSet = new HashSet<>();
            List<Tag> tagDbList = tagDao.getByUserId(principal.getId());
            for (Tag tagDb : tagDbList) {
                tagIdSet.add(tagDb.getId());
            }
            for (String tagId : tagList) {
                if (!tagIdSet.contains(tagId)) {
                    throw new ClientException("TagNotFound", MessageFormat.format("Tag not found: {0}", tagId));
                }
                tagSet.add(tagId);
            }
            tagDao.updateTagList(userBook.getId(), tagSet);
        }
    }
    
}
